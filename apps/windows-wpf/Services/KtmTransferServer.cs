using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace KnowToMigrate.Services
{
    public class KtmTransferServer : IDisposable
    {
        private readonly int _port;
        private readonly string _downloadDirectory;
        private TcpListener? _listener;
        private CancellationTokenSource? _cts;
        private bool _isDisposed;

        public event Func<KtmHandshake, Task<bool>>? OnHandshakeReceived;
        public event Action<TransferProgressInfo>? OnProgress;
        public event Action<string, bool, string>? OnTransferCompleted; // sessionId, success, message

        public KtmTransferServer(string downloadDirectory, int port = KtmConstants.TransferPort)
        {
            _downloadDirectory = downloadDirectory;
            _port = port;
            if (!Directory.Exists(_downloadDirectory))
                Directory.CreateDirectory(_downloadDirectory);
        }

        public void Start()
        {
            if (_cts != null) return;
            _cts = new CancellationTokenSource();
            var token = _cts.Token;

            Task.Run(() => ListenLoopAsync(token), token);
        }

        public void Stop()
        {
            _cts?.Cancel();
            try { _listener?.Stop(); } catch { }
            _listener = null;
            _cts = null;
        }

        private async Task ListenLoopAsync(CancellationToken token)
        {
            try
            {
                _listener = new TcpListener(IPAddress.Any, _port);
                _listener.Server.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
                _listener.Start();

                while (!token.IsCancellationRequested)
                {
                    var client = await _listener.AcceptTcpClientAsync(token);
                    _ = Task.Run(() => HandleIncomingClientAsync(client, token), token);
                }
            }
            catch (OperationCanceledException) { }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"[KtmServer] Listen error: {ex.Message}");
            }
        }

        private async Task HandleIncomingClientAsync(TcpClient client, CancellationToken token)
        {
            using (client)
            using (var stream = client.GetStream())
            {
                client.ReceiveBufferSize = 256 * 1024;
                client.SendBufferSize = 64 * 1024;
                client.NoDelay = true;

                string sessionId = string.Empty;
                var progress = new TransferProgressInfo();

                try
                {
                    // 1. Read HANDSHAKE
                    string handshakeJson = await ReadLengthPrefixedJsonAsync(stream, token);
                    var handshake = JsonSerializer.Deserialize<KtmHandshake>(handshakeJson);
                    if (handshake == null || handshake.Type != "HANDSHAKE")
                    {
                        await SendLengthPrefixedJsonAsync(stream, new KtmHandshakeAck { Accepted = false, Reason = "Invalid handshake" }, token);
                        return;
                    }

                    // Prompt or check acceptance
                    bool accepted = true;
                    if (OnHandshakeReceived != null)
                        accepted = await OnHandshakeReceived.Invoke(handshake);

                    if (!accepted)
                    {
                        await SendLengthPrefixedJsonAsync(stream, new KtmHandshakeAck { Accepted = false, Reason = "Transfer rejected by recipient" }, token);
                        return;
                    }

                    await SendLengthPrefixedJsonAsync(stream, new KtmHandshakeAck
                    {
                        Accepted = true,
                        Pin = handshake.Pin,
                        SelectedTransport = handshake.SelectedTransport
                    }, token);

                    // 2. Read MANIFEST
                    string manifestJson = await ReadLengthPrefixedJsonAsync(stream, token);
                    var manifest = JsonSerializer.Deserialize<KtmManifest>(manifestJson);
                    if (manifest == null || manifest.Type != "MANIFEST")
                    {
                        await SendLengthPrefixedJsonAsync(stream, new KtmManifestAck { Accepted = false, Reason = "Invalid manifest" }, token);
                        return;
                    }

                    sessionId = manifest.SessionId;
                    progress.SessionId = sessionId;
                    progress.PeerName = handshake.DeviceName;
                    progress.TransportType = KtmTransportCodes.GetDisplayName(handshake.SelectedTransport);
                    progress.TotalFiles = manifest.Files.Count;
                    progress.TotalBytes = manifest.TotalBytes;
                    progress.Direction = TransferDirection.Receiving;
                    progress.Status = TransferStatus.Connecting;
                    progress.StartTimeMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

                    // Check storage capacity preflight
                    var driveInfo = new DriveInfo(Path.GetPathRoot(Path.GetFullPath(_downloadDirectory)) ?? "C:\\");
                    if (driveInfo.AvailableFreeSpace < manifest.TotalBytes)
                    {
                        string reason = $"Not Enough Storage (Required: {KtmFormatting.FormatBytes(manifest.TotalBytes)}, Available: {KtmFormatting.FormatBytes(driveInfo.AvailableFreeSpace)})";
                        await SendLengthPrefixedJsonAsync(stream, new KtmManifestAck
                        {
                            Accepted = false,
                            Reason = reason
                        }, token);
                        progress.Status = TransferStatus.Failed;
                        progress.ErrorMessage = reason;
                        OnProgress?.Invoke(progress);
                        OnTransferCompleted?.Invoke(sessionId, false, reason);
                        return;
                    }

                    // Check for existing partial files for RESUME
                    var existingOffsets = new Dictionary<int, long>();
                    long totalAlreadyReceived = 0;

                    foreach (var fileItem in manifest.Files)
                    {
                        string safeRelPath = KtmSecurityUtils.SanitizeRelativePath(fileItem.RelativePath);
                        string fullTargetPath = Path.Combine(_downloadDirectory, safeRelPath);
                        string partPath = fullTargetPath + ".part";

                        if (File.Exists(partPath))
                        {
                            long existingLen = new FileInfo(partPath).Length;
                            if (existingLen < fileItem.Size)
                            {
                                existingOffsets[fileItem.FileIndex] = existingLen;
                                totalAlreadyReceived += existingLen;
                            }
                            else
                            {
                                // Stale or corrupted part larger than file -> reset
                                File.Delete(partPath);
                                existingOffsets[fileItem.FileIndex] = 0;
                            }
                        }
                        else
                        {
                            existingOffsets[fileItem.FileIndex] = 0;
                        }
                    }

                    progress.BytesTransferred = totalAlreadyReceived;
                    progress.Status = TransferStatus.Transferring;
                    OnProgress?.Invoke(progress);

                    // Send MANIFEST_ACK with resume offsets
                    await SendLengthPrefixedJsonAsync(stream, new KtmManifestAck
                    {
                        Accepted = true,
                        SessionId = sessionId,
                        ExistingOffsets = existingOffsets
                    }, token);

                    // 3. Receive Chunks
                    var speedTimer = System.Diagnostics.Stopwatch.StartNew();
                    long bytesSinceLastTimer = 0;

                    foreach (var fileItem in manifest.Files)
                    {
                        if (token.IsCancellationRequested) break;

                        string safeRelPath = KtmSecurityUtils.SanitizeRelativePath(fileItem.RelativePath);
                        string fullTargetPath = Path.Combine(_downloadDirectory, safeRelPath);
                        string? parentDir = Path.GetDirectoryName(fullTargetPath);
                        if (!string.IsNullOrEmpty(parentDir) && !Directory.Exists(parentDir))
                            Directory.CreateDirectory(parentDir);

                        string partPath = fullTargetPath + ".part";
                        long currentOffset = existingOffsets.TryGetValue(fileItem.FileIndex, out long resumeOff) ? resumeOff : 0;

                        progress.CurrentFileName = safeRelPath;
                        progress.CurrentFileIndex = fileItem.FileIndex + 1;
                        progress.Status = TransferStatus.Transferring;
                        OnProgress?.Invoke(progress);

                        using (var fileStream = new FileStream(partPath, FileMode.OpenOrCreate, FileAccess.Write, FileShare.ReadWrite, 131072, useAsync: true))
                        {
                            fileStream.Seek(currentOffset, SeekOrigin.Begin);

                            while (currentOffset < fileItem.Size)
                            {
                                if (token.IsCancellationRequested) break;

                                // Read frame header (20 bytes Big-Endian)
                                byte[] frameHeader = new byte[20];
                                await ReadExactBytesAsync(stream, frameHeader, 0, 20, token);

                                uint magic = System.Buffers.Binary.BinaryPrimitives.ReadUInt32BigEndian(frameHeader.AsSpan(0, 4));
                                if (magic != KtmConstants.ChunkMagic)
                                    throw new InvalidDataException($"Invalid frame magic received in stream: 0x{magic:X8}");

                                int chunkFileIdx = System.Buffers.Binary.BinaryPrimitives.ReadInt32BigEndian(frameHeader.AsSpan(4, 4));
                                long chunkOffset = System.Buffers.Binary.BinaryPrimitives.ReadInt64BigEndian(frameHeader.AsSpan(8, 8));
                                int payloadLen = System.Buffers.Binary.BinaryPrimitives.ReadInt32BigEndian(frameHeader.AsSpan(16, 4));

                                if (payloadLen <= 0 || payloadLen > KtmConstants.MaxChunkSize)
                                    throw new InvalidDataException($"Invalid payload length: {payloadLen}");

                                byte[] payload = new byte[payloadLen];
                                await ReadExactBytesAsync(stream, payload, 0, payloadLen, token);

                                // Write to file
                                fileStream.Seek(chunkOffset, SeekOrigin.Begin);
                                await fileStream.WriteAsync(payload, 0, payloadLen, token);

                                currentOffset += payloadLen;
                                progress.BytesTransferred += payloadLen;
                                bytesSinceLastTimer += payloadLen;
                                progress.ElapsedTimeMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - progress.StartTimeMs;

                                if (speedTimer.ElapsedMilliseconds >= 300)
                                {
                                    double secs = speedTimer.ElapsedMilliseconds / 1000.0;
                                    progress.SpeedMBps = (bytesSinceLastTimer / (1024.0 * 1024.0)) / secs;
                                    speedTimer.Restart();
                                    bytesSinceLastTimer = 0;
                                    OnProgress?.Invoke(progress);
                                }
                            }
                            await fileStream.FlushAsync(token);
                            fileStream.Flush(true);
                        }

                        // 4. Verify file SHA-256
                        progress.Status = TransferStatus.Verifying;
                        OnProgress?.Invoke(progress);

                        string computedSha = KtmSecurityUtils.ComputeFileSha256(partPath);
                        if (!string.IsNullOrEmpty(fileItem.Sha256) && !string.Equals(computedSha, fileItem.Sha256, StringComparison.OrdinalIgnoreCase))
                        {
                            // Hash mismatch
                            try { File.Delete(partPath); } catch { }
                            await SendLengthPrefixedJsonAsync(stream, new KtmFileComplete
                            {
                                FileIndex = fileItem.FileIndex,
                                Status = "HASH_MISMATCH",
                                Sha256 = computedSha
                            }, token);
                            throw new CryptographicException($"Checksum verification failed for {safeRelPath}");
                        }

                        // Atomically move .part to final destination with fallback copy & retried cleanup
                        if (File.Exists(fullTargetPath))
                            File.Delete(fullTargetPath);

                        try
                        {
                            File.Move(partPath, fullTargetPath);
                        }
                        catch
                        {
                            File.Copy(partPath, fullTargetPath, overwrite: true);
                        }

                        if (File.Exists(partPath))
                        {
                            for (int attempts = 0; attempts < 5; attempts++)
                            {
                                try
                                {
                                    GC.Collect();
                                    GC.WaitForPendingFinalizers();
                                    File.Delete(partPath);
                                    break;
                                }
                                catch
                                {
                                    await Task.Delay(50, token);
                                }
                            }
                        }

                        // Post-move validation
                        var finalInfo = new FileInfo(fullTargetPath);
                        if (!finalInfo.Exists || finalInfo.Length != fileItem.Size)
                        {
                            throw new IOException($"Target file verification failed on disk: {fullTargetPath}");
                        }

                        await SendLengthPrefixedJsonAsync(stream, new KtmFileComplete
                        {
                            FileIndex = fileItem.FileIndex,
                            Status = "OK",
                            Sha256 = computedSha
                        }, token);
                    }

                    // 5. Transfer Complete
                    await SendLengthPrefixedJsonAsync(stream, new KtmTransferComplete
                    {
                        SessionId = sessionId,
                        Success = true,
                        TotalBytesTransferred = progress.BytesTransferred
                    }, token);

                    progress.Status = TransferStatus.Completed;
                    progress.IsCompleted = true;
                    progress.ElapsedTimeMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - progress.StartTimeMs;
                    OnProgress?.Invoke(progress);
                    OnTransferCompleted?.Invoke(sessionId, true, "Transfer completed and verified successfully");
                }
                catch (Exception ex)
                {
                    progress.Status = TransferStatus.Failed;
                    progress.ErrorMessage = ex.Message;
                    OnProgress?.Invoke(progress);
                    OnTransferCompleted?.Invoke(sessionId, false, ex.Message);
                }
            }
        }

        private static async Task<string> ReadLengthPrefixedJsonAsync(NetworkStream stream, CancellationToken token)
        {
            byte[] lenBytes = new byte[4];
            await ReadExactBytesAsync(stream, lenBytes, 0, 4, token);
            int len = (lenBytes[0] << 24) | (lenBytes[1] << 16) | (lenBytes[2] << 8) | lenBytes[3];

            if (len <= 0 || len > 10 * 1024 * 1024)
                throw new InvalidDataException($"Invalid packet length: {len}");

            byte[] jsonBytes = new byte[len];
            await ReadExactBytesAsync(stream, jsonBytes, 0, len, token);
            return Encoding.UTF8.GetString(jsonBytes);
        }

        private static async Task SendLengthPrefixedJsonAsync<T>(NetworkStream stream, T obj, CancellationToken token)
        {
            string json = JsonSerializer.Serialize(obj);
            byte[] jsonBytes = Encoding.UTF8.GetBytes(json);
            int len = jsonBytes.Length;

            byte[] header = new byte[4];
            header[0] = (byte)((len >> 24) & 0xFF);
            header[1] = (byte)((len >> 16) & 0xFF);
            header[2] = (byte)((len >> 8) & 0xFF);
            header[3] = (byte)(len & 0xFF);

            await stream.WriteAsync(header, 0, 4, token);
            await stream.WriteAsync(jsonBytes, 0, len, token);
            await stream.FlushAsync(token);
        }

        private static async Task ReadExactBytesAsync(Stream stream, byte[] buffer, int offset, int count, CancellationToken token)
        {
            int totalRead = 0;
            while (totalRead < count)
            {
                int read = await stream.ReadAsync(buffer, offset + totalRead, count - totalRead, token);
                if (read == 0)
                    throw new EndOfStreamException("Remote host closed connection prematurely");
                totalRead += read;
            }
        }

        public void Dispose()
        {
            if (!_isDisposed)
            {
                _isDisposed = true;
                Stop();
            }
        }
    }
}

using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace KnowToMigrate.Services
{
    public class KtmTransferClient
    {
        public event Action<TransferProgressInfo>? OnProgress;

        public async Task<bool> SendFilesAsync(
            string targetIp,
            int targetPort,
            string localDeviceId,
            string localDeviceName,
            IReadOnlyList<string> filePaths,
            string selectedTransport = KtmTransportCodes.WifiLan,
            CancellationToken token = default)
        {
            if (filePaths.Count == 0) return true;

            using var client = new TcpClient();
            client.ReceiveBufferSize = 64 * 1024;
            client.SendBufferSize = 256 * 1024;
            client.NoDelay = true;

            long startMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            var progress = new TransferProgressInfo
            {
                PeerName = targetIp,
                TotalFiles = 0,
                TotalBytes = 0,
                TransportType = KtmTransportCodes.GetDisplayName(selectedTransport),
                Direction = TransferDirection.Sending,
                Status = TransferStatus.Connecting,
                StartTimeMs = startMs
            };

            try
            {
                await client.ConnectAsync(targetIp, targetPort, token);
                using var stream = client.GetStream();

                // 1. Build Manifest
                var manifest = new KtmManifest
                {
                    SessionId = Guid.NewGuid().ToString("N"),
                    SenderDeviceId = localDeviceId,
                    SenderDeviceName = localDeviceName
                };

                int fileIdx = 0;
                long totalBytes = 0;

                foreach (var path in filePaths)
                {
                    if (File.Exists(path))
                    {
                        var fi = new FileInfo(path);
                        string sha = KtmSecurityUtils.ComputeFileSha256(path);
                        manifest.Files.Add(new KtmManifestItem
                        {
                            FileIndex = fileIdx++,
                            RelativePath = Path.GetFileName(path),
                            Size = fi.Length,
                            Sha256 = sha,
                            IsFolder = false
                        });
                        totalBytes += fi.Length;
                    }
                    else if (Directory.Exists(path))
                    {
                        var dirInfo = new DirectoryInfo(path);
                        string baseDirName = dirInfo.Name;
                        foreach (var subFile in Directory.EnumerateFiles(path, "*", SearchOption.AllDirectories))
                        {
                            var fi = new FileInfo(subFile);
                            string rel = Path.Combine(baseDirName, Path.GetRelativePath(path, subFile));
                            string sha = KtmSecurityUtils.ComputeFileSha256(subFile);
                            manifest.Files.Add(new KtmManifestItem
                            {
                                FileIndex = fileIdx++,
                                RelativePath = rel,
                                Size = fi.Length,
                                Sha256 = sha,
                                IsFolder = false
                            });
                            totalBytes += fi.Length;
                        }
                    }
                }

                manifest.TotalFiles = manifest.Files.Count;
                manifest.TotalBytes = totalBytes;
                progress.SessionId = manifest.SessionId;
                progress.TotalFiles = manifest.TotalFiles;
                progress.TotalBytes = totalBytes;
                progress.Status = TransferStatus.Connecting;
                OnProgress?.Invoke(progress);

                // 2. Handshake with 6-digit confirmation PIN
                string pin = KtmSecurityUtils.Generate6DigitPin();
                var handshake = new KtmHandshake
                {
                    DeviceId = localDeviceId,
                    DeviceName = localDeviceName,
                    Platform = "Windows",
                    Pin = pin,
                    SelectedTransport = selectedTransport
                };

                await SendLengthPrefixedJsonAsync(stream, handshake, token);
                string ackJson = await ReadLengthPrefixedJsonAsync(stream, token);
                var handshakeAck = JsonSerializer.Deserialize<KtmHandshakeAck>(ackJson);
                if (handshakeAck == null || !handshakeAck.Accepted)
                {
                    string reason = handshakeAck?.Reason ?? "Unknown";
                    progress.Status = TransferStatus.Failed;
                    progress.ErrorMessage = reason;
                    OnProgress?.Invoke(progress);
                    throw new InvalidOperationException($"Transfer rejected by recipient: {reason}");
                }

                progress.Status = TransferStatus.Transferring;
                OnProgress?.Invoke(progress);

                // 3. Send Manifest
                await SendLengthPrefixedJsonAsync(stream, manifest, token);
                string manifestAckJson = await ReadLengthPrefixedJsonAsync(stream, token);
                var manifestAck = JsonSerializer.Deserialize<KtmManifestAck>(manifestAckJson);
                if (manifestAck == null || !manifestAck.Accepted)
                {
                    string reason = manifestAck?.Reason ?? "Unknown";
                    progress.Status = TransferStatus.Failed;
                    progress.ErrorMessage = reason;
                    OnProgress?.Invoke(progress);
                    throw new InvalidOperationException($"Manifest rejected by recipient: {reason}");
                }

                // 4. Stream Chunks with Resume Support
                var existingOffsets = manifestAck.ExistingOffsets ?? new Dictionary<int, long>();
                byte[] chunkBuffer = new byte[KtmConstants.DefaultChunkSize];
                var speedTimer = System.Diagnostics.Stopwatch.StartNew();
                long bytesSinceTimer = 0;

                // Map file index back to local source path
                var indexToSourcePath = new Dictionary<int, string>();
                int mapIdx = 0;
                foreach (var path in filePaths)
                {
                    if (File.Exists(path))
                    {
                        indexToSourcePath[mapIdx++] = path;
                    }
                    else if (Directory.Exists(path))
                    {
                        foreach (var subFile in Directory.EnumerateFiles(path, "*", SearchOption.AllDirectories))
                        {
                            indexToSourcePath[mapIdx++] = subFile;
                        }
                    }
                }

                foreach (var fileItem in manifest.Files)
                {
                    if (token.IsCancellationRequested) break;

                    string localSource = indexToSourcePath[fileItem.FileIndex];
                    long resumeOffset = existingOffsets.TryGetValue(fileItem.FileIndex, out long ro) ? ro : 0;

                    progress.CurrentFileName = fileItem.RelativePath;
                    progress.CurrentFileIndex = fileItem.FileIndex + 1;
                    progress.Status = TransferStatus.Transferring;
                    OnProgress?.Invoke(progress);

                    using (var fs = new FileStream(localSource, FileMode.Open, FileAccess.Read, FileShare.Read, 131072, useAsync: true))
                    {
                        if (resumeOffset > 0 && resumeOffset < fileItem.Size)
                        {
                            fs.Seek(resumeOffset, SeekOrigin.Begin);
                            progress.BytesTransferred += resumeOffset;
                        }

                        long fileBytesSent = resumeOffset;
                        while (fileBytesSent < fileItem.Size)
                        {
                            if (token.IsCancellationRequested) break;

                            int toRead = (int)Math.Min((long)chunkBuffer.Length, fileItem.Size - fileBytesSent);
                            int read = await fs.ReadAsync(chunkBuffer, 0, toRead, token);
                            if (read <= 0) break;

                            // Build frame header (20 bytes Big-Endian)
                            byte[] frameHeader = new byte[20];
                            System.Buffers.Binary.BinaryPrimitives.WriteUInt32BigEndian(frameHeader.AsSpan(0, 4), KtmConstants.ChunkMagic);
                            System.Buffers.Binary.BinaryPrimitives.WriteInt32BigEndian(frameHeader.AsSpan(4, 4), fileItem.FileIndex);
                            System.Buffers.Binary.BinaryPrimitives.WriteInt64BigEndian(frameHeader.AsSpan(8, 8), fileBytesSent);
                            System.Buffers.Binary.BinaryPrimitives.WriteInt32BigEndian(frameHeader.AsSpan(16, 4), read);

                            await stream.WriteAsync(frameHeader, 0, 20, token);
                            await stream.WriteAsync(chunkBuffer, 0, read, token);
                            await stream.FlushAsync(token);

                            fileBytesSent += read;
                            progress.BytesTransferred += read;
                            bytesSinceTimer += read;
                            progress.ElapsedTimeMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - startMs;

                            if (speedTimer.ElapsedMilliseconds >= 300 || bytesSinceTimer >= 512 * 1024)
                            {
                                double secs = Math.Max(0.05, speedTimer.ElapsedMilliseconds / 1000.0);
                                progress.SpeedMBps = (bytesSinceTimer / (1024.0 * 1024.0)) / secs;
                                speedTimer.Restart();
                                bytesSinceTimer = 0;
                                OnProgress?.Invoke(progress);
                            }
                        }
                    }

                    // Read FILE_COMPLETE ack
                    progress.Status = TransferStatus.Verifying;
                    OnProgress?.Invoke(progress);

                    string fileCompleteJson = await ReadLengthPrefixedJsonAsync(stream, token);
                    var fileComp = JsonSerializer.Deserialize<KtmFileComplete>(fileCompleteJson);
                    if (fileComp == null || fileComp.Status != "OK")
                    {
                        progress.Status = TransferStatus.Failed;
                        progress.ErrorMessage = $"Checksum mismatch on recipient for {fileItem.RelativePath}";
                        OnProgress?.Invoke(progress);
                        throw new CryptographicException($"Checksum mismatch on recipient for {fileItem.RelativePath}");
                    }
                }

                // 5. Transfer Complete
                string transferCompleteJson = await ReadLengthPrefixedJsonAsync(stream, token);
                var transferComp = JsonSerializer.Deserialize<KtmTransferComplete>(transferCompleteJson);

                progress.IsCompleted = transferComp != null && transferComp.Success;
                progress.Status = progress.IsCompleted ? TransferStatus.Completed : TransferStatus.Failed;
                progress.ElapsedTimeMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - startMs;
                OnProgress?.Invoke(progress);
                return progress.IsCompleted;
            }
            catch (Exception ex)
            {
                progress.Status = TransferStatus.Failed;
                progress.ErrorMessage = ex.Message;
                OnProgress?.Invoke(progress);
                return false;
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
    }
}

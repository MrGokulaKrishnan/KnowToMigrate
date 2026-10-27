using System;
using System.Diagnostics;
using System.IO;
using System.IO.Pipes;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace KnowToMigrate.Services
{
    public sealed class KtmIpcMessage
    {
        public string Command { get; set; } = "ACTIVATE";
        public string[]? Args { get; set; }
    }

    public static class KtmSingleInstance
    {
        public const string MutexName = "KnowToMigrate.SingleInstance.v1";
        public const string PipeName = "KnowToMigrate.IpcPipe.v1";

        private static Mutex? _mutex;
        private static CancellationTokenSource? _pipeCts;

        public static bool TryAcquire()
        {
            try
            {
                _mutex = new Mutex(true, MutexName, out bool createdNew);
                return createdNew;
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"[SingleInstance] Mutex check error: {ex.Message}");
                return true;
            }
        }

        public static bool TryAcquire(string[] args)
        {
            try
            {
                _mutex = new Mutex(true, MutexName, out bool createdNew);
                if (createdNew)
                {
                    return true;
                }

                // Secondary instance detected. Forward arguments over named pipe and exit.
                ForwardArguments(args);
                return false;
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"[SingleInstance] Mutex check error: {ex.Message}");
                return true;
            }
        }

        public static void StartIpcListener(Action<string[]> onActivateReceived, Action onShutdownReceived, string pipeName = PipeName)
            => StartListening(onActivateReceived, onShutdownReceived, pipeName);

        public static bool SendArgsToPrimary(string[] args, string pipeName = PipeName)
            => ForwardArguments(args, pipeName);

        public static void Release()
        {
            StopListening();
        }

        public static bool ForwardArguments(string[] args, string pipeName = PipeName, int timeoutMs = 2000)
        {
            try
            {
                using var client = new NamedPipeClientStream(".", pipeName, PipeDirection.Out);
                client.Connect(timeoutMs);

                var msg = new KtmIpcMessage
                {
                    Command = "ACTIVATE",
                    Args = args
                };

                string json = JsonSerializer.Serialize(msg);
                byte[] bytes = Encoding.UTF8.GetBytes(json);

                using var writer = new StreamWriter(client, Encoding.UTF8, leaveOpen: true);
                writer.WriteLine(json);
                writer.Flush();
                return true;
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"[SingleInstance] Failed to forward arguments to primary instance: {ex.Message}");
                return false;
            }
        }

        public static bool RequestShutdown(string pipeName = PipeName, int timeoutMs = 2000)
        {
            try
            {
                using var client = new NamedPipeClientStream(".", pipeName, PipeDirection.Out);
                client.Connect(timeoutMs);

                var msg = new KtmIpcMessage
                {
                    Command = "SHUTDOWN",
                    Args = Array.Empty<string>()
                };

                string json = JsonSerializer.Serialize(msg);
                using var writer = new StreamWriter(client, Encoding.UTF8, leaveOpen: true);
                writer.WriteLine(json);
                writer.Flush();
                return true;
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"[SingleInstance] Failed to send shutdown request: {ex.Message}");
                return false;
            }
        }

        public static void StartListening(Action<string[]> onArgumentsReceived, Action onShutdownRequested, string pipeName = PipeName)
        {
            _pipeCts = new CancellationTokenSource();
            var token = _pipeCts.Token;

            Task.Run(async () =>
            {
                while (!token.IsCancellationRequested)
                {
                    try
                    {
                        using var server = new NamedPipeServerStream(
                            pipeName,
                            PipeDirection.In,
                            1,
                            PipeTransmissionMode.Byte,
                            PipeOptions.Asynchronous);

                        await server.WaitForConnectionAsync(token);

                        using var reader = new StreamReader(server, Encoding.UTF8);
                        string? line = await reader.ReadLineAsync(token);
                        if (!string.IsNullOrWhiteSpace(line))
                        {
                            try
                            {
                                var msg = JsonSerializer.Deserialize<KtmIpcMessage>(line);
                                if (msg != null)
                                {
                                    if (string.Equals(msg.Command, "SHUTDOWN", StringComparison.OrdinalIgnoreCase))
                                    {
                                        onShutdownRequested();
                                    }
                                    else
                                    {
                                        onArgumentsReceived(msg.Args ?? Array.Empty<string>());
                                    }
                                }
                            }
                            catch
                            {
                                onArgumentsReceived(Array.Empty<string>());
                            }
                        }
                    }
                    catch (OperationCanceledException)
                    {
                        break;
                    }
                    catch (Exception ex)
                    {
                        Debug.WriteLine($"[SingleInstance] Pipe listener iteration error: {ex.Message}");
                        await Task.Delay(200, token);
                    }
                }
            }, token);
        }

        public static void StopListening()
        {
            try
            {
                _pipeCts?.Cancel();
                _pipeCts = null;
            }
            catch { }

            try
            {
                if (_mutex != null)
                {
                    _mutex.ReleaseMutex();
                    _mutex.Dispose();
                    _mutex = null;
                }
            }
            catch { }
        }
    }
}

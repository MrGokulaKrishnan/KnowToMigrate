using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text.Json;
using KnowToMigrate.Interop;
using Microsoft.UI.Dispatching;

namespace KnowToMigrate.Services
{
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Data models â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public class DeviceInfo
    {
        public string DeviceId  { get; set; } = "";
        public string Name      { get; set; } = "";
        public string Platform  { get; set; } = "";
        public string Ip        { get; set; } = "";
        public ushort Port      { get; set; }
        public bool   IsOnline  { get; set; }
    }

    public class TransferRecord
    {
        public string   SessionId   { get; set; } = "";
        public string   FileName    { get; set; } = "";
        public long     TotalBytes  { get; set; }
        public string   PeerName    { get; set; } = "";
        public string   Direction   { get; set; } = "Send"; // "Send" | "Receive"
        public string   Status      { get; set; } = "Pending";
        public DateTime StartedAt   { get; set; } = DateTime.UtcNow;
        public DateTime? CompletedAt { get; set; }

        public string SizeFormatted => FormatBytes(TotalBytes);

        private static string FormatBytes(long bytes) =>
            bytes >= 1_000_000_000 ? $"{bytes / 1_000_000_000.0:F1} GB" :
            bytes >= 1_000_000     ? $"{bytes / 1_000_000.0:F1} MB"     :
            bytes >= 1_000         ? $"{bytes / 1_000.0:F1} KB"         :
            $"{bytes} B";
    }

    public class TransferProgress
    {
        public string SessionId  { get; set; } = "";
        public long   BytesSent  { get; set; }
        public long   TotalBytes { get; set; }
        public double SpeedBps   { get; set; }
        public double EtaSecs    { get; set; }

        public double Percentage    => TotalBytes > 0 ? (double)BytesSent / TotalBytes * 100.0 : 0;
        public string SpeedFormatted => FormatBytes(SpeedBps) + "/s";
        public string EtaFormatted  => TimeSpan.FromSeconds(EtaSecs).ToString(@"mm\:ss");

        private static string FormatBytes(double bytes) =>
            bytes >= 1_000_000_000 ? $"{bytes / 1_000_000_000:F1} GB" :
            bytes >= 1_000_000     ? $"{bytes / 1_000_000:F1} MB"     :
            bytes >= 1_000         ? $"{bytes / 1_000:F1} KB"         :
            $"{bytes:F0} B";
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Service â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /// <summary>
    /// Thin managed wrapper around the native KTM engine.
    /// Falls back to demo mode gracefully when ktm.dll is absent.
    /// </summary>
    public class KtmService : IDisposable
    {
        private IntPtr _handle  = IntPtr.Zero;
        private bool   _disposed;

        // Keep delegates alive so the GC doesn't collect them while native
        // code holds a pointer to them.
        private KtmInterop.DeviceDiscoveredCallback?  _discoveryDelegate;
        private KtmInterop.TransferProgressCallback?  _progressDelegate;
        private KtmInterop.TransferRequestCallback?   _requestDelegate;

        private DispatcherQueue? _dispatcher;

        // â”€â”€â”€â”€â”€â”€â”€â”€ Public state â”€â”€â”€â”€â”€â”€â”€â”€

        public ObservableCollection<DeviceInfo>     NearbyDevices   { get; } = new();
        public ObservableCollection<TransferRecord> TransferHistory { get; } = new();

        public bool IsDemoMode { get; private set; }

        // â”€â”€â”€â”€â”€â”€â”€â”€ Events â”€â”€â”€â”€â”€â”€â”€â”€

        public event Action<TransferProgress>?           OnProgress;
        public event Action<DeviceInfo, string>?         OnIncomingRequest; // (device, manifestJson)
        public event Action<string>?                     OnError;

        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        public void Initialize()
        {
            _dispatcher = DispatcherQueue.GetForCurrentThread();

            try
            {
                _handle = KtmInterop.ktm_init();
                if (_handle == IntPtr.Zero)
                    throw new InvalidOperationException("ktm_init returned null handle");

                IsDemoMode = false;
            }
            catch (DllNotFoundException)
            {
                IsDemoMode = true;
                OnError?.Invoke("KTM engine not found (ktm.dll). Running in demo mode.");
                SeedDemoData();
            }
            catch (Exception ex)
            {
                IsDemoMode = true;
                OnError?.Invoke($"KTM init failed: {ex.Message}. Running in demo mode.");
                SeedDemoData();
            }
        }

        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Discovery â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        public void StartDiscovery()
        {
            if (_handle == IntPtr.Zero) return;

            _discoveryDelegate = (deviceJson) =>
            {
                try
                {
                    var device = JsonSerializer.Deserialize<DeviceInfo>(deviceJson,
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    if (device == null) return;

                    _dispatcher?.TryEnqueue(() =>
                    {
                        var existing = NearbyDevices.FirstOrDefault(d => d.DeviceId == device.DeviceId);
                        if (existing == null)
                        {
                            NearbyDevices.Add(device);
                        }
                        else
                        {
                            existing.IsOnline = true;
                            existing.Ip       = device.Ip;
                        }
                    });
                }
                catch { /* swallow parse errors */ }
            };

            KtmInterop.ktm_start_discovery(_handle, _discoveryDelegate);
        }

        public void StopDiscovery()
        {
            if (_handle == IntPtr.Zero) return;
            KtmInterop.ktm_stop_discovery(_handle);
        }

        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Send â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        /// <summary>
        /// Initiates a file send. Returns the session ID string (from native)
        /// or empty string in demo mode.
        /// </summary>
        public string SendFile(string targetIp, ushort targetPort, string filePath)
        {
            if (_handle == IntPtr.Zero) return string.Empty;

            _progressDelegate = BuildProgressDelegate();

            var sessionHandle = KtmInterop.ktm_send_file(
                _handle, targetIp, targetPort, filePath, _progressDelegate);

            // The native API returns a session handle; we return its address as a string key.
            return sessionHandle.ToString();
        }

        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Receive â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        public void StartReceiving(string receiveDir)
        {
            if (_handle == IntPtr.Zero) return;

            _requestDelegate  = BuildRequestDelegate();
            _progressDelegate = BuildProgressDelegate();

            KtmInterop.ktm_receive_start(_handle, receiveDir, _requestDelegate, _progressDelegate);
        }

        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Cancel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        public void CancelTransfer(string sessionId)
        {
            if (_handle == IntPtr.Zero) return;
            KtmInterop.ktm_cancel(_handle, sessionId);
        }

        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Info â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        public string GetDeviceId()
        {
            if (_handle == IntPtr.Zero) return "demo-device-id";
            return KtmInterop.ktm_get_device_id(_handle) ?? "unknown";
        }

        public string GetVersion()
        {
            try { return KtmInterop.ktm_get_version() ?? "unknown"; }
            catch { return "demo-1.0.0"; }
        }

        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Private helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        private KtmInterop.TransferProgressCallback BuildProgressDelegate() =>
            (sessionId, bytesSent, totalBytes, speedBps, etaSecs) =>
            {
                var progress = new TransferProgress
                {
                    SessionId  = sessionId,
                    BytesSent  = bytesSent,
                    TotalBytes = totalBytes,
                    SpeedBps   = speedBps,
                    EtaSecs    = etaSecs,
                };
                _dispatcher?.TryEnqueue(() => OnProgress?.Invoke(progress));
            };

        private KtmInterop.TransferRequestCallback BuildRequestDelegate() =>
            (deviceJson, manifestJson) =>
            {
                try
                {
                    var device = JsonSerializer.Deserialize<DeviceInfo>(deviceJson,
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    if (device == null) return;
                    _dispatcher?.TryEnqueue(() => OnIncomingRequest?.Invoke(device, manifestJson));
                }
                catch { }
            };

        private void SeedDemoData()
        {
            NearbyDevices.Add(new DeviceInfo
            {
                DeviceId = "demo-1", Name = "MacBook Pro (Demo)",
                Platform = "macOS", Ip = "192.168.1.42", Port = 7700, IsOnline = true
            });
            NearbyDevices.Add(new DeviceInfo
            {
                DeviceId = "demo-2", Name = "iPhone 16 (Demo)",
                Platform = "iOS", Ip = "192.168.1.55", Port = 7700, IsOnline = true
            });
            NearbyDevices.Add(new DeviceInfo
            {
                DeviceId = "demo-3", Name = "Ubuntu Laptop (Demo)",
                Platform = "Linux", Ip = "192.168.1.67", Port = 7700, IsOnline = false
            });

            TransferHistory.Add(new TransferRecord
            {
                SessionId = "hist-1", FileName = "Project_Final.zip",
                TotalBytes = 1_245_000_000, PeerName = "MacBook Pro",
                Direction = "Send", Status = "Completed",
                StartedAt = DateTime.UtcNow.AddDays(-1),
                CompletedAt = DateTime.UtcNow.AddDays(-1).AddMinutes(3)
            });
            TransferHistory.Add(new TransferRecord
            {
                SessionId = "hist-2", FileName = "Vacation_Photos.zip",
                TotalBytes = 480_000_000, PeerName = "iPhone 16",
                Direction = "Receive", Status = "Completed",
                StartedAt = DateTime.UtcNow.AddDays(-2),
                CompletedAt = DateTime.UtcNow.AddDays(-2).AddMinutes(1)
            });
            TransferHistory.Add(new TransferRecord
            {
                SessionId = "hist-3", FileName = "4K_Render.mp4",
                TotalBytes = 8_900_000_000, PeerName = "Ubuntu Laptop",
                Direction = "Send", Status = "Cancelled",
                StartedAt = DateTime.UtcNow.AddDays(-3)
            });
        }

        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Dispose â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        public void Dispose()
        {
            if (!_disposed)
            {
                if (_handle != IntPtr.Zero)
                {
                    KtmInterop.ktm_destroy(_handle);
                    _handle = IntPtr.Zero;
                }
                _disposed = true;
            }
            GC.SuppressFinalize(this);
        }
    }
}


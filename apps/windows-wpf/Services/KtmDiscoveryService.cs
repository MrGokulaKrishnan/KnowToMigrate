using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace KnowToMigrate.Services
{
    public class KtmDiscoveryService : IDisposable
    {
        private readonly DiscoveredDevice _localDevice;
        private readonly ConcurrentDictionary<string, DiscoveredDevice> _devices = new();
        private UdpClient? _listener;
        private UdpClient? _broadcaster;
        private CancellationTokenSource? _cts;
        private bool _isDisposed;

        public event Action<List<DiscoveredDevice>>? OnDevicesUpdated;

        public KtmDiscoveryService(string deviceId, string deviceName)
        {
            _localDevice = new DiscoveredDevice
            {
                DeviceId = deviceId,
                DeviceName = deviceName,
                Platform = "Windows",
                TransferPort = KtmConstants.TransferPort,
                Version = "1.0.0",
                SupportedTransports = KtmTransportManager.Instance.LocalSupportedTransports,
                WifiDirectName = "DIRECT-KM-" + deviceName.Replace(" ", "-"),
                WifiDirectPort = KtmConstants.TransferPort,
                BestTransport = KtmTransportCodes.WifiLan
            };
        }

        public void Start()
        {
            if (_cts != null) return;
            _cts = new CancellationTokenSource();
            var token = _cts.Token;

            // Start Listener
            Task.Run(() => RunListenerAsync(token), token);

            // Start Broadcaster
            Task.Run(() => RunBroadcasterAsync(token), token);

            // Start Cleanup Timer
            Task.Run(() => RunPrunerAsync(token), token);
        }

        public void Stop()
        {
            _cts?.Cancel();
            try { _listener?.Close(); } catch { }
            try { _broadcaster?.Close(); } catch { }
            _listener = null;
            _broadcaster = null;
            _cts = null;
        }

        private async Task RunListenerAsync(CancellationToken token)
        {
            try
            {
                _listener = new UdpClient();
                _listener.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
                _listener.Client.Bind(new IPEndPoint(IPAddress.Any, KtmConstants.DiscoveryPort));
                _listener.EnableBroadcast = true;

                while (!token.IsCancellationRequested)
                {
                    var result = await _listener.ReceiveAsync(token);
                    string json = Encoding.UTF8.GetString(result.Buffer);

                    if (!json.Contains(KtmConstants.DiscoveryMagic)) continue;

                    var dev = JsonSerializer.Deserialize<DiscoveredDevice>(json);
                    if (dev == null || dev.DeviceId == _localDevice.DeviceId)
                        continue; // Skip our own beacons

                    dev.IpAddress = result.RemoteEndPoint.Address.ToString();
                    dev.LastSeen = DateTime.UtcNow;

                    if (dev.SupportedTransports == null || dev.SupportedTransports.Count == 0)
                    {
                        dev.SupportedTransports = new List<string> { KtmTransportCodes.WifiLan, KtmTransportCodes.WifiDirect, KtmTransportCodes.Bluetooth };
                    }
                    if (string.IsNullOrEmpty(dev.BestTransport))
                    {
                        dev.BestTransport = KtmTransportCodes.WifiLan;
                    }

                    _devices.AddOrUpdate(dev.DeviceId, dev, (_, existing) =>
                    {
                        existing.DeviceName = dev.DeviceName;
                        existing.Platform = dev.Platform;
                        existing.TransferPort = dev.TransferPort;
                        existing.IpAddress = dev.IpAddress;
                        existing.LastSeen = DateTime.UtcNow;
                        existing.SupportedTransports = dev.SupportedTransports;
                        existing.WifiDirectName = dev.WifiDirectName;
                        existing.WifiDirectPort = dev.WifiDirectPort;
                        existing.BluetoothAddress = dev.BluetoothAddress;
                        existing.BestTransport = dev.BestTransport;
                        return existing;
                    });

                    NotifyDevicesChanged();
                }
            }
            catch (OperationCanceledException) { }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"[KtmDiscovery] Listener exception: {ex.Message}");
            }
        }

        private async Task RunBroadcasterAsync(CancellationToken token)
        {
            try
            {
                _broadcaster = new UdpClient();
                _broadcaster.EnableBroadcast = true;
                var endpoint = new IPEndPoint(IPAddress.Broadcast, KtmConstants.DiscoveryPort);

                while (!token.IsCancellationRequested)
                {
                    string json = JsonSerializer.Serialize(_localDevice);
                    byte[] bytes = Encoding.UTF8.GetBytes(json);

                    try
                    {
                        await _broadcaster.SendAsync(bytes, bytes.Length, endpoint);
                    }
                    catch (Exception ex)
                    {
                        System.Diagnostics.Debug.WriteLine($"[KtmDiscovery] Broadcast error: {ex.Message}");
                    }

                    await Task.Delay(KtmConstants.DiscoveryIntervalMs, token);
                }
            }
            catch (OperationCanceledException) { }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"[KtmDiscovery] Broadcaster exception: {ex.Message}");
            }
        }

        private async Task RunPrunerAsync(CancellationToken token)
        {
            while (!token.IsCancellationRequested)
            {
                await Task.Delay(2000, token);
                bool changed = false;
                var now = DateTime.UtcNow;

                foreach (var kvp in _devices)
                {
                    if ((now - kvp.Value.LastSeen).TotalMilliseconds > KtmConstants.DeviceTimeoutMs)
                    {
                        if (_devices.TryRemove(kvp.Key, out _))
                            changed = true;
                    }
                }

                if (changed)
                    NotifyDevicesChanged();
            }
        }

        public void AddManualDevice(string ipAddress, int port = KtmConstants.TransferPort, string name = "Manual Device")
        {
            string key = $"manual_{ipAddress}_{port}";
            var dev = new DiscoveredDevice
            {
                DeviceId = key,
                DeviceName = name,
                Platform = "Remote IP",
                TransferPort = port,
                IpAddress = ipAddress,
                LastSeen = DateTime.UtcNow
            };
            _devices.AddOrUpdate(key, dev, (_, existing) =>
            {
                existing.LastSeen = DateTime.UtcNow;
                return existing;
            });
            NotifyDevicesChanged();
        }

        public List<DiscoveredDevice> GetOnlineDevices()
        {
            return _devices.Values.Where(d => d.IsOnline).ToList();
        }

        private void NotifyDevicesChanged()
        {
            var list = GetOnlineDevices();
            OnDevicesUpdated?.Invoke(list);
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

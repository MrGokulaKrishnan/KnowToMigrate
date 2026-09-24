using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;

namespace KnowToMigrate.Services
{
    public class KtmManager : IDisposable
    {
        private static KtmManager? _instance;
        public static KtmManager Instance => _instance ??= new KtmManager();

        public string LocalDeviceId { get; }
        public string LocalDeviceName { get; }
        public string DownloadDirectory { get; set; }

        public KtmDiscoveryService DiscoveryService { get; }
        public KtmTransferServer TransferServer { get; }
        public KtmTransferClient TransferClient { get; }

        public ObservableCollection<DiscoveredDevice> NearbyDevices { get; } = new();
        public ObservableCollection<TransferProgressInfo> TransferHistory { get; } = new();

        public event Action<DiscoveredDevice>? OnDeviceFound;
        public event Action<TransferProgressInfo>? OnProgress;
        public event Action<string, bool, string>? OnTransferDone;

        private KtmManager()
        {
            LocalDeviceId = "ktm-win-" + Environment.MachineName.ToLowerInvariant() + "-" + Guid.NewGuid().ToString("N").Substring(0, 6);
            LocalDeviceName = Environment.MachineName;

            string defaultDownloads = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                "Downloads",
                "KnowToMigrate"
            );
            DownloadDirectory = defaultDownloads;

            DiscoveryService = new KtmDiscoveryService(LocalDeviceId, LocalDeviceName);
            TransferServer = new KtmTransferServer(DownloadDirectory);
            TransferClient = new KtmTransferClient();

            // Wire discovery
            DiscoveryService.OnDevicesUpdated += devices =>
            {
                Application.Current?.Dispatcher?.Invoke(() =>
                {
                    NearbyDevices.Clear();
                    foreach (var d in devices)
                    {
                        NearbyDevices.Add(d);
                    }
                });
            };

            // Wire server
            TransferServer.OnHandshakeReceived += async handshake =>
            {
                return await Application.Current.Dispatcher.InvokeAsync(() =>
                {
                    var result = MessageBox.Show(
                        $"Incoming transfer request from:\n\nDevice: {handshake.DeviceName} ({handshake.Platform})\nPIN: {handshake.Pin}\n\nDo you want to accept this transfer?",
                        "KnowToMigrate - Accept Transfer?",
                        MessageBoxButton.YesNo,
                        MessageBoxImage.Question
                    );
                    return result == MessageBoxResult.Yes;
                });
            };

            TransferServer.OnProgress += prog =>
            {
                Application.Current?.Dispatcher?.Invoke(() =>
                {
                    OnProgress?.Invoke(prog);
                });
            };

            TransferServer.OnTransferCompleted += (sessionId, success, msg) =>
            {
                Application.Current?.Dispatcher?.Invoke(() =>
                {
                    OnTransferDone?.Invoke(sessionId, success, msg);
                });
            };

            // Wire client progress
            TransferClient.OnProgress += prog =>
            {
                Application.Current?.Dispatcher?.Invoke(() =>
                {
                    OnProgress?.Invoke(prog);
                });
            };
        }

        public void Start()
        {
            DiscoveryService.Start();
            TransferServer.Start();
        }

        public void Stop()
        {
            DiscoveryService.Stop();
            TransferServer.Stop();
        }

        public async Task<bool> SendFilesAsync(DiscoveredDevice target, IReadOnlyList<string> files, CancellationToken token = default)
        {
            return await TransferClient.SendFilesAsync(
                target.IpAddress,
                target.TransferPort,
                LocalDeviceId,
                LocalDeviceName,
                files,
                token
            );
        }

        public void Dispose()
        {
            Stop();
            DiscoveryService.Dispose();
            TransferServer.Dispose();
        }
    }
}

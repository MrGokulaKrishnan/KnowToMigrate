using System;
using System.Collections.Generic;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;

namespace KnowToMigrate.Services
{
    public class KtmTransportManager
    {
        private static KtmTransportManager? _instance;
        public static KtmTransportManager Instance => _instance ??= new KtmTransportManager();

        public bool IsWifiDirectSupported => true; // Supported across Windows 10/11
        public bool IsBluetoothSupported => true;  // Universal Windows fallback

        public List<string> LocalSupportedTransports => new()
        {
            KtmTransportCodes.WifiLan,
            KtmTransportCodes.WifiDirect,
            KtmTransportCodes.Bluetooth
        };

        private KtmTransportManager() { }

        /// <summary>
        /// Probes Wi-Fi LAN reachability by attempting a fast TCP socket connect to the target port.
        /// Timeout is 800ms for instantaneous capability check.
        /// </summary>
        public async Task<bool> ProbeWifiLanReachabilityAsync(string ipAddress, int port = KtmConstants.TransferPort, int timeoutMs = 800)
        {
            if (string.IsNullOrWhiteSpace(ipAddress)) return false;

            try
            {
                using var client = new TcpClient();
                using var cts = new CancellationTokenSource(timeoutMs);
                await client.ConnectAsync(ipAddress, port, cts.Token);
                return client.Connected;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// Evaluates target device capabilities and selects the BEST transport:
        /// 1. Wi-Fi LAN: When direct TCP connects (Fastest: 50–120+ MB/s, 0 negotiation overhead).
        /// 2. Wi-Fi Direct: When on separate subnets, AP client isolation, or outdoor (High speed: 30–80 MB/s).
        /// 3. Bluetooth: Fallback when Wi-Fi is disabled (1–2 MB/s).
        /// </summary>
        public async Task<string> EvaluateAndSelectBestTransportAsync(DiscoveredDevice target)
        {
            // Step 1: Probe Wi-Fi LAN
            bool lanReachable = await ProbeWifiLanReachabilityAsync(target.IpAddress, target.TransferPort);
            target.IsWifiLanReachable = lanReachable;

            if (lanReachable && target.SupportedTransports.Contains(KtmTransportCodes.WifiLan))
            {
                target.BestTransport = KtmTransportCodes.WifiLan;
                target.ActiveTransport = KtmTransportCodes.WifiLan;
                return KtmTransportCodes.WifiLan;
            }

            // Step 2: Wi-Fi Direct
            if (target.SupportedTransports.Contains(KtmTransportCodes.WifiDirect))
            {
                target.BestTransport = KtmTransportCodes.WifiDirect;
                target.ActiveTransport = KtmTransportCodes.WifiDirect;
                return KtmTransportCodes.WifiDirect;
            }

            // Step 3: Bluetooth Fallback
            if (target.SupportedTransports.Contains(KtmTransportCodes.Bluetooth))
            {
                target.BestTransport = KtmTransportCodes.Bluetooth;
                target.ActiveTransport = KtmTransportCodes.Bluetooth;
                return KtmTransportCodes.Bluetooth;
            }

            // Default
            target.BestTransport = KtmTransportCodes.WifiLan;
            target.ActiveTransport = KtmTransportCodes.WifiLan;
            return KtmTransportCodes.WifiLan;
        }
    }
}

using System;
using System.Collections.Generic;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace KnowToMigrate.Services
{
    public static class KtmConstants
    {
        public const int DiscoveryPort = 54123;
        public const int TransferPort = 54124;
        public const string DiscoveryMagic = "KTM_DISCOVER";
        public const uint ChunkMagic = 0x4B544D43; // "KTMC"
        public const int DefaultChunkSize = 256 * 1024; // 256 KB
        public const int MaxChunkSize = 1024 * 1024; // 1 MB
        public const int DiscoveryIntervalMs = 2000;
        public const int DeviceTimeoutMs = 7000;
    }

    public static class KtmTransportCodes
    {
        public const string WifiLan = "WIFI_LAN";
        public const string WifiDirect = "WIFI_DIRECT";
        public const string Bluetooth = "BLUETOOTH";

        public static string GetDisplayName(string code) => code switch
        {
            WifiLan => "Wi-Fi LAN",
            WifiDirect => "Wi-Fi Direct",
            Bluetooth => "Bluetooth",
            _ => code
        };

        public static string GetSpeedRating(string code) => code switch
        {
            WifiLan => "50–120+ MB/s",
            WifiDirect => "30–80 MB/s",
            Bluetooth => "1–2 MB/s",
            _ => "Variable"
        };
    }

    public class DiscoveredDevice
    {
        [JsonPropertyName("magic")]
        public string Magic { get; set; } = KtmConstants.DiscoveryMagic;

        [JsonPropertyName("deviceId")]
        public string DeviceId { get; set; } = string.Empty;

        [JsonPropertyName("deviceName")]
        public string DeviceName { get; set; } = string.Empty;

        [JsonPropertyName("platform")]
        public string Platform { get; set; } = "Windows";

        [JsonPropertyName("transferPort")]
        public int TransferPort { get; set; } = KtmConstants.TransferPort;

        [JsonPropertyName("version")]
        public string Version { get; set; } = "1.0.0";

        [JsonPropertyName("supportedTransports")]
        public List<string> SupportedTransports { get; set; } = new() { "WIFI_LAN", "WIFI_DIRECT", "BLUETOOTH" };

        [JsonPropertyName("wifiDirectName")]
        public string WifiDirectName { get; set; } = string.Empty;

        [JsonPropertyName("wifiDirectPort")]
        public int WifiDirectPort { get; set; } = KtmConstants.TransferPort;

        [JsonPropertyName("bluetoothAddress")]
        public string BluetoothAddress { get; set; } = string.Empty;

        [JsonPropertyName("bestTransport")]
        public string BestTransport { get; set; } = "WIFI_LAN";

        [JsonIgnore]
        public string IpAddress { get; set; } = string.Empty;

        [JsonIgnore]
        public string ActiveTransport { get; set; } = "WIFI_LAN";

        [JsonIgnore]
        public bool IsWifiLanReachable { get; set; } = true;

        [JsonIgnore]
        public DateTime LastSeen { get; set; } = DateTime.UtcNow;

        [JsonIgnore]
        public bool IsOnline => (DateTime.UtcNow - LastSeen).TotalMilliseconds < KtmConstants.DeviceTimeoutMs;

        [JsonIgnore]
        public bool SupportsWifiLan => SupportedTransports.Contains(KtmTransportCodes.WifiLan);

        [JsonIgnore]
        public bool SupportsWifiDirect => SupportedTransports.Contains(KtmTransportCodes.WifiDirect);

        [JsonIgnore]
        public bool SupportsBluetooth => SupportedTransports.Contains(KtmTransportCodes.Bluetooth);

        [JsonIgnore]
        public bool IsBestWifiLan => BestTransport == KtmTransportCodes.WifiLan;

        [JsonIgnore]
        public bool IsBestWifiDirect => BestTransport == KtmTransportCodes.WifiDirect;

        [JsonIgnore]
        public bool IsBestBluetooth => BestTransport == KtmTransportCodes.Bluetooth;

        [JsonIgnore]
        public string BestTransportDisplay => KtmTransportCodes.GetDisplayName(BestTransport);
    }

    public class KtmManifestItem
    {
        [JsonPropertyName("fileIndex")]
        public int FileIndex { get; set; }

        [JsonPropertyName("relativePath")]
        public string RelativePath { get; set; } = string.Empty;

        [JsonPropertyName("size")]
        public long Size { get; set; }

        [JsonPropertyName("sha256")]
        public string Sha256 { get; set; } = string.Empty;

        [JsonPropertyName("isFolder")]
        public bool IsFolder { get; set; }
    }

    public class KtmManifest
    {
        [JsonPropertyName("type")]
        public string Type { get; set; } = "MANIFEST";

        [JsonPropertyName("sessionId")]
        public string SessionId { get; set; } = Guid.NewGuid().ToString("N");

        [JsonPropertyName("senderDeviceId")]
        public string SenderDeviceId { get; set; } = string.Empty;

        [JsonPropertyName("senderDeviceName")]
        public string SenderDeviceName { get; set; } = string.Empty;

        [JsonPropertyName("totalBytes")]
        public long TotalBytes { get; set; }

        [JsonPropertyName("totalFiles")]
        public int TotalFiles { get; set; }

        [JsonPropertyName("files")]
        public List<KtmManifestItem> Files { get; set; } = new();
    }

    public class KtmHandshake
    {
        [JsonPropertyName("type")]
        public string Type { get; set; } = "HANDSHAKE";

        [JsonPropertyName("deviceId")]
        public string DeviceId { get; set; } = string.Empty;

        [JsonPropertyName("deviceName")]
        public string DeviceName { get; set; } = string.Empty;

        [JsonPropertyName("platform")]
        public string Platform { get; set; } = "Windows";

        [JsonPropertyName("pin")]
        public string Pin { get; set; } = string.Empty;

        [JsonPropertyName("selectedTransport")]
        public string SelectedTransport { get; set; } = KtmTransportCodes.WifiLan;

        [JsonPropertyName("supportedTransports")]
        public List<string> SupportedTransports { get; set; } = new() { KtmTransportCodes.WifiLan, KtmTransportCodes.WifiDirect, KtmTransportCodes.Bluetooth };
    }

    public class KtmHandshakeAck
    {
        [JsonPropertyName("type")]
        public string Type { get; set; } = "HANDSHAKE_ACK";

        [JsonPropertyName("accepted")]
        public bool Accepted { get; set; }

        [JsonPropertyName("pin")]
        public string Pin { get; set; } = string.Empty;

        [JsonPropertyName("selectedTransport")]
        public string SelectedTransport { get; set; } = KtmTransportCodes.WifiLan;

        [JsonPropertyName("reason")]
        public string Reason { get; set; } = string.Empty;
    }

    public class KtmManifestAck
    {
        [JsonPropertyName("type")]
        public string Type { get; set; } = "MANIFEST_ACK";

        [JsonPropertyName("sessionId")]
        public string SessionId { get; set; } = string.Empty;

        [JsonPropertyName("accepted")]
        public bool Accepted { get; set; }

        [JsonPropertyName("reason")]
        public string Reason { get; set; } = string.Empty;

        /// <summary>
        /// Existing verified byte count per file index for resume support.
        /// </summary>
        [JsonPropertyName("existingOffsets")]
        public Dictionary<int, long> ExistingOffsets { get; set; } = new();
    }

    public class KtmFileComplete
    {
        [JsonPropertyName("type")]
        public string Type { get; set; } = "FILE_COMPLETE";

        [JsonPropertyName("fileIndex")]
        public int FileIndex { get; set; }

        [JsonPropertyName("status")]
        public string Status { get; set; } = "OK"; // "OK", "HASH_MISMATCH", "ERROR"

        [JsonPropertyName("sha256")]
        public string Sha256 { get; set; } = string.Empty;
    }

    public class KtmTransferComplete
    {
        [JsonPropertyName("type")]
        public string Type { get; set; } = "TRANSFER_COMPLETE";

        [JsonPropertyName("sessionId")]
        public string SessionId { get; set; } = string.Empty;

        [JsonPropertyName("success")]
        public bool Success { get; set; }

        [JsonPropertyName("totalBytesTransferred")]
        public long TotalBytesTransferred { get; set; }
    }

    public class TransferProgressInfo
    {
        public string SessionId { get; set; } = string.Empty;
        public string CurrentFileName { get; set; } = string.Empty;
        public int CurrentFileIndex { get; set; }
        public int TotalFiles { get; set; }
        public long BytesTransferred { get; set; }
        public long TotalBytes { get; set; }
        public double SpeedMBps { get; set; }
        public double Percentage => TotalBytes > 0 ? Math.Min(100.0, (double)BytesTransferred / TotalBytes * 100.0) : 0.0;
        public string PeerName { get; set; } = string.Empty;
        public bool IsCompleted { get; set; }
        public bool IsCancelled { get; set; }
        public string ErrorMessage { get; set; } = string.Empty;
        public string TransportType { get; set; } = "Wi-Fi (LAN)";
    }

    public static class KtmSecurityUtils
    {
        /// <summary>
        /// Sanitizes relative path to strictly prevent directory traversal (e.g., ../, .., drive letters, absolute paths).
        /// </summary>
        public static string SanitizeRelativePath(string relativePath)
        {
            if (string.IsNullOrWhiteSpace(relativePath))
                return "unnamed_file";

            // Normalize slashes
            string clean = relativePath.Replace('\\', '/');

            // Strip any leading slashes or drive letters (e.g. C:/)
            if (clean.Length >= 2 && char.IsLetter(clean[0]) && clean[1] == ':')
                clean = clean.Substring(2);

            while (clean.StartsWith("/"))
                clean = clean.TrimStart('/');

            // Split into segments and discard traversal segments
            var parts = clean.Split(new[] { '/' }, StringSplitOptions.RemoveEmptyEntries);
            var safeParts = new List<string>();

            foreach (var part in parts)
            {
                if (part == "." || part == "..") continue;

                // Strip invalid characters
                char[] invalidChars = Path.GetInvalidFileNameChars();
                var sb = new StringBuilder();
                foreach (char c in part)
                {
                    if (Array.IndexOf(invalidChars, c) < 0)
                        sb.Append(c);
                }
                string safeName = sb.ToString().Trim();
                if (!string.IsNullOrEmpty(safeName))
                    safeParts.Add(safeName);
            }

            if (safeParts.Count == 0)
                return "safe_file_" + Guid.NewGuid().ToString("N").Substring(0, 8);

            return string.Join(Path.DirectorySeparatorChar.ToString(), safeParts);
        }

        public static string ComputeFileSha256(string filePath, Action<long>? progressCallback = null)
        {
            using var sha256 = SHA256.Create();
            using var stream = new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.Read, 65536, useAsync: false);
            byte[] buffer = new byte[65536];
            int read;
            long totalRead = 0;

            while ((read = stream.Read(buffer, 0, buffer.Length)) > 0)
            {
                sha256.TransformBlock(buffer, 0, read, null, 0);
                totalRead += read;
                progressCallback?.Invoke(totalRead);
            }
            sha256.TransformFinalBlock(Array.Empty<byte>(), 0, 0);

            var hash = sha256.Hash;
            return hash != null ? BitConverter.ToString(hash).Replace("-", "").ToUpperInvariant() : string.Empty;
        }

        public static string Generate6DigitPin()
        {
            using var rng = RandomNumberGenerator.Create();
            byte[] bytes = new byte[4];
            rng.GetBytes(bytes);
            uint val = BitConverter.ToUInt32(bytes, 0) % 900000 + 100000;
            return val.ToString();
        }
    }
}

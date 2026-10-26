using Microsoft.Win32;
using System.Diagnostics;
using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text;
using System.Windows.Media;

namespace KnowToMigrate.Services
{
    public class KtmUserSettings
    {
        public string DeviceName { get; set; } = Environment.MachineName;
        public string DownloadDirectory { get; set; } = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            "Downloads",
            "KnowToMigrate"
        );
        public bool RequirePin { get; set; } = true;
        [JsonIgnore]
        public bool RequireSecurityPin { get => RequirePin; set => RequirePin = value; }

        public bool AutoAcceptTrusted { get; set; } = false;
        [JsonIgnore]
        public bool AutoAcceptTrustedDevices { get => AutoAcceptTrusted; set => AutoAcceptTrusted = value; }

        public bool CheckUpdatesAutomatically { get; set; } = true;
        [JsonIgnore]
        public bool AutoCheckForUpdates { get => CheckUpdatesAutomatically; set => CheckUpdatesAutomatically = value; }

        public bool MinimizeToTray { get; set; } = true;
        public bool SoundEffectsEnabled { get; set; } = true;
        public bool ShellContextMenuEnabled { get; set; } = true;
        public DuplicateResolutionMode DuplicateHandling { get; set; } = DuplicateResolutionMode.KeepBoth;
        public bool TemporaryReceiveEnabled { get; set; } = false;
        public int TemporaryReceiveMinutes { get; set; } = 10;

        public string UpdateChannel { get; set; } = "stable";
        public DateTime? LastUpdateCheckUtc { get; set; } = null;
        public string? SkippedUpdateVersion { get; set; } = null;
    }

    [JsonConverter(typeof(TransferDirectionJsonConverter))]
    public enum TransferDirection
    {
        Sent,
        Received,
        Web,
        Sending = Sent,
        Receiving = Received
    }

    public class TransferDirectionJsonConverter : JsonConverter<TransferDirection>
    {
        public override TransferDirection Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            if (reader.TokenType == JsonTokenType.String)
            {
                string? str = reader.GetString();
                if (string.Equals(str, "in", StringComparison.OrdinalIgnoreCase) ||
                    string.Equals(str, "received", StringComparison.OrdinalIgnoreCase) ||
                    string.Equals(str, "receiving", StringComparison.OrdinalIgnoreCase))
                {
                    return TransferDirection.Received;
                }
                if (string.Equals(str, "out", StringComparison.OrdinalIgnoreCase) ||
                    string.Equals(str, "sent", StringComparison.OrdinalIgnoreCase) ||
                    string.Equals(str, "sending", StringComparison.OrdinalIgnoreCase))
                {
                    return TransferDirection.Sent;
                }
                if (string.Equals(str, "web", StringComparison.OrdinalIgnoreCase))
                {
                    return TransferDirection.Web;
                }
            }
            else if (reader.TokenType == JsonTokenType.Number)
            {
                int val = reader.GetInt32();
                if (Enum.IsDefined(typeof(TransferDirection), val))
                    return (TransferDirection)val;
            }
            return TransferDirection.Received;
        }

        public override void Write(Utf8JsonWriter writer, TransferDirection value, JsonSerializerOptions options)
        {
            writer.WriteStringValue(value.ToString());
        }
    }

    public class HistoricalTransferItem
    {
        public string TransferId { get; set; } = Guid.NewGuid().ToString("N");
        public string SessionId { get; set; } = string.Empty;
        public string FileName { get; set; } = "";
        public long TotalBytes { get; set; }
        public string DeviceName { get; set; } = "";
        public TransferDirection Direction { get; set; } = TransferDirection.Received;
        public string Status { get; set; } = "Complete"; // "Complete", "Cancelled", "Failed"
        public DateTime TimestampUtc { get; set; } = DateTime.UtcNow;
        public string Sha256 { get; set; } = "";
        public string Transport { get; set; } = "Transit Direct";
        public string? FilePath { get; set; } = null;

        [JsonIgnore]
        public string FormattedSize => KtmFormatting.FormatBytes(TotalBytes);

        [JsonIgnore]
        public string FormattedTime => TimestampUtc.ToLocalTime().ToString("dd MMM yyyy, hh:mm tt");

        [JsonIgnore]
        public string DirectionDisplay => Direction switch
        {
            TransferDirection.Received => "Received",
            TransferDirection.Sent => "Sent",
            TransferDirection.Web => "Web Share",
            _ => "Transferred"
        };

        private static SolidColorBrush ResolveThemeBrush(string themeKey, Color fallback)
        {
            try
            {
                if (System.Windows.Application.Current?.Resources[themeKey] is SolidColorBrush brush)
                    return brush;
            }
            catch { }
            return new SolidColorBrush(fallback);
        }

        [JsonIgnore]
        public SolidColorBrush DirectionBrush => Direction switch
        {
            TransferDirection.Received => ResolveThemeBrush("TextSecondary", Color.FromRgb(204, 204, 204)), // Neutral light grey
            TransferDirection.Sent => ResolveThemeBrush("KmOrange", Color.FromRgb(255, 90, 0)),             // Orange accent
            TransferDirection.Web => ResolveThemeBrush("BrushWebBlue", Color.FromRgb(56, 189, 248)),        // Blue
            _ => new SolidColorBrush(Color.FromRgb(140, 140, 140))
        };

        [JsonIgnore]
        public SolidColorBrush DirectionBgBrush => Direction switch
        {
            TransferDirection.Received => new SolidColorBrush(Color.FromArgb(30, 204, 204, 204)),
            TransferDirection.Sent => new SolidColorBrush(Color.FromArgb(38, 255, 90, 0)),
            TransferDirection.Web => new SolidColorBrush(Color.FromArgb(38, 56, 189, 248)),
            _ => new SolidColorBrush(Color.FromArgb(20, 20, 20, 20))
        };

        [JsonIgnore]
        public bool IsShaVerified => !string.IsNullOrEmpty(Sha256) &&
                                     !string.Equals(Sha256, "pending", StringComparison.OrdinalIgnoreCase) &&
                                     !string.Equals(Sha256, "verified", StringComparison.OrdinalIgnoreCase);

        [JsonIgnore]
        public string VerifiedBadge => IsShaVerified ? "Verified" : "Not checked";

        [JsonIgnore]
        public SolidColorBrush VerifiedBadgeBrush => IsShaVerified
            ? ResolveThemeBrush("BrushSuccess", Color.FromRgb(34, 197, 94))
            : ResolveThemeBrush("TextMuted", Color.FromRgb(140, 140, 140));

        [JsonIgnore]
        public SolidColorBrush VerifiedBadgeBgBrush => IsShaVerified
            ? new SolidColorBrush(Color.FromArgb(38, 34, 197, 94))
            : new SolidColorBrush(Color.FromArgb(26, 140, 140, 140));

        [JsonIgnore]
        public string DayGroup
        {
            get
            {
                var local = TimestampUtc.ToLocalTime();
                if (local.Date == DateTime.Today) return "Today";
                if (local.Date == DateTime.Today.AddDays(-1)) return "Yesterday";
                return local.ToString("dd MMMM yyyy");
            }
        }

        [JsonIgnore]
        public string StatusBadge => Status;

        [JsonIgnore]
        public SolidColorBrush StatusBadgeBrush => Status == "Complete"
            ? ResolveThemeBrush("BrushSuccess", Color.FromRgb(34, 197, 94))
            : ResolveThemeBrush("BrushError", Color.FromRgb(239, 68, 68));

        [JsonIgnore]
        public SolidColorBrush StatusBadgeBgBrush => Status == "Complete"
            ? new SolidColorBrush(Color.FromArgb(38, 34, 197, 94))
            : new SolidColorBrush(Color.FromArgb(38, 239, 68, 68));
    }

    public class TransferRecord : HistoricalTransferItem
    {
    }

    public class HistoryContainer
    {
        [JsonPropertyName("schemaVersion")]
        public int SchemaVersion { get; set; } = 2;

        [JsonPropertyName("items")]
        public List<HistoricalTransferItem> Items { get; set; } = new();
    }

    public class TrustedDeviceItem
    {
        public string DeviceId { get; set; } = "";
        public string DeviceName { get; set; } = "";
        public DateTime AddedAtUtc { get; set; } = DateTime.UtcNow;
        public string Platform { get; set; } = "Android";
    }

    /// <summary>
    /// Manages persistent user settings, transfer history, and trusted devices in %LOCALAPPDATA%\KnowToMigrate.
    /// Ensures user data is strictly separated from installation binaries and survives upgrades/reinstalls.
    /// </summary>
    public static class UserSettingsManager
    {
        public static string BaseDataDirectory { get; } = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "KnowToMigrate"
        );

        public static string LogsDirectory => Path.Combine(BaseDataDirectory, "logs");
        public static string UpdatesDirectory => Path.Combine(BaseDataDirectory, "Updates");
        public static string PendingUpdateDirectory => Path.Combine(UpdatesDirectory, "pending");
        public static string BackupDirectory => Path.Combine(UpdatesDirectory, "backup");

        private static readonly string SettingsFile = Path.Combine(BaseDataDirectory, "settings.json");
        private static readonly string HistoryFile = Path.Combine(BaseDataDirectory, "history.json");
        private static readonly string TrustFile = Path.Combine(BaseDataDirectory, "trusted_devices.json");

        private static readonly object _syncLock = new();

        static UserSettingsManager()
        {
            EnsureDirectories();
            EnsureMigratedHistory();
        }

        public static void EnsureDirectories()
        {
            try
            {
                if (!Directory.Exists(BaseDataDirectory)) Directory.CreateDirectory(BaseDataDirectory);
                if (!Directory.Exists(LogsDirectory)) Directory.CreateDirectory(LogsDirectory);
                if (!Directory.Exists(PendingUpdateDirectory)) Directory.CreateDirectory(PendingUpdateDirectory);
                if (!Directory.Exists(BackupDirectory)) Directory.CreateDirectory(BackupDirectory);
            }
            catch { }
        }

        public static bool EnsureMigratedHistory(string? customHistoryPath = null)
        {
            lock (_syncLock)
            {
                try
                {
                    string targetFile = customHistoryPath ?? HistoryFile;
                    if (!File.Exists(targetFile)) return false;

                    string json = File.ReadAllText(targetFile).Trim();
                    if (string.IsNullOrWhiteSpace(json)) return false;

                    // Idempotency check: if schemaVersion >= 2, already migrated
                    if (json.StartsWith("{"))
                    {
                        try
                        {
                            using var doc = JsonDocument.Parse(json);
                            if (doc.RootElement.TryGetProperty("schemaVersion", out var ver) && ver.GetInt32() >= 2)
                            {
                                return false; // Already migrated, do nothing
                            }
                        }
                        catch { }
                    }

                    // Perform backup: history.json -> history.json.bak.<timestamp>
                    string timestamp = DateTime.UtcNow.ToString("yyyyMMdd_HHmmss");
                    string dir = Path.GetDirectoryName(targetFile) ?? BaseDataDirectory;
                    string backupFile = Path.Combine(dir, $"history.json.bak.{timestamp}");
                    File.Copy(targetFile, backupFile, true);

                    // Load items (either from legacy array or older format)
                    List<HistoricalTransferItem> items;
                    if (json.StartsWith("["))
                    {
                        items = JsonSerializer.Deserialize<List<HistoricalTransferItem>>(json) ?? new List<HistoricalTransferItem>();
                    }
                    else
                    {
                        var container = JsonSerializer.Deserialize<HistoryContainer>(json);
                        items = container?.Items ?? new List<HistoricalTransferItem>();
                    }

                    // Clean existing data:
                    // Remove records whose FileName ends with " items transferred" and whose DeviceName is "Nearby Target"
                    items.RemoveAll(r => (r.FileName != null && r.FileName.EndsWith(" items transferred", StringComparison.OrdinalIgnoreCase)) &&
                                         (r.DeviceName == "Nearby Target"));

                    // Also clear any literal "verified" strings
                    foreach (var item in items)
                    {
                        if (string.Equals(item.Sha256, "verified", StringComparison.OrdinalIgnoreCase))
                        {
                            item.Sha256 = "";
                        }
                    }

                    var newContainer = new HistoryContainer
                    {
                        SchemaVersion = 2,
                        Items = items
                    };

                    string migratedJson = JsonSerializer.Serialize(newContainer, new JsonSerializerOptions { WriteIndented = true });
                    File.WriteAllText(targetFile, migratedJson);
                    return true;
                }
                catch
                {
                    return false;
                }
            }
        }

        public static KtmUserSettings LoadSettings()
        {
            lock (_syncLock)
            {
                try
                {
                    if (File.Exists(SettingsFile))
                    {
                        string json = File.ReadAllText(SettingsFile);
                        var settings = JsonSerializer.Deserialize<KtmUserSettings>(json);
                        if (settings != null) return settings;
                    }
                }
                catch { }

                var def = new KtmUserSettings();
                SaveSettings(def);
                return def;
            }
        }

        public static void SaveSettings(KtmUserSettings settings)
        {
            lock (_syncLock)
            {
                try
                {
                    EnsureDirectories();
                    string json = JsonSerializer.Serialize(settings, new JsonSerializerOptions { WriteIndented = true });
                    File.WriteAllText(SettingsFile, json);
                }
                catch { }
            }
        }

        public static List<HistoricalTransferItem> LoadHistory(string? customHistoryPath = null)
        {
            lock (_syncLock)
            {
                try
                {
                    string targetFile = customHistoryPath ?? HistoryFile;
                    if (File.Exists(targetFile))
                    {
                        string json = File.ReadAllText(targetFile).Trim();
                        if (json.StartsWith("["))
                        {
                            var list = JsonSerializer.Deserialize<List<HistoricalTransferItem>>(json);
                            if (list != null) return list;
                        }
                        else if (json.StartsWith("{"))
                        {
                            var container = JsonSerializer.Deserialize<HistoryContainer>(json);
                            if (container != null && container.Items != null) return container.Items;
                        }
                    }
                }
                catch { }

                return new List<HistoricalTransferItem>();
            }
        }

        public static void AddHistoryItem(HistoricalTransferItem item, string? customHistoryPath = null)
        {
            lock (_syncLock)
            {
                try
                {
                    string targetFile = customHistoryPath ?? HistoryFile;
                    var list = LoadHistory(targetFile);
                    list.Insert(0, item);
                    if (list.Count > 200) list.RemoveRange(200, list.Count - 200);

                    var container = new HistoryContainer
                    {
                        SchemaVersion = 2,
                        Items = list
                    };

                    string json = JsonSerializer.Serialize(container, new JsonSerializerOptions { WriteIndented = true });
                    File.WriteAllText(targetFile, json);
                }
                catch { }
            }
        }

        public static void ClearHistory()
        {
            lock (_syncLock)
            {
                try
                {
                    if (File.Exists(HistoryFile)) File.Delete(HistoryFile);
                }
                catch { }
            }
        }

        public static string ExportHistoryText()
        {
            var list = LoadHistory();
            var sb = new StringBuilder();
            sb.AppendLine("================================================================================");
            sb.AppendLine("KNOWTOMIGRATE - MIGRATION LEDGER AUDIT LOG");
            sb.AppendLine($"Generated: {DateTime.Now:yyyy-MM-dd HH:mm:ss} | Local Device: {Environment.MachineName}");
            sb.AppendLine("================================================================================");
            sb.AppendLine();
            if (list.Count == 0)
            {
                sb.AppendLine("No transfer history records logged yet.");
            }
            else
            {
                foreach (var item in list)
                {
                    sb.AppendLine($"Transfer ID: {item.TransferId}");
                    sb.AppendLine($"Date & Time: {item.TimestampUtc.ToLocalTime():yyyy-MM-dd HH:mm:ss}");
                    sb.AppendLine($"Direction:   {item.DirectionDisplay}");
                    sb.AppendLine($"Item:        {item.FileName} ({item.FormattedSize})");
                    sb.AppendLine($"Peer Device: {item.DeviceName}");
                    sb.AppendLine($"Transport:   {item.Transport}");
                    sb.AppendLine($"Status:      {item.Status}");
                    sb.AppendLine($"Integrity:   {item.VerifiedBadge}");
                    if (!string.IsNullOrEmpty(item.FilePath)) sb.AppendLine($"Path:        {item.FilePath}");
                    sb.AppendLine("--------------------------------------------------------------------------------");
                }
            }
            return sb.ToString();
        }

        public static List<TrustedDeviceItem> LoadTrustedDevices()
        {
            lock (_syncLock)
            {
                try
                {
                    if (File.Exists(TrustFile))
                    {
                        string json = File.ReadAllText(TrustFile);
                        var list = JsonSerializer.Deserialize<List<TrustedDeviceItem>>(json);
                        if (list != null) return list;
                    }
                }
                catch { }

                return new List<TrustedDeviceItem>();
            }
        }

        public static void AddTrustedDevice(TrustedDeviceItem device)
        {
            lock (_syncLock)
            {
                try
                {
                    var list = LoadTrustedDevices();
                    list.RemoveAll(d => d.DeviceId == device.DeviceId);
                    list.Add(device);

                    string json = JsonSerializer.Serialize(list, new JsonSerializerOptions { WriteIndented = true });
                    File.WriteAllText(TrustFile, json);
                }
                catch { }
            }
        }

        public static void RemoveTrustedDevice(string deviceId)
        {
            lock (_syncLock)
            {
                try
                {
                    var list = LoadTrustedDevices();
                    list.RemoveAll(d => d.DeviceId == deviceId);

                    string json = JsonSerializer.Serialize(list, new JsonSerializerOptions { WriteIndented = true });
                    File.WriteAllText(TrustFile, json);
                }
                catch { }
            }
        }
    
        public static void SetShellContextMenu(bool enable)
        {
            try
            {
                string exePath = Process.GetCurrentProcess().MainModule?.FileName ?? "";
                if (string.IsNullOrEmpty(exePath) || !File.Exists(exePath))
                {
                    exePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "KnowToMigrate.exe");
                }

                string[] targetKeys = {
                    @"Software\Classes\*\shell\KnowToMigrate",
                    @"Software\Classes\Directory\shell\KnowToMigrate"
                };

                foreach (var relKey in targetKeys)
                {
                    if (enable)
                    {
                        using var key = Registry.CurrentUser.CreateSubKey(relKey);
                        if (key != null)
                        {
                            key.SetValue("", "Send with KnowToMigrate");
                            key.SetValue("Icon", $"\"{exePath}\"");
                            using var cmdKey = key.CreateSubKey("command");
                            cmdKey?.SetValue("", $"\"{exePath}\" \"%1\"");
                        }
                    }
                    else
                    {
                        Registry.CurrentUser.DeleteSubKeyTree(relKey, false);
                    }
                }
            }
            catch { }
        }
    }
}
using Microsoft.Win32;
using System.Diagnostics;
using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization;

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

    public class HistoricalTransferItem
    {
        public string TransferId { get; set; } = Guid.NewGuid().ToString("N");
        public string FileName { get; set; } = "";
        public long TotalBytes { get; set; }
        public string DeviceName { get; set; } = "";
        public string Direction { get; set; } = "in"; // "in" or "out"
        public string Status { get; set; } = "Complete"; // "Complete", "Cancelled", "Failed"
        public DateTime TimestampUtc { get; set; } = DateTime.UtcNow;
        public string Sha256 { get; set; } = "";
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

        public static List<HistoricalTransferItem> LoadHistory()
        {
            lock (_syncLock)
            {
                try
                {
                    if (File.Exists(HistoryFile))
                    {
                        string json = File.ReadAllText(HistoryFile);
                        var list = JsonSerializer.Deserialize<List<HistoricalTransferItem>>(json);
                        if (list != null) return list;
                    }
                }
                catch { }

                return new List<HistoricalTransferItem>();
            }
        }

        public static void AddHistoryItem(HistoricalTransferItem item)
        {
            lock (_syncLock)
            {
                try
                {
                    var list = LoadHistory();
                    list.Insert(0, item);
                    if (list.Count > 200) list.RemoveRange(200, list.Count - 200);

                    string json = JsonSerializer.Serialize(list, new JsonSerializerOptions { WriteIndented = true });
                    File.WriteAllText(HistoryFile, json);
                }
                catch { }
            }
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
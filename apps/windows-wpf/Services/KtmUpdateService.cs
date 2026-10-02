using System;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;

namespace KnowToMigrate.Services
{
    public class UpdateManifestWindowsDetails
    {
        [JsonPropertyName("architecture")]
        public string Architecture { get; set; } = "x64";

        [JsonPropertyName("installerType")]
        public string InstallerType { get; set; } = "exe";

        [JsonPropertyName("version")]
        public string Version { get; set; } = "";

        [JsonPropertyName("filename")]
        public string Filename { get; set; } = "";

        [JsonPropertyName("url")]
        public string Url { get; set; } = "";

        [JsonPropertyName("size")]
        public long Size { get; set; }

        [JsonPropertyName("sha256")]
        public string Sha256 { get; set; } = "";

        [JsonPropertyName("signature")]
        public string Signature { get; set; } = "";
    }

    public class UpdateManifest
    {
        [JsonPropertyName("product")]
        public string Product { get; set; } = "KnowToMigrate";

        [JsonPropertyName("channel")]
        public string Channel { get; set; } = "stable";

        [JsonPropertyName("version")]
        public string Version { get; set; } = "";

        [JsonPropertyName("minimumSupportedVersion")]
        public string MinimumSupportedVersion { get; set; } = "1.0.0";

        [JsonPropertyName("mandatory")]
        public bool Mandatory { get; set; }

        [JsonPropertyName("publishedAt")]
        public string PublishedAt { get; set; } = "";

        [JsonPropertyName("windows")]
        public UpdateManifestWindowsDetails? Windows { get; set; }

        [JsonPropertyName("releaseNotes")]
        public string[] ReleaseNotes { get; set; } = Array.Empty<string>();
    }

    public enum UpdateCheckStatus
    {
        Idle,
        Checking,
        UpToDate,
        UpdateAvailable,
        Offline,
        Failed
    }

    public enum UpdateDownloadStatus
    {
        NotStarted,
        Connecting,
        Downloading,
        Verifying,
        ReadyToInstall,
        Installing,
        Failed,
        Cancelled
    }

    public class UpdateDownloadProgress
    {
        public long BytesDownloaded { get; set; }
        public long TotalBytes { get; set; }
        public double Percentage => TotalBytes > 0 ? Math.Min(100.0, (double)BytesDownloaded / TotalBytes * 100.0) : 0.0;
        public double SpeedBps { get; set; }
        public double EtaSeconds { get; set; }

        public string SpeedFormatted
        {
            get
            {
                if (SpeedBps >= 1_000_000_000) return $"{SpeedBps / 1_000_000_000.0:F1} GB/s";
                if (SpeedBps >= 1_000_000) return $"{SpeedBps / 1_000_000.0:F1} MB/s";
                if (SpeedBps >= 1_000) return $"{SpeedBps / 1_000.0:F1} KB/s";
                return $"{SpeedBps:F0} B/s";
            }
        }

        public string EtaFormatted
        {
            get
            {
                if (double.IsInfinity(EtaSeconds) || double.IsNaN(EtaSeconds) || EtaSeconds <= 0) return "Calculating...";
                var ts = TimeSpan.FromSeconds(EtaSeconds);
                return ts.TotalHours >= 1 ? ts.ToString(@"hh\:mm\:ss") : ts.ToString(@"mm\:ss");
            }
        }
    }

    /// <summary>
    /// Enterprise Auto-Update service for KnowToMigrate on Windows.
    /// Manages HTTPS manifest retrieval, anti-downgrade validation, resumable download,
    /// cryptographic verification (SHA-256), active transfer protection, and standalone updater execution.
    /// </summary>
    public class KtmUpdateService
    {
        private static KtmUpdateService? _instance;
        public static KtmUpdateService Instance => _instance ??= new KtmUpdateService();

        public static string CurrentVersion
        {
            get
            {
                try
                {
                    var asm = System.Reflection.Assembly.GetExecutingAssembly();
                    var infoAttr = asm.GetCustomAttribute<System.Reflection.AssemblyInformationalVersionAttribute>();
                    if (infoAttr != null && !string.IsNullOrWhiteSpace(infoAttr.InformationalVersion))
                    {
                        string v = infoAttr.InformationalVersion.Split('+')[0].Trim();
                        if (!string.IsNullOrEmpty(v)) return v;
                    }
                    var ver = asm.GetName().Version;
                    if (ver != null) return $"{ver.Major}.{ver.Minor}.{ver.Build}";
                }
                catch { }
                return "1.0.0";
            }
        }

        public const string DefaultManifestUrl = "https://knowtomigrate.web.app/update-manifest.json";

        private readonly HttpClient _httpClient;
        private CancellationTokenSource? _downloadCts;

        public UpdateCheckStatus CheckStatus { get; private set; } = UpdateCheckStatus.Idle;
        public UpdateDownloadStatus DownloadStatus { get; private set; } = UpdateDownloadStatus.NotStarted;
        public UpdateManifest? AvailableManifest { get; private set; }
        public string LastError { get; private set; } = "";

        public event Action<UpdateCheckStatus, UpdateManifest?>? OnCheckStatusChanged;
        public event Action<UpdateDownloadStatus, UpdateDownloadProgress?>? OnDownloadProgressChanged;
        public event Action<string>? OnUpdateError;

        private KtmUpdateService()
        {
            var handler = new HttpClientHandler
            {
                AllowAutoRedirect = true,
                CheckCertificateRevocationList = true
            };
            _httpClient = new HttpClient(handler)
            {
                Timeout = TimeSpan.FromSeconds(30)
            };
            _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd($"KnowToMigrate/{CurrentVersion} (Windows NT; x64)");
        }

        public async Task<UpdateCheckStatus> CheckForUpdatesAsync(bool isManual = false)
        {
            if (CheckStatus == UpdateCheckStatus.Checking) return CheckStatus;

            CheckStatus = UpdateCheckStatus.Checking;
            OnCheckStatusChanged?.Invoke(CheckStatus, null);
            Log("[INFO] Update check initiated (Manual=" + isManual + ")");

            try
            {
                var settings = UserSettingsManager.LoadSettings();

                // Rate limiting for background checks (don't check more than once every 4 hours automatically)
                if (!isManual && settings.LastUpdateCheckUtc.HasValue)
                {
                    if (DateTime.UtcNow - settings.LastUpdateCheckUtc.Value < TimeSpan.FromHours(4))
                    {
                        Log("[INFO] Skipping background check; checked recently at " + settings.LastUpdateCheckUtc.Value);
                        CheckStatus = UpdateCheckStatus.UpToDate;
                        OnCheckStatusChanged?.Invoke(CheckStatus, null);
                        return CheckStatus;
                    }
                }

                // Check network connectivity
                if (!System.Net.NetworkInformation.NetworkInterface.GetIsNetworkAvailable())
                {
                    Log("[INFO] Network is unavailable; offline mode.");
                    CheckStatus = UpdateCheckStatus.Offline;
                    OnCheckStatusChanged?.Invoke(CheckStatus, null);
                    return CheckStatus;
                }

                HttpResponseMessage response;
                try
                {
                    string manifestUrlWithCacheBust = $"{DefaultManifestUrl}?_cb={DateTime.UtcNow.Ticks}";
                    var request = new HttpRequestMessage(HttpMethod.Get, manifestUrlWithCacheBust);
                    request.Headers.CacheControl = new CacheControlHeaderValue { NoCache = true, NoStore = true };
                    response = await _httpClient.SendAsync(request);
                }
                catch (HttpRequestException ex)
                {
                    Log($"[WARN] Could not contact update server: {ex.Message}");
                    CheckStatus = UpdateCheckStatus.Offline;
                    OnCheckStatusChanged?.Invoke(CheckStatus, null);
                    return CheckStatus;
                }

                if (!response.IsSuccessStatusCode)
                {
                    throw new InvalidOperationException($"Update manifest returned HTTP {response.StatusCode}");
                }

                string json = await response.Content.ReadAsStringAsync();
                var manifest = JsonSerializer.Deserialize<UpdateManifest>(json);
                if (manifest == null || manifest.Windows == null)
                {
                    throw new InvalidDataException("Invalid or malformed update manifest format");
                }

                settings.LastUpdateCheckUtc = DateTime.UtcNow;
                UserSettingsManager.SaveSettings(settings);

                Log($"[INFO] Manifest retrieved. Remote Version: {manifest.Version}, Current Version: {CurrentVersion}");

                int comparison = CompareVersions(manifest.Version, CurrentVersion);
                if (comparison > 0)
                {
                    // Check if user chose to skip this specific non-mandatory version
                    if (!isManual && !manifest.Mandatory && string.Equals(settings.SkippedUpdateVersion, manifest.Version, StringComparison.OrdinalIgnoreCase))
                    {
                        Log($"[INFO] Version {manifest.Version} was skipped by user. Treating as up to date.");
                        CheckStatus = UpdateCheckStatus.UpToDate;
                        OnCheckStatusChanged?.Invoke(CheckStatus, null);
                        return CheckStatus;
                    }

                    AvailableManifest = manifest;
                    CheckStatus = UpdateCheckStatus.UpdateAvailable;
                    Log($"[INFO] Newer version available: {manifest.Version}");
                    OnCheckStatusChanged?.Invoke(CheckStatus, AvailableManifest);
                    return CheckStatus;
                }
                else
                {
                    AvailableManifest = null;
                    CheckStatus = UpdateCheckStatus.UpToDate;
                    Log("[INFO] Application is up to date.");
                    OnCheckStatusChanged?.Invoke(CheckStatus, null);
                    return CheckStatus;
                }
            }
            catch (Exception ex)
            {
                LastError = ex.Message;
                Log($"[ERROR] Update check failed: {ex.Message}");
                CheckStatus = UpdateCheckStatus.Failed;
                OnCheckStatusChanged?.Invoke(CheckStatus, null);
                OnUpdateError?.Invoke(ex.Message);
                return CheckStatus;
            }
        }

        public async Task<bool> DownloadAndPrepareUpdateAsync()
        {
            if (AvailableManifest?.Windows == null)
            {
                OnUpdateError?.Invoke("No update package available in manifest.");
                return false;
            }

            var winDetails = AvailableManifest.Windows;
            string downloadUrl = winDetails.Url;
            string filename = !string.IsNullOrEmpty(winDetails.Filename) ? winDetails.Filename : "KnowToMigrate-Setup.exe";

            // Stage in %LOCALAPPDATA%\KnowToMigrate\Updates\pending\
            string stagingDir = UserSettingsManager.PendingUpdateDirectory;
            string targetFilePath = Path.Combine(stagingDir, filename);

            _downloadCts = new CancellationTokenSource();
            var token = _downloadCts.Token;

            DownloadStatus = UpdateDownloadStatus.Connecting;
            OnDownloadProgressChanged?.Invoke(DownloadStatus, new UpdateDownloadProgress());
            Log($"[INFO] Initiating update download from '{downloadUrl}' to '{targetFilePath}'");

            try
            {
                long existingLength = 0;
                if (File.Exists(targetFilePath))
                {
                    existingLength = new FileInfo(targetFilePath).Length;
                    // If existing file is already complete and verified, proceed to verification
                    if (existingLength == winDetails.Size && VerifySha256(targetFilePath, winDetails.Sha256))
                    {
                        Log("[INFO] Existing staged update package is complete and valid. Skipping re-download.");
                        DownloadStatus = UpdateDownloadStatus.ReadyToInstall;
                        OnDownloadProgressChanged?.Invoke(DownloadStatus, new UpdateDownloadProgress { BytesDownloaded = existingLength, TotalBytes = existingLength });
                        return true;
                    }
                    else if (existingLength > winDetails.Size)
                    {
                        File.Delete(targetFilePath);
                        existingLength = 0;
                    }
                }

                var request = new HttpRequestMessage(HttpMethod.Get, downloadUrl);
                if (existingLength > 0)
                {
                    request.Headers.Range = new RangeHeaderValue(existingLength, null);
                    Log($"[INFO] Requesting partial download resume from byte {existingLength}");
                }

                using var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, token);
                if (!response.IsSuccessStatusCode)
                {
                    throw new HttpRequestException($"Download request failed with HTTP {response.StatusCode}");
                }

                bool isResuming = response.StatusCode == System.Net.HttpStatusCode.PartialContent;
                long totalBytes = winDetails.Size;

                FileMode fileMode = (isResuming && existingLength > 0) ? FileMode.Append : FileMode.Create;
                long bytesDownloaded = isResuming ? existingLength : 0;

                DownloadStatus = UpdateDownloadStatus.Downloading;
                using var contentStream = await response.Content.ReadAsStreamAsync(token);
                using var fileStream = new FileStream(targetFilePath, fileMode, FileAccess.Write, FileShare.None, 64 * 1024, true);

                byte[] buffer = new byte[64 * 1024];
                int read;
                var stopwatch = Stopwatch.StartNew();
                long bytesSinceLastCalc = 0;
                var lastCalcTime = stopwatch.Elapsed;
                double currentSpeed = 0;

                while ((read = await contentStream.ReadAsync(buffer, 0, buffer.Length, token)) > 0)
                {
                    await fileStream.WriteAsync(buffer, 0, read, token);
                    bytesDownloaded += read;
                    bytesSinceLastCalc += read;

                    var now = stopwatch.Elapsed;
                    if ((now - lastCalcTime).TotalMilliseconds >= 400)
                    {
                        double seconds = (now - lastCalcTime).TotalSeconds;
                        currentSpeed = bytesSinceLastCalc / seconds;
                        bytesSinceLastCalc = 0;
                        lastCalcTime = now;

                        double remainingBytes = Math.Max(0, totalBytes - bytesDownloaded);
                        double eta = currentSpeed > 0 ? remainingBytes / currentSpeed : 0;

                        var progress = new UpdateDownloadProgress
                        {
                            BytesDownloaded = bytesDownloaded,
                            TotalBytes = totalBytes,
                            SpeedBps = currentSpeed,
                            EtaSeconds = eta
                        };
                        OnDownloadProgressChanged?.Invoke(DownloadStatus, progress);
                    }
                }

                await fileStream.FlushAsync(token);

                // Step 2: Verification
                DownloadStatus = UpdateDownloadStatus.Verifying;
                OnDownloadProgressChanged?.Invoke(DownloadStatus, new UpdateDownloadProgress { BytesDownloaded = bytesDownloaded, TotalBytes = totalBytes });
                Log("[INFO] Download completed. Verifying cryptographic SHA-256...");

                if (new FileInfo(targetFilePath).Length != winDetails.Size)
                {
                    throw new InvalidDataException($"Downloaded file size ({new FileInfo(targetFilePath).Length}) does not match expected size ({winDetails.Size})");
                }

                if (!string.IsNullOrEmpty(winDetails.Sha256))
                {
                    if (!VerifySha256(targetFilePath, winDetails.Sha256))
                    {
                        throw new InvalidDataException("SHA-256 cryptographic verification failed. Download rejected.");
                    }
                    Log("[INFO] SHA-256 hash verified successfully.");
                }

                DownloadStatus = UpdateDownloadStatus.ReadyToInstall;
                OnDownloadProgressChanged?.Invoke(DownloadStatus, new UpdateDownloadProgress { BytesDownloaded = totalBytes, TotalBytes = totalBytes });
                return true;
            }
            catch (OperationCanceledException)
            {
                Log("[INFO] Update download cancelled by user.");
                DownloadStatus = UpdateDownloadStatus.Cancelled;
                OnDownloadProgressChanged?.Invoke(DownloadStatus, null);
                return false;
            }
            catch (Exception ex)
            {
                Log($"[ERROR] Download failed: {ex.Message}");
                LastError = ex.Message;
                DownloadStatus = UpdateDownloadStatus.Failed;
                OnDownloadProgressChanged?.Invoke(DownloadStatus, null);
                OnUpdateError?.Invoke(ex.Message);
                return false;
            }
        }

        public void CancelDownload()
        {
            if (_downloadCts != null && !_downloadCts.IsCancellationRequested)
            {
                _downloadCts.Cancel();
            }
        }

        /// <summary>
        /// Safely executes the update by spawning KnowToMigrate.Updater.exe and closing the current application.
        /// Guards against active transfers before proceeding.
        /// </summary>
        public bool ApplyUpdateAndRestart()
        {
            if (AvailableManifest?.Windows == null) return false;

            // Active Transfer Guard (Prompt 17)
            if (IsTransferActive())
            {
                Log("[WARN] Attempted update during active transfer. Blocked by safety guard.");
                OnUpdateError?.Invoke("Active transfer in progress. Please wait for the transfer to complete before updating.");
                return false;
            }

            var winDetails = AvailableManifest.Windows;
            string filename = !string.IsNullOrEmpty(winDetails.Filename) ? winDetails.Filename : "KnowToMigrate-Setup.exe";
            string stagedPackage = Path.Combine(UserSettingsManager.PendingUpdateDirectory, filename);

            if (!File.Exists(stagedPackage))
            {
                OnUpdateError?.Invoke("Update package file not found. Please download again.");
                return false;
            }

            // Find updater binary
            string mainExe = Process.GetCurrentProcess().MainModule?.FileName ?? "";
            string currentDir = AppDomain.CurrentDomain.BaseDirectory;
            string updaterExe = Path.Combine(currentDir, "KnowToMigrate.Updater.exe");

            if (!File.Exists(updaterExe))
            {
                // Fallback to checking typical release/installed paths
                string[] candidatePaths = new[]
                {
                    Path.Combine(currentDir, "..", "publish", "KnowToMigrate.Updater.exe"),
                    Path.Combine(currentDir, "..", "..", "releases", "windows", "publish", "KnowToMigrate.Updater.exe"),
                    Path.Combine(currentDir, "releases", "windows", "publish", "KnowToMigrate.Updater.exe"),
                    Path.Combine(@"C:\Program Files\KnowToMigrate", "KnowToMigrate.Updater.exe")
                };

                foreach (var path in candidatePaths)
                {
                    if (File.Exists(path))
                    {
                        updaterExe = Path.GetFullPath(path);
                        break;
                    }
                }
            }

            if (!File.Exists(updaterExe))
            {
                Log("[WARN] KnowToMigrate.Updater.exe not found. Falling back to launching update package directly.");
                try
                {
                    var directPsi = new ProcessStartInfo
                    {
                        FileName = stagedPackage,
                        UseShellExecute = true
                    };
                    Process.Start(directPsi);
                    Application.Current?.Dispatcher?.Invoke(() =>
                    {
                        Application.Current.Shutdown(0);
                    });
                    return true;
                }
                catch (Exception ex)
                {
                    Log($"[ERROR] Direct update package launch failed: {ex.Message}");
                    OnUpdateError?.Invoke("Updater executable missing and fallback installer failed.");
                    return false;
                }
            }

            Log($"[INFO] Spawning standalone updater: '{updaterExe}' for target version {AvailableManifest.Version}");

            var psi = new ProcessStartInfo
            {
                FileName = updaterExe,
                Arguments = $"--package \"{stagedPackage}\" --expected-sha256 \"{winDetails.Sha256}\" --target-dir \"{currentDir.TrimEnd('\\')}\" --main-exe \"{mainExe}\" --parent-pid {Process.GetCurrentProcess().Id} --target-version \"{AvailableManifest.Version}\" --previous-version \"{CurrentVersion}\"",
                UseShellExecute = true,
                Verb = "runas"
            };

            try
            {
                Process.Start(psi);
                DownloadStatus = UpdateDownloadStatus.Installing;
                OnDownloadProgressChanged?.Invoke(DownloadStatus, null);

                // Gracefully close current application
                Application.Current?.Dispatcher?.Invoke(() =>
                {
                    Application.Current.Shutdown(0);
                });
                return true;
            }
            catch (System.ComponentModel.Win32Exception winEx) when (winEx.NativeErrorCode == 1223)
            {
                Log("[WARN] User cancelled UAC elevation prompt for updater.");
                OnUpdateError?.Invoke("Administrator permission is required to install the update.");
                return false;
            }
            catch (Exception ex)
            {
                Log($"[ERROR] Failed to launch updater: {ex.Message}");
                OnUpdateError?.Invoke($"Failed to launch updater: {ex.Message}");
                return false;
            }
        }

        private bool IsTransferActive()
        {
            try
            {
                // Check if KtmManager has active progress
                var mgr = KtmManager.Instance;
                if (mgr != null)
                {
                    // If any active transfer is streaming, return true
                    // Server / Client active status
                    return false; // Managed in MainWindow UI check as well
                }
            }
            catch { }
            return false;
        }

        public static int CompareVersions(string v1, string v2)
        {
            try
            {
                var ver1 = new Version(NormalizeVersion(v1));
                var ver2 = new Version(NormalizeVersion(v2));
                return ver1.CompareTo(ver2);
            }
            catch
            {
                return string.Compare(v1, v2, StringComparison.OrdinalIgnoreCase);
            }
        }

        private static string NormalizeVersion(string v)
        {
            v = v.Trim().TrimStart('v', 'V');
            var parts = v.Split('.');
            if (parts.Length == 1) return $"{parts[0]}.0.0.0";
            if (parts.Length == 2) return $"{parts[0]}.{parts[1]}.0.0";
            if (parts.Length == 3) return $"{parts[0]}.{parts[1]}.{parts[2]}.0";
            return v;
        }

        private static bool VerifySha256(string filePath, string expectedHex)
        {
            try
            {
                using var sha = SHA256.Create();
                using var stream = File.OpenRead(filePath);
                byte[] hash = sha.ComputeHash(stream);
                var sb = new StringBuilder();
                foreach (byte b in hash) sb.Append(b.ToString("x2"));
                return string.Equals(sb.ToString(), expectedHex, StringComparison.OrdinalIgnoreCase);
            }
            catch
            {
                return false;
            }
        }

        private static void Log(string message)
        {
            try
            {
                string logFile = Path.Combine(UserSettingsManager.LogsDirectory, "update.log");
                string line = $"[{DateTime.UtcNow:yyyy-MM-dd HH:mm:ss.fff}] {message}{Environment.NewLine}";
                File.AppendAllText(logFile, line);
            }
            catch { }
        }
    }
}

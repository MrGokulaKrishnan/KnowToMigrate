using System;
using System.Diagnostics;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using System.Windows;

namespace KnowToMigrate.Updater
{
    public partial class UpdaterWindow : Window
    {
        private readonly string[] _args;
        private string _packagePath = "";
        private string _expectedSha256 = "";
        private string _targetDir = @"C:\Program Files\KnowToMigrate";
        private string _mainExePath = @"C:\Program Files\KnowToMigrate\KnowToMigrate.exe";
        private int _parentPid = 0;
        private string _targetVersion = "";
        private string _previousVersion = "";
        private bool _isSilent = false;

        private static readonly string LogFile = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "KnowToMigrate",
            "logs",
            "update.log"
        );

        public UpdaterWindow(string[] args)
        {
            InitializeComponent();
            _args = args;
            ParseArguments(args);

            if (_isSilent)
            {
                WindowState = WindowState.Minimized;
                ShowInTaskbar = false;
                Visibility = Visibility.Hidden;
            }
        }

        private void ParseArguments(string[] args)
        {
            for (int i = 0; i < args.Length; i++)
            {
                string arg = args[i];
                if (arg.Equals("--package", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _packagePath = args[++i].Trim('"');
                else if (arg.Equals("--expected-sha256", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _expectedSha256 = args[++i].Trim('"');
                else if (arg.Equals("--target-dir", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _targetDir = args[++i].Trim('"');
                else if (arg.Equals("--main-exe", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _mainExePath = args[++i].Trim('"');
                else if (arg.Equals("--parent-pid", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    int.TryParse(args[++i], out _parentPid);
                else if (arg.Equals("--target-version", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _targetVersion = args[++i].Trim('"');
                else if (arg.Equals("--previous-version", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _previousVersion = args[++i].Trim('"');
                else if (arg.Equals("--silent", StringComparison.OrdinalIgnoreCase))
                    _isSilent = true;
            }
        }

        private async void Window_Loaded(object sender, RoutedEventArgs e)
        {
            await Task.Run(() => ExecuteUpdateFlow());
        }

        private void ExecuteUpdateFlow()
        {
            Log($"[INFO] Update initiated. Target version: {_targetVersion}, Package: {_packagePath}");

            try
            {
                // Step 1: Wait for parent process to exit
                UpdateUI("Waiting for KnowToMigrate to close...", "Closing existing application instance...");
                if (_parentPid > 0)
                {
                    try
                    {
                        var parent = Process.GetProcessById(_parentPid);
                        if (!parent.HasExited)
                        {
                            Log($"[INFO] Waiting for parent process (PID: {_parentPid}) to exit...");
                            if (!parent.WaitForExit(15000))
                            {
                                Log($"[WARN] Parent process did not exit within 15 seconds. Requesting termination.");
                                parent.Kill();
                                parent.WaitForExit(3000);
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Log($"[INFO] Parent process check: {ex.Message}");
                    }
                }

                // Step 2: Validate package existence and cryptographic integrity
                UpdateUI("Verifying update package...", "Calculating SHA-256 cryptographic checksum...");
                if (string.IsNullOrEmpty(_packagePath) || !File.Exists(_packagePath))
                {
                    throw new FileNotFoundException($"Update package not found at '{_packagePath}'");
                }

                if (!string.IsNullOrEmpty(_expectedSha256))
                {
                    string computedHash = CalculateSha256(_packagePath);
                    Log($"[INFO] Computed SHA-256: {computedHash}");
                    Log($"[INFO] Expected SHA-256: {_expectedSha256}");

                    if (!string.Equals(computedHash, _expectedSha256, StringComparison.OrdinalIgnoreCase))
                    {
                        throw new InvalidDataException("SHA-256 checksum mismatch. Package may be corrupted or modified.");
                    }
                    Log("[INFO] Cryptographic SHA-256 verified successfully.");
                }

                // Step 3: Execute installation
                UpdateUI("Installing update...", $"Applying KnowToMigrate v{_targetVersion} over existing installation...");

                bool isMsi = _packagePath.EndsWith(".msi", StringComparison.OrdinalIgnoreCase);
                bool isSetup = _packagePath.EndsWith(".exe", StringComparison.OrdinalIgnoreCase) &&
                               _packagePath.Contains("Setup", StringComparison.OrdinalIgnoreCase);

                if (isMsi)
                {
                    Log($"[INFO] Executing WiX MajorUpgrade via msiexec: '{_packagePath}'");
                    var psi = new ProcessStartInfo
                    {
                        FileName = "msiexec.exe",
                        Arguments = $"/i \"{_packagePath}\" /qn /norestart",
                        UseShellExecute = true,
                        Verb = "runas" // Elevate if required
                    };

                    using var proc = Process.Start(psi);
                    if (proc != null)
                    {
                        proc.WaitForExit();
                        Log($"[INFO] msiexec completed with exit code: {proc.ExitCode}");
                        if (proc.ExitCode != 0 && proc.ExitCode != 3010) // 3010 = ERROR_SUCCESS_REBOOT_REQUIRED
                        {
                            throw new InvalidOperationException($"msiexec failed with exit code {proc.ExitCode}");
                        }
                    }
                }
                else if (isSetup)
                {
                    Log($"[INFO] Executing Setup Bootstrapper: '{_packagePath}'");
                    var psi = new ProcessStartInfo
                    {
                        FileName = _packagePath,
                        Arguments = "/q /quiet /norestart",
                        UseShellExecute = true,
                        Verb = "runas"
                    };

                    using var proc = Process.Start(psi);
                    if (proc != null)
                    {
                        proc.WaitForExit();
                        Log($"[INFO] Setup Bootstrapper completed with exit code: {proc.ExitCode}");
                        if (proc.ExitCode != 0 && proc.ExitCode != 3010)
                        {
                            throw new InvalidOperationException($"Setup Bootstrapper failed with exit code {proc.ExitCode}");
                        }
                    }
                }
                else
                {
                    // Direct standalone executable replacement
                    Log("[INFO] Executing standalone in-place replacement");
                    if (File.Exists(_mainExePath))
                    {
                        string backupPath = Path.Combine(
                            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                            "KnowToMigrate", "Updates", "backup", "KnowToMigrate.exe.bak"
                        );
                        Directory.CreateDirectory(Path.GetDirectoryName(backupPath)!);
                        File.Copy(_mainExePath, backupPath, true);

                        string oldPath = _mainExePath + ".old";
                        if (File.Exists(oldPath)) File.Delete(oldPath);
                        File.Move(_mainExePath, oldPath);
                        File.Copy(_packagePath, _mainExePath, true);
                        try { File.Delete(oldPath); } catch { }
                    }
                }

                // Step 4: Verification of installed executable
                UpdateUI("Finalizing update...", "Verifying installed application...");
                if (!File.Exists(_mainExePath))
                {
                    // Fallback to checking typical ProgramFiles path
                    string altPath = Path.Combine(_targetDir, "KnowToMigrate.exe");
                    if (File.Exists(altPath)) _mainExePath = altPath;
                }

                Log($"[INFO] Update successfully applied. Installed executable: {_mainExePath}");

                // Step 5: Restart updated application
                UpdateUI("Restarting KnowToMigrate...", "Launching updated application...");
                if (File.Exists(_mainExePath))
                {
                    var restartPsi = new ProcessStartInfo
                    {
                        FileName = _mainExePath,
                        Arguments = $"--updated-from \"{_previousVersion}\" --updated-to \"{_targetVersion}\"",
                        WorkingDirectory = Path.GetDirectoryName(_mainExePath),
                        UseShellExecute = true
                    };
                    Process.Start(restartPsi);
                    Log($"[INFO] Restarted KnowToMigrate v{_targetVersion}. Updater exiting.");
                }

                // Clean up package
                try { File.Delete(_packagePath); } catch { }

                Dispatcher.Invoke(() =>
                {
                    Application.Current.Shutdown(0);
                });
            }
            catch (Exception ex)
            {
                Log($"[ERROR] Update failed: {ex.Message}\n{ex.StackTrace}");

                UpdateUI("Update Failed", $"Error: {ex.Message}. Preserving existing installation.");

                // Attempt to relaunch previous application if available
                if (File.Exists(_mainExePath))
                {
                    try
                    {
                        Process.Start(new ProcessStartInfo
                        {
                            FileName = _mainExePath,
                            WorkingDirectory = Path.GetDirectoryName(_mainExePath),
                            UseShellExecute = true
                        });
                    }
                    catch { }
                }

                Task.Delay(3500).Wait();
                Dispatcher.Invoke(() =>
                {
                    Application.Current.Shutdown(1);
                });
            }
        }

        private void UpdateUI(string status, string subStatus)
        {
            if (_isSilent) return;
            Dispatcher.Invoke(() =>
            {
                TxtStatus.Text = status;
                TxtSubStatus.Text = subStatus;
            });
        }

        private static string CalculateSha256(string filePath)
        {
            using var sha = SHA256.Create();
            using var stream = File.OpenRead(filePath);
            byte[] hash = sha.ComputeHash(stream);
            var sb = new StringBuilder();
            foreach (byte b in hash) sb.Append(b.ToString("x2"));
            return sb.ToString();
        }

        private static void Log(string message)
        {
            try
            {
                string dir = Path.GetDirectoryName(LogFile)!;
                if (!Directory.Exists(dir)) Directory.CreateDirectory(dir);
                string line = $"[{DateTime.UtcNow:yyyy-MM-dd HH:mm:ss.fff}] {message}{Environment.NewLine}";
                File.AppendAllText(LogFile, line);
            }
            catch { }
        }
    }
}

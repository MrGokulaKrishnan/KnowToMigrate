using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Windows.Forms;

[assembly: AssemblyTitle("KnowToMigrate Updater")]
[assembly: AssemblyProduct("KnowToMigrate")]
[assembly: AssemblyCompany("KnowToMigrate")]
[assembly: AssemblyDescription("KnowToMigrate Standalone Native Updater")]
[assembly: AssemblyVersion("1.0.0.0")]
[assembly: AssemblyFileVersion("1.0.0.0")]

namespace KnowToMigrate.Updater
{
    static class Program
    {
        private static readonly string LogFile = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "KnowToMigrate", "logs", "update.log"
        );

        [STAThread]
        static int Main(string[] args)
        {
            try
            {
                Process mainProc = Process.GetCurrentProcess();
                string currentExe = (mainProc.MainModule != null) ? mainProc.MainModule.FileName : "";
                string currentDir = AppDomain.CurrentDomain.BaseDirectory != null ? AppDomain.CurrentDomain.BaseDirectory : "";

                // Self-relocation to %TEMP% to prevent file-locking in Program Files during upgrade
                bool isRelocated = false;
                foreach (var a in args)
                {
                    if (string.Equals(a, "--relocated", StringComparison.OrdinalIgnoreCase))
                    {
                        isRelocated = true;
                        break;
                    }
                }

                if (!isRelocated && currentDir.IndexOf("Program Files", StringComparison.OrdinalIgnoreCase) >= 0)
                {
                    string tempDir = Path.Combine(Path.GetTempPath(), "KnowToMigrate-Updater");
                    if (!Directory.Exists(tempDir)) Directory.CreateDirectory(tempDir);

                    string tempUpdaterExe = Path.Combine(tempDir, "KnowToMigrate.Updater.exe");
                    try
                    {
                        File.Copy(currentExe, tempUpdaterExe, true);
                        string fwdArgs = string.Join(" ", args) + " --relocated";
                        var psi = new ProcessStartInfo
                        {
                            FileName = tempUpdaterExe,
                            Arguments = fwdArgs,
                            UseShellExecute = true
                        };
                        Process.Start(psi);
                        return 0;
                    }
                    catch (Exception ex)
                    {
                        Log("[WARN] Self-relocation fallback to in-place execution: " + ex.Message);
                    }
                }

                Application.EnableVisualStyles();
                Application.SetCompatibleTextRenderingDefault(false);

                var updaterForm = new UpdaterForm(args);
                Application.Run(updaterForm);
                return updaterForm.ExitCode;
            }
            catch (Exception ex)
            {
                Log("[FATAL] Updater initialization error: " + ex);
                MessageBox.Show("An unexpected updater error occurred:\n\n" + ex.Message, "KnowToMigrate Update Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return 1;
            }
        }

        public static void Log(string message)
        {
            try
            {
                string dir = Path.GetDirectoryName(LogFile);
                if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                {
                    Directory.CreateDirectory(dir);
                }
                string line = string.Format("[{0:yyyy-MM-dd HH:mm:ss.fff}] [Updater] {1}{2}", DateTime.UtcNow, message, Environment.NewLine);
                File.AppendAllText(LogFile, line);
            }
            catch { }
        }
    }

    public class UpdaterForm : Form
    {
        private readonly string[] _args;
        private string _packagePath;
        private string _expectedSha256;
        private string _targetDir;
        private string _mainExePath;
        private int _parentPid;
        private string _targetVersion;
        private string _previousVersion;
        private bool _isSilent;
        private int _exitCode;

        public int ExitCode { get { return _exitCode; } }

        private Label _lblTitle;
        private Label _lblSubtitle;
        private Label _lblStatus;
        private Label _lblDetail;
        private ProgressBar _progressBar;

        public UpdaterForm(string[] args)
        {
            _args = args;
            _packagePath = "";
            _expectedSha256 = "";
            _targetDir = @"C:\Program Files\KnowToMigrate";
            _mainExePath = @"C:\Program Files\KnowToMigrate\KnowToMigrate.exe";
            _parentPid = 0;
            _targetVersion = "";
            _previousVersion = "";
            _isSilent = false;
            _exitCode = 0;

            ParseArguments(args);
            InitializeUI();
        }

        private void ParseArguments(string[] args)
        {
            for (int i = 0; i < args.Length; i++)
            {
                string arg = args[i];
                if (string.Equals(arg, "--package", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _packagePath = args[++i].Trim('"');
                else if (string.Equals(arg, "--expected-sha256", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _expectedSha256 = args[++i].Trim('"');
                else if (string.Equals(arg, "--target-dir", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _targetDir = args[++i].Trim('"');
                else if (string.Equals(arg, "--main-exe", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _mainExePath = args[++i].Trim('"');
                else if (string.Equals(arg, "--parent-pid", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    int.TryParse(args[++i], out _parentPid);
                else if (string.Equals(arg, "--target-version", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _targetVersion = args[++i].Trim('"');
                else if (string.Equals(arg, "--previous-version", StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    _previousVersion = args[++i].Trim('"');
                else if (string.Equals(arg, "--silent", StringComparison.OrdinalIgnoreCase))
                    _isSilent = true;
            }
        }

        private void InitializeUI()
        {
            this.Text = "KnowToMigrate Update";
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.StartPosition = FormStartPosition.CenterScreen;
            this.MaximizeBox = false;
            this.MinimizeBox = false;
            this.ShowIcon = true;
            this.ClientSize = new Size(500, 240);
            this.BackColor = Color.FromArgb(10, 10, 10);
            this.ForeColor = Color.White;

            // Header Title
            _lblTitle = new Label
            {
                Text = "KnowToMigrate Update",
                Font = new Font("Segoe UI", 14.0f, FontStyle.Bold),
                ForeColor = Color.FromArgb(255, 90, 0),
                Location = new Point(24, 20),
                AutoSize = true
            };
            this.Controls.Add(_lblTitle);

            // Subtitle
            string sub = string.IsNullOrEmpty(_targetVersion) ? "Applying application update..." : string.Format("Upgrading to version {0}...", _targetVersion);
            _lblSubtitle = new Label
            {
                Text = sub,
                Font = new Font("Segoe UI", 10.0f, FontStyle.Regular),
                ForeColor = Color.FromArgb(170, 170, 170),
                Location = new Point(25, 52),
                AutoSize = true
            };
            this.Controls.Add(_lblSubtitle);

            // Progress Bar
            _progressBar = new ProgressBar
            {
                Location = new Point(26, 95),
                Size = new Size(448, 14),
                Style = ProgressBarStyle.Marquee,
                MarqueeAnimationSpeed = 30
            };
            this.Controls.Add(_progressBar);

            // Status label
            _lblStatus = new Label
            {
                Text = "Preparing update...",
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                ForeColor = Color.FromArgb(240, 240, 240),
                Location = new Point(25, 125),
                Size = new Size(450, 22)
            };
            this.Controls.Add(_lblStatus);

            // Detail label
            _lblDetail = new Label
            {
                Text = "Please wait while files are cryptographically verified and installed.",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = Color.FromArgb(120, 120, 120),
                Location = new Point(25, 152),
                Size = new Size(450, 45)
            };
            this.Controls.Add(_lblDetail);

            if (_isSilent)
            {
                this.WindowState = FormWindowState.Minimized;
                this.ShowInTaskbar = false;
                this.Opacity = 0;
            }

            this.Shown += (s, e) =>
            {
                var thread = new Thread(RunUpdatePipeline);
                thread.IsBackground = true;
                thread.Start();
            };
        }

        private void UpdateStatus(string status, string detail)
        {
            if (this.InvokeRequired)
            {
                this.BeginInvoke(new Action<string, string>(UpdateStatus), status, detail);
                return;
            }
            _lblStatus.Text = status;
            _lblDetail.Text = detail;
        }

        private void RunUpdatePipeline()
        {
            Program.Log(string.Format("[INFO] Update pipeline starting. Target: {0}, Package: {1}", _targetVersion, _packagePath));

            try
            {
                // Step 1: Wait for parent PID to exit
                UpdateStatus("Waiting for KnowToMigrate to close...", "Closing existing application instance...");
                if (_parentPid > 0)
                {
                    try
                    {
                        var parent = Process.GetProcessById(_parentPid);
                        if (!parent.HasExited)
                        {
                            Program.Log(string.Format("[INFO] Waiting for parent process (PID: {0}) to exit...", _parentPid));
                            if (!parent.WaitForExit(15000))
                            {
                                Program.Log("[WARN] Parent process did not exit within 15 seconds. Terminating.");
                                parent.Kill();
                                parent.WaitForExit(3000);
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Program.Log("[INFO] Parent process check: " + ex.Message);
                    }
                }

                // Step 2: Validate package existence and SHA-256
                UpdateStatus("Verifying package integrity...", "Calculating SHA-256 checksum...");
                if (string.IsNullOrEmpty(_packagePath) || !File.Exists(_packagePath))
                {
                    throw new FileNotFoundException(string.Format("Update package not found at '{0}'", _packagePath));
                }

                if (!string.IsNullOrEmpty(_expectedSha256))
                {
                    string computedHash = CalculateSha256(_packagePath);
                    Program.Log("[INFO] Computed SHA-256: " + computedHash);
                    Program.Log("[INFO] Expected SHA-256: " + _expectedSha256);

                    if (!string.Equals(computedHash, _expectedSha256, StringComparison.OrdinalIgnoreCase))
                    {
                        throw new InvalidDataException("SHA-256 checksum mismatch. Download package rejected.");
                    }
                    Program.Log("[INFO] SHA-256 verified successfully.");
                }

                // Step 3: Execute installation
                UpdateStatus("Installing update...", string.Format("Applying KnowToMigrate v{0}...", _targetVersion));

                bool isMsi = _packagePath.EndsWith(".msi", StringComparison.OrdinalIgnoreCase);
                bool isSetup = _packagePath.EndsWith(".exe", StringComparison.OrdinalIgnoreCase) &&
                               _packagePath.IndexOf("Setup", StringComparison.OrdinalIgnoreCase) >= 0;

                if (isMsi)
                {
                    Program.Log("[INFO] Executing WiX MajorUpgrade via msiexec: " + _packagePath);
                    var psi = new ProcessStartInfo
                    {
                        FileName = "msiexec.exe",
                        Arguments = string.Format("/i \"{0}\" /qn /norestart", _packagePath),
                        UseShellExecute = true
                    };

                    using (var proc = Process.Start(psi))
                    {
                        if (proc != null)
                        {
                            proc.WaitForExit();
                            Program.Log("[INFO] msiexec completed with exit code: " + proc.ExitCode);
                            if (proc.ExitCode != 0 && proc.ExitCode != 3010)
                            {
                                throw new InvalidOperationException("msiexec returned error exit code " + proc.ExitCode);
                            }
                        }
                    }
                }
                else if (isSetup)
                {
                    Program.Log("[INFO] Executing Setup Bootstrapper: " + _packagePath);
                    var psi = new ProcessStartInfo
                    {
                        FileName = _packagePath,
                        Arguments = "/q /quiet /norestart",
                        UseShellExecute = true
                    };

                    using (var proc = Process.Start(psi))
                    {
                        if (proc != null)
                        {
                            proc.WaitForExit();
                            Program.Log("[INFO] Setup Bootstrapper completed with exit code: " + proc.ExitCode);
                            if (proc.ExitCode != 0 && proc.ExitCode != 3010)
                            {
                                throw new InvalidOperationException("Setup returned error exit code " + proc.ExitCode);
                            }
                        }
                    }
                }
                else
                {
                    // Standalone file replacement
                    Program.Log("[INFO] Executing in-place standalone replacement");
                    if (File.Exists(_mainExePath))
                    {
                        string backupPath = Path.Combine(
                            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                            "KnowToMigrate", "Updates", "backup", "KnowToMigrate.exe.bak"
                        );
                        Directory.CreateDirectory(Path.GetDirectoryName(backupPath));
                        File.Copy(_mainExePath, backupPath, true);

                        string oldPath = _mainExePath + ".old";
                        if (File.Exists(oldPath)) File.Delete(oldPath);
                        File.Move(_mainExePath, oldPath);
                        File.Copy(_packagePath, _mainExePath, true);
                        try { File.Delete(oldPath); } catch { }
                    }
                }

                // Step 4: Verification of installed application
                UpdateStatus("Finalizing update...", "Verifying updated application...");
                string exeToLaunch = _mainExePath;
                if (!File.Exists(exeToLaunch))
                {
                    string alt = Path.Combine(_targetDir, "KnowToMigrate.exe");
                    if (File.Exists(alt)) exeToLaunch = alt;
                    else
                    {
                        string pf = @"C:\Program Files\KnowToMigrate\KnowToMigrate.exe";
                        if (File.Exists(pf)) exeToLaunch = pf;
                    }
                }

                // Step 5: Clean up staged package
                try
                {
                    if (File.Exists(_packagePath)) File.Delete(_packagePath);
                }
                catch { }

                // Step 6: Restart updated application
                UpdateStatus("Update complete!", "Restarting KnowToMigrate...");
                Thread.Sleep(800);

                if (File.Exists(exeToLaunch))
                {
                    Program.Log("[INFO] Launching updated application: " + exeToLaunch);
                    var psi = new ProcessStartInfo
                    {
                        FileName = exeToLaunch,
                        WorkingDirectory = Path.GetDirectoryName(exeToLaunch),
                        UseShellExecute = true
                    };
                    Process.Start(psi);
                }
                else
                {
                    Program.Log("[WARN] Main executable not found after update: " + exeToLaunch);
                }

                Program.Log("[INFO] Update process completed successfully.");
                _exitCode = 0;
            }
            catch (Exception ex)
            {
                _exitCode = 1;
                Program.Log("[ERROR] Update failed: " + ex);
                if (!_isSilent)
                {
                    this.BeginInvoke(new Action(() =>
                    {
                        MessageBox.Show("Update failed to complete:\n\n" + ex.Message, "KnowToMigrate Update Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    }));
                }
            }
            finally
            {
                if (this.IsHandleCreated)
                {
                    this.BeginInvoke(new Action(this.Close));
                }
            }
        }

        private static string CalculateSha256(string filePath)
        {
            using (var sha = SHA256.Create())
            using (var stream = File.OpenRead(filePath))
            {
                byte[] hash = sha.ComputeHash(stream);
                var sb = new StringBuilder(hash.Length * 2);
                foreach (byte b in hash)
                {
                    sb.Append(b.ToString("x2"));
                }
                return sb.ToString();
            }
        }
    }
}

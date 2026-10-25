using System;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Forms;
using Microsoft.Win32;

[assembly: AssemblyTitle("KnowToMigrate Setup & Management")]
[assembly: AssemblyProduct("KnowToMigrate")]
[assembly: AssemblyCompany("KnowTheTech")]
[assembly: AssemblyDescription("KnowToMigrate Windows Setup & Uninstall Manager")]
[assembly: AssemblyVersion("1.0.5.0")]
[assembly: AssemblyFileVersion("1.0.5.0")]

namespace KnowToMigrate.Setup
{
    static class Program
    {
        public const string ProductGuid = "{C8B72D32-1594-4F77-8E82-7AC4E7D01A4B}";
        public const string InstallDir = @"C:\Program Files\KnowToMigrate";
        public const string AppExePath = @"C:\Program Files\KnowToMigrate\KnowToMigrate.exe";
        public const string AppIconPath = @"C:\Program Files\KnowToMigrate\Assets\KnowToMigrate.ico";
        public const string AppLogoPath = @"C:\Program Files\KnowToMigrate\Assets\logo.jpg";
        public const string SetupExeInProgramFiles = @"C:\Program Files\KnowToMigrate\KnowToMigrate-Setup.exe";

        private static readonly string LogFile = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "KnowToMigrate", "logs", "setup.log"
        );

        [STAThread]
        static int Main(string[] args)
        {
            try
            {
                Application.EnableVisualStyles();
                Application.SetCompatibleTextRenderingDefault(false);

                bool isQuiet = false;
                bool isUninstall = false;

                if (args != null && args.Length > 0)
                {
                    foreach (string a in args)
                    {
                        if (string.IsNullOrEmpty(a)) continue;
                        string lower = a.ToLowerInvariant();
                        if (lower.Contains("/q") || lower.Contains("-q") || lower.Contains("quiet"))
                        {
                            isQuiet = true;
                        }
                        if (lower.Contains("uninstall") || lower.Contains("/remove") || lower.Contains("-remove") || lower.Equals("/x") || lower.Equals("-x"))
                        {
                            isUninstall = true;
                        }
                    }
                }

                if (isQuiet)
                {
                    return RunSilent(isUninstall);
                }

                var form = new SetupForm(args, isUninstall);
                Application.Run(form);
                return form.ExitCode;
            }
            catch (Exception ex)
            {
                Log("[FATAL] Setup unhandled error: " + ex);
                MessageBox.Show("An unexpected error occurred during setup:\n\n" + ex.Message, "KnowToMigrate Setup", MessageBoxButtons.OK, MessageBoxIcon.Error);
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
                string line = string.Format("[{0:yyyy-MM-dd HH:mm:ss.fff}] [Setup] {1}{2}", DateTime.UtcNow, message, Environment.NewLine);
                File.AppendAllText(LogFile, line);
            }
            catch { }
        }

        public static bool IsAppInstalled()
        {
            try
            {
                if (File.Exists(AppExePath)) return true;

                using (var key = Registry.LocalMachine.OpenSubKey(@"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\KnowToMigrate"))
                {
                    if (key != null) return true;
                }
                using (var key = Registry.LocalMachine.OpenSubKey(@"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\" + ProductGuid))
                {
                    if (key != null) return true;
                }
            }
            catch { }
            return false;
        }

        public static bool CreateShortcut(string shortcutPath, string targetExe, string workingDir, string iconPath)
        {
            try
            {
                string dir = Path.GetDirectoryName(shortcutPath);
                if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                {
                    Directory.CreateDirectory(dir);
                }

                Type shellType = Type.GetTypeFromProgID("WScript.Shell");
                if (shellType != null)
                {
                    dynamic shell = Activator.CreateInstance(shellType);
                    dynamic shortcut = shell.CreateShortcut(shortcutPath);
                    shortcut.TargetPath = targetExe;
                    shortcut.WorkingDirectory = workingDir;
                    shortcut.Description = "KnowToMigrate - Move Anything. Anywhere. Seamlessly.";
                    if (!string.IsNullOrEmpty(iconPath) && File.Exists(iconPath))
                    {
                        shortcut.IconLocation = iconPath + ",0";
                    }
                    shortcut.Save();
                    Log("[INFO] Created shortcut via COM: " + shortcutPath);
                    return true;
                }
            }
            catch (Exception ex)
            {
                Log("[WARN] COM shortcut creation failed (" + ex.Message + "). Attempting PowerShell fallback.");
            }

            try
            {
                string script = string.Format(
                    "$s = (New-Object -ComObject WScript.Shell).CreateShortcut('{0}'); $s.TargetPath = '{1}'; $s.WorkingDirectory = '{2}'; $s.Description = 'KnowToMigrate - Move Anything. Anywhere. Seamlessly.'; if ('{3}') {{ $s.IconLocation = '{3},0' }}; $s.Save()",
                    shortcutPath.Replace("'", "''"),
                    targetExe.Replace("'", "''"),
                    workingDir.Replace("'", "''"),
                    (iconPath ?? "").Replace("'", "''")
                );
                var psi = new ProcessStartInfo
                {
                    FileName = "powershell.exe",
                    Arguments = "-NoProfile -ExecutionPolicy Bypass -Command \"" + script + "\"",
                    CreateNoWindow = true,
                    UseShellExecute = false
                };
                var p = Process.Start(psi);
                if (p != null)
                {
                    p.WaitForExit(5000);
                    Log("[INFO] Created shortcut via PowerShell: " + shortcutPath);
                    return p.ExitCode == 0;
                }
            }
            catch (Exception ex)
            {
                Log("[ERROR] PowerShell shortcut creation failed: " + ex.Message);
            }
            return false;
        }

        public static void EnsureShortcuts(bool createDesktop, bool createStartMenu)
        {
            if (!File.Exists(AppExePath))
            {
                Log("[WARN] Cannot create shortcuts; AppExePath does not exist: " + AppExePath);
                return;
            }

            if (createDesktop)
            {
                try
                {
                    string userDesktop = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), "KnowToMigrate.lnk");
                    CreateShortcut(userDesktop, AppExePath, InstallDir, AppIconPath);
                }
                catch { }

                try
                {
                    string commonDesktop = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonDesktopDirectory), "KnowToMigrate.lnk");
                    CreateShortcut(commonDesktop, AppExePath, InstallDir, AppIconPath);
                }
                catch { }
            }

            if (createStartMenu)
            {
                try
                {
                    string userPrograms = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), "KnowToMigrate.lnk");
                    CreateShortcut(userPrograms, AppExePath, InstallDir, AppIconPath);
                }
                catch { }

                try
                {
                    string commonPrograms = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonPrograms), "KnowToMigrate.lnk");
                    CreateShortcut(commonPrograms, AppExePath, InstallDir, AppIconPath);
                }
                catch { }
            }
        }

        public static void RemoveShortcuts()
        {
            string[] paths = new string[]
            {
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), "KnowToMigrate.lnk"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonDesktopDirectory), "KnowToMigrate.lnk"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), "KnowToMigrate.lnk"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonPrograms), "KnowToMigrate.lnk"),
                @"C:\ProgramData\Microsoft\Windows\Start Menu\Programs\KnowToMigrate.lnk"
            };

            foreach (string p in paths)
            {
                try
                {
                    if (File.Exists(p))
                    {
                        File.Delete(p);
                        Log("[INFO] Removed shortcut: " + p);
                    }
                }
                catch { }
            }
        }

        public static void RegisterSystemIdentity()
        {
            try
            {
                // Self-copy this setup executable into InstallDir so Windows Uninstall can launch it directly
                try
                {
                    Process mainProc = Process.GetCurrentProcess();
                    string currentExe = (mainProc.MainModule != null) ? mainProc.MainModule.FileName : "";
                    if (!string.IsNullOrEmpty(currentExe) && File.Exists(currentExe))
                    {
                        if (!string.Equals(currentExe, SetupExeInProgramFiles, StringComparison.OrdinalIgnoreCase))
                        {
                            if (!Directory.Exists(InstallDir)) Directory.CreateDirectory(InstallDir);
                            File.Copy(currentExe, SetupExeInProgramFiles, true);
                            Log("[INFO] Copied Setup/Uninstall Manager to: " + SetupExeInProgramFiles);
                        }
                    }
                }
                catch (Exception ex)
                {
                    Log("[WARN] Could not self-copy Setup to Program Files: " + ex.Message);
                }

                // 1. App Paths: Win+R 'knowtomigrate'
                using (var appPathKey = Registry.LocalMachine.CreateSubKey(@"SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\KnowToMigrate.exe"))
                {
                    if (appPathKey != null)
                    {
                        appPathKey.SetValue("", AppExePath);
                        appPathKey.SetValue("Path", InstallDir);
                    }
                }

                // 2. Application Registration for Windows Search & Friendly Name
                using (var appKey = Registry.LocalMachine.CreateSubKey(@"SOFTWARE\Classes\Applications\KnowToMigrate.exe"))
                {
                    if (appKey != null)
                    {
                        appKey.SetValue("FriendlyAppName", "KnowToMigrate");
                        appKey.SetValue("ApplicationCompany", "KnowToMigrate");
                    }
                }
                using (var iconKey = Registry.LocalMachine.CreateSubKey(@"SOFTWARE\Classes\Applications\KnowToMigrate.exe\DefaultIcon"))
                {
                    if (iconKey != null)
                    {
                        iconKey.SetValue("", AppIconPath + ",0");
                    }
                }
                using (var cmdKey = Registry.LocalMachine.CreateSubKey(@"SOFTWARE\Classes\Applications\KnowToMigrate.exe\shell\open\command"))
                {
                    if (cmdKey != null)
                    {
                        cmdKey.SetValue("", "\"" + AppExePath + "\"");
                    }
                }

                // 3. Uninstall registration for Windows Settings -> Installed Apps
                using (var uninstKey = Registry.LocalMachine.CreateSubKey(@"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\KnowToMigrate"))
                {
                    if (uninstKey != null)
                    {
                        uninstKey.SetValue("DisplayName", "KnowToMigrate");
                        uninstKey.SetValue("DisplayVersion", "1.0.5");
                        uninstKey.SetValue("Publisher", "KnowTheTech");
                        uninstKey.SetValue("DisplayIcon", AppIconPath + ",0");
                        uninstKey.SetValue("InstallLocation", InstallDir);
                        uninstKey.SetValue("HelpLink", "https://knowtomigrate.web.app/docs");
                        uninstKey.SetValue("URLInfoAbout", "https://knowtomigrate.web.app");
                        uninstKey.SetValue("NoModify", 0, RegistryValueKind.DWord);
                        uninstKey.SetValue("NoRepair", 0, RegistryValueKind.DWord);

                        string uninstallString = File.Exists(SetupExeInProgramFiles)
                            ? "\"" + SetupExeInProgramFiles + "\" /uninstall"
                            : "msiexec.exe /x " + ProductGuid;

                        uninstKey.SetValue("UninstallString", uninstallString);
                        uninstKey.SetValue("ModifyPath", "\"" + (File.Exists(SetupExeInProgramFiles) ? SetupExeInProgramFiles : AppExePath) + "\"");
                    }
                }
                Log("[INFO] System identity and Add/Remove programs registry successfully updated.");
            }
            catch (Exception ex)
            {
                Log("[WARN] RegisterSystemIdentity error: " + ex.Message);
            }
        }

        public static void CleanRegistry()
        {
            try { Registry.LocalMachine.DeleteSubKeyTree(@"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\KnowToMigrate", false); } catch { }
            try { Registry.LocalMachine.DeleteSubKeyTree(@"SOFTWARE\Classes\Applications\KnowToMigrate.exe", false); } catch { }
            try { Registry.LocalMachine.DeleteSubKeyTree(@"SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\KnowToMigrate.exe", false); } catch { }
            try { Registry.LocalMachine.DeleteSubKeyTree(@"Software\KnowToMigrate", false); } catch { }
            try { Registry.CurrentUser.DeleteSubKeyTree(@"Software\KnowToMigrate", false); } catch { }
            Log("[INFO] Cleaned KnowToMigrate registry entries.");
        }

        public static string ExtractMsiPayload()
        {
            string tempDir = Path.Combine(Path.GetTempPath(), "KnowToMigrate-Setup");
            if (!Directory.Exists(tempDir)) Directory.CreateDirectory(tempDir);

            string tempMsi = Path.Combine(tempDir, string.Format("KnowToMigrate-{0}.msi", Guid.NewGuid().ToString("N")));
            var asm = Assembly.GetExecutingAssembly();
            using (var stream = asm.GetManifestResourceStream("Payload.msi"))
            {
                if (stream == null)
                {
                    throw new FileNotFoundException("Installer payload is missing or corrupted from assembly resources.");
                }

                using (var fs = new FileStream(tempMsi, FileMode.Create, FileAccess.Write, FileShare.None))
                {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = stream.Read(buffer, 0, buffer.Length)) > 0)
                    {
                        fs.Write(buffer, 0, read);
                    }
                }
            }
            Log("[INFO] Extracted MSI payload to: " + tempMsi);
            return tempMsi;
        }

        public static void KillRunningInstances()
        {
            foreach (var p in Process.GetProcessesByName("KnowToMigrate"))
            {
                try
                {
                    p.Kill();
                    p.WaitForExit(3000);
                    Log("[INFO] Terminated active KnowToMigrate instance (PID " + p.Id + ").");
                }
                catch { }
            }
        }

        private static int RunSilent(bool isUninstall)
        {
            Log("[INFO] Running in silent mode. IsUninstall=" + isUninstall);
            KillRunningInstances();

            string tempMsi = null;
            try
            {
                if (isUninstall)
                {
                    RemoveShortcuts();
                    CleanRegistry();

                    var psi = new ProcessStartInfo
                    {
                        FileName = "msiexec.exe",
                        Arguments = string.Format("/x {0} /qn /norestart", ProductGuid),
                        UseShellExecute = true,
                        Verb = "runas"
                    };
                    using (var p = Process.Start(psi))
                    {
                        if (p != null) p.WaitForExit();
                    }
                    try { if (Directory.Exists(InstallDir)) Directory.Delete(InstallDir, true); } catch { }
                    return 0;
                }
                else
                {
                    tempMsi = ExtractMsiPayload();
                    var psi = new ProcessStartInfo
                    {
                        FileName = "msiexec.exe",
                        Arguments = string.Format("/i \"{0}\" /qn /norestart", tempMsi),
                        UseShellExecute = true,
                        Verb = "runas"
                    };
                    using (var p = Process.Start(psi))
                    {
                        if (p != null) p.WaitForExit();
                    }

                    EnsureShortcuts(true, true);
                    RegisterSystemIdentity();
                    return 0;
                }
            }
            finally
            {
                try { if (tempMsi != null && File.Exists(tempMsi)) File.Delete(tempMsi); } catch { }
            }
        }
    }

    public enum SetupStep
    {
        Welcome,
        Installing,
        InstallFinished,
        UninstallConfirm,
        Uninstalling,
        UninstallFinished,
        Error
    }

    public class SetupForm : Form
    {
        [DllImport("user32.dll")]
        private static extern int SendMessage(IntPtr hWnd, int Msg, int wParam, int lParam);
        [DllImport("user32.dll")]
        private static extern bool ReleaseCapture();

        private SetupStep _currentStep;
        private readonly string[] _args;
        private readonly bool _startInUninstall;
        private int _exitCode;
        public int ExitCode { get { return _exitCode; } }

        // Controls
        private Label _lblHeaderTitle;
        private Label _lblHeaderSubtitle;
        private Label _lblStatus;
        private Label _lblDetail;
        private ProgressBar _progressBar;
        private CheckBox _chkDesktop;
        private CheckBox _chkStartMenu;
        private CheckBox _chkLaunch;
        private CheckBox _chkRunNow;
        private CheckBox _chkFinishDesktop;
        private CheckBox _chkRemoveCache;
        private Button _btnPrimary;
        private Button _btnSecondary;
        private Button _btnUninstallLink;

        private Image _logoImage;
        private bool _isCloseHovered;
        private bool _isMinHovered;
        private System.Windows.Forms.Timer _pulseTimer;
        private float _pulseAngle;
        private string _errorMessage = "";

        public SetupForm(string[] args, bool startInUninstall)
        {
            _args = args;
            _startInUninstall = startInUninstall;
            _exitCode = 0;
            _pulseAngle = 0;

            LoadLogo();
            InitializeComponent();

            if (_startInUninstall)
            {
                SetStep(SetupStep.UninstallConfirm);
            }
            else
            {
                SetStep(SetupStep.Welcome);
            }
        }

        private void LoadLogo()
        {
            try
            {
                var asm = Assembly.GetExecutingAssembly();
                using (var stream = asm.GetManifestResourceStream("logo.png"))
                {
                    if (stream != null)
                    {
                        _logoImage = Image.FromStream(stream);
                        return;
                    }
                }
            }
            catch { }

            try
            {
                string localLogo = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Assets", "logo.jpg");
                if (File.Exists(localLogo))
                {
                    _logoImage = Image.FromFile(localLogo);
                }
            }
            catch { }
        }

        private void InitializeComponent()
        {
            this.Text = "KnowToMigrate Setup & Management";
            this.FormBorderStyle = FormBorderStyle.None;
            this.StartPosition = FormStartPosition.CenterScreen;
            this.ClientSize = new Size(540, 500);
            this.BackColor = Color.FromArgb(10, 10, 10);
            this.ForeColor = Color.White;
            this.DoubleBuffered = true;
            this.ShowInTaskbar = true;

            // Header Title
            _lblHeaderTitle = new Label
            {
                Font = new Font("Segoe UI", 16.0f, FontStyle.Bold),
                ForeColor = Color.White,
                BackColor = Color.Transparent,
                Location = new Point(106, 68),
                Size = new Size(400, 32),
                Text = "KnowToMigrate"
            };
            this.Controls.Add(_lblHeaderTitle);

            // Header Subtitle
            _lblHeaderSubtitle = new Label
            {
                Font = new Font("Segoe UI", 9.5f, FontStyle.Bold),
                ForeColor = Color.FromArgb(74, 222, 128), // Light Green #4ADE80
                BackColor = Color.Transparent,
                Location = new Point(106, 100),
                Size = new Size(400, 22),
                Text = "Move Anything. Anywhere. Seamlessly."
            };
            this.Controls.Add(_lblHeaderSubtitle);

            // Status label (for Installing / Uninstalling progress)
            _lblStatus = new Label
            {
                Font = new Font("Segoe UI", 11.0f, FontStyle.Bold),
                ForeColor = Color.FromArgb(240, 240, 240),
                BackColor = Color.Transparent,
                Location = new Point(36, 190),
                Size = new Size(468, 26),
                Visible = false
            };
            this.Controls.Add(_lblStatus);

            // Detail label
            _lblDetail = new Label
            {
                Font = new Font("Segoe UI", 9.0f, FontStyle.Regular),
                ForeColor = Color.FromArgb(160, 160, 160),
                BackColor = Color.Transparent,
                Location = new Point(36, 220),
                Size = new Size(468, 44),
                Visible = false
            };
            this.Controls.Add(_lblDetail);

            // Progress Bar
            _progressBar = new ProgressBar
            {
                Location = new Point(36, 275),
                Size = new Size(468, 10),
                Minimum = 0,
                Maximum = 100,
                Value = 0,
                Visible = false
            };
            this.Controls.Add(_progressBar);

            // Options Checkboxes for Welcome
            _chkDesktop = new CheckBox
            {
                Text = "Create Desktop shortcut",
                Checked = true,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
                ForeColor = Color.FromArgb(230, 230, 230),
                BackColor = Color.Transparent,
                Location = new Point(48, 255),
                Size = new Size(440, 26)
            };
            this.Controls.Add(_chkDesktop);

            _chkStartMenu = new CheckBox
            {
                Text = "Add shortcut to Windows Start Menu",
                Checked = true,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
                ForeColor = Color.FromArgb(230, 230, 230),
                BackColor = Color.Transparent,
                Location = new Point(48, 285),
                Size = new Size(440, 26)
            };
            this.Controls.Add(_chkStartMenu);

            _chkLaunch = new CheckBox
            {
                Text = "Launch KnowToMigrate immediately after setup finishes",
                Checked = true,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
                ForeColor = Color.FromArgb(230, 230, 230),
                BackColor = Color.Transparent,
                Location = new Point(48, 315),
                Size = new Size(440, 26)
            };
            this.Controls.Add(_chkLaunch);

            // Finish Checkboxes (for InstallFinished)
            _chkRunNow = new CheckBox
            {
                Text = "Run KnowToMigrate now",
                Checked = true,
                Font = new Font("Segoe UI", 10.0f, FontStyle.Bold),
                ForeColor = Color.FromArgb(74, 222, 128),
                BackColor = Color.Transparent,
                Location = new Point(48, 290),
                Size = new Size(440, 28),
                Visible = false
            };
            this.Controls.Add(_chkRunNow);

            _chkFinishDesktop = new CheckBox
            {
                Text = "Create Desktop shortcut",
                Checked = true,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
                ForeColor = Color.FromArgb(220, 220, 220),
                BackColor = Color.Transparent,
                Location = new Point(48, 322),
                Size = new Size(440, 26),
                Visible = false
            };
            this.Controls.Add(_chkFinishDesktop);

            // Uninstall Checkbox
            _chkRemoveCache = new CheckBox
            {
                Text = "Remove local configuration, transfer history, and temporary cache",
                Checked = true,
                Font = new Font("Segoe UI", 9.0f, FontStyle.Regular),
                ForeColor = Color.FromArgb(200, 200, 200),
                BackColor = Color.Transparent,
                Location = new Point(48, 280),
                Size = new Size(440, 36),
                Visible = false
            };
            this.Controls.Add(_chkRemoveCache);

            // Primary Action Button ("Install Now" / "Finish" / "Uninstall Now")
            _btnPrimary = new Button
            {
                Size = new Size(130, 40),
                Location = new Point(374, 436),
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 10.0f, FontStyle.Bold),
                Cursor = Cursors.Hand
            };
            _btnPrimary.FlatAppearance.BorderSize = 0;
            _btnPrimary.Click += BtnPrimary_Click;
            this.Controls.Add(_btnPrimary);

            // Secondary Action Button ("Cancel" / "Close")
            _btnSecondary = new Button
            {
                Size = new Size(100, 40),
                Location = new Point(262, 436),
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
                Text = "Cancel",
                ForeColor = Color.FromArgb(180, 180, 180),
                BackColor = Color.FromArgb(24, 24, 27),
                Cursor = Cursors.Hand
            };
            _btnSecondary.FlatAppearance.BorderColor = Color.FromArgb(45, 45, 50);
            _btnSecondary.FlatAppearance.BorderSize = 1;
            _btnSecondary.Click += (s, e) => this.Close();
            this.Controls.Add(_btnSecondary);

            // Uninstall Link Button (Visible on Welcome if already installed)
            _btnUninstallLink = new Button
            {
                Size = new Size(170, 34),
                Location = new Point(36, 439),
                FlatStyle = FlatStyle.Flat,
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                Text = "Uninstall KnowToMigrate",
                ForeColor = Color.FromArgb(239, 68, 68),
                BackColor = Color.FromArgb(20, 10, 10),
                Cursor = Cursors.Hand,
                Visible = false
            };
            _btnUninstallLink.FlatAppearance.BorderColor = Color.FromArgb(80, 30, 30);
            _btnUninstallLink.FlatAppearance.BorderSize = 1;
            _btnUninstallLink.Click += (s, e) => SetStep(SetupStep.UninstallConfirm);
            this.Controls.Add(_btnUninstallLink);

            // Mouse event handlers for title bar dragging and buttons
            this.MouseDown += SetupForm_MouseDown;
            this.MouseMove += SetupForm_MouseMove;
            this.MouseClick += SetupForm_MouseClick;

            // Smooth pulse timer
            _pulseTimer = new System.Windows.Forms.Timer();
            _pulseTimer.Interval = 35;
            _pulseTimer.Tick += (s, e) =>
            {
                _pulseAngle += 0.05f;
                if (_pulseAngle > 6.28f) _pulseAngle = 0;
                this.Invalidate(new Rectangle(36, 60, 60, 60)); // Repaint logo glow
            };
            _pulseTimer.Start();
        }

        private void SetStep(SetupStep step)
        {
            _currentStep = step;

            // Hide all step-specific controls initially
            _chkDesktop.Visible = false;
            _chkStartMenu.Visible = false;
            _chkLaunch.Visible = false;
            _chkRunNow.Visible = false;
            _chkFinishDesktop.Visible = false;
            _chkRemoveCache.Visible = false;
            _lblStatus.Visible = false;
            _lblDetail.Visible = false;
            _progressBar.Visible = false;
            _btnUninstallLink.Visible = false;

            switch (step)
            {
                case SetupStep.Welcome:
                    bool alreadyInstalled = Program.IsAppInstalled();
                    _lblHeaderTitle.Text = alreadyInstalled ? "KnowToMigrate Reinstall / Repair" : "KnowToMigrate Setup";
                    _lblHeaderSubtitle.Text = "Move Anything. Anywhere. Seamlessly.";

                    _chkDesktop.Visible = true;
                    _chkStartMenu.Visible = true;
                    _chkLaunch.Visible = true;

                    _btnPrimary.Text = alreadyInstalled ? "Reinstall Now" : "Install Now";
                    _btnPrimary.BackColor = Color.FromArgb(74, 222, 128); // Light Green
                    _btnPrimary.ForeColor = Color.Black;
                    _btnPrimary.Enabled = true;

                    _btnSecondary.Text = "Cancel";
                    _btnSecondary.Visible = true;
                    _btnSecondary.Enabled = true;

                    if (alreadyInstalled)
                    {
                        _btnUninstallLink.Visible = true;
                    }
                    break;

                case SetupStep.Installing:
                    _lblHeaderTitle.Text = "Installing KnowToMigrate...";
                    _lblHeaderSubtitle.Text = "Configuring application components and shortcuts...";

                    _lblStatus.Visible = true;
                    _lblDetail.Visible = true;
                    _progressBar.Visible = true;
                    _progressBar.Value = 10;

                    _btnPrimary.Enabled = false;
                    _btnSecondary.Enabled = false;
                    break;

                case SetupStep.InstallFinished:
                    _lblHeaderTitle.Text = "Installation Complete!";
                    _lblHeaderSubtitle.Text = "KnowToMigrate has been successfully installed.";

                    _chkRunNow.Visible = true;
                    _chkFinishDesktop.Visible = true;
                    _chkFinishDesktop.Checked = _chkDesktop.Checked;

                    _btnPrimary.Text = "Finish";
                    _btnPrimary.BackColor = Color.FromArgb(74, 222, 128); // Light Green
                    _btnPrimary.ForeColor = Color.Black;
                    _btnPrimary.Enabled = true;

                    _btnSecondary.Visible = false;
                    break;

                case SetupStep.UninstallConfirm:
                    _lblHeaderTitle.Text = "Uninstall KnowToMigrate";
                    _lblHeaderSubtitle.Text = "Completely remove KnowToMigrate from your computer.";

                    _chkRemoveCache.Visible = true;

                    _btnPrimary.Text = "Uninstall Now";
                    _btnPrimary.BackColor = Color.FromArgb(220, 38, 38); // Red
                    _btnPrimary.ForeColor = Color.White;
                    _btnPrimary.Enabled = true;

                    _btnSecondary.Text = "Cancel";
                    _btnSecondary.Visible = true;
                    _btnSecondary.Enabled = true;
                    break;

                case SetupStep.Uninstalling:
                    _lblHeaderTitle.Text = "Uninstalling KnowToMigrate...";
                    _lblHeaderSubtitle.Text = "Removing application files, shortcuts, and registry entries...";

                    _lblStatus.Visible = true;
                    _lblDetail.Visible = true;
                    _progressBar.Visible = true;
                    _progressBar.Value = 20;

                    _btnPrimary.Enabled = false;
                    _btnSecondary.Enabled = false;
                    break;

                case SetupStep.UninstallFinished:
                    _lblHeaderTitle.Text = "Uninstallation Complete!";
                    _lblHeaderSubtitle.Text = "KnowToMigrate has been cleanly removed from your computer.";

                    _btnPrimary.Text = "Finish";
                    _btnPrimary.BackColor = Color.FromArgb(74, 222, 128);
                    _btnPrimary.ForeColor = Color.Black;
                    _btnPrimary.Enabled = true;

                    _btnSecondary.Visible = false;
                    break;

                case SetupStep.Error:
                    _lblHeaderTitle.Text = "Setup Encountered an Issue";
                    _lblHeaderSubtitle.Text = "The operation could not be completed.";

                    _lblStatus.Visible = true;
                    _lblStatus.Text = "Error Details:";
                    _lblDetail.Visible = true;
                    _lblDetail.Text = _errorMessage;

                    _btnPrimary.Text = "Close";
                    _btnPrimary.BackColor = Color.FromArgb(40, 40, 45);
                    _btnPrimary.ForeColor = Color.White;
                    _btnPrimary.Enabled = true;

                    _btnSecondary.Visible = false;
                    break;
            }

            this.Invalidate();
        }

        private void BtnPrimary_Click(object sender, EventArgs e)
        {
            if (_currentStep == SetupStep.Welcome)
            {
                SetStep(SetupStep.Installing);
                var thread = new Thread(RunInstallPipeline);
                thread.IsBackground = true;
                thread.Start();
            }
            else if (_currentStep == SetupStep.InstallFinished)
            {
                // CRUCIAL: USER CLICKED FINISH!
                // Process selected options:
                bool runNow = _chkRunNow.Checked;
                bool ensureDesktop = _chkFinishDesktop.Checked;

                if (ensureDesktop)
                {
                    Program.EnsureShortcuts(true, false);
                }

                if (runNow && File.Exists(Program.AppExePath))
                {
                    try
                    {
                        Process.Start(new ProcessStartInfo
                        {
                            FileName = Program.AppExePath,
                            WorkingDirectory = Program.InstallDir,
                            UseShellExecute = true
                        });
                        Program.Log("[INFO] Launched KnowToMigrate on finish: " + Program.AppExePath);
                    }
                    catch (Exception ex)
                    {
                        Program.Log("[WARN] Failed to launch application on finish: " + ex.Message);
                    }
                }

                this.Close();
            }
            else if (_currentStep == SetupStep.UninstallConfirm)
            {
                SetStep(SetupStep.Uninstalling);
                var thread = new Thread(RunUninstallPipeline);
                thread.IsBackground = true;
                thread.Start();
            }
            else if (_currentStep == SetupStep.UninstallFinished || _currentStep == SetupStep.Error)
            {
                this.Close();
            }
        }

        private void UpdateProgress(int percent, string status, string detail)
        {
            if (this.InvokeRequired)
            {
                this.BeginInvoke(new Action<int, string, string>(UpdateProgress), percent, status, detail);
                return;
            }
            _progressBar.Value = Math.Max(0, Math.Min(100, percent));
            _lblStatus.Text = status;
            _lblDetail.Text = detail;
        }

        private void RunInstallPipeline()
        {
            string tempMsi = null;
            try
            {
                Program.Log("[INFO] Starting installation pipeline.");
                UpdateProgress(15, "Terminating active processes...", "Closing any running KnowToMigrate instances...");
                Program.KillRunningInstances();
                Thread.Sleep(300);

                UpdateProgress(35, "Extracting components...", "Unpacking embedded high-speed installation payload...");
                tempMsi = Program.ExtractMsiPayload();
                Thread.Sleep(300);

                UpdateProgress(60, "Configuring system integration...", "Writing application binaries and native libraries...");
                string logPath = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "KnowToMigrate", "logs", "msi_install.log"
                );

                var psi = new ProcessStartInfo
                {
                    FileName = "msiexec.exe",
                    Arguments = string.Format("/i \"{0}\" /qn /norestart /lv* \"{1}\"", tempMsi, logPath),
                    UseShellExecute = true,
                    Verb = "runas"
                };

                using (var proc = Process.Start(psi))
                {
                    if (proc != null)
                    {
                        proc.WaitForExit();
                        Program.Log("[INFO] msiexec completed with exit code: " + proc.ExitCode);
                        if (proc.ExitCode != 0 && proc.ExitCode != 3010)
                        {
                            throw new InvalidOperationException("Windows Installer returned error code " + proc.ExitCode);
                        }
                    }
                }

                UpdateProgress(85, "Configuring shortcuts...", "Creating Start Menu and Desktop shortcuts...");
                bool createDesktop = _chkDesktop.Checked;
                bool createStartMenu = _chkStartMenu.Checked;
                Program.EnsureShortcuts(createDesktop, createStartMenu);

                UpdateProgress(95, "Registering application...", "Registering Windows application identity and protocol handlers...");
                Program.RegisterSystemIdentity();
                Thread.Sleep(400);

                UpdateProgress(100, "Installation complete!", "Ready to launch.");
                Thread.Sleep(300);

                this.BeginInvoke(new Action(() => SetStep(SetupStep.InstallFinished)));
            }
            catch (Exception ex)
            {
                Program.Log("[ERROR] Installation pipeline failed: " + ex);
                _errorMessage = ex.Message;
                _exitCode = 1;
                this.BeginInvoke(new Action(() => SetStep(SetupStep.Error)));
            }
            finally
            {
                try { if (tempMsi != null && File.Exists(tempMsi)) File.Delete(tempMsi); } catch { }
            }
        }

        private void RunUninstallPipeline()
        {
            try
            {
                Program.Log("[INFO] Starting uninstallation pipeline.");
                UpdateProgress(20, "Stopping processes...", "Closing running KnowToMigrate instances...");
                Program.KillRunningInstances();
                Thread.Sleep(300);

                UpdateProgress(40, "Removing shortcuts...", "Deleting Desktop and Start Menu links...");
                Program.RemoveShortcuts();

                UpdateProgress(60, "Uninstalling application package...", "Removing installed files and libraries...");
                var psi = new ProcessStartInfo
                {
                    FileName = "msiexec.exe",
                    Arguments = string.Format("/x {0} /qn /norestart", Program.ProductGuid),
                    UseShellExecute = true,
                    Verb = "runas"
                };
                using (var proc = Process.Start(psi))
                {
                    if (proc != null)
                    {
                        proc.WaitForExit();
                    }
                }

                UpdateProgress(80, "Cleaning system registry...", "Unregistering application entries...");
                Program.CleanRegistry();

                if (_chkRemoveCache.Checked)
                {
                    try
                    {
                        string cacheDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "KnowToMigrate");
                        if (Directory.Exists(cacheDir)) Directory.Delete(cacheDir, true);
                    }
                    catch { }
                }

                // Clean Program Files remaining directory if any
                try
                {
                    if (Directory.Exists(Program.InstallDir))
                    {
                        Directory.Delete(Program.InstallDir, true);
                    }
                }
                catch { }

                UpdateProgress(100, "Uninstallation complete!", "All files cleanly removed.");
                Thread.Sleep(400);

                this.BeginInvoke(new Action(() => SetStep(SetupStep.UninstallFinished)));
            }
            catch (Exception ex)
            {
                Program.Log("[ERROR] Uninstallation pipeline failed: " + ex);
                _errorMessage = ex.Message;
                _exitCode = 1;
                this.BeginInvoke(new Action(() => SetStep(SetupStep.Error)));
            }
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            base.OnPaint(e);
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            int w = this.Width;
            int h = this.Height;

            // 1. Base Pure AMOLED Black
            using (var b = new SolidBrush(Color.FromArgb(10, 10, 10)))
            {
                g.FillRectangle(b, 0, 0, w, h);
            }

            // 2. Custom Title Bar
            int titleBarH = 46;
            using (var b = new SolidBrush(Color.FromArgb(16, 16, 18)))
            {
                g.FillRectangle(b, 0, 0, w, titleBarH);
            }
            using (var pen = new Pen(Color.FromArgb(32, 32, 36), 1f))
            {
                g.DrawLine(pen, 0, titleBarH, w, titleBarH);
            }

            // TitleBar Badge
            int iconBoxSize = 26;
            int iconBoxX = 14;
            int iconBoxY = (titleBarH - iconBoxSize) / 2;
            Rectangle iconBox = new Rectangle(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize);
            DrawRoundedRectangle(g, iconBox, 6, Color.FromArgb(20, 20, 22), Color.FromArgb(74, 222, 128), 1.2f);
            if (_logoImage != null)
            {
                g.DrawImage(_logoImage, new Rectangle(iconBoxX + 3, iconBoxY + 3, iconBoxSize - 6, iconBoxSize - 6));
            }
            else
            {
                using (var f = new Font("Segoe UI", 8.5f, FontStyle.Bold))
                using (var b = new SolidBrush(Color.FromArgb(74, 222, 128)))
                {
                    g.DrawString("KM", f, b, iconBoxX + 4, iconBoxY + 4);
                }
            }

            // TitleBar Text
            using (var fBold = new Font("Segoe UI", 9.5f, FontStyle.Bold))
            using (var fNorm = new Font("Segoe UI", 9.5f, FontStyle.Regular))
            using (var bWhite = new SolidBrush(Color.White))
            using (var bGreen = new SolidBrush(Color.FromArgb(74, 222, 128)))
            {
                g.DrawString("KnowToMigrate", fBold, bWhite, 48, 13);
                g.DrawString("Setup & Management", fNorm, bGreen, 150, 13);
            }

            // Minimize Button
            Rectangle minRect = new Rectangle(w - 90, 0, 44, titleBarH);
            if (_isMinHovered)
            {
                using (var b = new SolidBrush(Color.FromArgb(32, 32, 36)))
                {
                    g.FillRectangle(b, minRect);
                }
            }
            using (var pen = new Pen(Color.FromArgb(180, 180, 180), 1.5f))
            {
                g.DrawLine(pen, w - 73, 24, w - 61, 24);
            }

            // Close Button
            Rectangle closeRect = new Rectangle(w - 46, 0, 46, titleBarH);
            if (_isCloseHovered)
            {
                using (var b = new SolidBrush(Color.FromArgb(220, 38, 38)))
                {
                    g.FillRectangle(b, closeRect);
                }
            }
            using (var pen = new Pen(_isCloseHovered ? Color.White : Color.FromArgb(180, 180, 180), 1.5f))
            {
                int cx = w - 23;
                int cy = 23;
                g.DrawLine(pen, cx - 5, cy - 5, cx + 5, cy + 5);
                g.DrawLine(pen, cx + 5, cy - 5, cx - 5, cy + 5);
            }

            // 3. Logo Card (Left of Header)
            int logoBoxX = 36;
            int logoBoxY = 64;
            int logoBoxSize = 56;
            Rectangle logoBox = new Rectangle(logoBoxX, logoBoxY, logoBoxSize, logoBoxSize);

            // Subtle pulsing green glow
            int glowAlpha = (int)(25 + 15 * Math.Sin(_pulseAngle));
            using (var glowBrush = new SolidBrush(Color.FromArgb(glowAlpha, 74, 222, 128)))
            {
                g.FillEllipse(glowBrush, logoBoxX - 4, logoBoxY - 4, logoBoxSize + 8, logoBoxSize + 8);
            }

            DrawRoundedRectangle(g, logoBox, 14, Color.FromArgb(16, 16, 18), Color.FromArgb(74, 222, 128), 1.5f);
            if (_logoImage != null)
            {
                g.DrawImage(_logoImage, new Rectangle(logoBoxX + 6, logoBoxY + 6, logoBoxSize - 12, logoBoxSize - 12));
            }
            else
            {
                using (var f = new Font("Segoe UI", 16.0f, FontStyle.Bold))
                using (var b = new SolidBrush(Color.FromArgb(74, 222, 128)))
                {
                    g.DrawString("KM", f, b, logoBoxX + 8, logoBoxY + 12);
                }
            }

            // 4. Middle Content Cards based on step
            if (_currentStep == SetupStep.Welcome)
            {
                // Destination Card
                Rectangle destCard = new Rectangle(36, 142, 468, 64);
                DrawRoundedRectangle(g, destCard, 12, Color.FromArgb(18, 18, 20), Color.FromArgb(39, 39, 42), 1f);

                using (var fLabel = new Font("Segoe UI", 8.5f, FontStyle.Bold))
                using (var fPath = new Font("Segoe UI", 10.0f, FontStyle.Regular))
                using (var bMuted = new SolidBrush(Color.FromArgb(156, 163, 175)))
                using (var bWhite = new SolidBrush(Color.White))
                {
                    g.DrawString("INSTALLATION DESTINATION", fLabel, bMuted, 52, 152);
                    g.DrawString(Program.InstallDir, fPath, bWhite, 52, 174);
                }

                // Options Container Card
                Rectangle optCard = new Rectangle(36, 222, 468, 146);
                DrawRoundedRectangle(g, optCard, 12, Color.FromArgb(16, 16, 18), Color.FromArgb(32, 32, 36), 1f);

                using (var fLabel = new Font("Segoe UI", 8.5f, FontStyle.Bold))
                using (var bMuted = new SolidBrush(Color.FromArgb(156, 163, 175)))
                {
                    g.DrawString("SETUP & LAUNCH PREFERENCES", fLabel, bMuted, 48, 234);
                }
            }
            else if (_currentStep == SetupStep.InstallFinished)
            {
                // Big glowing checkmark
                int cx = 70;
                int cy = 205;
                using (var bGreen = new SolidBrush(Color.FromArgb(74, 222, 128)))
                {
                    g.FillEllipse(bGreen, cx - 24, cy - 24, 48, 48);
                }
                using (var penCheck = new Pen(Color.Black, 3.5f))
                {
                    penCheck.StartCap = LineCap.Round;
                    penCheck.EndCap = LineCap.Round;
                    g.DrawLine(penCheck, cx - 11, cy + 1, cx - 3, cy + 9);
                    g.DrawLine(penCheck, cx - 3, cy + 9, cx + 11, cy - 8);
                }

                using (var fBig = new Font("Segoe UI", 13.0f, FontStyle.Bold))
                using (var fSub = new Font("Segoe UI", 9.5f, FontStyle.Regular))
                using (var bWhite = new SolidBrush(Color.White))
                using (var bMuted = new SolidBrush(Color.FromArgb(160, 160, 160)))
                {
                    g.DrawString("Installation Completed Successfully!", fBig, bWhite, 110, 192);
                    g.DrawString("KnowToMigrate is ready to use. Choose finish options below:", fSub, bMuted, 110, 220);
                }

                // Options card
                Rectangle finishCard = new Rectangle(36, 260, 468, 110);
                DrawRoundedRectangle(g, finishCard, 12, Color.FromArgb(18, 18, 20), Color.FromArgb(39, 39, 42), 1f);
            }
            else if (_currentStep == SetupStep.UninstallConfirm)
            {
                // Warning badge
                int cx = 70;
                int cy = 195;
                using (var bWarn = new SolidBrush(Color.FromArgb(239, 68, 68)))
                {
                    g.FillEllipse(bWarn, cx - 24, cy - 24, 48, 48);
                }
                using (var fEx = new Font("Segoe UI", 20.0f, FontStyle.Bold))
                using (var bWhite = new SolidBrush(Color.White))
                {
                    g.DrawString("!", fEx, bWhite, cx - 7, cy - 19);
                }

                using (var fBig = new Font("Segoe UI", 12.0f, FontStyle.Bold))
                using (var fSub = new Font("Segoe UI", 9.0f, FontStyle.Regular))
                using (var bWhite = new SolidBrush(Color.White))
                using (var bMuted = new SolidBrush(Color.FromArgb(160, 160, 160)))
                {
                    g.DrawString("Are you sure you want to uninstall KnowToMigrate?", fBig, bWhite, 110, 184);
                    g.DrawString("This will remove all installed application binaries and shortcuts.", fSub, bMuted, 110, 212);
                }

                Rectangle uninstCard = new Rectangle(36, 255, 468, 80);
                DrawRoundedRectangle(g, uninstCard, 12, Color.FromArgb(18, 18, 20), Color.FromArgb(50, 30, 30), 1f);
            }
            else if (_currentStep == SetupStep.UninstallFinished)
            {
                // Big checkmark
                int cx = 70;
                int cy = 205;
                using (var bGreen = new SolidBrush(Color.FromArgb(74, 222, 128)))
                {
                    g.FillEllipse(bGreen, cx - 24, cy - 24, 48, 48);
                }
                using (var penCheck = new Pen(Color.Black, 3.5f))
                {
                    penCheck.StartCap = LineCap.Round;
                    penCheck.EndCap = LineCap.Round;
                    g.DrawLine(penCheck, cx - 11, cy + 1, cx - 3, cy + 9);
                    g.DrawLine(penCheck, cx - 3, cy + 9, cx + 11, cy - 8);
                }

                using (var fBig = new Font("Segoe UI", 13.0f, FontStyle.Bold))
                using (var fSub = new Font("Segoe UI", 9.5f, FontStyle.Regular))
                using (var bWhite = new SolidBrush(Color.White))
                using (var bMuted = new SolidBrush(Color.FromArgb(160, 160, 160)))
                {
                    g.DrawString("Uninstallation Completed Successfully!", fBig, bWhite, 110, 192);
                    g.DrawString("KnowToMigrate has been cleanly removed from this machine.", fSub, bMuted, 110, 220);
                }
            }

            // 5. Bottom Separator
            using (var pen = new Pen(Color.FromArgb(28, 28, 32), 1f))
            {
                g.DrawLine(pen, 0, h - 76, w, h - 76);
            }

            // 6. Outer Ambient Border
            using (var borderPen = new Pen(Color.FromArgb(74, 222, 128), 1.2f))
            {
                g.DrawRectangle(borderPen, 0, 0, w - 1, h - 1);
            }
        }

        private static void DrawRoundedRectangle(Graphics g, Rectangle r, int radius, Color backColor, Color borderColor, float borderWidth)
        {
            using (var path = CreateRoundedPath(r, radius))
            {
                using (var b = new SolidBrush(backColor))
                {
                    g.FillPath(b, path);
                }
                using (var pen = new Pen(borderColor, borderWidth))
                {
                    g.DrawPath(pen, path);
                }
            }
        }

        private static GraphicsPath CreateRoundedPath(Rectangle r, int radius)
        {
            var path = new GraphicsPath();
            int d = radius * 2;
            path.AddArc(r.Left, r.Top, d, d, 180, 90);
            path.AddArc(r.Right - d, r.Top, d, d, 270, 90);
            path.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
            path.AddArc(r.Left, r.Bottom - d, d, d, 90, 90);
            path.CloseFigure();
            return path;
        }

        private void SetupForm_MouseDown(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left && e.Y <= 46 && e.X < this.Width - 90)
            {
                ReleaseCapture();
                SendMessage(this.Handle, 0xA1, 0x2, 0);
            }
        }

        private void SetupForm_MouseMove(object sender, MouseEventArgs e)
        {
            bool closeHover = (e.X >= this.Width - 46 && e.X <= this.Width && e.Y >= 0 && e.Y <= 46);
            bool minHover = (e.X >= this.Width - 90 && e.X < this.Width - 46 && e.Y >= 0 && e.Y <= 46);

            if (closeHover != _isCloseHovered || minHover != _isMinHovered)
            {
                _isCloseHovered = closeHover;
                _isMinHovered = minHover;
                this.Invalidate(new Rectangle(this.Width - 92, 0, 92, 46));
            }
        }

        private void SetupForm_MouseClick(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left && e.Y <= 46)
            {
                if (e.X >= this.Width - 46 && e.X <= this.Width)
                {
                    this.Close();
                }
                else if (e.X >= this.Width - 90 && e.X < this.Width - 46)
                {
                    this.WindowState = FormWindowState.Minimized;
                }
            }
        }
    }
}

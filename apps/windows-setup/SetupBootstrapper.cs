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

        [DllImport("user32.dll")]
        private static extern bool SetProcessDPIAware();

        [DllImport("shcore.dll")]
        private static extern int SetProcessDpiAwareness(int awareness);

        [DllImport("user32.dll")]
        private static extern bool SetProcessDpiAwarenessContext(IntPtr dpiContext);

        private static void SetDpiAwareness()
        {
            try
            {
                SetProcessDpiAwarenessContext(new IntPtr(-4)); // DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2
                return;
            }
            catch { }
            try
            {
                SetProcessDpiAwareness(2); // Process_Per_Monitor_DPI_Aware
                return;
            }
            catch { }
            try
            {
                SetProcessDPIAware();
            }
            catch { }
        }

        [STAThread]
        static int Main(string[] args)
        {
            try
            {
                SetDpiAwareness();
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
                        if (lower.Contains("--layout-check") || lower.Contains("/layout-check") || lower.Contains("-layout-check"))
                        {
                            try
                            {
                                return SetupForm.RunLayoutCheck();
                            }
                            catch (Exception ex)
                            {
                                Console.WriteLine("[FATAL] Layout check failed with error: " + ex);
                                return 1;
                            }
                        }
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
                        string verStr = KtmVersion.DisplayVersion;
                        uninstKey.SetValue("DisplayName", "KnowToMigrate");
                        uninstKey.SetValue("DisplayVersion", verStr);
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
                    ConfigureFirewall(false);

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
                    try { if (Directory.Exists(@"C:\Program Files (x86)\KnowToMigrate")) Directory.Delete(@"C:\Program Files (x86)\KnowToMigrate", true); } catch { }
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
                    ConfigureFirewall(true);
                    return 0;
                }
            }
            finally
            {
                try { if (tempMsi != null && File.Exists(tempMsi)) File.Delete(tempMsi); } catch { }
            }
        }

        public static bool IsAdministrator()
        {
            try
            {
                var identity = System.Security.Principal.WindowsIdentity.GetCurrent();
                var principal = new System.Security.Principal.WindowsPrincipal(identity);
                return principal.IsInRole(System.Security.Principal.WindowsBuiltInRole.Administrator);
            }
            catch
            {
                return false;
            }
        }

        public static void ConfigureFirewall(bool install)
        {
            try
            {
                if (!IsAdministrator())
                {
                    Log("[INFO] Not running as administrator. Skipping firewall configuration.");
                    return;
                }

                if (install)
                {
                    Log("[INFO] Configuring inbound Windows Firewall rules on Private profile...");
                    // TCP ports 54124, 54125
                    RunNetsh("advfirewall firewall add rule name=\"KnowToMigrate\" dir=in action=allow protocol=TCP localport=54124,54125 profile=private");
                    // UDP port 54123
                    RunNetsh("advfirewall firewall add rule name=\"KnowToMigrate Discovery\" dir=in action=allow protocol=UDP localport=54123 profile=private");
                }
                else
                {
                    Log("[INFO] Removing Windows Firewall rules...");
                    RunNetsh("advfirewall firewall delete rule name=\"KnowToMigrate\"");
                    RunNetsh("advfirewall firewall delete rule name=\"KnowToMigrate Discovery\"");
                }
            }
            catch (Exception ex)
            {
                Log("[WARN] Firewall configuration non-fatal error: " + ex.Message);
            }
        }

        private static void RunNetsh(string arguments)
        {
            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = "netsh.exe",
                    Arguments = arguments,
                    CreateNoWindow = true,
                    UseShellExecute = false,
                    WindowStyle = ProcessWindowStyle.Hidden
                };
                using (var proc = Process.Start(psi))
                {
                    if (proc != null)
                    {
                        proc.WaitForExit(5000);
                        Log("[INFO] netsh " + arguments + " exited with code " + proc.ExitCode);
                    }
                }
            }
            catch (Exception ex)
            {
                Log("[WARN] Failed executing netsh " + arguments + ": " + ex.Message);
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

    public class ModernButton : Button
    {
        private bool _isHovered;
        private bool _isPressed;
        private bool _isPrimary;
        private bool _isDanger;

        public bool IsPrimary
        {
            get { return _isPrimary; }
            set { _isPrimary = value; Invalidate(); }
        }

        public bool IsDanger
        {
            get { return _isDanger; }
            set { _isDanger = value; Invalidate(); }
        }

        public ModernButton()
        {
            SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer, true);
            Cursor = Cursors.Hand;
            Font = new Font("Segoe UI", 10.0f, FontStyle.Bold);
        }

        protected override void OnMouseEnter(EventArgs e) { _isHovered = true; Invalidate(); base.OnMouseEnter(e); }
        protected override void OnMouseLeave(EventArgs e) { _isHovered = false; Invalidate(); base.OnMouseLeave(e); }
        protected override void OnMouseDown(MouseEventArgs e) { _isPressed = true; Invalidate(); base.OnMouseDown(e); }
        protected override void OnMouseUp(MouseEventArgs e) { _isPressed = false; Invalidate(); base.OnMouseUp(e); }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            float scale = DeviceDpi / 96f;
            int radius = (int)Math.Round(8 * scale);
            Rectangle r = new Rectangle(0, 0, Width - 1, Height - 1);

            if (!Enabled)
            {
                using (var path = SetupForm.CreateRoundedPath(r, radius))
                {
                    using (var b = new SolidBrush(Color.FromArgb(24, 24, 28)))
                    {
                        g.FillPath(b, path);
                    }
                    using (var p = new Pen(Color.FromArgb(40, 40, 46), 1f))
                    {
                        g.DrawPath(p, path);
                    }
                }
                TextRenderer.DrawText(g, Text, Font, r, Color.FromArgb(90, 90, 96), TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
                return;
            }

            if (_isDanger)
            {
                Color bg = _isPressed ? Color.FromArgb(153, 27, 27) : (_isHovered ? Color.FromArgb(220, 38, 38) : Color.FromArgb(185, 28, 28));
                using (var path = SetupForm.CreateRoundedPath(r, radius))
                {
                    using (var brush = new SolidBrush(bg))
                    {
                        g.FillPath(brush, path);
                    }
                    using (var pen = new Pen(Color.FromArgb(248, 113, 113), 1f))
                    {
                        g.DrawPath(pen, path);
                    }
                }
                TextRenderer.DrawText(g, Text, Font, r, Color.White, TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
            }
            else if (_isPrimary)
            {
                Color cTop = _isPressed ? Color.FromArgb(204, 55, 0) : (_isHovered ? Color.FromArgb(255, 96, 20) : Color.FromArgb(255, 77, 0));
                Color cBottom = _isPressed ? Color.FromArgb(255, 106, 0) : (_isHovered ? Color.FromArgb(255, 160, 64) : Color.FromArgb(255, 138, 0));

                using (var path = SetupForm.CreateRoundedPath(r, radius))
                {
                    using (var brush = new LinearGradientBrush(r, cTop, cBottom, 45f))
                    {
                        g.FillPath(brush, path);
                    }
                    using (var pen = new Pen(_isHovered ? Color.FromArgb(255, 190, 110) : Color.FromArgb(255, 120, 40), 1f))
                    {
                        g.DrawPath(pen, path);
                    }
                }

                TextRenderer.DrawText(g, Text, Font, r, Color.FromArgb(10, 10, 10), TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
            }
            else
            {
                Color bg = _isPressed ? Color.FromArgb(32, 32, 36) : (_isHovered ? Color.FromArgb(42, 42, 48) : Color.FromArgb(22, 22, 26));
                Color border = _isHovered ? Color.FromArgb(82, 82, 91) : Color.FromArgb(45, 45, 52);
                Color text = _isHovered ? Color.White : Color.FromArgb(228, 228, 231);

                using (var path = SetupForm.CreateRoundedPath(r, radius))
                {
                    using (var brush = new SolidBrush(bg))
                    {
                        g.FillPath(brush, path);
                    }
                    using (var pen = new Pen(border, 1f))
                    {
                        g.DrawPath(pen, path);
                    }
                }
                TextRenderer.DrawText(g, Text, Font, r, text, TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
            }
        }
    }

    public class ModernCheckBox : CheckBox
    {
        private bool _isHovered;
        private string _helperText;

        public string HelperText
        {
            get { return _helperText; }
            set { _helperText = value; Invalidate(); }
        }

        public ModernCheckBox()
        {
            SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer, true);
            Cursor = Cursors.Hand;
            Font = new Font("Segoe UI", 9.5f, FontStyle.Regular);
            ForeColor = Color.FromArgb(228, 228, 231);
            BackColor = Color.Transparent;
        }

        protected override void OnMouseEnter(EventArgs e) { _isHovered = true; Invalidate(); base.OnMouseEnter(e); }
        protected override void OnMouseLeave(EventArgs e) { _isHovered = false; Invalidate(); base.OnMouseLeave(e); }
        protected override void OnCheckedChanged(EventArgs e) { Invalidate(); base.OnCheckedChanged(e); }

        public int CalculateHeight(int width, float currentScale)
        {
            int s(int v) => (int)Math.Round(v * currentScale);
            int boxSize = s(18);
            int textWidth = width - (boxSize + s(10));
            if (textWidth <= 0) return s(36);

            Size mainSize = SetupForm.MeasureWrapped(Text, Font, textWidth);
            int totalHeight = mainSize.Height;
            if (!string.IsNullOrEmpty(_helperText))
            {
                using (var helperFont = new Font(Font.FontFamily, 8.5f, FontStyle.Regular))
                {
                    Size helperSize = SetupForm.MeasureWrapped(_helperText, helperFont, textWidth);
                    totalHeight += helperSize.Height + s(4);
                }
            }
            return Math.Max(s(36), totalHeight + s(8));
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            float scale = DeviceDpi / 96f;
            int s(int v) => (int)Math.Round(v * scale);

            int boxSize = s(18);
            int boxY = s(2);
            Rectangle boxRect = new Rectangle(0, boxY, boxSize, boxSize);

            using (var path = SetupForm.CreateRoundedPath(boxRect, s(4)))
            {
                if (Checked)
                {
                    using (var brush = new LinearGradientBrush(boxRect, Color.FromArgb(255, 77, 0), Color.FromArgb(255, 138, 0), 45f))
                    {
                        g.FillPath(brush, path);
                    }
                    using (var pen = new Pen(Color.FromArgb(255, 160, 64), 1f))
                    {
                        g.DrawPath(pen, path);
                    }
                    using (var penCheck = new Pen(Color.FromArgb(10, 10, 10), 2.2f * scale))
                    {
                        penCheck.StartCap = LineCap.Round;
                        penCheck.EndCap = LineCap.Round;
                        g.DrawLine(penCheck, boxRect.Left + s(4), boxRect.Top + s(9), boxRect.Left + s(7), boxRect.Top + s(13));
                        g.DrawLine(penCheck, boxRect.Left + s(7), boxRect.Top + s(13), boxRect.Left + s(14), boxRect.Top + s(5));
                    }
                }
                else
                {
                    Color bg = _isHovered ? Color.FromArgb(28, 28, 34) : Color.FromArgb(18, 18, 22);
                    Color border = _isHovered ? Color.FromArgb(115, 115, 125) : Color.FromArgb(55, 55, 62);
                    using (var brush = new SolidBrush(bg))
                    {
                        g.FillPath(brush, path);
                    }
                    using (var pen = new Pen(border, 1f))
                    {
                        g.DrawPath(pen, path);
                    }
                }
            }

            int textX = boxSize + s(10);
            int textWidth = Width - textX;
            if (textWidth <= 0) return;

            Color textColor = _isHovered ? Color.White : ForeColor;
            Size mainSize = SetupForm.MeasureWrapped(Text, Font, textWidth);
            Rectangle mainRect = new Rectangle(textX, 0, textWidth, mainSize.Height);
            SetupForm.DrawWrapped(g, Text, Font, textColor, mainRect);

            if (!string.IsNullOrEmpty(_helperText))
            {
                using (var helperFont = new Font(Font.FontFamily, 8.5f, FontStyle.Regular))
                {
                    int helperY = mainSize.Height + s(3);
                    Size helperSize = SetupForm.MeasureWrapped(_helperText, helperFont, textWidth);
                    Rectangle helperRect = new Rectangle(textX, helperY, textWidth, helperSize.Height);
                    Color helperColor = Color.FromArgb(161, 161, 170);
                    SetupForm.DrawWrapped(g, _helperText, helperFont, helperColor, helperRect);
                }
            }
        }
    }

    public class ModernProgressBar : Control
    {
        private int _value;
        public int Value
        {
            get { return _value; }
            set
            {
                _value = Math.Max(0, Math.Min(100, value));
                Invalidate();
            }
        }

        public ModernProgressBar()
        {
            SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer, true);
            BackColor = Color.FromArgb(18, 18, 22);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;

            Rectangle r = new Rectangle(0, 0, Width - 1, Height - 1);
            int radius = Height / 2;

            using (var path = SetupForm.CreateRoundedPath(r, radius))
            {
                using (var b = new SolidBrush(Color.FromArgb(18, 18, 22)))
                {
                    g.FillPath(b, path);
                }
                using (var pen = new Pen(Color.FromArgb(42, 42, 48), 1f))
                {
                    g.DrawPath(pen, path);
                }

                if (_value > 0)
                {
                    int fillWidth = Math.Max(Height, (int)((Width - 2) * (_value / 100.0f)));
                    Rectangle fillRect = new Rectangle(1, 1, fillWidth, Height - 2);
                    using (var fillPath = SetupForm.CreateRoundedPath(fillRect, Math.Max(1, radius - 1)))
                    {
                        using (var brush = new LinearGradientBrush(fillRect, Color.FromArgb(255, 77, 0), Color.FromArgb(255, 138, 0), 0f))
                        {
                            g.FillPath(brush, fillPath);
                        }
                    }
                }
            }
        }
    }

    public class SetupForm : Form
    {
        [DllImport("user32.dll")]
        private static extern int SendMessage(IntPtr hWnd, int Msg, int wParam, int lParam);
        [DllImport("user32.dll")]
        private static extern bool ReleaseCapture();
        [DllImport("kernel32.dll")]
        private static extern bool AttachConsole(int dwProcessId);
        private const int ATTACH_PARENT_PROCESS = -1;

        protected override CreateParams CreateParams
        {
            get
            {
                CreateParams cp = base.CreateParams;
                cp.ClassStyle |= 0x00020000; // CS_DROPSHADOW
                return cp;
            }
        }

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
        private ModernProgressBar _progressBar;
        private ModernCheckBox _chkDesktop;
        private ModernCheckBox _chkStartMenu;
        private ModernCheckBox _chkLaunch;
        private ModernCheckBox _chkRunNow;
        private ModernCheckBox _chkFinishDesktop;
        private ModernCheckBox _chkRemoveCache;
        private ModernButton _btnPrimary;
        private ModernButton _btnSecondary;
        private ModernButton _btnUninstallLink;

        private Image _logoImage;
        private bool _isCloseHovered;
        private bool _isMinHovered;
        private System.Windows.Forms.Timer _pulseTimer;
        private float _pulseAngle;
        private string _errorMessage = "";
        private bool _isInitialized;

        // DPI Scaling Helpers
        private float CurrentScale => (DeviceDpi > 0) ? ((float)DeviceDpi / 96f) : 1.0f;
        private int S(int v) => (int)Math.Round(v * CurrentScale);

        // Cached Render Objects
        private Bitmap _bgCache;
        private PathGradientBrush _glowBrush;
        private GraphicsPath _glowPath;
        private Rectangle _glowRectangle;

        // Scaled Fonts
        private Font _fTitle;
        private Font _fSub;
        private Font _fStatus;
        private Font _fDetail;
        private Font _fTitleBar;
        private Font _fPill;
        private Font _fCardHeader;
        private Font _fCardSub;
        private Font _fWarnEx;
        private Font _fWarnBig;
        private Font _fWarnSub;

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

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                _pulseTimer?.Dispose();
                _bgCache?.Dispose();
                _glowBrush?.Dispose();
                _glowPath?.Dispose();
                _fTitle?.Dispose();
                _fSub?.Dispose();
                _fStatus?.Dispose();
                _fDetail?.Dispose();
                _fTitleBar?.Dispose();
                _fPill?.Dispose();
                _fCardHeader?.Dispose();
                _fCardSub?.Dispose();
                _fWarnEx?.Dispose();
                _fWarnBig?.Dispose();
                _fWarnSub?.Dispose();
            }
            base.Dispose(disposing);
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
                string localLogo = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Assets", "logo.png");
                if (File.Exists(localLogo))
                {
                    _logoImage = Image.FromFile(localLogo);
                    return;
                }
                string localLogoJpg = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Assets", "logo.jpg");
                if (File.Exists(localLogoJpg))
                {
                    _logoImage = Image.FromFile(localLogoJpg);
                    return;
                }
            }
            catch { }
        }

        private void InitializeComponent()
        {
            this.Text = "KnowToMigrate Setup";
            this.FormBorderStyle = FormBorderStyle.None;
            this.StartPosition = FormStartPosition.CenterScreen;
            this.AutoScaleMode = AutoScaleMode.None;
            this.BackColor = Color.FromArgb(9, 9, 11);
            this.ForeColor = Color.White;
            this.DoubleBuffered = true;
            this.ShowInTaskbar = true;

            UpdateFonts(1.0f);

            // Header Title
            _lblHeaderTitle = new Label
            {
                Font = _fTitle,
                ForeColor = Color.FromArgb(250, 250, 250),
                BackColor = Color.Transparent,
                Text = "KnowToMigrate",
                AutoSize = false,
                UseCompatibleTextRendering = false
            };
            this.Controls.Add(_lblHeaderTitle);

            // Header Subtitle
            _lblHeaderSubtitle = new Label
            {
                Font = _fSub,
                ForeColor = Color.FromArgb(161, 161, 170),
                BackColor = Color.Transparent,
                Text = "Move Anything. Anywhere. Seamlessly.",
                AutoSize = false,
                UseCompatibleTextRendering = false
            };
            this.Controls.Add(_lblHeaderSubtitle);

            // Status label (for Installing / Uninstalling progress)
            _lblStatus = new Label
            {
                Font = _fStatus,
                ForeColor = Color.FromArgb(245, 245, 245),
                BackColor = Color.Transparent,
                Visible = false,
                AutoSize = false,
                UseCompatibleTextRendering = false
            };
            this.Controls.Add(_lblStatus);

            // Detail label
            _lblDetail = new Label
            {
                Font = _fDetail,
                ForeColor = Color.FromArgb(161, 161, 170),
                BackColor = Color.Transparent,
                Visible = false,
                AutoSize = false,
                UseCompatibleTextRendering = false
            };
            this.Controls.Add(_lblDetail);

            // Progress Bar
            _progressBar = new ModernProgressBar
            {
                Value = 0,
                Visible = false
            };
            this.Controls.Add(_progressBar);

            // Options Checkboxes for Welcome
            _chkDesktop = new ModernCheckBox
            {
                Text = "Create Desktop shortcut",
                Checked = true
            };
            this.Controls.Add(_chkDesktop);

            _chkStartMenu = new ModernCheckBox
            {
                Text = "Add shortcut to Windows Start Menu",
                Checked = true
            };
            this.Controls.Add(_chkStartMenu);

            _chkLaunch = new ModernCheckBox
            {
                Text = "Launch KnowToMigrate immediately after setup finishes",
                Checked = true
            };
            this.Controls.Add(_chkLaunch);

            // Finish Checkboxes (for InstallFinished)
            _chkRunNow = new ModernCheckBox
            {
                Text = "Run KnowToMigrate now",
                Checked = true,
                Font = new Font("Segoe UI", 10.0f, FontStyle.Bold),
                ForeColor = Color.FromArgb(255, 138, 0),
                Visible = false
            };
            this.Controls.Add(_chkRunNow);

            _chkFinishDesktop = new ModernCheckBox
            {
                Text = "Create Desktop shortcut",
                Checked = true,
                Visible = false
            };
            this.Controls.Add(_chkFinishDesktop);

            // Uninstall Checkbox
            _chkRemoveCache = new ModernCheckBox
            {
                Text = "Also remove settings, transfer history, and cache",
                HelperText = "Turn this off if you plan to reinstall and keep your history.",
                Checked = true,
                Visible = false
            };
            this.Controls.Add(_chkRemoveCache);

            // Primary Action Button ("Install Now" / "Finish" / "Uninstall")
            _btnPrimary = new ModernButton
            {
                IsPrimary = true
            };
            _btnPrimary.Click += BtnPrimary_Click;
            this.Controls.Add(_btnPrimary);

            // Secondary Action Button ("Cancel" / "Close")
            _btnSecondary = new ModernButton
            {
                Text = "Cancel"
            };
            _btnSecondary.Click += (s, e) => this.Close();
            this.Controls.Add(_btnSecondary);

            // Uninstall Link Button (Visible on Welcome if already installed)
            _btnUninstallLink = new ModernButton
            {
                Text = "Uninstall KnowToMigrate",
                IsDanger = false,
                Visible = false
            };
            _btnUninstallLink.Click += (s, e) => SetStep(SetupStep.UninstallConfirm);
            this.Controls.Add(_btnUninstallLink);

            // Mouse event handlers for title bar dragging and buttons
            this.MouseDown += SetupForm_MouseDown;
            this.MouseMove += SetupForm_MouseMove;
            this.MouseClick += SetupForm_MouseClick;

            // Pulse timer for ambient radial glow (33ms interval, ~30 FPS)
            _pulseTimer = new System.Windows.Forms.Timer();
            _pulseTimer.Interval = 33;
            _pulseTimer.Tick += (s, e) =>
            {
                _pulseAngle += (float)(2 * Math.PI * 0.033 / 2.4);
                if (_pulseAngle > (float)(2 * Math.PI)) _pulseAngle -= (float)(2 * Math.PI);
                if (!_glowRectangle.IsEmpty)
                {
                    this.Invalidate(_glowRectangle);
                }
            };
            _pulseTimer.Start();

            _isInitialized = true;
            ApplyLayout();
            RebuildCachedBitmaps();
        }

        public void SetStep(SetupStep step)
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

            _btnPrimary.IsPrimary = true;
            _btnPrimary.IsDanger = false;

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
                    _btnPrimary.Enabled = true;

                    _btnSecondary.Visible = false;
                    break;

                case SetupStep.UninstallConfirm:
                    _lblHeaderTitle.Text = "Uninstall KnowToMigrate";
                    _lblHeaderSubtitle.Text = "Completely remove KnowToMigrate from your computer.";

                    _chkRemoveCache.Visible = true;

                    _btnPrimary.IsPrimary = false;
                    _btnPrimary.IsDanger = true;
                    _btnPrimary.Text = "Uninstall";
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

                    _btnPrimary.IsPrimary = false;
                    _btnPrimary.IsDanger = false;
                    _btnPrimary.Text = "Close";
                    _btnPrimary.Enabled = true;

                    _btnSecondary.Visible = false;
                    break;
            }

            ApplyLayout();
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

                try
                {
                    string legacyDir = @"C:\Program Files (x86)\KnowToMigrate";
                    if (Directory.Exists(legacyDir))
                    {
                        Directory.Delete(legacyDir, true);
                        Program.Log("[INFO] Cleaned up legacy 32-bit install directory: " + legacyDir);
                    }
                }
                catch (Exception ex)
                {
                    Program.Log("[WARN] Legacy directory cleanup: " + ex.Message);
                }

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
                Program.ConfigureFirewall(true);
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
                Program.ConfigureFirewall(false);

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
                    string legacyDir = @"C:\Program Files (x86)\KnowToMigrate";
                    if (Directory.Exists(legacyDir))
                    {
                        Directory.Delete(legacyDir, true);
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
            g.SmoothingMode = SmoothingMode.HighQuality;
            g.InterpolationMode = InterpolationMode.HighQualityBicubic;
            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;
            g.CompositingQuality = CompositingQuality.HighQuality;

            int w = this.ClientSize.Width;
            int h = this.ClientSize.Height;

            // 1. Static Cached Background (AMOLED base, title bar fill, top accent, bottom separator, border)
            if (_bgCache != null)
            {
                g.DrawImageUnscaled(_bgCache, 0, 0);
            }
            else
            {
                using (var b = new SolidBrush(Color.FromArgb(9, 9, 11)))
                {
                    g.FillRectangle(b, 0, 0, w, h);
                }
            }

            // 2. Custom Title Bar Elements
            int titleBarH = S(46);

            // TitleBar Icon Badge
            int iconBoxSize = S(26);
            int iconBoxX = S(14);
            int iconBoxY = (titleBarH - iconBoxSize) / 2;
            Rectangle iconBox = new Rectangle(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize);
            DrawRoundedRectangle(g, iconBox, S(6), Color.FromArgb(20, 20, 24), Color.FromArgb(48, 48, 56), 1f);
            if (_logoImage != null)
            {
                g.DrawImage(_logoImage, new Rectangle(iconBoxX + S(3), iconBoxY + S(3), iconBoxSize - S(6), iconBoxSize - S(6)));
            }
            else
            {
                using (var f = new Font("Segoe UI", 8.0f, FontStyle.Bold))
                using (var b = new SolidBrush(Color.FromArgb(255, 138, 0)))
                {
                    TextRenderer.DrawText(g, "KM", f, iconBox, Color.FromArgb(255, 138, 0), TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
                }
            }

            // TitleBar Text & Version Pill
            int titleX = S(48);
            Size titleSize = TextRenderer.MeasureText("KnowToMigrate Setup", _fTitleBar, Size.Empty, TextFormatFlags.NoPadding);
            int titleY = (titleBarH - titleSize.Height) / 2;
            using (var bWhite = new SolidBrush(Color.FromArgb(245, 245, 245)))
            {
                g.DrawString("KnowToMigrate Setup", _fTitleBar, bWhite, titleX, titleY);
            }

            // Version Badge Pill ("v" + KtmVersion.DisplayVersion)
            string pillText = "v" + KtmVersion.DisplayVersion;
            Size pillTextSize = TextRenderer.MeasureText(pillText, _fPill, Size.Empty, TextFormatFlags.NoPadding);
            int pillW = pillTextSize.Width + S(20);
            int pillH = S(20);
            int pillX = titleX + titleSize.Width + S(12);
            int pillY = (titleBarH - pillH) / 2;
            Rectangle verPill = new Rectangle(pillX, pillY, pillW, pillH);
            DrawRoundedRectangle(g, verPill, S(10), Color.FromArgb(28, 18, 12), Color.FromArgb(124, 45, 18), 1f);
            TextRenderer.DrawText(g, pillText, _fPill, verPill, Color.FromArgb(255, 138, 0), TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);

            // Minimize Button
            int minW = S(44);
            int closeW = S(46);
            Rectangle minRect = new Rectangle(w - minW - closeW, 0, minW, titleBarH);
            if (_isMinHovered)
            {
                using (var b = new SolidBrush(Color.FromArgb(32, 32, 38)))
                {
                    g.FillRectangle(b, minRect);
                }
            }
            using (var pen = new Pen(Color.FromArgb(180, 180, 180), 1.5f))
            {
                g.DrawLine(pen, minRect.Left + S(16), titleBarH / 2, minRect.Right - S(16), titleBarH / 2);
            }

            // Close Button
            Rectangle closeRect = new Rectangle(w - closeW, 0, closeW, titleBarH);
            if (_isCloseHovered)
            {
                using (var b = new SolidBrush(Color.FromArgb(220, 38, 38)))
                {
                    g.FillRectangle(b, closeRect);
                }
            }
            using (var pen = new Pen(_isCloseHovered ? Color.White : Color.FromArgb(180, 180, 180), 1.5f))
            {
                int cx = closeRect.Left + closeW / 2;
                int cy = titleBarH / 2;
                int arm = S(5);
                g.DrawLine(pen, cx - arm, cy - arm, cx + arm, cy + arm);
                g.DrawLine(pen, cx + arm, cy - arm, cx - arm, cy + arm);
            }

            // 3. Logo Card (Left of Header)
            int logoBoxX = S(36);
            int logoBoxY = S(64);
            int logoBoxSize = S(56);
            Rectangle logoBox = new Rectangle(logoBoxX, logoBoxY, logoBoxSize, logoBoxSize);

            // Pulsing Radial Glow (70 to 110 alpha, 2.4s cycle)
            int glowAlpha = (int)Math.Round(90 + 20 * Math.Sin(_pulseAngle));
            glowAlpha = Math.Max(70, Math.Min(110, glowAlpha));

            if (_glowBrush != null && _glowPath != null)
            {
                _glowBrush.CenterColor = Color.FromArgb(glowAlpha, 255, 90, 0);
                var prevSmoothing = g.SmoothingMode;
                g.SmoothingMode = SmoothingMode.AntiAlias;
                g.FillPath(_glowBrush, _glowPath);
                g.SmoothingMode = prevSmoothing;
            }

            // Logo card background with 1.5px gradient rim (#FF8A00 to #FF4D00 at 80% alpha)
            using (var cardPath = CreateRoundedPath(logoBox, S(14)))
            {
                using (var cardBg = new SolidBrush(Color.FromArgb(16, 16, 20)))
                {
                    g.FillPath(cardBg, cardPath);
                }
                using (var rimBrush = new LinearGradientBrush(logoBox, Color.FromArgb(204, 255, 138, 0), Color.FromArgb(204, 255, 77, 0), 45f))
                using (var rimPen = new Pen(rimBrush, 1.5f))
                {
                    g.DrawPath(rimPen, cardPath);
                }
            }

            if (_logoImage != null)
            {
                g.DrawImage(_logoImage, new Rectangle(logoBoxX + S(6), logoBoxY + S(6), logoBoxSize - S(12), logoBoxSize - S(12)));
            }
            else
            {
                using (var f = new Font("Segoe UI", 16.0f, FontStyle.Bold))
                using (var b = new SolidBrush(Color.FromArgb(255, 138, 0)))
                {
                    TextRenderer.DrawText(g, "KM", f, logoBox, Color.FromArgb(255, 138, 0), TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
                }
            }

            // 4. Middle Content Cards based on step
            int contentX = S(36);
            int contentW = w - S(72);

            if (_currentStep == SetupStep.Welcome)
            {
                // Destination Card
                Rectangle destCard = new Rectangle(contentX, S(140), contentW, S(64));
                DrawRoundedRectangle(g, destCard, S(10), Color.FromArgb(16, 16, 20), Color.FromArgb(36, 36, 42), 1f);

                using (var bMuted = new SolidBrush(Color.FromArgb(161, 161, 170)))
                using (var bWhite = new SolidBrush(Color.White))
                using (var bDim = new SolidBrush(Color.FromArgb(113, 113, 122)))
                {
                    g.DrawString("INSTALLATION DESTINATION", _fCardHeader, bMuted, contentX + S(16), destCard.Top + S(10));
                    g.DrawString(Program.InstallDir, _fCardSub, bWhite, contentX + S(16), destCard.Top + S(32));

                    string extraText = "Native 64-bit • ~73 MB";
                    Size extraSize = TextRenderer.MeasureText(extraText, _fCardHeader, Size.Empty, TextFormatFlags.NoPadding);
                    g.DrawString(extraText, _fCardHeader, bDim, destCard.Right - S(16) - extraSize.Width, destCard.Top + S(32));
                }

                // Options Container Card
                Rectangle optCard = new Rectangle(contentX, S(216), contentW, S(150));
                DrawRoundedRectangle(g, optCard, S(10), Color.FromArgb(14, 14, 18), Color.FromArgb(30, 30, 36), 1f);

                using (var bMuted = new SolidBrush(Color.FromArgb(161, 161, 170)))
                {
                    g.DrawString("SETUP PREFERENCES", _fCardHeader, bMuted, contentX + S(16), optCard.Top + S(12));
                }
            }
            else if (_currentStep == SetupStep.InstallFinished)
            {
                // Glowing orange checkmark circle
                int cx = contentX + S(34);
                int cy = S(205);
                using (var bGlow = new SolidBrush(Color.FromArgb(40, 255, 90, 0)))
                {
                    g.FillEllipse(bGlow, cx - S(30), cy - S(30), S(60), S(60));
                }
                Rectangle checkCircle = new Rectangle(cx - S(24), cy - S(24), S(48), S(48));
                using (var brush = new LinearGradientBrush(checkCircle, Color.FromArgb(255, 77, 0), Color.FromArgb(255, 138, 0), 45f))
                {
                    g.FillEllipse(brush, checkCircle);
                }
                using (var penCheck = new Pen(Color.FromArgb(10, 10, 10), 3.5f))
                {
                    penCheck.StartCap = LineCap.Round;
                    penCheck.EndCap = LineCap.Round;
                    g.DrawLine(penCheck, cx - S(11), cy + S(1), cx - S(3), cy + S(9));
                    g.DrawLine(penCheck, cx - S(3), cy + S(9), cx + S(11), cy - S(8));
                }

                int textStartX = cx + S(36);
                int textW = contentX + contentW - textStartX;
                Rectangle hlRect = new Rectangle(textStartX, S(190), textW, S(28));
                DrawWrapped(g, "Installation Complete!", _fWarnBig, Color.White, hlRect);

                Rectangle subRect = new Rectangle(textStartX, hlRect.Bottom + S(4), textW, S(22));
                DrawWrapped(g, "KnowToMigrate is ready. Transit Transfer Engine is installed.", _fSub, Color.FromArgb(161, 161, 170), subRect);

                // Options card
                Rectangle finishCard = new Rectangle(contentX, S(260), contentW, S(110));
                DrawRoundedRectangle(g, finishCard, S(10), Color.FromArgb(14, 14, 18), Color.FromArgb(32, 32, 38), 1f);
            }
            else if (_currentStep == SetupStep.UninstallConfirm)
            {
                int cardX = contentX;
                int cardY = S(140);
                int cardW = contentW;

                int badgeSize = S(44);
                int badgeX = cardX + S(18);
                int badgeY = cardY + S(18);
                Rectangle badgeRect = new Rectangle(badgeX, badgeY, badgeSize, badgeSize);

                int textX = badgeX + badgeSize + S(16);
                int textW = cardX + cardW - S(18) - textX;

                string headline = "Remove KnowToMigrate from this PC?";
                Size hlSize = MeasureWrapped(headline, _fWarnBig, textW);
                Rectangle hlRect = new Rectangle(textX, cardY + S(18), textW, hlSize.Height);

                string body = "Application files, shortcuts, and registry entries will be removed.";
                int bodyY = hlRect.Bottom + S(6);
                Size bodySize = MeasureWrapped(body, _fWarnSub, textW);
                Rectangle bodyRect = new Rectangle(textX, bodyY, textW, bodySize.Height);

                int cardH = Math.Max(badgeSize + S(36), S(18) + hlSize.Height + S(6) + bodySize.Height + S(18));
                Rectangle uninstCard = new Rectangle(cardX, cardY, cardW, cardH);
                DrawRoundedRectangle(g, uninstCard, S(10), Color.FromArgb(18, 12, 12), Color.FromArgb(60, 30, 30), 1f);

                // Warning badge
                using (var bWarn = new SolidBrush(Color.FromArgb(220, 38, 38)))
                {
                    g.FillEllipse(bWarn, badgeRect);
                }
                TextRenderer.DrawText(g, "!", _fWarnEx, badgeRect, Color.White, TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);

                DrawWrapped(g, headline, _fWarnBig, Color.White, hlRect);
                DrawWrapped(g, body, _fWarnSub, Color.FromArgb(161, 161, 170), bodyRect);

                // Checkbox card container
                int chkCardY = uninstCard.Bottom + S(16);
                int chkCardH = _chkRemoveCache.Height + S(20);
                Rectangle chkCard = new Rectangle(cardX, chkCardY, cardW, chkCardH);
                DrawRoundedRectangle(g, chkCard, S(10), Color.FromArgb(14, 14, 18), Color.FromArgb(32, 32, 38), 1f);
            }
            else if (_currentStep == SetupStep.UninstallFinished)
            {
                // Orange Checkmark
                int cx = contentX + S(34);
                int cy = S(205);
                Rectangle checkCircle = new Rectangle(cx - S(24), cy - S(24), S(48), S(48));
                using (var brush = new LinearGradientBrush(checkCircle, Color.FromArgb(255, 77, 0), Color.FromArgb(255, 138, 0), 45f))
                {
                    g.FillEllipse(brush, checkCircle);
                }
                using (var penCheck = new Pen(Color.FromArgb(10, 10, 10), 3.5f))
                {
                    penCheck.StartCap = LineCap.Round;
                    penCheck.EndCap = LineCap.Round;
                    g.DrawLine(penCheck, cx - S(11), cy + S(1), cx - S(3), cy + S(9));
                    g.DrawLine(penCheck, cx - S(3), cy + S(9), cx + S(11), cy - S(8));
                }

                int textStartX = cx + S(36);
                int textW = contentX + contentW - textStartX;
                Rectangle hlRect = new Rectangle(textStartX, S(190), textW, S(28));
                DrawWrapped(g, "Uninstallation Complete!", _fWarnBig, Color.White, hlRect);

                Rectangle subRect = new Rectangle(textStartX, hlRect.Bottom + S(4), textW, S(22));
                DrawWrapped(g, "KnowToMigrate has been cleanly removed from this machine.", _fSub, Color.FromArgb(161, 161, 170), subRect);
            }
        }

        public static Size MeasureWrapped(string text, Font font, int maxWidth)
        {
            if (string.IsNullOrEmpty(text) || maxWidth <= 0) return Size.Empty;
            return TextRenderer.MeasureText(text, font, new Size(maxWidth, int.MaxValue), TextFormatFlags.WordBreak | TextFormatFlags.NoPadding);
        }

        public static void DrawWrapped(Graphics g, string text, Font font, Color color, Rectangle rect)
        {
            if (string.IsNullOrEmpty(text) || rect.Width <= 0 || rect.Height <= 0) return;
            TextRenderer.DrawText(g, text, font, rect, color, TextFormatFlags.WordBreak | TextFormatFlags.Top | TextFormatFlags.NoPadding);
        }

        public static void DrawRoundedRectangle(Graphics g, Rectangle r, int radius, Color backColor, Color borderColor, float borderWidth)
        {
            if (r.Width <= 0 || r.Height <= 0) return;
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

        public static GraphicsPath CreateRoundedPath(Rectangle r, int radius)
        {
            var path = new GraphicsPath();
            int d = radius * 2;
            if (d > r.Width) d = r.Width;
            if (d > r.Height) d = r.Height;
            if (d <= 0)
            {
                path.AddRectangle(r);
                return path;
            }
            path.AddArc(r.Left, r.Top, d, d, 180, 90);
            path.AddArc(r.Right - d, r.Top, d, d, 270, 90);
            path.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
            path.AddArc(r.Left, r.Bottom - d, d, d, 90, 90);
            path.CloseFigure();
            return path;
        }

        private void SetupForm_MouseDown(object sender, MouseEventArgs e)
        {
            int titleBarH = S(46);
            int buttonsW = S(90);
            if (e.Button == MouseButtons.Left && e.Y <= titleBarH && e.X < this.Width - buttonsW)
            {
                ReleaseCapture();
                SendMessage(this.Handle, 0xA1, 0x2, 0);
            }
        }

        private void SetupForm_MouseMove(object sender, MouseEventArgs e)
        {
            int titleBarH = S(46);
            int closeW = S(46);
            int minW = S(44);
            int buttonsW = closeW + minW;

            bool closeHover = (e.X >= this.Width - closeW && e.X <= this.Width && e.Y >= 0 && e.Y <= titleBarH);
            bool minHover = (e.X >= this.Width - buttonsW && e.X < this.Width - closeW && e.Y >= 0 && e.Y <= titleBarH);

            if (closeHover != _isCloseHovered || minHover != _isMinHovered)
            {
                _isCloseHovered = closeHover;
                _isMinHovered = minHover;
                this.Invalidate(new Rectangle(this.Width - buttonsW - 2, 0, buttonsW + 2, titleBarH));
            }
        }

        private void SetupForm_MouseClick(object sender, MouseEventArgs e)
        {
            int titleBarH = S(46);
            int closeW = S(46);
            int minW = S(44);
            int buttonsW = closeW + minW;

            if (e.Button == MouseButtons.Left && e.Y <= titleBarH)
            {
                if (e.X >= this.Width - closeW && e.X <= this.Width)
                {
                    this.Close();
                }
                else if (e.X >= this.Width - buttonsW && e.X < this.Width - closeW)
                {
                    this.WindowState = FormWindowState.Minimized;
                }
            }
        }

        protected override void OnDpiChanged(DpiChangedEventArgs e)
        {
            base.OnDpiChanged(e);
            UpdateFonts(1.0f);
            ApplyLayout();
            RebuildCachedBitmaps();
            this.Invalidate();
        }

        protected override void OnResize(EventArgs e)
        {
            base.OnResize(e);
            if (!_isInitialized) return;
            if (this.WindowState == FormWindowState.Minimized)
            {
                if (_pulseTimer != null && _pulseTimer.Enabled) _pulseTimer.Stop();
            }
            else
            {
                if (Form.ActiveForm == this && _pulseTimer != null && !_pulseTimer.Enabled) _pulseTimer.Start();
                RebuildCachedBitmaps();
                ApplyLayout();
                this.Invalidate();
            }
        }

        protected override void OnActivated(EventArgs e)
        {
            base.OnActivated(e);
            if (this.WindowState != FormWindowState.Minimized && _pulseTimer != null && !_pulseTimer.Enabled)
            {
                _pulseTimer.Start();
            }
        }

        protected override void OnDeactivate(EventArgs e)
        {
            base.OnDeactivate(e);
            if (_pulseTimer != null && _pulseTimer.Enabled)
            {
                _pulseTimer.Stop();
            }
        }

        private void RebuildGlow()
        {
            _glowBrush?.Dispose();
            _glowPath?.Dispose();

            int logoBoxX = S(36);
            int logoBoxY = S(64);
            int logoBoxSize = S(56);
            Rectangle logoBox = new Rectangle(logoBoxX, logoBoxY, logoBoxSize, logoBoxSize);

            int cx = logoBoxX + logoBoxSize / 2;
            int cy = logoBoxY + logoBoxSize / 2;
            int glowRadius = S(38);

            _glowPath = new GraphicsPath();
            _glowPath.AddEllipse(cx - glowRadius, cy - glowRadius, glowRadius * 2, glowRadius * 2);

            _glowBrush = new PathGradientBrush(_glowPath)
            {
                CenterColor = Color.FromArgb(90, 255, 90, 0),
                SurroundColors = new Color[] { Color.FromArgb(0, 255, 90, 0) }
            };

            int inflate = glowRadius - logoBoxSize / 2 + S(4);
            _glowRectangle = Rectangle.Inflate(logoBox, inflate, inflate);
        }

        private void RebuildCachedBitmaps()
        {
            _bgCache?.Dispose();
            int w = this.ClientSize.Width;
            int h = this.ClientSize.Height;
            if (w <= 0 || h <= 0) return;

            _bgCache = new Bitmap(w, h);
            using (Graphics bgG = Graphics.FromImage(_bgCache))
            {
                bgG.SmoothingMode = SmoothingMode.HighQuality;
                bgG.InterpolationMode = InterpolationMode.HighQualityBicubic;
                bgG.PixelOffsetMode = PixelOffsetMode.HighQuality;

                // 1. Base Pure AMOLED Black
                using (var b = new SolidBrush(Color.FromArgb(9, 9, 11)))
                {
                    bgG.FillRectangle(b, 0, 0, w, h);
                }

                // 2. Custom Title Bar Background (Height: S(46))
                int titleBarH = S(46);
                using (var b = new SolidBrush(Color.FromArgb(14, 14, 18)))
                {
                    bgG.FillRectangle(b, 0, 0, w, titleBarH);
                }
                using (var pen = new Pen(Color.FromArgb(28, 28, 34), 1f))
                {
                    bgG.DrawLine(pen, 0, titleBarH, w, titleBarH);
                }

                // Top edge brand gradient accent (2px)
                using (var gradBrush = new LinearGradientBrush(new Rectangle(0, 0, w, 2), Color.FromArgb(255, 77, 0), Color.FromArgb(255, 138, 0), 0f))
                {
                    bgG.FillRectangle(gradBrush, 0, 0, w, 2);
                }

                // Bottom Separator
                using (var pen = new Pen(Color.FromArgb(26, 26, 32), 1f))
                {
                    bgG.DrawLine(pen, 0, h - S(78), w, h - S(78));
                }

                // Outer Subtle Border
                using (var borderPen = new Pen(Color.FromArgb(42, 42, 50), 1f))
                {
                    bgG.DrawRectangle(borderPen, 0, 0, w - 1, h - 1);
                }
            }

            RebuildGlow();
        }

        private void UpdateFonts(float fontScaleMultiplier)
        {
            _fTitle?.Dispose();
            _fSub?.Dispose();
            _fStatus?.Dispose();
            _fDetail?.Dispose();
            _fTitleBar?.Dispose();
            _fPill?.Dispose();
            _fCardHeader?.Dispose();
            _fCardSub?.Dispose();
            _fWarnEx?.Dispose();
            _fWarnBig?.Dispose();
            _fWarnSub?.Dispose();

            _fTitle = new Font("Segoe UI", 16.5f * fontScaleMultiplier, FontStyle.Bold);
            _fSub = new Font("Segoe UI", 9.5f * fontScaleMultiplier, FontStyle.Regular);
            _fStatus = new Font("Segoe UI", 11.0f * fontScaleMultiplier, FontStyle.Bold);
            _fDetail = new Font("Segoe UI", 9.0f * fontScaleMultiplier, FontStyle.Regular);
            _fTitleBar = new Font("Segoe UI", 9.5f * fontScaleMultiplier, FontStyle.Bold);
            _fPill = new Font("Segoe UI", 8.0f * fontScaleMultiplier, FontStyle.Bold);
            _fCardHeader = new Font("Segoe UI", 8.5f * fontScaleMultiplier, FontStyle.Bold);
            _fCardSub = new Font("Segoe UI", 9.5f * fontScaleMultiplier, FontStyle.Regular);
            _fWarnEx = new Font("Segoe UI", 20.0f * fontScaleMultiplier, FontStyle.Bold);
            _fWarnBig = new Font("Segoe UI", 12.0f * fontScaleMultiplier, FontStyle.Bold);
            _fWarnSub = new Font("Segoe UI", 9.0f * fontScaleMultiplier, FontStyle.Regular);

            if (_lblHeaderTitle != null) _lblHeaderTitle.Font = _fTitle;
            if (_lblHeaderSubtitle != null) _lblHeaderSubtitle.Font = _fSub;
            if (_lblStatus != null) _lblStatus.Font = _fStatus;
            if (_lblDetail != null) _lblDetail.Font = _fDetail;
        }

        public void ApplyLayoutForScale(float scale)
        {
            if (!_isInitialized) return;
            int s(int v) => (int)Math.Round(v * scale);

            this.ClientSize = new Size(s(560), s(520));
            int w = this.ClientSize.Width;
            int h = this.ClientSize.Height;

            int contentX = s(36);
            int contentW = w - s(72);

            // Header Labels
            int headerLeft = s(106);
            int headerW = w - headerLeft - s(36);

            _lblHeaderTitle.AutoSize = false;
            _lblHeaderTitle.UseCompatibleTextRendering = false;
            _lblHeaderTitle.Location = new Point(headerLeft, s(64));
            _lblHeaderTitle.Width = headerW;
            Size titleMeasured = MeasureWrapped(_lblHeaderTitle.Text, _lblHeaderTitle.Font, headerW);
            _lblHeaderTitle.Height = Math.Max(s(28), titleMeasured.Height);

            _lblHeaderSubtitle.AutoSize = false;
            _lblHeaderSubtitle.UseCompatibleTextRendering = false;
            _lblHeaderSubtitle.Location = new Point(headerLeft, _lblHeaderTitle.Bottom + s(2));
            _lblHeaderSubtitle.Width = headerW;
            Size subMeasured = MeasureWrapped(_lblHeaderSubtitle.Text, _lblHeaderSubtitle.Font, headerW);
            _lblHeaderSubtitle.Height = Math.Max(s(20), subMeasured.Height);

            // Status & Detail labels for Installing / Uninstalling
            _lblStatus.Location = new Point(s(40), s(190));
            _lblStatus.Size = new Size(w - s(80), s(26));
            _lblDetail.Location = new Point(s(40), s(222));
            _lblDetail.Size = new Size(w - s(80), s(44));
            _progressBar.Location = new Point(s(40), s(275));
            _progressBar.Size = new Size(w - s(80), s(10));

            // Welcome Checkboxes
            int chkX = contentX + s(16);
            int chkW = contentW - s(32);
            int welcomeOptY = s(216);

            _chkDesktop.Width = chkW;
            _chkDesktop.Height = _chkDesktop.CalculateHeight(chkW, scale);
            _chkDesktop.Location = new Point(chkX, welcomeOptY + s(36));

            _chkStartMenu.Width = chkW;
            _chkStartMenu.Height = _chkStartMenu.CalculateHeight(chkW, scale);
            _chkStartMenu.Location = new Point(chkX, _chkDesktop.Bottom + s(4));

            _chkLaunch.Width = chkW;
            _chkLaunch.Height = _chkLaunch.CalculateHeight(chkW, scale);
            _chkLaunch.Location = new Point(chkX, _chkStartMenu.Bottom + s(4));

            // Finish Checkboxes
            int finishOptY = s(260);
            _chkRunNow.Width = chkW;
            _chkRunNow.Height = _chkRunNow.CalculateHeight(chkW, scale);
            _chkRunNow.Location = new Point(chkX, finishOptY + s(16));

            _chkFinishDesktop.Width = chkW;
            _chkFinishDesktop.Height = _chkFinishDesktop.CalculateHeight(chkW, scale);
            _chkFinishDesktop.Location = new Point(chkX, _chkRunNow.Bottom + s(8));

            // Uninstall Checkbox
            int uninstCardY = s(140);
            int badgeSize = s(44);
            int textX = contentX + s(18) + badgeSize + s(16);
            int textW = contentX + contentW - s(18) - textX;
            Size hlSize = MeasureWrapped("Remove KnowToMigrate from this PC?", _fWarnBig, textW);
            Size bodySize = MeasureWrapped("Application files, shortcuts, and registry entries will be removed.", _fWarnSub, textW);
            int uninstCardH = Math.Max(badgeSize + s(36), s(18) + hlSize.Height + s(6) + bodySize.Height + s(18));
            int chkCardY = uninstCardY + uninstCardH + s(16);

            _chkRemoveCache.Text = "Also remove settings, transfer history, and cache";
            _chkRemoveCache.HelperText = "Turn this off if you plan to reinstall and keep your history.";
            _chkRemoveCache.Width = contentW - s(28);
            _chkRemoveCache.Height = _chkRemoveCache.CalculateHeight(_chkRemoveCache.Width, scale);
            _chkRemoveCache.Location = new Point(contentX + s(14), chkCardY + s(10));

            // Buttons
            int btnH = s(40);
            int btnY = h - s(78) + (s(78) - btnH) / 2;

            Size primaryTextSize = TextRenderer.MeasureText(_btnPrimary.Text, _btnPrimary.Font, Size.Empty, TextFormatFlags.NoPadding);
            int primaryW = Math.Max(s(120), primaryTextSize.Width + s(40));
            _btnPrimary.Size = new Size(primaryW, btnH);
            _btnPrimary.Location = new Point(w - s(36) - primaryW, btnY);

            Size secTextSize = TextRenderer.MeasureText(_btnSecondary.Text, _btnSecondary.Font, Size.Empty, TextFormatFlags.NoPadding);
            int secW = Math.Max(s(104), secTextSize.Width + s(36));
            _btnSecondary.Size = new Size(secW, btnH);
            _btnSecondary.Location = new Point(_btnPrimary.Left - s(12) - secW, btnY);

            Size linkTextSize = TextRenderer.MeasureText(_btnUninstallLink.Text, _btnUninstallLink.Font, Size.Empty, TextFormatFlags.NoPadding);
            int linkW = Math.Max(s(160), linkTextSize.Width + s(24));
            _btnUninstallLink.Size = new Size(linkW, btnH);
            _btnUninstallLink.Location = new Point(s(36), btnY);
        }

        public void ApplyLayout()
        {
            ApplyLayoutForScale(CurrentScale);
        }

        public static int RunLayoutCheck()
        {
            AttachConsole(ATTACH_PARENT_PROCESS);

            Console.WriteLine("==================================================================");
            Console.WriteLine("KnowToMigrate Setup - Layout & Text Integrity Regression Check");
            Console.WriteLine("==================================================================");

            try
            {
                int totalViolations = 0;
                float[] testScales = new float[] { 1.0f, 1.25f, 1.50f, 1.75f };

                using (var form = new SetupForm(new string[0], false))
                {
                    form._pulseTimer?.Stop();
                    form.CreateControl();

                    foreach (float testScale in testScales)
                    {
                        Console.WriteLine(string.Format("\n--- Testing Scale: {0:0}% (DPI: {1}) ---", testScale * 100f, (int)Math.Round(testScale * 96f)));
                        int scaleViolations = 0;

                        float current = (form.CurrentScale > 0) ? form.CurrentScale : 1.0f;
                        form.UpdateFonts(testScale / current);

                        foreach (SetupStep step in Enum.GetValues(typeof(SetupStep)))
                        {
                            form.SetStep(step);
                            form.ApplyLayoutForScale(testScale);

                    Rectangle clientRect = form.ClientRectangle;

                    // 1. Control Bounds Check
                    foreach (Control c in form.Controls)
                    {
                        if (!c.Visible) continue;

                        if (c.Left < clientRect.Left || c.Top < clientRect.Top ||
                            c.Right > clientRect.Right || c.Bottom > clientRect.Bottom)
                        {
                            Console.WriteLine(string.Format("[VIOLATION] [Step: {0}] Control '{1}' (Bounds: {2}) exceeds Client bounds ({3}).", step, c.GetType().Name, c.Bounds, clientRect));
                            scaleViolations++;
                        }
                    }

                    // 2. Control Overlap Check
                    var visibleList = new System.Collections.Generic.List<Control>();
                    foreach (Control c in form.Controls)
                    {
                        if (c.Visible) visibleList.Add(c);
                    }

                    for (int i = 0; i < visibleList.Count; i++)
                    {
                        for (int j = i + 1; j < visibleList.Count; j++)
                        {
                            Control c1 = visibleList[i];
                            Control c2 = visibleList[j];

                            if (c1.Bounds.IntersectsWith(c2.Bounds))
                            {
                                Console.WriteLine(string.Format("[VIOLATION] [Step: {0}] Controls '{1}' and '{2}' overlap: {3}.", step, c1.GetType().Name, c2.GetType().Name, Rectangle.Intersect(c1.Bounds, c2.Bounds)));
                                scaleViolations++;
                            }
                        }
                    }

                    // 3. Text Fitting Checks
                    if (form._btnPrimary.Visible)
                    {
                        Size pTextSize = TextRenderer.MeasureText(form._btnPrimary.Text, form._btnPrimary.Font, Size.Empty, TextFormatFlags.NoPadding);
                        if (pTextSize.Width > form._btnPrimary.Width - (int)(16 * testScale))
                        {
                            Console.WriteLine(string.Format("[VIOLATION] [Step: {0}] Primary button text '{1}' ({2}px) clipped in button width ({3}px).", step, form._btnPrimary.Text, pTextSize.Width, form._btnPrimary.Width));
                            scaleViolations++;
                        }
                    }

                    if (form._btnSecondary.Visible)
                    {
                        Size sTextSize = TextRenderer.MeasureText(form._btnSecondary.Text, form._btnSecondary.Font, Size.Empty, TextFormatFlags.NoPadding);
                        if (sTextSize.Width > form._btnSecondary.Width - (int)(16 * testScale))
                        {
                            Console.WriteLine(string.Format("[VIOLATION] [Step: {0}] Secondary button text '{1}' ({2}px) clipped in button width ({3}px).", step, form._btnSecondary.Text, sTextSize.Width, form._btnSecondary.Width));
                            scaleViolations++;
                        }
                    }

                    // Header Labels
                    Size titleTextSize = MeasureWrapped(form._lblHeaderTitle.Text, form._lblHeaderTitle.Font, form._lblHeaderTitle.Width);
                    if (titleTextSize.Height > form._lblHeaderTitle.Height)
                    {
                        Console.WriteLine(string.Format("[VIOLATION] [Step: {0}] Header title '{1}' height ({2}px) exceeds label height ({3}px).", step, form._lblHeaderTitle.Text, titleTextSize.Height, form._lblHeaderTitle.Height));
                        scaleViolations++;
                    }

                    Size subTextSize = MeasureWrapped(form._lblHeaderSubtitle.Text, form._lblHeaderSubtitle.Font, form._lblHeaderSubtitle.Width);
                    if (subTextSize.Height > form._lblHeaderSubtitle.Height)
                    {
                        Console.WriteLine(string.Format("[VIOLATION] [Step: {0}] Header subtitle '{1}' height ({2}px) exceeds label height ({3}px).", step, form._lblHeaderSubtitle.Text, subTextSize.Height, form._lblHeaderSubtitle.Height));
                        scaleViolations++;
                    }

                    // Title Bar checks
                    int s(int v) => (int)Math.Round(v * testScale);
                    int titleX = s(48);
                    Size tSize = TextRenderer.MeasureText("KnowToMigrate Setup", form._fTitleBar, Size.Empty, TextFormatFlags.NoPadding);
                    int pillX = titleX + tSize.Width + s(12);
                    Size pillTSize = TextRenderer.MeasureText("v" + KtmVersion.DisplayVersion, form._fPill, Size.Empty, TextFormatFlags.NoPadding);
                    int pillW = pillTSize.Width + s(20);
                    int minBtnX = form.ClientSize.Width - s(90);

                    if (pillX <= titleX + tSize.Width)
                    {
                        Console.WriteLine(string.Format("[VIOLATION] [TitleBar] Title text overlaps version pill (Title Right: {0}, Pill Left: {1}).", titleX + tSize.Width, pillX));
                        scaleViolations++;
                    }
                    if (pillX + pillW >= minBtnX)
                    {
                        Console.WriteLine(string.Format("[VIOLATION] [TitleBar] Version pill overlaps minimize button (Pill Right: {0}, Min Left: {1}).", pillX + pillW, minBtnX));
                        scaleViolations++;
                    }

                    // UninstallConfirm Card & Headline check
                    if (step == SetupStep.UninstallConfirm)
                    {
                        int cardX = s(36);
                        int cardW = form.ClientSize.Width - s(72);
                        int badgeSize = s(44);
                        int tX = cardX + s(18) + badgeSize + s(16);
                        int tW = cardX + cardW - s(18) - tX;

                        Size hlSize = MeasureWrapped("Remove KnowToMigrate from this PC?", form._fWarnBig, tW);
                        Size bSize = MeasureWrapped("Application files, shortcuts, and registry entries will be removed.", form._fWarnSub, tW);
                        int cardH = Math.Max(badgeSize + s(36), s(18) + hlSize.Height + s(6) + bSize.Height + s(18));

                        if (cardX + cardW > form.ClientSize.Width)
                        {
                            Console.WriteLine(string.Format("[VIOLATION] [Step: UninstallConfirm] Card width exceeds window width."));
                            scaleViolations++;
                        }

                        if (form._chkRemoveCache.Bottom > form.ClientSize.Height - s(78))
                        {
                            Console.WriteLine(string.Format("[VIOLATION] [Step: UninstallConfirm] Checkbox bottom ({0}) overlaps bottom bar ({1}).", form._chkRemoveCache.Bottom, form.ClientSize.Height - s(78)));
                            scaleViolations++;
                        }
                    }
                }

                if (scaleViolations == 0)
                {
                    Console.WriteLine(string.Format("[PASS] Scale {0:0}%: 0 violations found. All controls within client bounds, no overlaps, all text fits.", testScale * 100f));
                }
                else
                {
                    Console.WriteLine(string.Format("[FAIL] Scale {0:0}%: {1} violations found.", testScale * 100f, scaleViolations));
                    totalViolations += scaleViolations;
                }
            }

                }

                Console.WriteLine("\n------------------------------------------------------------------");
                if (totalViolations == 0)
                {
                    Console.WriteLine("[ALL PASSED] Layout regression check PASSED across all scales (100%, 125%, 150%, 175%).");
                    Console.WriteLine("==================================================================");
                    return 0;
                }
                else
                {
                    Console.WriteLine(string.Format("[FAILED] Total layout violations: {0}.", totalViolations));
                    Console.WriteLine("==================================================================");
                    return 1;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("[FATAL] Layout regression check crashed with error:\n" + ex);
                Console.WriteLine("==================================================================");
                return 1;
            }
        }
    }
}

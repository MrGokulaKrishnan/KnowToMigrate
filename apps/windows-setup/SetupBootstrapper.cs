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
[assembly: AssemblyVersion("1.0.6.0")]
[assembly: AssemblyFileVersion("1.0.6.0")]

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
                        Version v = typeof(Program).Assembly.GetName().Version;
                        string verStr = (v != null) ? string.Format("{0}.{1}.{2}", v.Major, v.Minor, v.Build) : "1.0.6";
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

            Rectangle r = new Rectangle(0, 0, Width - 1, Height - 1);
            int radius = 8;

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

            if (_isPrimary)
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
            else if (_isDanger)
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

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            int boxSize = 18;
            int boxY = (Height - boxSize) / 2;
            Rectangle boxRect = new Rectangle(0, boxY, boxSize, boxSize);

            using (var path = SetupForm.CreateRoundedPath(boxRect, 4))
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
                    using (var penCheck = new Pen(Color.FromArgb(10, 10, 10), 2.2f))
                    {
                        penCheck.StartCap = LineCap.Round;
                        penCheck.EndCap = LineCap.Round;
                        g.DrawLine(penCheck, 4, boxY + 9, 7, boxY + 13);
                        g.DrawLine(penCheck, 7, boxY + 13, 13, boxY + 5);
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

            Rectangle textRect = new Rectangle(boxSize + 10, 0, Width - (boxSize + 10), Height);
            Color textColor = _isHovered ? Color.White : ForeColor;
            TextRenderer.DrawText(g, Text, Font, textRect, textColor, TextFormatFlags.Left | TextFormatFlags.VerticalCenter);
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
            this.ClientSize = new Size(560, 520);
            this.BackColor = Color.FromArgb(9, 9, 11);
            this.ForeColor = Color.White;
            this.DoubleBuffered = true;
            this.ShowInTaskbar = true;

            // Header Title
            _lblHeaderTitle = new Label
            {
                Font = new Font("Segoe UI", 16.5f, FontStyle.Bold),
                ForeColor = Color.FromArgb(250, 250, 250),
                BackColor = Color.Transparent,
                Location = new Point(106, 66),
                Size = new Size(420, 32),
                Text = "KnowToMigrate"
            };
            this.Controls.Add(_lblHeaderTitle);

            // Header Subtitle
            _lblHeaderSubtitle = new Label
            {
                Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
                ForeColor = Color.FromArgb(161, 161, 170),
                BackColor = Color.Transparent,
                Location = new Point(106, 100),
                Size = new Size(420, 22),
                Text = "Move Anything. Anywhere. Seamlessly."
            };
            this.Controls.Add(_lblHeaderSubtitle);

            // Status label (for Installing / Uninstalling progress)
            _lblStatus = new Label
            {
                Font = new Font("Segoe UI", 11.0f, FontStyle.Bold),
                ForeColor = Color.FromArgb(245, 245, 245),
                BackColor = Color.Transparent,
                Location = new Point(40, 190),
                Size = new Size(480, 26),
                Visible = false
            };
            this.Controls.Add(_lblStatus);

            // Detail label
            _lblDetail = new Label
            {
                Font = new Font("Segoe UI", 9.0f, FontStyle.Regular),
                ForeColor = Color.FromArgb(161, 161, 170),
                BackColor = Color.Transparent,
                Location = new Point(40, 220),
                Size = new Size(480, 44),
                Visible = false
            };
            this.Controls.Add(_lblDetail);

            // Progress Bar
            _progressBar = new ModernProgressBar
            {
                Location = new Point(40, 275),
                Size = new Size(480, 10),
                Value = 0,
                Visible = false
            };
            this.Controls.Add(_progressBar);

            // Options Checkboxes for Welcome
            _chkDesktop = new ModernCheckBox
            {
                Text = "Create Desktop shortcut",
                Checked = true,
                Location = new Point(52, 255),
                Size = new Size(450, 26)
            };
            this.Controls.Add(_chkDesktop);

            _chkStartMenu = new ModernCheckBox
            {
                Text = "Add shortcut to Windows Start Menu",
                Checked = true,
                Location = new Point(52, 285),
                Size = new Size(450, 26)
            };
            this.Controls.Add(_chkStartMenu);

            _chkLaunch = new ModernCheckBox
            {
                Text = "Launch KnowToMigrate immediately after setup finishes",
                Checked = true,
                Location = new Point(52, 315),
                Size = new Size(450, 26)
            };
            this.Controls.Add(_chkLaunch);

            // Finish Checkboxes (for InstallFinished)
            _chkRunNow = new ModernCheckBox
            {
                Text = "Run KnowToMigrate now",
                Checked = true,
                Font = new Font("Segoe UI", 10.0f, FontStyle.Bold),
                ForeColor = Color.FromArgb(255, 138, 0),
                Location = new Point(52, 290),
                Size = new Size(450, 28),
                Visible = false
            };
            this.Controls.Add(_chkRunNow);

            _chkFinishDesktop = new ModernCheckBox
            {
                Text = "Create Desktop shortcut",
                Checked = true,
                Location = new Point(52, 324),
                Size = new Size(450, 26),
                Visible = false
            };
            this.Controls.Add(_chkFinishDesktop);

            // Uninstall Checkbox
            _chkRemoveCache = new ModernCheckBox
            {
                Text = "Remove local configuration, transfer history, and temporary cache",
                Checked = true,
                Location = new Point(52, 280),
                Size = new Size(450, 36),
                Visible = false
            };
            this.Controls.Add(_chkRemoveCache);

            // Primary Action Button ("Install Now" / "Finish" / "Uninstall Now")
            _btnPrimary = new ModernButton
            {
                Size = new Size(130, 42),
                Location = new Point(394, 452),
                IsPrimary = true
            };
            _btnPrimary.Click += BtnPrimary_Click;
            this.Controls.Add(_btnPrimary);

            // Secondary Action Button ("Cancel" / "Close")
            _btnSecondary = new ModernButton
            {
                Size = new Size(104, 42),
                Location = new Point(278, 452),
                Text = "Cancel"
            };
            _btnSecondary.Click += (s, e) => this.Close();
            this.Controls.Add(_btnSecondary);

            // Uninstall Link Button (Visible on Welcome if already installed)
            _btnUninstallLink = new ModernButton
            {
                Size = new Size(174, 36),
                Location = new Point(36, 455),
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

            // Smooth pulse timer for ambient glow
            _pulseTimer = new System.Windows.Forms.Timer();
            _pulseTimer.Interval = 35;
            _pulseTimer.Tick += (s, e) =>
            {
                _pulseAngle += 0.05f;
                if (_pulseAngle > 6.28f) _pulseAngle = 0;
                this.Invalidate(new Rectangle(30, 58, 70, 70));
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
                    _btnPrimary.Text = "Uninstall Now";
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

            int w = this.Width;
            int h = this.Height;

            // 1. Base Pure AMOLED Black
            using (var b = new SolidBrush(Color.FromArgb(9, 9, 11)))
            {
                g.FillRectangle(b, 0, 0, w, h);
            }

            // 2. Custom Title Bar (Height: 46)
            int titleBarH = 46;
            using (var b = new SolidBrush(Color.FromArgb(14, 14, 18)))
            {
                g.FillRectangle(b, 0, 0, w, titleBarH);
            }
            using (var pen = new Pen(Color.FromArgb(28, 28, 34), 1f))
            {
                g.DrawLine(pen, 0, titleBarH, w, titleBarH);
            }

            // Top edge brand gradient accent (2px)
            using (var gradBrush = new LinearGradientBrush(new Rectangle(0, 0, w, 2), Color.FromArgb(255, 77, 0), Color.FromArgb(255, 138, 0), 0f))
            {
                g.FillRectangle(gradBrush, 0, 0, w, 2);
            }

            // TitleBar Icon Badge
            int iconBoxSize = 26;
            int iconBoxX = 14;
            int iconBoxY = (titleBarH - iconBoxSize) / 2;
            Rectangle iconBox = new Rectangle(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize);
            DrawRoundedRectangle(g, iconBox, 6, Color.FromArgb(20, 20, 24), Color.FromArgb(48, 48, 56), 1f);
            if (_logoImage != null)
            {
                g.DrawImage(_logoImage, new Rectangle(iconBoxX + 3, iconBoxY + 3, iconBoxSize - 6, iconBoxSize - 6));
            }
            else
            {
                using (var f = new Font("Segoe UI", 8.0f, FontStyle.Bold))
                using (var b = new SolidBrush(Color.FromArgb(255, 138, 0)))
                {
                    g.DrawString("KM", f, b, iconBoxX + 3, iconBoxY + 4);
                }
            }

            // TitleBar Text & Version Pill
            using (var fBold = new Font("Segoe UI", 9.5f, FontStyle.Bold))
            using (var bWhite = new SolidBrush(Color.FromArgb(245, 245, 245)))
            {
                g.DrawString("KnowToMigrate Setup", fBold, bWhite, 48, 13);
            }

            // Version Badge Pill
            Rectangle verPill = new Rectangle(204, 13, 108, 20);
            DrawRoundedRectangle(g, verPill, 10, Color.FromArgb(28, 18, 12), Color.FromArgb(124, 45, 18), 1f);
            using (var fPill = new Font("Segoe UI", 8.0f, FontStyle.Bold))
            {
                TextRenderer.DrawText(g, "v1.0.6 • Transit", fPill, verPill, Color.FromArgb(255, 138, 0), TextFormatFlags.HorizontalCenter | TextFormatFlags.VerticalCenter);
            }

            // Minimize Button
            Rectangle minRect = new Rectangle(w - 90, 0, 44, titleBarH);
            if (_isMinHovered)
            {
                using (var b = new SolidBrush(Color.FromArgb(32, 32, 38)))
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

            // Subtle pulsing brand orange glow
            int glowAlpha = (int)(28 + 14 * Math.Sin(_pulseAngle));
            using (var glowBrush = new SolidBrush(Color.FromArgb(glowAlpha, 255, 90, 0)))
            {
                g.FillEllipse(glowBrush, logoBoxX - 6, logoBoxY - 6, logoBoxSize + 12, logoBoxSize + 12);
            }

            DrawRoundedRectangle(g, logoBox, 14, Color.FromArgb(16, 16, 20), Color.FromArgb(255, 90, 0), 1.5f);
            if (_logoImage != null)
            {
                g.DrawImage(_logoImage, new Rectangle(logoBoxX + 6, logoBoxY + 6, logoBoxSize - 12, logoBoxSize - 12));
            }
            else
            {
                using (var f = new Font("Segoe UI", 16.0f, FontStyle.Bold))
                using (var b = new SolidBrush(Color.FromArgb(255, 138, 0)))
                {
                    g.DrawString("KM", f, b, logoBoxX + 8, logoBoxY + 12);
                }
            }

            // 4. Middle Content Cards based on step
            if (_currentStep == SetupStep.Welcome)
            {
                // Destination Card
                Rectangle destCard = new Rectangle(36, 140, 488, 64);
                DrawRoundedRectangle(g, destCard, 10, Color.FromArgb(16, 16, 20), Color.FromArgb(36, 36, 42), 1f);

                using (var fLabel = new Font("Segoe UI", 8.5f, FontStyle.Bold))
                using (var fPath = new Font("Segoe UI", 9.5f, FontStyle.Regular))
                using (var fExtra = new Font("Segoe UI", 8.5f, FontStyle.Regular))
                using (var bMuted = new SolidBrush(Color.FromArgb(161, 161, 170)))
                using (var bWhite = new SolidBrush(Color.White))
                using (var bDim = new SolidBrush(Color.FromArgb(113, 113, 122)))
                {
                    g.DrawString("INSTALLATION DESTINATION", fLabel, bMuted, 52, 150);
                    g.DrawString(Program.InstallDir, fPath, bWhite, 52, 172);
                    g.DrawString("Native 64-bit • ~73 MB", fExtra, bDim, 370, 172);
                }

                // Options Container Card
                Rectangle optCard = new Rectangle(36, 218, 488, 150);
                DrawRoundedRectangle(g, optCard, 10, Color.FromArgb(14, 14, 18), Color.FromArgb(30, 30, 36), 1f);

                using (var fLabel = new Font("Segoe UI", 8.5f, FontStyle.Bold))
                using (var bMuted = new SolidBrush(Color.FromArgb(161, 161, 170)))
                {
                    g.DrawString("SETUP PREFERENCES", fLabel, bMuted, 52, 230);
                }
            }
            else if (_currentStep == SetupStep.InstallFinished)
            {
                // Glowing orange checkmark circle
                int cx = 70;
                int cy = 205;
                using (var bGlow = new SolidBrush(Color.FromArgb(40, 255, 90, 0)))
                {
                    g.FillEllipse(bGlow, cx - 30, cy - 30, 60, 60);
                }
                Rectangle checkCircle = new Rectangle(cx - 24, cy - 24, 48, 48);
                using (var brush = new LinearGradientBrush(checkCircle, Color.FromArgb(255, 77, 0), Color.FromArgb(255, 138, 0), 45f))
                {
                    g.FillEllipse(brush, checkCircle);
                }
                using (var penCheck = new Pen(Color.FromArgb(10, 10, 10), 3.5f))
                {
                    penCheck.StartCap = LineCap.Round;
                    penCheck.EndCap = LineCap.Round;
                    g.DrawLine(penCheck, cx - 11, cy + 1, cx - 3, cy + 9);
                    g.DrawLine(penCheck, cx - 3, cy + 9, cx + 11, cy - 8);
                }

                using (var fBig = new Font("Segoe UI", 13.5f, FontStyle.Bold))
                using (var fSub = new Font("Segoe UI", 9.5f, FontStyle.Regular))
                using (var bWhite = new SolidBrush(Color.White))
                using (var bMuted = new SolidBrush(Color.FromArgb(161, 161, 170)))
                {
                    g.DrawString("Installation Complete!", fBig, bWhite, 110, 192);
                    g.DrawString("KnowToMigrate is ready. Transit Transfer Engine is installed.", fSub, bMuted, 110, 220);
                }

                // Options card
                Rectangle finishCard = new Rectangle(36, 260, 488, 110);
                DrawRoundedRectangle(g, finishCard, 10, Color.FromArgb(14, 14, 18), Color.FromArgb(32, 32, 38), 1f);
            }
            else if (_currentStep == SetupStep.UninstallConfirm)
            {
                // Warning badge
                int cx = 70;
                int cy = 195;
                using (var bWarn = new SolidBrush(Color.FromArgb(220, 38, 38)))
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
                using (var bMuted = new SolidBrush(Color.FromArgb(161, 161, 170)))
                {
                    g.DrawString("Are you sure you want to uninstall KnowToMigrate?", fBig, bWhite, 110, 184);
                    g.DrawString("This will remove all installed application binaries and shortcuts.", fSub, bMuted, 110, 212);
                }

                Rectangle uninstCard = new Rectangle(36, 255, 488, 80);
                DrawRoundedRectangle(g, uninstCard, 10, Color.FromArgb(18, 12, 12), Color.FromArgb(60, 30, 30), 1f);
            }
            else if (_currentStep == SetupStep.UninstallFinished)
            {
                // Orange Checkmark
                int cx = 70;
                int cy = 205;
                Rectangle checkCircle = new Rectangle(cx - 24, cy - 24, 48, 48);
                using (var brush = new LinearGradientBrush(checkCircle, Color.FromArgb(255, 77, 0), Color.FromArgb(255, 138, 0), 45f))
                {
                    g.FillEllipse(brush, checkCircle);
                }
                using (var penCheck = new Pen(Color.FromArgb(10, 10, 10), 3.5f))
                {
                    penCheck.StartCap = LineCap.Round;
                    penCheck.EndCap = LineCap.Round;
                    g.DrawLine(penCheck, cx - 11, cy + 1, cx - 3, cy + 9);
                    g.DrawLine(penCheck, cx - 3, cy + 9, cx + 11, cy - 8);
                }

                using (var fBig = new Font("Segoe UI", 13.5f, FontStyle.Bold))
                using (var fSub = new Font("Segoe UI", 9.5f, FontStyle.Regular))
                using (var bWhite = new SolidBrush(Color.White))
                using (var bMuted = new SolidBrush(Color.FromArgb(161, 161, 170)))
                {
                    g.DrawString("Uninstallation Complete!", fBig, bWhite, 110, 192);
                    g.DrawString("KnowToMigrate has been cleanly removed from this machine.", fSub, bMuted, 110, 220);
                }
            }

            // 5. Bottom Separator
            using (var pen = new Pen(Color.FromArgb(26, 26, 32), 1f))
            {
                g.DrawLine(pen, 0, h - 78, w, h - 78);
            }

            // 6. Outer Subtle Border
            using (var borderPen = new Pen(Color.FromArgb(42, 42, 50), 1f))
            {
                g.DrawRectangle(borderPen, 0, 0, w - 1, h - 1);
            }
        }

        public static void DrawRoundedRectangle(Graphics g, Rectangle r, int radius, Color backColor, Color borderColor, float borderWidth)
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

        public static GraphicsPath CreateRoundedPath(Rectangle r, int radius)
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

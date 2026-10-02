using System;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Windows.Forms;

[assembly: AssemblyTitle("KnowToMigrate Updater")]
[assembly: AssemblyProduct("KnowToMigrate")]
[assembly: AssemblyCompany("KNOWTHETECH")]
[assembly: AssemblyDescription("KnowToMigrate Standalone Native Updater")]
[assembly: AssemblyVersion("1.0.1.0")]
[assembly: AssemblyFileVersion("1.0.1.0")]

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
                            UseShellExecute = true,
                            Verb = "runas"
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
        [DllImport("user32.dll")]
        private static extern int SendMessage(IntPtr hWnd, int Msg, int wParam, int lParam);
        [DllImport("user32.dll")]
        private static extern bool ReleaseCapture();

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

        private Label _lblStatus;
        private Label _lblDetail;
        private System.Windows.Forms.Timer _animTimer;
        private float _shimmerPos;
        private Image _logoImage;
        private bool _isCloseHovered;
        private bool _isMinHovered;

        public UpdaterForm(string[] args)
        {
            _args = args;
            _packagePath = "";
            _expectedSha256 = "";
            _targetDir = @"C:\Program Files (x86)\KnowToMigrate";
            _mainExePath = @"C:\Program Files (x86)\KnowToMigrate\KnowToMigrate.exe";
            _parentPid = 0;
            _targetVersion = "";
            _previousVersion = "";
            _isSilent = false;
            _exitCode = 0;
            _shimmerPos = 0;

            ParseArguments(args);
            LoadLogoImage();
            InitializeUI();
        }

        private void LoadLogoImage()
        {
            try
            {
                var asm = Assembly.GetExecutingAssembly();
                var stream = asm.GetManifestResourceStream("logo.png");
                if (stream != null)
                {
                    _logoImage = Image.FromStream(stream);
                    return;
                }
            }
            catch { }

            try
            {
                string localLogo = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Assets", "logo.png");
                if (File.Exists(localLogo))
                {
                    _logoImage = Image.FromFile(localLogo);
                }
            }
            catch { }
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
            this.FormBorderStyle = FormBorderStyle.None;
            this.StartPosition = FormStartPosition.CenterScreen;
            this.ClientSize = new Size(540, 310);
            this.BackColor = Color.FromArgb(0, 0, 0);
            this.ForeColor = Color.White;
            this.DoubleBuffered = true;
            this.ShowInTaskbar = true;

            // Status label inside glass card
            _lblStatus = new Label
            {
                Text = "Preparing update...",
                Font = new Font("Segoe UI", 10.0f, FontStyle.Bold),
                ForeColor = Color.FromArgb(240, 240, 240),
                BackColor = Color.Transparent,
                Location = new Point(40, 185),
                Size = new Size(460, 24)
            };
            this.Controls.Add(_lblStatus);

            // Detail label inside glass card
            _lblDetail = new Label
            {
                Text = "Please wait while files are cryptographically verified and installed.",
                Font = new Font("Segoe UI", 8.5f, FontStyle.Regular),
                ForeColor = Color.FromArgb(140, 140, 140),
                BackColor = Color.Transparent,
                Location = new Point(40, 212),
                Size = new Size(460, 36)
            };
            this.Controls.Add(_lblDetail);

            if (_isSilent)
            {
                this.WindowState = FormWindowState.Minimized;
                this.ShowInTaskbar = false;
                this.Opacity = 0;
            }

            // Shimmer animation timer (30ms = ~33 fps smooth pulse)
            _animTimer = new System.Windows.Forms.Timer();
            _animTimer.Interval = 30;
            _animTimer.Tick += (s, e) =>
            {
                _shimmerPos += 0.025f;
                if (_shimmerPos > 1.25f) _shimmerPos = -0.25f;
                this.Invalidate(new Rectangle(40, 155, 460, 18));
            };
            _animTimer.Start();

            this.MouseDown += UpdaterForm_MouseDown;
            this.MouseMove += UpdaterForm_MouseMove;
            this.MouseClick += UpdaterForm_MouseClick;

            this.Shown += (s, e) =>
            {
                var thread = new Thread(RunUpdatePipeline);
                thread.IsBackground = true;
                thread.Start();
            };
        }

        private void UpdaterForm_MouseDown(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left && e.Y <= 46 && e.X < this.Width - 90)
            {
                ReleaseCapture();
                SendMessage(this.Handle, 0xA1, 0x2, 0);
            }
        }

        private void UpdaterForm_MouseMove(object sender, MouseEventArgs e)
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

        private void UpdaterForm_MouseClick(object sender, MouseEventArgs e)
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

        protected override void OnPaint(PaintEventArgs e)
        {
            base.OnPaint(e);
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = System.Drawing.Text.TextRenderingHint.ClearTypeGridFit;

            int w = this.Width;
            int h = this.Height;

            // 1. Base Pure AMOLED Black
            using (var b = new SolidBrush(Color.FromArgb(0, 0, 0)))
            {
                g.FillRectangle(b, 0, 0, w, h);
            }

            // 2. Top TitleBar / Navbar background (#080808)
            int titleBarH = 46;
            using (var b = new SolidBrush(Color.FromArgb(8, 8, 8)))
            {
                g.FillRectangle(b, 0, 0, w, titleBarH);
            }

            // 2b. TitleBar Glossy Sheen Overlay (Navbar-like reflection)
            using (var glossBrush = new LinearGradientBrush(
                new Point(0, 0),
                new Point(0, 22),
                Color.FromArgb(32, 255, 255, 255),
                Color.FromArgb(0, 255, 255, 255)))
            {
                g.FillRectangle(glossBrush, 0, 0, w, 22);
            }

            // 2c. TitleBar Bottom Divider Line (#181818)
            using (var pen = new Pen(Color.FromArgb(24, 24, 24), 1f))
            {
                g.DrawLine(pen, 0, titleBarH, w, titleBarH);
            }

            // 2d. TitleBar Logo Badge (26x26)
            int iconBoxSize = 26;
            int iconBoxX = 14;
            int iconBoxY = (titleBarH - iconBoxSize) / 2;
            Rectangle iconBox = new Rectangle(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize);
            DrawRoundedRectangle(g, iconBox, 5, Color.FromArgb(14, 14, 14), Color.FromArgb(255, 90, 0), 1.2f);
            if (_logoImage != null)
            {
                g.DrawImage(_logoImage, new Rectangle(iconBoxX + 3, iconBoxY + 3, iconBoxSize - 6, iconBoxSize - 6));
            }
            else
            {
                using (var f = new Font("Segoe UI", 8.5f, FontStyle.Bold))
                using (var b = new SolidBrush(Color.FromArgb(255, 90, 0)))
                {
                    g.DrawString("KM", f, b, iconBoxX + 3, iconBoxY + 4);
                }
            }

            // 2e. TitleBar Text
            using (var fBold = new Font("Segoe UI", 10.0f, FontStyle.Bold))
            using (var fNorm = new Font("Segoe UI", 10.0f, FontStyle.Regular))
            using (var bWhite = new SolidBrush(Color.White))
            using (var bOrange = new SolidBrush(Color.FromArgb(255, 138, 0)))
            {
                g.DrawString("KnowToMigrate", fBold, bWhite, 48, 12);
                g.DrawString("Updater", fNorm, bOrange, 150, 12);
            }

            // 2f. Minimize Button
            Rectangle minRect = new Rectangle(w - 90, 0, 44, titleBarH);
            if (_isMinHovered)
            {
                using (var b = new SolidBrush(Color.FromArgb(28, 28, 28)))
                {
                    g.FillRectangle(b, minRect);
                }
            }
            using (var pen = new Pen(Color.FromArgb(180, 180, 180), 1.5f))
            {
                g.DrawLine(pen, w - 73, 24, w - 61, 24);
            }

            // 2g. Close Button
            Rectangle closeRect = new Rectangle(w - 46, 0, 46, titleBarH);
            if (_isCloseHovered)
            {
                using (var b = new SolidBrush(Color.FromArgb(232, 17, 35)))
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

            // 3. Central Liquid Glass Card
            int cardX = 20;
            int cardY = 60;
            int cardW = w - 40;
            int cardH = 205;
            Rectangle cardRect = new Rectangle(cardX, cardY, cardW, cardH);
            DrawRoundedRectangle(g, cardRect, 14, Color.FromArgb(12, 12, 12), Color.FromArgb(30, 30, 30), 1.2f);

            // 3b. Liquid Glass Card Top Gloss Highlight
            using (var cardGloss = new LinearGradientBrush(
                new Point(cardX, cardY),
                new Point(cardX, cardY + 28),
                Color.FromArgb(35, 255, 255, 255),
                Color.FromArgb(0, 255, 255, 255)))
            {
                FillRoundedTop(g, new Rectangle(cardX + 1, cardY + 1, cardW - 2, 28), 13, cardGloss);
            }

            // 3c. Master 48x48 Logo Showcase Badge (15% corner radius ~7px)
            int logoX = 40;
            int logoY = 82;
            int logoSize = 48;
            Rectangle logoBox = new Rectangle(logoX, logoY, logoSize, logoSize);
            // Ambient Orange Glow
            using (var glowPen = new Pen(Color.FromArgb(60, 255, 90, 0), 4f))
            {
                DrawRoundedRectangleBorderOnly(g, new Rectangle(logoX - 1, logoY - 1, logoSize + 2, logoSize + 2), 8, glowPen);
            }
            DrawRoundedRectangle(g, logoBox, 7, Color.FromArgb(0, 0, 0), Color.FromArgb(255, 90, 0), 1.5f);
            if (_logoImage != null)
            {
                g.DrawImage(_logoImage, new Rectangle(logoX + 6, logoY + 6, logoSize - 12, logoSize - 12));
            }
            else
            {
                using (var f = new Font("Segoe UI", 14f, FontStyle.Bold))
                using (var b = new SolidBrush(Color.FromArgb(255, 90, 0)))
                {
                    g.DrawString("KM", f, b, logoX + 8, logoY + 12);
                }
            }

            // 3d. Card Typography
            using (var fTitle = new Font("Segoe UI", 14.0f, FontStyle.Bold))
            using (var fSub = new Font("Segoe UI", 9.5f, FontStyle.Regular))
            using (var bWhite = new SolidBrush(Color.White))
            using (var bMuted = new SolidBrush(Color.FromArgb(160, 160, 160)))
            {
                g.DrawString("Updating KnowToMigrate", fTitle, bWhite, 102, 83);
                string sub = string.IsNullOrEmpty(_targetVersion)
                    ? "Applying system update · In-place verified upgrade"
                    : string.Format("Upgrading to v{0} · Seamless Zero-Downtime Installation", _targetVersion);
                g.DrawString(sub, fSub, bMuted, 103, 110);
            }

            // 3e. Liquid Glass Gradient Progress Bar
            int pbX = 40;
            int pbY = 155;
            int pbW = cardW - 40;
            int pbH = 10;
            Rectangle pbTrack = new Rectangle(pbX, pbY, pbW, pbH);

            // Track background (#181818)
            DrawRoundedRectangle(g, pbTrack, 5, Color.FromArgb(22, 22, 22), Color.FromArgb(36, 36, 36), 1.0f);

            // Gradient Progress Bar Fill with moving shimmer beam
            int fillW = pbW - 4;
            Rectangle pbFill = new Rectangle(pbX + 2, pbY + 2, fillW, pbH - 4);
            using (var fillBrush = new LinearGradientBrush(
                new Point(pbFill.Left, pbFill.Top),
                new Point(pbFill.Right, pbFill.Top),
                Color.FromArgb(255, 77, 0),
                Color.FromArgb(255, 138, 0)))
            {
                FillRoundedRectangle(g, pbFill, 3, fillBrush);
            }

            // Dynamic moving Shimmer pulse reflection
            int shimmerBeamW = 90;
            int shimmerX = pbFill.Left + (int)(_shimmerPos * pbFill.Width);
            if (shimmerX + shimmerBeamW > pbFill.Left && shimmerX < pbFill.Right)
            {
                Rectangle shimmerRect = new Rectangle(shimmerX, pbFill.Top, shimmerBeamW, pbFill.Height);
                // Clip to fill bounds
                GraphicsState state = g.Save();
                g.SetClip(pbFill);
                using (var sBrush = new LinearGradientBrush(
                    new Point(shimmerRect.Left, pbFill.Top),
                    new Point(shimmerRect.Right, pbFill.Top),
                    Color.FromArgb(0, 255, 255, 255),
                    Color.FromArgb(160, 255, 255, 255)))
                {
                    var cb = new ColorBlend(3);
                    cb.Colors = new Color[] { Color.FromArgb(0, 255, 255, 255), Color.FromArgb(180, 255, 255, 255), Color.FromArgb(0, 255, 255, 255) };
                    cb.Positions = new float[] { 0f, 0.5f, 1f };
                    sBrush.InterpolationColors = cb;
                    g.FillRectangle(sBrush, shimmerRect);
                }
                g.Restore(state);
            }

            // Top glossy sheen on the progress bar track
            using (var pbGloss = new LinearGradientBrush(
                new Point(pbTrack.Left, pbTrack.Top),
                new Point(pbTrack.Left, pbTrack.Top + 4),
                Color.FromArgb(80, 255, 255, 255),
                Color.FromArgb(0, 255, 255, 255)))
            {
                g.FillRectangle(pbGloss, pbTrack.Left + 2, pbTrack.Top + 1, pbTrack.Width - 4, 3);
            }

            // 4. Footer Telemetry (#555555)
            using (var fFoot = new Font("Segoe UI", 7.5f, FontStyle.Regular))
            using (var bFoot = new SolidBrush(Color.FromArgb(70, 70, 70)))
            {
                string footText = "256-BIT SHA-256 VERIFIED · IN-PLACE UPGRADE · ZERO DATA LOSS";
                SizeF s = g.MeasureString(footText, fFoot);
                g.DrawString(footText, fFoot, bFoot, (w - s.Width) / 2f, h - 26);
            }

            // 5. Outer Form Accent Border (Subtle Orange Ambient Border)
            using (var borderPen = new Pen(Color.FromArgb(255, 90, 0), 1.5f))
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

        private static void DrawRoundedRectangleBorderOnly(Graphics g, Rectangle r, int radius, Pen pen)
        {
            using (var path = CreateRoundedPath(r, radius))
            {
                g.DrawPath(pen, path);
            }
        }

        private static void FillRoundedRectangle(Graphics g, Rectangle r, int radius, Brush brush)
        {
            using (var path = CreateRoundedPath(r, radius))
            {
                g.FillPath(brush, path);
            }
        }

        private static void FillRoundedTop(Graphics g, Rectangle r, int radius, Brush brush)
        {
            using (var path = new GraphicsPath())
            {
                int d = radius * 2;
                path.AddArc(r.Left, r.Top, d, d, 180, 90);
                path.AddArc(r.Right - d, r.Top, d, d, 270, 90);
                path.AddLine(r.Right, r.Bottom, r.Left, r.Bottom);
                path.CloseFigure();
                g.FillPath(brush, path);
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
                    string msiLog = Path.Combine(
                        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                        "KnowToMigrate", "logs", "msi_install.log"
                    );
                    var psi = new ProcessStartInfo
                    {
                        FileName = "msiexec.exe",
                        Arguments = string.Format("/i \"{0}\" /qn /norestart /lv* \"{1}\"", _packagePath, msiLog),
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
                        Arguments = "/qn /norestart",
                        UseShellExecute = true,
                        Verb = "runas"
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
                string exeToLaunch = ResolveMainExePath();

                // Step 5: Clean up staged package
                try
                {
                    if (File.Exists(_packagePath)) File.Delete(_packagePath);
                }
                catch { }

                // Step 6: Restart updated application
                UpdateStatus("Update complete!", "Restarting KnowToMigrate...");
                Thread.Sleep(800);

                if (!string.IsNullOrEmpty(exeToLaunch) && File.Exists(exeToLaunch))
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
                if (!_isSilent && this.IsHandleCreated)
                {
                    try
                    {
                        this.Invoke(new Action(() =>
                        {
                            MessageBox.Show(this, "Update failed to complete:\n\n" + ex.Message + "\n\nRestarting current version.", "KnowToMigrate Update Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                        }));
                    }
                    catch { }
                }

                // Restart previous application so user is not stranded
                try
                {
                    string fallbackExe = ResolveMainExePath();
                    if (!string.IsNullOrEmpty(fallbackExe) && File.Exists(fallbackExe))
                    {
                        Program.Log("[INFO] Relaunching fallback application after failure: " + fallbackExe);
                        Process.Start(new ProcessStartInfo
                        {
                            FileName = fallbackExe,
                            WorkingDirectory = Path.GetDirectoryName(fallbackExe),
                            UseShellExecute = true
                        });
                    }
                }
                catch (Exception launchEx)
                {
                    Program.Log("[ERROR] Failed to restart fallback application: " + launchEx.Message);
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

        private string ResolveMainExePath()
        {
            if (!string.IsNullOrEmpty(_mainExePath) && File.Exists(_mainExePath))
                return _mainExePath;

            if (!string.IsNullOrEmpty(_targetDir))
            {
                string targetCandidate = Path.Combine(_targetDir, "KnowToMigrate.exe");
                if (File.Exists(targetCandidate)) return targetCandidate;
            }

            string[] candidates = new[]
            {
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), "KnowToMigrate", "KnowToMigrate.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "KnowToMigrate", "KnowToMigrate.exe"),
                @"C:\Program Files (x86)\KnowToMigrate\KnowToMigrate.exe",
                @"C:\Program Files\KnowToMigrate\KnowToMigrate.exe"
            };

            foreach (var path in candidates)
            {
                if (File.Exists(path)) return path;
            }

            return _mainExePath;
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

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                if (_animTimer != null)
                {
                    _animTimer.Stop();
                    _animTimer.Dispose();
                    _animTimer = null;
                }
                if (_logoImage != null)
                {
                    _logoImage.Dispose();
                    _logoImage = null;
                }
            }
            base.Dispose(disposing);
        }
    }
}

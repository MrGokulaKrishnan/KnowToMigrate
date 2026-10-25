using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Threading.Tasks;
using System.Windows;
using Microsoft.Win32;

namespace KnowToMigrate.Setup
{
    public partial class MainWindow : Window
    {
        private const string InstallDir = @"C:\Program Files\KnowToMigrate";
        private const string AppExePath = @"C:\Program Files\KnowToMigrate\KnowToMigrate.exe";
        private const string AppIconPath = @"C:\Program Files\KnowToMigrate\Assets\KnowToMigrate.ico";
        private const string AppLogoPath = @"C:\Program Files\KnowToMigrate\Assets\logo.jpg";
        private const string StartMenuLnk = @"C:\ProgramData\Microsoft\Windows\Start Menu\Programs\KnowToMigrate.lnk";
        private static readonly string UserDesktopLnk = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), "KnowToMigrate.lnk");
        private static readonly string CommonDesktopLnk = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonDesktopDirectory), "KnowToMigrate.lnk");

        private bool _isFinished = false;
        private bool _isUninstallMode = false;

        public MainWindow()
        {
            InitializeComponent();
            Loaded += MainWindow_Loaded;
        }

        private void MainWindow_Loaded(object sender, RoutedEventArgs e)
        {
            string[] args = Environment.GetCommandLineArgs();
            if (args != null && args.Length > 1)
            {
                foreach (string a in args)
                {
                    if (string.IsNullOrEmpty(a)) continue;
                    string lower = a.ToLowerInvariant();
                    if (lower.Contains("uninstall") || lower.Contains("/remove") || lower.Contains("-remove") || lower.Equals("/x"))
                    {
                        SwitchToUninstallMode();
                        return;
                    }
                }
            }

            if (File.Exists(AppExePath))
            {
                BtnInstall.Content = "Reinstall Now";
                BtnSwitchUninstall.Visibility = Visibility.Visible;
            }
        }

        private void SwitchToUninstallMode()
        {
            _isUninstallMode = true;
            PnlSetupMode.Visibility = Visibility.Collapsed;
            PnlFinish.Visibility = Visibility.Collapsed;
            PnlUninstall.Visibility = Visibility.Visible;
            BtnSwitchUninstall.Visibility = Visibility.Collapsed;
            BtnInstall.Content = "Uninstall Now";
            BtnInstall.Background = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(220, 38, 38));
            BtnInstall.Foreground = System.Windows.Media.Brushes.White;
        }

        private void BtnClose_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }

        private void BtnCancel_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }

        private void BtnSwitchUninstall_Click(object sender, RoutedEventArgs e)
        {
            SwitchToUninstallMode();
        }

        private async void BtnInstall_Click(object sender, RoutedEventArgs e)
        {
            if (_isFinished)
            {
                // CRUCIAL: USER CLICKED FINISH!
                if (!_isUninstallMode)
                {
                    bool ensureDesktop = ChkEnsureDesktop.IsChecked == true;
                    bool runNow = ChkRunNow.IsChecked == true;

                    if (ensureDesktop)
                    {
                        CreateShortcuts(true, true);
                    }

                    if (runNow && File.Exists(AppExePath))
                    {
                        try
                        {
                            Process.Start(new ProcessStartInfo
                            {
                                FileName = AppExePath,
                                WorkingDirectory = InstallDir,
                                UseShellExecute = true
                            });
                        }
                        catch { }
                    }
                }
                Close();
                return;
            }

            if (_isUninstallMode)
            {
                await PerformUninstallAsync();
                return;
            }

            await PerformInstallAsync();
        }

        private async Task PerformInstallAsync()
        {
            BtnInstall.IsEnabled = false;
            BtnCancel.IsEnabled = false;
            PnlProgress.Visibility = Visibility.Visible;

            bool createDesktop = ChkDesktop.IsChecked == true;
            bool createStartMenu = ChkStartMenu.IsChecked == true;

            try
            {
                TxtStatus.Text = "Closing any active KnowToMigrate instances...";
                ProgressBarInstall.Value = 10;
                await Task.Run(() =>
                {
                    foreach (var p in Process.GetProcessesByName("KnowToMigrate"))
                    {
                        try { p.Kill(); p.WaitForExit(3000); } catch { }
                    }
                });

                TxtStatus.Text = "Creating installation directories...";
                ProgressBarInstall.Value = 25;
                await Task.Run(() =>
                {
                    string assetsDir = Path.Combine(InstallDir, "Assets");
                    if (!Directory.Exists(assetsDir))
                    {
                        Directory.CreateDirectory(assetsDir);
                    }
                });

                TxtStatus.Text = "Extracting application components...";
                ProgressBarInstall.Value = 55;
                await Task.Run(() =>
                {
                    var asm = Assembly.GetExecutingAssembly();

                    // Extract KnowToMigrate.exe
                    using (var stream = asm.GetManifestResourceStream("Payload.KnowToMigrate.exe"))
                    {
                        if (stream != null)
                        {
                            using (var file = File.Create(AppExePath))
                            {
                                stream.CopyTo(file);
                            }
                        }
                    }

                    // Extract KnowToMigrate.ico
                    using (var stream = asm.GetManifestResourceStream("Assets.KnowToMigrate.ico"))
                    {
                        if (stream != null)
                        {
                            using (var file = File.Create(AppIconPath))
                            {
                                stream.CopyTo(file);
                            }
                        }
                    }

                    // Extract logo.jpg
                    using (var stream = asm.GetManifestResourceStream("Assets.logo.jpg"))
                    {
                        if (stream != null)
                        {
                            using (var file = File.Create(AppLogoPath))
                            {
                                stream.CopyTo(file);
                            }
                        }
                    }
                });

                TxtStatus.Text = "Configuring Start Menu and Desktop shortcuts...";
                ProgressBarInstall.Value = 75;
                await Task.Run(() =>
                {
                    CreateShortcuts(createDesktop, createStartMenu);
                });

                TxtStatus.Text = "Registering system application identity...";
                ProgressBarInstall.Value = 90;
                await Task.Run(() =>
                {
                    RegisterSystemIdentity();
                });

                ProgressBarInstall.Value = 100;
                TxtStatus.Text = "Installation completed successfully!";

                // TRANSITION TO FINISH SCREEN — NEVER CLOSE AUTOMATICALLY!
                PnlSetupMode.Visibility = Visibility.Collapsed;
                PnlFinish.Visibility = Visibility.Visible;
                BtnSwitchUninstall.Visibility = Visibility.Collapsed;
                BtnCancel.Visibility = Visibility.Collapsed;

                BtnInstall.Content = "Finish";
                BtnInstall.Background = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(74, 222, 128));
                BtnInstall.Foreground = System.Windows.Media.Brushes.Black;
                BtnInstall.IsEnabled = true;
                _isFinished = true;
            }
            catch (Exception ex)
            {
                TxtStatus.Text = "Error: " + ex.Message;
                MessageBox.Show("Installation failed:\n" + ex.Message, "KnowToMigrate Setup", MessageBoxButton.OK, MessageBoxImage.Error);
                BtnInstall.IsEnabled = true;
                BtnCancel.IsEnabled = true;
            }
        }

        private async Task PerformUninstallAsync()
        {
            BtnInstall.IsEnabled = false;
            BtnCancel.IsEnabled = false;
            PnlUninstallProgress.Visibility = Visibility.Visible;
            bool removeCache = ChkRemoveConfig.IsChecked == true;

            try
            {
                TxtUninstallStatus.Text = "Closing running KnowToMigrate instances...";
                ProgressBarUninstall.Value = 20;
                await Task.Run(() =>
                {
                    foreach (var p in Process.GetProcessesByName("KnowToMigrate"))
                    {
                        try { p.Kill(); p.WaitForExit(3000); } catch { }
                    }
                });

                TxtUninstallStatus.Text = "Removing Desktop and Start Menu shortcuts...";
                ProgressBarUninstall.Value = 45;
                await Task.Run(() =>
                {
                    RemoveAllShortcuts();
                });

                TxtUninstallStatus.Text = "Unregistering system entries and URL schemes...";
                ProgressBarUninstall.Value = 70;
                await Task.Run(() =>
                {
                    CleanRegistry();
                });

                TxtUninstallStatus.Text = "Removing application files...";
                ProgressBarUninstall.Value = 90;
                await Task.Run(() =>
                {
                    try { if (Directory.Exists(InstallDir)) Directory.Delete(InstallDir, true); } catch { }

                    if (removeCache)
                    {
                        try
                        {
                            string localData = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "KnowToMigrate");
                            if (Directory.Exists(localData)) Directory.Delete(localData, true);
                        }
                        catch { }
                    }
                });

                ProgressBarUninstall.Value = 100;
                TxtUninstallStatus.Text = "Uninstallation complete!";

                // TRANSITION TO UNINSTALL FINISH
                PnlUninstall.Visibility = Visibility.Collapsed;
                PnlFinish.Visibility = Visibility.Visible;
                ChkRunNow.Visibility = Visibility.Collapsed;
                ChkEnsureDesktop.Visibility = Visibility.Collapsed;

                BtnCancel.Visibility = Visibility.Collapsed;
                BtnInstall.Content = "Finish";
                BtnInstall.Background = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(74, 222, 128));
                BtnInstall.Foreground = System.Windows.Media.Brushes.Black;
                BtnInstall.IsEnabled = true;
                _isFinished = true;
            }
            catch (Exception ex)
            {
                MessageBox.Show("Uninstallation failed:\n" + ex.Message, "KnowToMigrate Setup", MessageBoxButton.OK, MessageBoxImage.Error);
                BtnInstall.IsEnabled = true;
                BtnCancel.IsEnabled = true;
            }
        }

        private static void CreateShortcuts(bool createDesktop, bool createStartMenu)
        {
            if (createStartMenu)
            {
                CreateShortcut(StartMenuLnk, AppExePath, InstallDir, AppIconPath);
                try
                {
                    string userStartMenu = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), "KnowToMigrate.lnk");
                    CreateShortcut(userStartMenu, AppExePath, InstallDir, AppIconPath);
                }
                catch { }
            }

            if (createDesktop)
            {
                CreateShortcut(UserDesktopLnk, AppExePath, InstallDir, AppIconPath);
                CreateShortcut(CommonDesktopLnk, AppExePath, InstallDir, AppIconPath);
            }
        }

        private static void RemoveAllShortcuts()
        {
            string[] paths = new string[]
            {
                UserDesktopLnk,
                CommonDesktopLnk,
                StartMenuLnk,
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), "KnowToMigrate.lnk")
            };

            foreach (var p in paths)
            {
                try
                {
                    if (File.Exists(p)) File.Delete(p);
                }
                catch { }
            }
        }

        private static void CreateShortcut(string shortcutPath, string targetExe, string workingDir, string iconPath)
        {
            try
            {
                string? dir = Path.GetDirectoryName(shortcutPath);
                if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                {
                    Directory.CreateDirectory(dir);
                }

                Type? shellType = Type.GetTypeFromProgID("WScript.Shell");
                if (shellType != null)
                {
                    dynamic shell = Activator.CreateInstance(shellType)!;
                    dynamic shortcut = shell.CreateShortcut(shortcutPath);
                    shortcut.TargetPath = targetExe;
                    shortcut.WorkingDirectory = workingDir;
                    shortcut.Description = "KnowToMigrate - Move Anything. Anywhere. Seamlessly.";
                    shortcut.IconLocation = iconPath + ",0";
                    shortcut.Save();
                    return;
                }
            }
            catch { }

            // Fallback via PowerShell
            try
            {
                string script = $"$s = (New-Object -ComObject WScript.Shell).CreateShortcut('{shortcutPath}'); $s.TargetPath = '{targetExe}'; $s.WorkingDirectory = '{workingDir}'; $s.IconLocation = '{iconPath},0'; $s.Save()";
                Process.Start(new ProcessStartInfo
                {
                    FileName = "powershell.exe",
                    Arguments = $"-NoProfile -Command \"{script}\"",
                    CreateNoWindow = true,
                    UseShellExecute = false
                })?.WaitForExit();
            }
            catch { }
        }

        private static void RegisterSystemIdentity()
        {
            try
            {
                // 1. App Paths: Win+R "knowtomigrate"
                using (var appPathKey = Registry.LocalMachine.CreateSubKey(@"SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\KnowToMigrate.exe"))
                {
                    appPathKey.SetValue("", AppExePath);
                    appPathKey.SetValue("Path", InstallDir);
                }

                // 2. Applications registration for Windows Search FriendlyAppName & Icon
                using (var appKey = Registry.LocalMachine.CreateSubKey(@"SOFTWARE\Classes\Applications\KnowToMigrate.exe"))
                {
                    appKey.SetValue("FriendlyAppName", "KnowToMigrate");
                    appKey.SetValue("ApplicationCompany", "KnowToMigrate");
                }
                using (var iconKey = Registry.LocalMachine.CreateSubKey(@"SOFTWARE\Classes\Applications\KnowToMigrate.exe\DefaultIcon"))
                {
                    iconKey.SetValue("", AppIconPath + ",0");
                }
                using (var cmdKey = Registry.LocalMachine.CreateSubKey(@"SOFTWARE\Classes\Applications\KnowToMigrate.exe\shell\open\command"))
                {
                    cmdKey.SetValue("", $"\"{AppExePath}\"");
                }

                // 3. Uninstall registration for Windows Settings -> Installed Apps
                using (var uninstKey = Registry.LocalMachine.CreateSubKey(@"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\KnowToMigrate"))
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

                    string uninstallCmd = $"\"{AppExePath}\" /uninstall";
                    uninstKey.SetValue("UninstallString", uninstallCmd);
                }
            }
            catch { }
        }

        private static void CleanRegistry()
        {
            try { Registry.LocalMachine.DeleteSubKeyTree(@"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\KnowToMigrate", false); } catch { }
            try { Registry.LocalMachine.DeleteSubKeyTree(@"SOFTWARE\Classes\Applications\KnowToMigrate.exe", false); } catch { }
            try { Registry.LocalMachine.DeleteSubKeyTree(@"SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\KnowToMigrate.exe", false); } catch { }
            try { Registry.LocalMachine.DeleteSubKeyTree(@"Software\KnowToMigrate", false); } catch { }
            try { Registry.CurrentUser.DeleteSubKeyTree(@"Software\KnowToMigrate", false); } catch { }
        }
    }
}

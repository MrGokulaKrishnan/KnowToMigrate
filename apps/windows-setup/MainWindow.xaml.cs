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
        private static readonly string DesktopLnk = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonDesktopDirectory), "KnowToMigrate.lnk");

        public MainWindow()
        {
            InitializeComponent();
        }

        private void BtnClose_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }

        private void BtnCancel_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }

        private async void BtnInstall_Click(object sender, RoutedEventArgs e)
        {
            BtnInstall.IsEnabled = false;
            BtnCancel.IsEnabled = false;

            bool createDesktop = ChkDesktop.IsChecked == true;
            bool launchOnFinish = ChkLaunch.IsChecked == true;

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
                ProgressBarInstall.Value = 50;
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
                    CreateShortcut(StartMenuLnk, AppExePath, InstallDir, AppIconPath);

                    if (createDesktop)
                    {
                        CreateShortcut(DesktopLnk, AppExePath, InstallDir, AppIconPath);
                    }
                });

                TxtStatus.Text = "Registering system application identity...";
                ProgressBarInstall.Value = 90;
                await Task.Run(() =>
                {
                    RegisterSystemIdentity();
                });

                ProgressBarInstall.Value = 100;
                TxtStatus.Text = "Installation completed successfully!";
                TxtResult.Text = "Ready to use";
                TxtResult.Visibility = Visibility.Visible;

                if (launchOnFinish && File.Exists(AppExePath))
                {
                    Process.Start(new ProcessStartInfo
                    {
                        FileName = AppExePath,
                        WorkingDirectory = InstallDir,
                        UseShellExecute = true
                    });
                }

                await Task.Delay(1000);
                Close();
            }
            catch (Exception ex)
            {
                TxtStatus.Text = "Error: " + ex.Message;
                MessageBox.Show("Installation failed:\n" + ex.Message, "KnowToMigrate Setup", MessageBoxButton.OK, MessageBoxImage.Error);
                BtnInstall.IsEnabled = true;
                BtnCancel.IsEnabled = true;
            }
        }

        private static void CreateShortcut(string shortcutPath, string targetExe, string workingDir, string iconPath)
        {
            try
            {
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
                }
            }
            catch
            {
                // Fallback via PowerShell
                string script = $"$s = (New-Object -ComObject WScript.Shell).CreateShortcut('{shortcutPath}'); $s.TargetPath = '{targetExe}'; $s.WorkingDirectory = '{workingDir}'; $s.IconLocation = '{iconPath},0'; $s.Save()";
                Process.Start(new ProcessStartInfo
                {
                    FileName = "powershell.exe",
                    Arguments = $"-NoProfile -Command \"{script}\"",
                    CreateNoWindow = true,
                    UseShellExecute = false
                })?.WaitForExit();
            }
        }

        private static void RegisterSystemIdentity()
        {
            try
            {
                // 1. App Paths: Win+R "knowtomigrate" and direct Windows Search target
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
                    uninstKey.SetValue("DisplayVersion", "1.0.0");
                    uninstKey.SetValue("Publisher", "KnowToMigrate");
                    uninstKey.SetValue("DisplayIcon", AppIconPath + ",0");
                    uninstKey.SetValue("InstallLocation", InstallDir);
                    uninstKey.SetValue("HelpLink", "https://knowtomigrate.web.app/docs");
                    uninstKey.SetValue("URLInfoAbout", "https://knowtomigrate.web.app");
                    uninstKey.SetValue("NoModify", 1, RegistryValueKind.DWord);
                    uninstKey.SetValue("NoRepair", 1, RegistryValueKind.DWord);

                    string uninstallCmd = $"powershell.exe -WindowStyle Hidden -Command \"Remove-Item -Recurse -Force '{InstallDir}'; Remove-Item -Force '{StartMenuLnk}'; Remove-Item -Force '{DesktopLnk}'; Remove-Item -Recurse -Force 'HKLM:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\KnowToMigrate'; Remove-Item -Recurse -Force 'HKLM:\\SOFTWARE\\Classes\\Applications\\KnowToMigrate.exe'; Remove-Item -Recurse -Force 'HKLM:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\KnowToMigrate.exe'\"";
                    uninstKey.SetValue("UninstallString", uninstallCmd);
                }
            }
            catch { }
        }
    }
}

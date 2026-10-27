using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Interop;
using System.Windows.Threading;
using KnowToMigrate.Services;

namespace KnowToMigrate
{
    public static class KtmUiConstants
    {
        public const bool IsEncrypted = false;
        public const string SecurityLabel = "Not encrypted yet";
        public const string SecurityCaption = "Use on trusted networks only.";
        public const string ReleaseDate = "25 October 2026";
    }

    public partial class App : Application
    {
        [System.Runtime.InteropServices.DllImport("shell32.dll", SetLastError = true)]
        private static extern void SetCurrentProcessExplicitAppUserModelID([System.Runtime.InteropServices.MarshalAs(System.Runtime.InteropServices.UnmanagedType.LPWStr)] string AppID);

        public const string AppUserModelId = "KnowToMigrate.App";
        public static string[] InitialFilesToStage { get; private set; } = Array.Empty<string>();

        private MainWindow? _mainWindow;

        protected override void OnStartup(StartupEventArgs e)
        {
            // 1. Single Instance Check & Argument Forwarding
            if (!KtmSingleInstance.TryAcquire(e.Args))
            {
                Shutdown(0);
                return;
            }

            // 2. Windows AppUserModelID
            try
            {
                SetCurrentProcessExplicitAppUserModelID(AppUserModelId);
            }
            catch { }

            base.OnStartup(e);

            DispatcherUnhandledException += App_DispatcherUnhandledException;
            AppDomain.CurrentDomain.UnhandledException += CurrentDomain_UnhandledException;
            TaskScheduler.UnobservedTaskException += TaskScheduler_UnobservedTaskException;

            // 3. Command Line & Background Check
            bool isBackground = e.Args.Any(a => string.Equals(a, "--background", StringComparison.OrdinalIgnoreCase) ||
                                                string.Equals(a, "/background", StringComparison.OrdinalIgnoreCase) ||
                                                string.Equals(a, "-background", StringComparison.OrdinalIgnoreCase));

            var files = e.Args.Where(a => !a.StartsWith("-") && !a.StartsWith("/")).ToArray();
            if (files.Length > 0)
            {
                InitialFilesToStage = files;
            }

            if (isBackground)
            {
                var settings = UserSettingsManager.LoadSettings();
                if (!settings.StartWhenSignedIn)
                {
                    LogMessage("[Startup] Started with --background but StartWhenSignedIn is disabled. Exiting.");
                    Shutdown(0);
                    return;
                }
            }

            // 4. Instantiate MainWindow
            _mainWindow = new MainWindow();
            MainWindow = _mainWindow;

            if (isBackground)
            {
                // Force Win32 window handle creation without showing window so tray icon can register
                new WindowInteropHelper(_mainWindow).EnsureHandle();
                _mainWindow.InitializeBackgroundMode();
            }
            else
            {
                _mainWindow.Show();
                _mainWindow.Activate();
            }

            // 5. Start IPC Listener for argument forwarding and graceful updater shutdown
            KtmSingleInstance.StartListening(
                args =>
                {
                    Dispatcher.Invoke(() =>
                    {
                        _mainWindow?.HandleIpcArgs(args);
                    });
                },
                () =>
                {
                    Dispatcher.Invoke(() =>
                    {
                        _mainWindow?.TrayManager?.ForceShutdown();
                    });
                });
        }

        protected override void OnExit(ExitEventArgs e)
        {
            KtmSingleInstance.StopListening();
            base.OnExit(e);
        }

        private void App_DispatcherUnhandledException(object sender, DispatcherUnhandledExceptionEventArgs e)
        {
            LogCrash(e.Exception);
            MessageBox.Show(
                $"KnowToMigrate encountered an error:\n\n{e.Exception.Message}\n\nCheck the log file for details.",
                "KnowToMigrate",
                MessageBoxButton.OK,
                MessageBoxImage.Warning
            );
            e.Handled = true;
        }

        private void CurrentDomain_UnhandledException(object sender, UnhandledExceptionEventArgs e)
        {
            if (e.ExceptionObject is Exception ex)
            {
                LogCrash(ex);
            }
        }

        private void TaskScheduler_UnobservedTaskException(object? sender, UnobservedTaskExceptionEventArgs e)
        {
            LogCrash(e.Exception);
            e.SetObserved();
        }

        private void LogCrash(Exception ex)
        {
            try
            {
                string logPath = Path.Combine(UserSettingsManager.LogsDirectory, "crash.log");
                File.AppendAllText(logPath, $"[{DateTime.UtcNow:O}] {ex}\n\n");
            }
            catch { }
        }

        public static void LogMessage(string message)
        {
            try
            {
                string logPath = Path.Combine(UserSettingsManager.LogsDirectory, "application.log");
                File.AppendAllText(logPath, $"[{DateTime.UtcNow:O}] {message}\n");
            }
            catch { }
        }
    }
}

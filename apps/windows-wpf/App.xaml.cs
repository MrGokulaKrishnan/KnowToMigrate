using System;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Threading;

namespace KnowToMigrate
{
    public partial class App : Application
    {
        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            DispatcherUnhandledException += App_DispatcherUnhandledException;
            AppDomain.CurrentDomain.UnhandledException += CurrentDomain_UnhandledException;
            TaskScheduler.UnobservedTaskException += TaskScheduler_UnobservedTaskException;
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
                string logPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "crash.log");
                File.AppendAllText(logPath, $"[{DateTime.UtcNow:O}] {ex}\n\n");
            }
            catch { }
        }
    }
}

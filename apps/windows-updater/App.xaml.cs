using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Windows;

namespace KnowToMigrate.Updater
{
    public partial class App : Application
    {
        private void Application_Startup(object sender, StartupEventArgs e)
        {
            string currentExe = Process.GetCurrentProcess().MainModule?.FileName ?? "";
            string tempUpdaterDir = Path.Combine(Path.GetTempPath(), "KnowToMigrate-Updater");
            string tempUpdaterExe = Path.Combine(tempUpdaterDir, "KnowToMigrate.Updater.exe");

            bool isRelocated = false;
            foreach (var arg in e.Args)
            {
                if (arg.Equals("--relocated", StringComparison.OrdinalIgnoreCase))
                {
                    isRelocated = true;
                    break;
                }
            }

            // If running inside Program Files or not yet relocated, copy to %TEMP% and relaunch
            bool isInProgramFiles = currentExe.IndexOf("Program Files", StringComparison.OrdinalIgnoreCase) >= 0;
            if (!isRelocated && isInProgramFiles)
            {
                try
                {
                    if (!Directory.Exists(tempUpdaterDir)) Directory.CreateDirectory(tempUpdaterDir);
                    File.Copy(currentExe, tempUpdaterExe, true);

                    var psi = new ProcessStartInfo
                    {
                        FileName = tempUpdaterExe,
                        Arguments = string.Join(" ", e.Args) + " --relocated",
                        UseShellExecute = true
                    };
                    Process.Start(psi);
                    Shutdown(0);
                    return;
                }
                catch { }
            }

            var window = new UpdaterWindow(e.Args);
            window.Show();
        }
    }
}

using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Windows.Forms;

[assembly: AssemblyTitle("KnowToMigrate Setup")]
[assembly: AssemblyProduct("KnowToMigrate")]
[assembly: AssemblyCompany("KNOWTHETECH")]
[assembly: AssemblyDescription("KnowToMigrate Windows Installer")]
[assembly: AssemblyVersion("1.0.1.0")]
[assembly: AssemblyFileVersion("1.0.1.0")]

namespace KnowToMigrate.Setup
{
    static class Program
    {
        private const string MsiResourceName = "Payload.msi";

        [STAThread]
        static int Main(string[] args)
        {
            string tempMsi = null;
            try
            {
                string tempDir = Path.Combine(Path.GetTempPath(), "KnowToMigrate-Setup");
                if (!Directory.Exists(tempDir))
                {
                    Directory.CreateDirectory(tempDir);
                }

                tempMsi = Path.Combine(tempDir, string.Format("KnowToMigrate-Setup-{0}.msi", Guid.NewGuid().ToString("N")));

                // Extract embedded MSI payload
                var asm = Assembly.GetExecutingAssembly();
                using (var stream = asm.GetManifestResourceStream(MsiResourceName))
                {
                    if (stream == null)
                    {
                        MessageBox.Show("Installer payload is missing or corrupted.", "KnowToMigrate Setup", MessageBoxButtons.OK, MessageBoxIcon.Error);
                        return 1;
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

                // Analyze incoming arguments
                bool isQuiet = false;
                bool isNoRestart = false;
                if (args != null && args.Length > 0)
                {
                    foreach (var a in args)
                    {
                        if (a.IndexOf("/q", StringComparison.OrdinalIgnoreCase) >= 0 ||
                            a.IndexOf("-q", StringComparison.OrdinalIgnoreCase) >= 0 ||
                            a.IndexOf("quiet", StringComparison.OrdinalIgnoreCase) >= 0)
                        {
                            isQuiet = true;
                        }
                        if (a.IndexOf("norestart", StringComparison.OrdinalIgnoreCase) >= 0)
                        {
                            isNoRestart = true;
                        }
                    }
                }

                string logPath = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "KnowToMigrate", "logs", "msi_install.log"
                );
                try
                {
                    string logDir = Path.GetDirectoryName(logPath);
                    if (!string.IsNullOrEmpty(logDir) && !Directory.Exists(logDir))
                    {
                        Directory.CreateDirectory(logDir);
                    }
                }
                catch { }

                string msiArgs;
                if (isQuiet)
                {
                    msiArgs = string.Format("/i \"{0}\" /qn {1} /lv* \"{2}\"",
                        tempMsi,
                        isNoRestart ? "/norestart" : "",
                        logPath);
                }
                else
                {
                    msiArgs = string.Format("/i \"{0}\" /lv* \"{1}\"", tempMsi, logPath);
                }

                var psi = new ProcessStartInfo
                {
                    FileName = "msiexec.exe",
                    Arguments = msiArgs,
                    UseShellExecute = true,
                    Verb = "runas"
                };

                using (var proc = Process.Start(psi))
                {
                    if (proc != null)
                    {
                        proc.WaitForExit();
                        int exitCode = proc.ExitCode;

                        // Clean up temp MSI
                        try { if (File.Exists(tempMsi)) File.Delete(tempMsi); } catch { }

                        // Exit code 0 = ERROR_SUCCESS, 3010 = ERROR_SUCCESS_REBOOT_REQUIRED
                        if (exitCode == 0 || exitCode == 3010)
                        {
                            // If running interactively, launch the installed application
                            if (!isQuiet)
                            {
                                string installedExe = ResolveInstalledExecutable();
                                if (!string.IsNullOrEmpty(installedExe) && File.Exists(installedExe))
                                {
                                    try
                                    {
                                        Process.Start(new ProcessStartInfo
                                        {
                                            FileName = installedExe,
                                            WorkingDirectory = Path.GetDirectoryName(installedExe),
                                            UseShellExecute = true
                                        });
                                    }
                                    catch { }
                                }
                            }
                        }

                        return exitCode;
                    }
                }

                return 0;
            }
            catch (Exception ex)
            {
                try { if (tempMsi != null && File.Exists(tempMsi)) File.Delete(tempMsi); } catch { }
                MessageBox.Show("Installation failed:\n" + ex.Message, "KnowToMigrate Setup", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return 1;
            }
        }

        private static string ResolveInstalledExecutable()
        {
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
            return null;
        }
    }
}

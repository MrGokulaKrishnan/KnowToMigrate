using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Windows.Forms;

[assembly: AssemblyTitle("KnowToMigrate Setup")]
[assembly: AssemblyProduct("KnowToMigrate")]
[assembly: AssemblyCompany("KNOWTHETECH")]
[assembly: AssemblyDescription("KnowToMigrate Windows Installer")]
[assembly: AssemblyVersion("1.0.0.0")]
[assembly: AssemblyFileVersion("1.0.0.0")]

namespace KnowToMigrate.Setup
{
    static class Program
    {
        private const string MsiResourceName = "Payload.msi";
        private const string InstalledExePath = @"C:\Program Files\KnowToMigrate\KnowToMigrate.exe";

        [STAThread]
        static int Main(string[] args)
        {
            try
            {
                string tempDir = Path.Combine(Path.GetTempPath(), "KnowToMigrate-Setup");
                if (!Directory.Exists(tempDir))
                {
                    Directory.CreateDirectory(tempDir);
                }

                string tempMsi = Path.Combine(tempDir, "KnowToMigrate-1.0.0-x64.msi");

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

                // Build msiexec arguments: forward any command-line arguments, or run standard interactive install
                string msiArgs = string.Format("/i \"{0}\"", tempMsi);
                if (args != null && args.Length > 0)
                {
                    msiArgs += " " + string.Join(" ", args);
                }

                var psi = new ProcessStartInfo
                {
                    FileName = "msiexec.exe",
                    Arguments = msiArgs,
                    UseShellExecute = true
                };

                var proc = Process.Start(psi);
                if (proc != null)
                {
                    proc.WaitForExit();
                    int exitCode = proc.ExitCode;

                    // Clean up temp MSI
                    try { File.Delete(tempMsi); } catch { }

                    // Exit code 0 = ERROR_SUCCESS
                    if (exitCode == 0)
                    {
                        // Launch installed application if it exists and not in quiet mode
                        bool isQuiet = msiArgs.IndexOf("/q", StringComparison.OrdinalIgnoreCase) >= 0;
                        if (!isQuiet && File.Exists(InstalledExePath))
                        {
                            try
                            {
                                Process.Start(new ProcessStartInfo
                                {
                                    FileName = InstalledExePath,
                                    WorkingDirectory = Path.GetDirectoryName(InstalledExePath),
                                    UseShellExecute = true
                                });
                            }
                            catch { }
                        }
                    }

                    return exitCode;
                }

                return 0;
            }
            catch (Exception ex)
            {
                MessageBox.Show("Installation failed:\n" + ex.Message, "KnowToMigrate Setup", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return 1;
            }
        }
    }
}

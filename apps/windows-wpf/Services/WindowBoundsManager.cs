using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Text.Json;
using System.Windows;

namespace KnowToMigrate.Services
{
    public class SavedWindowState
    {
        public double Left   { get; set; } = -1;
        public double Top    { get; set; } = -1;
        public double Width  { get; set; } = 1280;
        public double Height { get; set; } = 820;
        public bool IsMaximized { get; set; } = false;
    }

    /// <summary>
    /// Saves and restores WPF window bounds with full work-area validation.
    /// Uses Win32 P/Invoke (MonitorFromPoint / GetMonitorInfo) — no Windows Forms dependency.
    /// Called from MainWindow.Loaded (after the window is shown/activated).
    /// </summary>
    public static class WindowBoundsManager
    {
        // ── Win32 P/Invoke ────────────────────────────────────────────────────────
        private const int MONITOR_DEFAULTTONEAREST = 2;
        private const int MONITOR_DEFAULTTOPRIMARY = 1;

        [StructLayout(LayoutKind.Sequential)]
        private struct RECT
        {
            public int Left, Top, Right, Bottom;
            public int Width  => Right  - Left;
            public int Height => Bottom - Top;
        }

        [StructLayout(LayoutKind.Sequential)]
        private struct MONITORINFO
        {
            public int    cbSize;
            public RECT   rcMonitor;
            public RECT   rcWork;   // Work area (excludes taskbar)
            public int    dwFlags;
        }

        [StructLayout(LayoutKind.Sequential)]
        private struct POINT { public int X, Y; }

        [DllImport("user32.dll")]
        private static extern IntPtr MonitorFromPoint(POINT pt, int dwFlags);

        [DllImport("user32.dll")]
        private static extern IntPtr MonitorFromRect(ref RECT rc, int dwFlags);

        [DllImport("user32.dll")]
        private static extern bool GetMonitorInfo(IntPtr hMonitor, ref MONITORINFO lpmi);

        [DllImport("user32.dll")]
        private static extern bool SystemParametersInfo(int uiAction, int uiParam, ref RECT pvParam, int fWinIni);

        private const int SPI_GETWORKAREA = 0x0030;

        // ── Storage ───────────────────────────────────────────────────────────────
        private static readonly string SettingsFilePath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "KnowToMigrate",
            "window_state.json"
        );

        // ── Public API ────────────────────────────────────────────────────────────

        /// <summary>
        /// Apply saved window state (or safe centered defaults) to the window.
        /// Must be called from MainWindow.Loaded — never in the constructor.
        /// </summary>
        public static void ApplyToWindow(Window window)
        {
            try
            {
                var saved = LoadSavedState();

                // Get DPI scale for this window (needed to convert between WPF units and physical pixels)
                var source = System.Windows.Interop.HwndSource.FromHwnd(
                    new System.Windows.Interop.WindowInteropHelper(window).Handle);
                double dpiX = 1.0, dpiY = 1.0;
                if (source?.CompositionTarget != null)
                {
                    dpiX = source.CompositionTarget.TransformToDevice.M11;
                    dpiY = source.CompositionTarget.TransformToDevice.M22;
                }

                // ── MAXIMIZED ─────────────────────────────────────────────────────
                if (saved.IsMaximized)
                {
                    window.WindowState = WindowState.Maximized;
                    System.Diagnostics.Debug.WriteLine("[WBM] Restored: MAXIMIZED");
                    return;
                }

                // ── NORMAL STATE ──────────────────────────────────────────────────
                double wpfWidth  = saved.Width  > 0 ? saved.Width  : 1280;
                double wpfHeight = saved.Height > 0 ? saved.Height : 820;

                bool hasValidPos = false;
                double targetLeft = saved.Left;
                double targetTop  = saved.Top;

                if (saved.Left != -1 && saved.Top != -1)
                {
                    // Convert WPF coords to physical pixels for Win32 API
                    int pxLeft = (int)(saved.Left * dpiX);
                    int pxTop  = (int)(saved.Top  * dpiY);
                    int pxW    = (int)(wpfWidth   * dpiX);
                    int pxH    = (int)(wpfHeight  * dpiY);

                    // Use the center point of the saved rect to find the right monitor
                    var center = new POINT { X = pxLeft + pxW / 2, Y = pxTop + pxH / 2 };
                    var hMon = MonitorFromPoint(center, MONITOR_DEFAULTTONEAREST);

                    if (hMon != IntPtr.Zero)
                    {
                        var mi = new MONITORINFO { cbSize = Marshal.SizeOf<MONITORINFO>() };
                        GetMonitorInfo(hMon, ref mi);

                        // Work area in physical pixels
                        var wa = mi.rcWork;

                        // Clamp width/height to fit work area
                        pxW = Math.Min(pxW, wa.Width);
                        pxH = Math.Min(pxH, wa.Height);
                        wpfWidth  = pxW / dpiX;
                        wpfHeight = pxH / dpiY;

                        // The title bar top must be AT OR BELOW the work area top
                        bool yOk = pxTop  >= wa.Top  && pxTop  <= wa.Top  + wa.Height - 48;
                        bool xOk = pxLeft >= wa.Left - pxW + 150 && pxLeft <= wa.Left + wa.Width - 150;

                        if (yOk && xOk)
                        {
                            hasValidPos = true;
                            // Hard clamp: title bar cannot be above work area
                            int clampedTop  = Math.Max(wa.Top,  pxTop);
                            int clampedLeft = Math.Clamp(pxLeft, wa.Left, wa.Left + wa.Width - Math.Min(pxW, 300));
                            targetLeft = clampedLeft / dpiX;
                            targetTop  = clampedTop  / dpiY;

                            System.Diagnostics.Debug.WriteLine(
                                $"[WBM] Valid saved pos: L={targetLeft} T={targetTop} W={wpfWidth} H={wpfHeight}");
                        }
                        else
                        {
                            System.Diagnostics.Debug.WriteLine(
                                $"[WBM] Invalid saved pos (yOk={yOk} xOk={xOk}), using center");
                        }
                    }
                }

                if (!hasValidPos)
                {
                    // Center on primary monitor work area
                    var waRect = new RECT();
                    SystemParametersInfo(SPI_GETWORKAREA, 0, ref waRect, 0);

                    // Clamp to work area
                    wpfWidth  = Math.Min(wpfWidth,  waRect.Width  / dpiX);
                    wpfHeight = Math.Min(wpfHeight, waRect.Height / dpiY);

                    targetLeft = waRect.Left  / dpiX + (waRect.Width  / dpiX - wpfWidth)  / 2;
                    targetTop  = waRect.Top   / dpiY + (waRect.Height / dpiY - wpfHeight) / 2;

                    System.Diagnostics.Debug.WriteLine(
                        $"[WBM] Safe center: L={targetLeft} T={targetTop} W={wpfWidth} H={wpfHeight}");
                }

                window.WindowStartupLocation = WindowStartupLocation.Manual;
                window.Left   = targetLeft;
                window.Top    = targetTop;
                window.Width  = wpfWidth;
                window.Height = wpfHeight;
                window.WindowState = WindowState.Normal;
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"[WBM] ApplyToWindow error: {ex.Message}");
            }
        }

        /// <summary>
        /// Save current window state to disk (call from Closing event).
        /// </summary>
        public static void SaveWindowState(Window window)
        {
            try
            {
                var state = new SavedWindowState();

                if (window.WindowState == WindowState.Maximized)
                {
                    state.IsMaximized = true;
                    // Preserve previous normal position so restore-from-maximized works
                    var prev = LoadSavedState();
                    state.Left   = prev.Left;
                    state.Top    = prev.Top;
                    state.Width  = prev.Width;
                    state.Height = prev.Height;
                }
                else if (window.WindowState == WindowState.Normal)
                {
                    state.IsMaximized = false;
                    state.Left   = window.Left;
                    state.Top    = window.Top;
                    state.Width  = window.Width;
                    state.Height = window.Height;
                }
                else
                {
                    // Minimized — save previous normal position, never restore to minimized
                    var prev = LoadSavedState();
                    state.Left        = prev.Left;
                    state.Top         = prev.Top;
                    state.Width       = prev.Width;
                    state.Height      = prev.Height;
                    state.IsMaximized = false;
                }

                var dir = Path.GetDirectoryName(SettingsFilePath);
                if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                    Directory.CreateDirectory(dir);

                var json = JsonSerializer.Serialize(state, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(SettingsFilePath, json);
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"[WBM] SaveWindowState error: {ex.Message}");
            }
        }

        private static SavedWindowState LoadSavedState()
        {
            try
            {
                if (File.Exists(SettingsFilePath))
                {
                    var json  = File.ReadAllText(SettingsFilePath);
                    var state = JsonSerializer.Deserialize<SavedWindowState>(json);
                    if (state != null) return state;
                }
            }
            catch { }
            return new SavedWindowState();
        }
    }
}

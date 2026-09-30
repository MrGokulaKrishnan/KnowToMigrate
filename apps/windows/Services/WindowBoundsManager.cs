using System;
using System.IO;
using System.Text.Json;
using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Windows.Graphics;

namespace KnowToMigrate.Services
{
    public class SavedWindowState
    {
        public int X { get; set; } = -1;
        public int Y { get; set; } = -1;
        public int Width { get; set; } = 1280;
        public int Height { get; set; } = 800;
        public bool IsMaximized { get; set; } = false;
    }

    public static class WindowBoundsManager
    {
        private static readonly string SettingsFilePath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "KnowToMigrate",
            "window_state.json"
        );

        public static void ApplySavedBoundsOrSafeDefaults(Window window)
        {
            try
            {
                var appWindow = window.AppWindow;
                if (appWindow == null) return;

                var saved = LoadSavedState();

                // Called AFTER window.Activate() so the window is on its real monitor.
                // DisplayArea.GetFromWindowId is now reliable.
                var windowId = appWindow.Id;
                var displayArea = DisplayArea.GetFromWindowId(windowId, DisplayAreaFallback.Primary);
                var workArea = displayArea.WorkArea;

                System.Diagnostics.Debug.WriteLine(
                    $"[WindowBoundsManager] WorkArea: {workArea.X},{workArea.Y} " +
                    $"{workArea.Width}x{workArea.Height}");
                System.Diagnostics.Debug.WriteLine(
                    $"[WindowBoundsManager] Saved: X={saved.X} Y={saved.Y} " +
                    $"W={saved.Width} H={saved.Height} Max={saved.IsMaximized}");

                // ── MAXIMIZED STATE ───────────────────────────────────────────────────
                // When restoring maximized, let the OS handle geometry entirely.
                // Do NOT call MoveAndResize before Maximize() — the two fight each other
                // and can result in the window clipping above the work area.
                if (saved.IsMaximized)
                {
                    if (appWindow.Presenter is OverlappedPresenter presenterMax)
                    {
                        presenterMax.IsMinimizable = true;
                        presenterMax.IsMaximizable = true;
                        presenterMax.IsResizable = true;
                        presenterMax.IsAlwaysOnTop = false;
                        presenterMax.Maximize();
                        System.Diagnostics.Debug.WriteLine("[WindowBoundsManager] Restored: MAXIMIZED");
                    }
                    return;
                }

                // ── NORMAL / RESTORED STATE ───────────────────────────────────────────
                // Determine safe width & height (min 960x640, max workArea)
                int minWidth = Math.Min(960, workArea.Width);
                int minHeight = Math.Min(640, workArea.Height);

                int width = saved.Width > 0 ? saved.Width : 1280;
                int height = saved.Height > 0 ? saved.Height : 800;

                width = Math.Clamp(width, minWidth, workArea.Width);
                height = Math.Clamp(height, minHeight, workArea.Height);

                // Check if saved position is valid & visible on any display work area
                bool hasValidPos = false;
                int targetX = saved.X;
                int targetY = saved.Y;

                if (saved.X != -1 && saved.Y != -1)
                {
                    // Use DisplayAreaFallback.None so disconnected-monitor positions
                    // correctly return null (not snapped to wrong monitor)
                    var targetRect = new RectInt32(saved.X, saved.Y, width, height);
                    var targetDisplay = DisplayArea.GetFromRect(targetRect, DisplayAreaFallback.None);

                    if (targetDisplay != null)
                    {
                        var targetWorkArea = targetDisplay.WorkArea;
                        // ALL of the title bar must be inside the work area
                        // (Y must be >= workArea.Y; enough horizontal space to grab the window)
                        bool yOk = targetY >= targetWorkArea.Y &&
                                   targetY <= targetWorkArea.Y + targetWorkArea.Height - 60;
                        bool xOk = targetX >= targetWorkArea.X - width + 150 &&
                                   targetX <= targetWorkArea.X + targetWorkArea.Width - 150;

                        if (yOk && xOk)
                        {
                            hasValidPos = true;
                            // Strict clamp: title bar top must be AT OR BELOW work-area top
                            targetY = Math.Max(targetWorkArea.Y, targetY);
                            // Keep window horizontally within the work area
                            targetX = Math.Clamp(targetX,
                                targetWorkArea.X,
                                targetWorkArea.X + targetWorkArea.Width - Math.Min(width, 300));
                        }
                    }
                }

                if (!hasValidPos)
                {
                    // Safe default: centered on current monitor work area
                    targetX = workArea.X + Math.Max(0, (workArea.Width - width) / 2);
                    targetY = workArea.Y + Math.Max(0, (workArea.Height - height) / 2);
                    System.Diagnostics.Debug.WriteLine(
                        $"[WindowBoundsManager] Using safe center: {targetX},{targetY}");
                }

                System.Diagnostics.Debug.WriteLine(
                    $"[WindowBoundsManager] Applying: X={targetX} Y={targetY} W={width} H={height}");

                // Apply position and size to the activated, visible window
                appWindow.MoveAndResize(new RectInt32(targetX, targetY, width, height));

                // Restore normal presenter state
                if (appWindow.Presenter is OverlappedPresenter presenter)
                {
                    presenter.IsMinimizable = true;
                    presenter.IsMaximizable = true;
                    presenter.IsResizable = true;
                    presenter.IsAlwaysOnTop = false;
                    presenter.Restore();
                    System.Diagnostics.Debug.WriteLine("[WindowBoundsManager] Restored: NORMAL");
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine(
                    $"[WindowBoundsManager] Error applying bounds: {ex.Message}");
            }
        }

        public static void SaveWindowState(Window window)
        {
            try
            {
                var appWindow = window.AppWindow;
                if (appWindow == null) return;

                var state = new SavedWindowState();

                if (appWindow.Presenter is OverlappedPresenter presenter)
                {
                    state.IsMaximized = (presenter.State == OverlappedPresenterState.Maximized);

                    // If currently minimized, do NOT save minimized state as launch state
                    if (presenter.State == OverlappedPresenterState.Minimized)
                    {
                        state.IsMaximized = false;
                    }
                }

                var position = appWindow.Position;
                var size = appWindow.Size;

                // Only save position if normal state
                if (!state.IsMaximized && appWindow.Presenter is OverlappedPresenter p && p.State == OverlappedPresenterState.Restored)
                {
                    state.X = position.X;
                    state.Y = position.Y;
                    state.Width = size.Width;
                    state.Height = size.Height;
                }
                else
                {
                    // Preserve previous position/size if maximized or minimized
                    var existing = LoadSavedState();
                    if (existing.Width > 0 && existing.Height > 0)
                    {
                        state.X = existing.X;
                        state.Y = existing.Y;
                        state.Width = existing.Width;
                        state.Height = existing.Height;
                    }
                }

                var dir = Path.GetDirectoryName(SettingsFilePath);
                if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                {
                    Directory.CreateDirectory(dir);
                }

                var json = JsonSerializer.Serialize(state, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(SettingsFilePath, json);
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"[WindowBoundsManager] Error saving state: {ex.Message}");
            }
        }

        private static SavedWindowState LoadSavedState()
        {
            try
            {
                if (File.Exists(SettingsFilePath))
                {
                    var json = File.ReadAllText(SettingsFilePath);
                    var state = JsonSerializer.Deserialize<SavedWindowState>(json);
                    if (state != null) return state;
                }
            }
            catch { }
            return new SavedWindowState();
        }
    }
}

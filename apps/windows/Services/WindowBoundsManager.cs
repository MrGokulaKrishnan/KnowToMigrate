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

                // Get display work area for default/fallback calculations
                var windowId = appWindow.Id;
                var displayArea = DisplayArea.GetFromWindowId(windowId, DisplayAreaFallback.Primary);
                var workArea = displayArea.WorkArea;

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
                    // Check rect against display area
                    var targetRect = new RectInt32(saved.X, saved.Y, width, height);
                    var targetDisplay = DisplayArea.GetFromRect(targetRect, DisplayAreaFallback.None);

                    if (targetDisplay != null)
                    {
                        var targetWorkArea = targetDisplay.WorkArea;
                        // Ensure title bar top is at or below workArea.Y and window is sufficiently visible
                        if (targetY >= targetWorkArea.Y &&
                            targetY <= targetWorkArea.Y + targetWorkArea.Height - 60 &&
                            targetX >= targetWorkArea.X - width + 150 &&
                            targetX <= targetWorkArea.X + targetWorkArea.Width - 150)
                        {
                            hasValidPos = true;
                            // Clamp Y to work area top to prevent negative / hidden title bar
                            targetY = Math.Max(targetWorkArea.Y, targetY);
                        }
                    }
                }

                if (!hasValidPos)
                {
                    // Center on primary/current monitor work area
                    targetX = workArea.X + Math.Max(0, (workArea.Width - width) / 2);
                    targetY = workArea.Y + Math.Max(0, (workArea.Height - height) / 2);
                }

                // Move and Resize window BEFORE showing or restoring presenter
                appWindow.MoveAndResize(new RectInt32(targetX, targetY, width, height));

                // Presenter mode: NORMAL / RESTORED (or MAXIMIZED if user intentionally closed maximized)
                if (appWindow.Presenter is OverlappedPresenter presenter)
                {
                    presenter.IsMinimizable = true;
                    presenter.IsMaximizable = true;
                    presenter.IsResizable = true;
                    presenter.IsAlwaysOnTop = false;

                    if (saved.IsMaximized)
                    {
                        presenter.Maximize();
                    }
                    else
                    {
                        presenter.Restore();
                    }
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"[WindowBoundsManager] Error applying bounds: {ex.Message}");
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

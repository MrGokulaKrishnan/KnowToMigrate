using System;
using System.Collections.Generic;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Threading;
using KnowToMigrate.Services;

namespace KnowToMigrate.Views
{
    public partial class IncomingTransferWindow : Window
    {
        // ── Win32 API for Non-Intrusive Taskbar Notification ────────────────────
        [StructLayout(LayoutKind.Sequential)]
        private struct FLASHWINFO
        {
            public uint cbSize;
            public IntPtr hwnd;
            public uint dwFlags;
            public uint uCount;
            public uint dwTimeout;
        }

        private const uint FLASHW_STOP = 0;
        private const uint FLASHW_CAPTION = 1;
        private const uint FLASHW_TRAY = 2;
        private const uint FLASHW_ALL = 3;
        private const uint FLASHW_TIMERNOFG = 12;

        [DllImport("user32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool FlashWindowEx(ref FLASHWINFO pwfi);

        // ── Static Request Queue Management ─────────────────────────────────────
        private class TransferPromptRequest
        {
            public KtmHandshake Handshake { get; set; }
            public TaskCompletionSource<bool> Tcs { get; set; } = new();
            public TransferPromptRequest(KtmHandshake handshake) => Handshake = handshake;
        }

        private static readonly Queue<TransferPromptRequest> _requestQueue = new();
        private static IncomingTransferWindow? _activeWindow;
        private static readonly object _queueLock = new();

        private readonly TaskCompletionSource<bool> _tcs;
        private readonly DispatcherTimer _timeoutTimer;
        private bool _isResolved;

        public IncomingTransferWindow(KtmHandshake handshake, TaskCompletionSource<bool> tcs, int pendingCount)
        {
            InitializeComponent();
            _tcs = tcs;

            // Populate handshake information
            TxtDeviceName.Text = string.IsNullOrEmpty(handshake.DeviceName) ? "Nearby Device" : handshake.DeviceName;
            TxtPlatform.Text = string.IsNullOrEmpty(handshake.Platform) ? "Android" : handshake.Platform;

            // Set platform appropriate device icon
            string plat = (handshake.Platform ?? "").ToLowerInvariant();
            if (plat.Contains("win") || plat.Contains("pc") || plat.Contains("mac") || plat.Contains("linux"))
            {
                // Laptop / Desktop PC vector path
                PathDeviceIcon.Data = System.Windows.Media.Geometry.Parse("M20 18c1.1 0 1.99-.9 1.99-2L22 5c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2H0c0 1.1.9 2 2 2h20c1.1 0 2-.9 2-2h-4zM4 5h16v11H4V5z");
            }
            else
            {
                // Mobile phone vector path
                PathDeviceIcon.Data = System.Windows.Media.Geometry.Parse("M17 1H7c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-2-2-2zm0 18H7V5h10v14z");
            }

            string transportName = KtmTransportCodes.GetDisplayName(handshake.SelectedTransport);
            TxtTransport.Text = $"● {transportName} · Verified Connection";

            // Group PIN digits into 354 446 / 437 254 readable format
            string rawPin = handshake.Pin ?? "";
            TxtPin.Text = rawPin.Length == 6 ? $"{rawPin.Substring(0, 3)} {rawPin.Substring(3, 3)}" : rawPin;

            // Queue count indicator
            if (pendingCount > 1)
            {
                BadgeQueue.Visibility = Visibility.Visible;
                TxtQueueCount.Text = $"{pendingCount} Pending";
            }
            else
            {
                BadgeQueue.Visibility = Visibility.Collapsed;
            }

            // 60-second auto-timeout
            _timeoutTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(60) };
            _timeoutTimer.Tick += (s, e) =>
            {
                _timeoutTimer.Stop();
                Resolve(false);
            };
            _timeoutTimer.Start();
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            // Transient bring-to-top without permanently locking TopMost
            try
            {
                Topmost = true;
                Topmost = false;
                Activate();
            }
            catch { }

            // Flash taskbar to notify user if working in Chrome, VS Code, or other app
            FlashTaskbarNotification();

            // Smooth animated entrance (Section 15: Opacity 0 -> 1, Scale 0.96 -> 1.0, 450ms)
            Opacity = 0;
            var fadeAnim = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(400))
            {
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };
            var scaleXAnim = new DoubleAnimation(0.96, 1.0, TimeSpan.FromMilliseconds(450))
            {
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };
            var scaleYAnim = new DoubleAnimation(0.96, 1.0, TimeSpan.FromMilliseconds(450))
            {
                EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
            };

            BeginAnimation(OpacityProperty, fadeAnim);
            WindowScale.BeginAnimation(ScaleTransform.ScaleXProperty, scaleXAnim);
            WindowScale.BeginAnimation(ScaleTransform.ScaleYProperty, scaleYAnim);
        }

        private void FlashTaskbarNotification()
        {
            try
            {
                var helper = new WindowInteropHelper(this);
                if (helper.Handle != IntPtr.Zero)
                {
                    var fInfo = new FLASHWINFO
                    {
                        cbSize = (uint)Marshal.SizeOf<FLASHWINFO>(),
                        hwnd = helper.Handle,
                        dwFlags = FLASHW_TRAY | FLASHW_TIMERNOFG,
                        uCount = 5,
                        dwTimeout = 0
                    };
                    FlashWindowEx(ref fInfo);
                }
            }
            catch { }
        }

        private void BtnAccept_Click(object sender, RoutedEventArgs e)
        {
            Resolve(true);
        }

        private void BtnDecline_Click(object sender, RoutedEventArgs e)
        {
            Resolve(false);
        }

        private void Window_Closing(object? sender, System.ComponentModel.CancelEventArgs e)
        {
            Resolve(false);
        }

        private void Resolve(bool accepted)
        {
            if (_isResolved) return;
            _isResolved = true;

            _timeoutTimer.Stop();
            _tcs.TrySetResult(accepted);

            try
            {
                Close();
            }
            catch { }

            // Process next queued request if any
            lock (_queueLock)
            {
                _activeWindow = null;
                ProcessNextInQueue();
            }
        }

        // ── Public Dispatcher Entry Point ────────────────────────────────────────

        public static Task<bool> EnqueueAndPromptAsync(KtmHandshake handshake)
        {
            var req = new TransferPromptRequest(handshake);

            Application.Current.Dispatcher.Invoke(() =>
            {
                lock (_queueLock)
                {
                    _requestQueue.Enqueue(req);
                    if (_activeWindow == null)
                    {
                        ProcessNextInQueue();
                    }
                    else
                    {
                        // Update active window badge with new queue length
                        _activeWindow.BadgeQueue.Visibility = Visibility.Visible;
                        _activeWindow.TxtQueueCount.Text = $"{_requestQueue.Count + 1} Pending";
                    }
                }
            });

            return req.Tcs.Task;
        }

        public static void CancelPending()
        {
            Application.Current.Dispatcher.Invoke(() =>
            {
                lock (_queueLock)
                {
                    if (_activeWindow != null)
                    {
                        _activeWindow.Resolve(false);
                    }
                    while (_requestQueue.Count > 0)
                    {
                        var req = _requestQueue.Dequeue();
                        req.Tcs.TrySetResult(false);
                    }
                }
            });
        }

        private static void ProcessNextInQueue()
        {
            if (_requestQueue.Count == 0) return;

            var next = _requestQueue.Dequeue();
            int remaining = _requestQueue.Count + 1;

            _activeWindow = new IncomingTransferWindow(next.Handshake, next.Tcs, remaining);
            _activeWindow.Show();
        }
    }
}

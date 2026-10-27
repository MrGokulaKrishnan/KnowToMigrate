using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Threading;

namespace KnowToMigrate.Services
{
    public enum TrayState
    {
        Ready,
        Receiving,
        Paused,
        Error
    }

    public sealed class KtmTrayManager : IDisposable
    {
        private const int WM_USER = 0x0400;
        private const int WM_TRAYICON = WM_USER + 1024;
        private const int WM_LBUTTONUP = 0x0202;
        private const int WM_LBUTTONDBLCLK = 0x0203;
        private const int WM_RBUTTONUP = 0x0205;

        private const uint NIM_ADD = 0x00000000;
        private const uint NIM_MODIFY = 0x00000001;
        private const uint NIM_DELETE = 0x00000002;

        private const uint NIF_MESSAGE = 0x00000001;
        private const uint NIF_ICON = 0x00000002;
        private const uint NIF_TIP = 0x00000004;
        private const uint NIF_INFO = 0x00000010;

        private const uint NIIF_INFO = 0x00000001;
        private const uint NIIF_WARNING = 0x00000002;
        private const uint NIIF_ERROR = 0x00000003;

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct NOTIFYICONDATA
        {
            public int cbSize;
            public IntPtr hWnd;
            public uint uID;
            public uint uFlags;
            public uint uCallbackMessage;
            public IntPtr hIcon;
            [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 128)]
            public string szTip;
            public uint dwState;
            public uint dwStateMask;
            [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 256)]
            public string szInfo;
            public uint uTimeoutOrVersion;
            [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 64)]
            public string szInfoTitle;
            public uint dwInfoFlags;
            public Guid guidItem;
            public IntPtr hBalloonIcon;
        }

        [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
        private static extern bool Shell_NotifyIcon(uint dwMessage, ref NOTIFYICONDATA lpData);

        [DllImport("user32.dll", CharSet = CharSet.Auto)]
        private static extern IntPtr LoadIcon(IntPtr hInstance, IntPtr lpIconName);

        [DllImport("user32.dll", SetLastError = true)]
        private static extern bool DestroyIcon(IntPtr hIcon);

        [StructLayout(LayoutKind.Sequential)]
        private struct ICONINFO
        {
            public bool fIcon;
            public int xHotspot;
            public int yHotspot;
            public IntPtr hbmMask;
            public IntPtr hbmColor;
        }

        [DllImport("gdi32.dll")]
        private static extern IntPtr CreateBitmap(int nWidth, int nHeight, uint cPlanes, uint cBitsPerPel, IntPtr lpvBits);

        [DllImport("gdi32.dll")]
        private static extern bool DeleteObject(IntPtr hObject);

        [DllImport("user32.dll")]
        private static extern IntPtr CreateIconIndirect(ref ICONINFO piconinfo);

        [DllImport("shell32.dll", CharSet = CharSet.Auto)]
        private static extern IntPtr ExtractIcon(IntPtr hInst, string lpszExeFileName, int nIconIndex);

        [DllImport("user32.dll", CharSet = CharSet.Auto)]
        private static extern IntPtr LoadImage(IntPtr hinst, string lpszName, uint uType, int cxDesired, int cyDesired, uint fuLoad);

        private const uint IMAGE_ICON = 1;
        private const uint LR_LOADFROMFILE = 0x00000010;

        private static readonly IntPtr IDI_APPLICATION = new IntPtr(32512);

        private readonly Window _window;
        private ContextMenu? _contextMenu;
        private MenuItem? _menuItemPause;
        private MenuItem? _menuItemStartWhenSignedIn;
        private HwndSource? _hwndSource;
        private IntPtr _hBaseIcon = IntPtr.Zero;
        private IntPtr _hCurrentIcon = IntPtr.Zero;
        private ImageSource? _baseWpfImage;
        private bool _isAdded = false;

        private TrayState _currentState = TrayState.Ready;
        private string _customStatus = "";
        private DispatcherTimer? _pulseTimer;
        private bool _pulseState = false;

        public event Action? OnOpenRequested;
        public event Action? OnSendFilesRequested;
        public event Action? OnOpenSettingsRequested;
        public event Action? OnExitRequested;

        public TrayState State => _currentState;

        public KtmTrayManager(Window window)
        {
            _window = window;
        }

        public void Initialize()
        {
            var helper = new WindowInteropHelper(_window);
            IntPtr hwnd = helper.Handle;

            if (hwnd == IntPtr.Zero)
            {
                _window.SourceInitialized += (s, e) => SetupTray();
            }
            else
            {
                SetupTray();
            }
        }

        private void SetupTray()
        {
            var helper = new WindowInteropHelper(_window);
            IntPtr hwnd = helper.Handle;
            if (hwnd == IntPtr.Zero || _isAdded) return;

            _hwndSource = HwndSource.FromHwnd(hwnd);
            _hwndSource?.AddHook(WndProc);

            LoadBaseIcon();
            _contextMenu = CreateTrayContextMenu();

            _pulseTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(500) };
            _pulseTimer.Tick += (s, e) =>
            {
                if (_currentState == TrayState.Receiving)
                {
                    _pulseState = !_pulseState;
                    UpdateTrayIcon();
                }
            };

            var nid = new NOTIFYICONDATA
            {
                cbSize = Marshal.SizeOf<NOTIFYICONDATA>(),
                hWnd = hwnd,
                uID = 1001,
                uFlags = NIF_MESSAGE | NIF_ICON | NIF_TIP,
                uCallbackMessage = WM_TRAYICON,
                hIcon = _hBaseIcon,
                szTip = "KnowToMigrate · Ready to receive"
            };

            _isAdded = Shell_NotifyIcon(NIM_ADD, ref nid);
            _hCurrentIcon = _hBaseIcon;
        }

        private void LoadBaseIcon()
        {
            try
            {
                var iconUri = new Uri("pack://application:,,,/KnowToMigrate;component/Assets/KnowToMigrate.ico", UriKind.Absolute);
                var streamInfo = Application.GetResourceStream(iconUri);
                if (streamInfo != null)
                {
                    using (var s = streamInfo.Stream)
                    {
                        _baseWpfImage = BitmapFrame.Create(s, BitmapCreateOptions.None, BitmapCacheOption.OnLoad);
                    }
                }
            }
            catch { }

            try
            {
                string localIco = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Assets", "KnowToMigrate.ico");
                if (File.Exists(localIco))
                {
                    _hBaseIcon = LoadImage(IntPtr.Zero, localIco, IMAGE_ICON, 32, 32, LR_LOADFROMFILE);
                    if (_hBaseIcon != IntPtr.Zero) return;
                }
            }
            catch { }

            try
            {
                string exePath = Process.GetCurrentProcess().MainModule?.FileName ?? "";
                if (!string.IsNullOrEmpty(exePath) && File.Exists(exePath))
                {
                    _hBaseIcon = ExtractIcon(IntPtr.Zero, exePath, 0);
                    if (_hBaseIcon != IntPtr.Zero) return;
                }
            }
            catch { }

            _hBaseIcon = LoadIcon(IntPtr.Zero, IDI_APPLICATION);
        }

        public void SetState(TrayState state, string customStatus = "")
        {
            _currentState = state;
            _customStatus = customStatus;

            if (_currentState == TrayState.Receiving)
            {
                if (_pulseTimer?.IsEnabled != true)
                {
                    _pulseState = true;
                    _pulseTimer?.Start();
                }
            }
            else
            {
                _pulseTimer?.Stop();
                _pulseState = false;
            }

            UpdateTrayIcon();
            UpdateMenuLabels();
        }

        private void UpdateTrayIcon()
        {
            if (!_isAdded) return;
            var helper = new WindowInteropHelper(_window);
            IntPtr hwnd = helper.Handle;
            if (hwnd == IntPtr.Zero) return;

            string tip = GetTooltipText();
            IntPtr newHIcon = GenerateStateIcon();

            var nid = new NOTIFYICONDATA
            {
                cbSize = Marshal.SizeOf<NOTIFYICONDATA>(),
                hWnd = hwnd,
                uID = 1001,
                uFlags = NIF_ICON | NIF_TIP,
                hIcon = newHIcon,
                szTip = tip.Length > 127 ? tip.Substring(0, 127) : tip
            };

            Shell_NotifyIcon(NIM_MODIFY, ref nid);

            if (_hCurrentIcon != IntPtr.Zero && _hCurrentIcon != _hBaseIcon)
            {
                try { DestroyIcon(_hCurrentIcon); } catch { }
            }
            _hCurrentIcon = newHIcon;
        }

        private string GetTooltipText()
        {
            switch (_currentState)
            {
                case TrayState.Receiving:
                    return string.IsNullOrEmpty(_customStatus)
                        ? "KnowToMigrate · Receiving files"
                        : $"KnowToMigrate · Receiving: {_customStatus}";
                case TrayState.Paused:
                    return "KnowToMigrate · Paused";
                case TrayState.Error:
                    return string.IsNullOrEmpty(_customStatus)
                        ? "KnowToMigrate · Error"
                        : $"KnowToMigrate · Error: {_customStatus}";
                case TrayState.Ready:
                default:
                    return "KnowToMigrate · Ready to receive";
            }
        }

        private IntPtr GenerateStateIcon()
        {
            if (_currentState == TrayState.Ready || _hBaseIcon == IntPtr.Zero)
            {
                return _hBaseIcon;
            }

            try
            {
                int size = 32;
                var visual = new DrawingVisual();
                using (var dc = visual.RenderOpen())
                {
                    if (_baseWpfImage != null)
                    {
                        dc.DrawImage(_baseWpfImage, new Rect(0, 0, size, size));
                    }

                    int dotSize = 10;
                    int dotX = size - dotSize - 1;
                    int dotY = size - dotSize - 1;

                    if (_currentState == TrayState.Receiving)
                    {
                        // Pulse dot: bright brand orange (#FF5A00) when pulseState is true
                        byte alpha = (byte)(_pulseState ? 255 : 120);
                        var brush = new SolidColorBrush(System.Windows.Media.Color.FromArgb(alpha, 255, 90, 0));
                        var pen = new System.Windows.Media.Pen(new SolidColorBrush(System.Windows.Media.Color.FromArgb(200, 10, 10, 10)), 1.5);
                        dc.DrawEllipse(brush, pen, new System.Windows.Point(dotX + dotSize / 2.0, dotY + dotSize / 2.0), dotSize / 2.0, dotSize / 2.0);
                    }
                    else if (_currentState == TrayState.Paused)
                    {
                        var brush = new SolidColorBrush(System.Windows.Media.Color.FromRgb(245, 158, 11));
                        var pen = new System.Windows.Media.Pen(new SolidColorBrush(System.Windows.Media.Color.FromArgb(200, 10, 10, 10)), 1.5);
                        dc.DrawRectangle(brush, pen, new Rect(dotX, dotY, dotSize, dotSize));
                    }
                    else if (_currentState == TrayState.Error)
                    {
                        var brush = new SolidColorBrush(System.Windows.Media.Color.FromRgb(239, 68, 68));
                        var pen = new System.Windows.Media.Pen(new SolidColorBrush(System.Windows.Media.Color.FromArgb(200, 10, 10, 10)), 1.5);
                        dc.DrawEllipse(brush, pen, new System.Windows.Point(dotX + dotSize / 2.0, dotY + dotSize / 2.0), dotSize / 2.0, dotSize / 2.0);
                    }
                }

                var rtb = new RenderTargetBitmap(size, size, 96, 96, PixelFormats.Pbgra32);
                rtb.Render(visual);

                int stride = size * 4;
                byte[] pixels = new byte[size * stride];
                rtb.CopyPixels(pixels, stride, 0);

                GCHandle colorHandle = GCHandle.Alloc(pixels, GCHandleType.Pinned);
                IntPtr hbmColor = CreateBitmap(size, size, 1, 32, colorHandle.AddrOfPinnedObject());
                colorHandle.Free();

                byte[] maskBytes = new byte[(size * size) / 8];
                GCHandle maskHandle = GCHandle.Alloc(maskBytes, GCHandleType.Pinned);
                IntPtr hbmMask = CreateBitmap(size, size, 1, 1, maskHandle.AddrOfPinnedObject());
                maskHandle.Free();

                var iconInfo = new ICONINFO
                {
                    fIcon = true,
                    hbmColor = hbmColor,
                    hbmMask = hbmMask
                };

                IntPtr hIcon = CreateIconIndirect(ref iconInfo);
                DeleteObject(hbmColor);
                DeleteObject(hbmMask);

                return hIcon != IntPtr.Zero ? hIcon : _hBaseIcon;
            }
            catch
            {
                return _hBaseIcon;
            }
        }

        private IntPtr WndProc(IntPtr hwnd, int msg, IntPtr wParam, IntPtr lParam, ref bool handled)
        {
            if (msg == WM_TRAYICON)
            {
                int mouseMsg = lParam.ToInt32();
                if (mouseMsg == WM_LBUTTONUP || mouseMsg == WM_LBUTTONDBLCLK)
                {
                    RestoreWindow();
                    handled = true;
                }
                else if (mouseMsg == WM_RBUTTONUP)
                {
                    ShowTrayMenu();
                    handled = true;
                }
            }
            return IntPtr.Zero;
        }

        public void RestoreWindow()
        {
            _window.Dispatcher.Invoke(() =>
            {
                if (_window.WindowState == WindowState.Minimized)
                {
                    _window.WindowState = WindowState.Normal;
                }
                _window.Show();
                _window.Activate();
                OnOpenRequested?.Invoke();
            });
        }

        public void MinimizeToTray()
        {
            _window.Dispatcher.Invoke(() =>
            {
                _window.Hide();
            });
        }

        public void ShowFirstTimeBalloonTip()
        {
            try
            {
                var settings = UserSettingsManager.LoadSettings();
                if (!settings.HasShownCloseToTrayTip)
                {
                    settings.HasShownCloseToTrayTip = true;
                    UserSettingsManager.SaveSettings(settings);
                    ShowNotification("KnowToMigrate", "KnowToMigrate is still running and receiving files. Right-click the tray icon to exit.");
                }
            }
            catch { }
        }

        public void ShowNotification(string title, string message, bool isError = false)
        {
            if (!_isAdded) return;
            var helper = new WindowInteropHelper(_window);
            IntPtr hwnd = helper.Handle;
            if (hwnd == IntPtr.Zero) return;

            var nid = new NOTIFYICONDATA
            {
                cbSize = Marshal.SizeOf<NOTIFYICONDATA>(),
                hWnd = hwnd,
                uID = 1001,
                uFlags = NIF_INFO,
                szInfo = message ?? "",
                szInfoTitle = title ?? "KnowToMigrate",
                dwInfoFlags = isError ? NIIF_ERROR : NIIF_INFO
            };

            Shell_NotifyIcon(NIM_MODIFY, ref nid);
        }

        private ContextMenu CreateTrayContextMenu()
        {
            var menu = new ContextMenu();

            // 1. Open KnowToMigrate (Bold)
            var itemOpen = new MenuItem { Header = "Open KnowToMigrate", FontWeight = FontWeights.Bold };
            itemOpen.Click += (s, e) => RestoreWindow();
            menu.Items.Add(itemOpen);

            // 2. Send files…
            var itemSend = new MenuItem { Header = "Send files…" };
            itemSend.Click += (s, e) =>
            {
                RestoreWindow();
                OnSendFilesRequested?.Invoke();
            };
            menu.Items.Add(itemSend);

            // 3. Pause receiving / Resume receiving
            _menuItemPause = new MenuItem { Header = "Pause receiving" };
            _menuItemPause.Click += (s, e) =>
            {
                bool nowPaused = !KtmManager.Instance.IsReceivingPaused;
                KtmManager.Instance.IsReceivingPaused = nowPaused;
                SetState(nowPaused ? TrayState.Paused : (KtmManager.Instance.HasActiveTransfers ? TrayState.Receiving : TrayState.Ready));
            };
            menu.Items.Add(_menuItemPause);

            menu.Items.Add(new Separator());

            // 4. Start when I sign in (checkbox bound to StartWhenSignedIn)
            var settings = UserSettingsManager.LoadSettings();
            _menuItemStartWhenSignedIn = new MenuItem
            {
                Header = "Start when I sign in",
                IsCheckable = true,
                IsChecked = settings.StartWhenSignedIn
            };
            _menuItemStartWhenSignedIn.Click += (s, e) =>
            {
                var cur = UserSettingsManager.LoadSettings();
                cur.StartWhenSignedIn = _menuItemStartWhenSignedIn.IsChecked;
                UserSettingsManager.SaveSettings(cur);
                UserSettingsManager.SetStartWithWindows(cur.StartWhenSignedIn);
            };
            menu.Items.Add(_menuItemStartWhenSignedIn);

            // 5. Settings…
            var itemSettings = new MenuItem { Header = "Settings…" };
            itemSettings.Click += (s, e) =>
            {
                RestoreWindow();
                OnOpenSettingsRequested?.Invoke();
            };
            menu.Items.Add(itemSettings);

            menu.Items.Add(new Separator());

            // 6. Exit (prompts if transfer active)
            var itemExit = new MenuItem { Header = "Exit" };
            itemExit.Click += (s, e) => HandleExit();
            menu.Items.Add(itemExit);

            return menu;
        }

        private void UpdateMenuLabels()
        {
            if (_menuItemPause != null)
            {
                _menuItemPause.Header = KtmManager.Instance.IsReceivingPaused ? "Resume receiving" : "Pause receiving";
            }
            if (_menuItemStartWhenSignedIn != null)
            {
                var cur = UserSettingsManager.LoadSettings();
                _menuItemStartWhenSignedIn.IsChecked = cur.StartWhenSignedIn;
            }
        }

        public void UpdateStartWhenSignedInMenu(bool isChecked)
        {
            if (_menuItemStartWhenSignedIn != null)
            {
                _menuItemStartWhenSignedIn.IsChecked = isChecked;
            }
        }

        public void HandleExit()
        {
            if (KtmManager.Instance.HasActiveTransfers)
            {
                var result = MessageBox.Show(
                    "A transfer is in progress. Exit anyway?",
                    "KnowToMigrate",
                    MessageBoxButton.YesNo,
                    MessageBoxImage.Warning);

                if (result != MessageBoxResult.Yes)
                {
                    return;
                }
            }

            ForceShutdown();
        }

        public void ForceShutdown()
        {
            try
            {
                OnExitRequested?.Invoke();
            }
            catch { }

            try
            {
                KtmManager.Instance.Stop();
                KtmWebShareServer.Instance.Stop();
            }
            catch { }

            Dispose();
            Application.Current?.Dispatcher?.Invoke(() =>
            {
                Application.Current.Shutdown();
            });
        }

        private void ShowTrayMenu()
        {
            _window.Dispatcher.Invoke(() =>
            {
                UpdateMenuLabels();
                if (_contextMenu != null)
                {
                    _contextMenu.IsOpen = true;
                }
            });
        }

        public void Dispose()
        {
            _pulseTimer?.Stop();
            _pulseTimer = null;

            if (_isAdded)
            {
                var helper = new WindowInteropHelper(_window);
                IntPtr hwnd = helper.Handle;
                if (hwnd != IntPtr.Zero)
                {
                    var nid = new NOTIFYICONDATA
                    {
                        cbSize = Marshal.SizeOf<NOTIFYICONDATA>(),
                        hWnd = hwnd,
                        uID = 1001
                    };
                    Shell_NotifyIcon(NIM_DELETE, ref nid);
                }
                _isAdded = false;
            }

            if (_hCurrentIcon != IntPtr.Zero && _hCurrentIcon != _hBaseIcon)
            {
                try { DestroyIcon(_hCurrentIcon); } catch { }
                _hCurrentIcon = IntPtr.Zero;
            }

            if (_hwndSource != null)
            {
                _hwndSource.RemoveHook(WndProc);
                _hwndSource = null;
            }
        }
    }
}

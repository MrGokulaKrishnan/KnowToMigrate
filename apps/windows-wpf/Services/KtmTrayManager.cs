using System;
using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;

namespace KnowToMigrate.Services
{
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

        private static readonly IntPtr IDI_APPLICATION = new IntPtr(32512);

        private readonly Window _window;
        private readonly ContextMenu _contextMenu;
        private HwndSource? _hwndSource;
        private IntPtr _hIcon = IntPtr.Zero;
        private bool _isAdded = false;

        public event Action? OnOpenRequested;
        public event Action? OnExitRequested;

        public KtmTrayManager(Window window)
        {
            _window = window;
            _contextMenu = CreateTrayContextMenu();
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

            try
            {
                _hIcon = LoadIcon(IntPtr.Zero, IDI_APPLICATION);
            }
            catch { }

            var nid = new NOTIFYICONDATA
            {
                cbSize = Marshal.SizeOf<NOTIFYICONDATA>(),
                hWnd = hwnd,
                uID = 1001,
                uFlags = NIF_MESSAGE | NIF_ICON | NIF_TIP,
                uCallbackMessage = WM_TRAYICON,
                hIcon = _hIcon,
                szTip = "KnowToMigrate — Pluto Engine Active"
            };

            _isAdded = Shell_NotifyIcon(NIM_ADD, ref nid);
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
            var itemOpen = new MenuItem { Header = "Open KnowToMigrate", FontWeight = FontWeights.Bold };
            itemOpen.Click += (s, e) => RestoreWindow();
            menu.Items.Add(itemOpen);

            menu.Items.Add(new Separator());

            var itemExit = new MenuItem { Header = "Exit" };
            itemExit.Click += (s, e) =>
            {
                OnExitRequested?.Invoke();
                Application.Current.Shutdown();
            };
            menu.Items.Add(itemExit);

            return menu;
        }

        private void ShowTrayMenu()
        {
            _window.Dispatcher.Invoke(() =>
            {
                _contextMenu.IsOpen = true;
            });
        }

        public void Dispose()
        {
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

            if (_hwndSource != null)
            {
                _hwndSource.RemoveHook(WndProc);
                _hwndSource = null;
            }
        }
    }
}
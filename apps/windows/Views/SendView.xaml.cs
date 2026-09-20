using System;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using KnowToMigrate.Services;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using Windows.Storage.Pickers;

namespace KnowToMigrate.Views
{
    public sealed partial class SendView : Page
    {
        private KtmService?  _ktm;
        private DeviceInfo?  _selectedDevice;

        public ObservableCollection<DeviceInfo> Devices       { get; } = new();
        public ObservableCollection<string>     SelectedPaths { get; } = new();

        public SendView()
        {
            this.InitializeComponent();
        }

        // ─── Navigation ────────────────────────────────────────────────

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);

            if (e.Parameter is (KtmService ktm, DeviceInfo? device, string[] paths))
            {
                _ktm = ktm;

                foreach (var d in _ktm.NearbyDevices) Devices.Add(d);
                foreach (var p in paths) SelectedPaths.Add(p);

                if (device != null)
                {
                    _selectedDevice = device;
                    // Pre-select the passed device
                    var idx = Devices.IndexOf(Devices.FirstOrDefault(d => d.DeviceId == device.DeviceId)!);
                    if (idx >= 0) DeviceSelector.SelectedIndex = idx;
                }

                RefreshSendButton();
                RecalcSize();
            }
        }

        // ─── Device selection ───────────────────────────────────────────

        private void DeviceSelector_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            _selectedDevice  = DeviceSelector.SelectedItem as DeviceInfo;
            NoDeviceWarning.Visibility = _selectedDevice == null ? Visibility.Visible : Visibility.Collapsed;
            RefreshSendButton();
        }

        // ─── Add more files ─────────────────────────────────────────────

        private async void BtnAddMore_Click(object sender, RoutedEventArgs e)
        {
            var picker = new FileOpenPicker { ViewMode = PickerViewMode.Thumbnail };
            picker.FileTypeFilter.Add("*");

            var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(
                (Application.Current as App)?.GetMainWindow());
            WinRT.Interop.InitializeWithWindow.Initialize(picker, hwnd);

            var files = await picker.PickMultipleFilesAsync();
            foreach (var f in files)
                if (!SelectedPaths.Contains(f.Path))
                    SelectedPaths.Add(f.Path);

            RefreshSendButton();
            RecalcSize();
        }

        // ─── Send ────────────────────────────────────────────────────────

        private void BtnSend_Click(object sender, RoutedEventArgs e)
        {
            if (_ktm == null || _selectedDevice == null || SelectedPaths.Count == 0) return;

            Frame.Navigate(typeof(TransferView),
                (_ktm, _selectedDevice, SelectedPaths.ToArray()));
        }

        // ─── Helpers ─────────────────────────────────────────────────────

        private void RefreshSendButton()
        {
            BtnSend.IsEnabled = _selectedDevice != null && SelectedPaths.Count > 0;

            if (_selectedDevice != null && SelectedPaths.Count > 0)
            {
                SendSummary.Text  = $"Sending {SelectedPaths.Count} item(s) to {_selectedDevice.Name}";
                SendSubtitle.Text = _selectedDevice.Ip;
            }
            else if (_selectedDevice == null)
            {
                SendSummary.Text  = "No device selected";
                SendSubtitle.Text = "Pick a device from the list";
            }
            else
            {
                SendSummary.Text  = "No files chosen";
                SendSubtitle.Text = "Add files using the + Add button";
            }
        }

        private void RecalcSize()
        {
            long total = 0;
            foreach (var path in SelectedPaths)
            {
                try { total += new FileInfo(path).Length; } catch { }
            }
            TotalSizeText.Text = $"Total: {FormatBytes(total)}  ·  {SelectedPaths.Count} item(s)";
        }

        private static string FormatBytes(long bytes) =>
            bytes >= 1_000_000_000 ? $"{bytes / 1_000_000_000.0:F1} GB" :
            bytes >= 1_000_000     ? $"{bytes / 1_000_000.0:F1} MB"     :
            bytes >= 1_000         ? $"{bytes / 1_000.0:F1} KB"         :
            $"{bytes} B";
    }
}

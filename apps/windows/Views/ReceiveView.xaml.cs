using System;
using System.IO;
using System.Text.Json;
using KnowToMigrate.Services;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using Windows.Storage.Pickers;

namespace KnowToMigrate.Views
{
    public sealed partial class ReceiveView : Page
    {
        private KtmService?  _ktm;
        private DeviceInfo?  _pendingDevice;
        private string       _pendingManifest = "";
        private string       _receiveDir      = "";

        public ReceiveView()
        {
            this.InitializeComponent();
        }

        // ─── Navigation ────────────────────────────────────────────────

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);

            if (e.Parameter is KtmService ktm)
            {
                _ktm = ktm;

                // Default receive directory
                _receiveDir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                    "Downloads", "KnowToMigrate");
                ReceiveDirBox.Text = _receiveDir;
                QrIpText.Text      = "LAN  ·  port 7700";

                _ktm.OnIncomingRequest += OnIncomingRequest;

                if (DiscoverableToggle.IsOn)
                    _ktm.StartReceiving(_receiveDir);
            }
        }

        protected override void OnNavigatedFrom(NavigationEventArgs e)
        {
            base.OnNavigatedFrom(e);
            if (_ktm != null) _ktm.OnIncomingRequest -= OnIncomingRequest;
        }

        // ─── Discoverability toggle ──────────────────────────────────────

        private void DiscoverableToggle_Toggled(object sender, RoutedEventArgs e)
        {
            if (_ktm == null) return;

            if (DiscoverableToggle.IsOn)
            {
                DiscoverableSubtext.Text =
                    "Other KnowToMigrate devices can see you on the local network";
                _ktm.StartReceiving(_receiveDir);
            }
            else
            {
                DiscoverableSubtext.Text = "You are hidden — no incoming transfers allowed";
            }
        }

        // ─── Browse save dir ─────────────────────────────────────────────

        private async void BtnBrowseDir_Click(object sender, RoutedEventArgs e)
        {
            var picker = new FolderPicker
            {
                SuggestedStartLocation = PickerLocationId.Downloads,
                ViewMode               = PickerViewMode.List
            };
            picker.FileTypeFilter.Add("*");

            var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(
                (Application.Current as App)?.GetMainWindow());
            WinRT.Interop.InitializeWithWindow.Initialize(picker, hwnd);

            var folder = await picker.PickSingleFolderAsync();
            if (folder != null)
            {
                _receiveDir        = folder.Path;
                ReceiveDirBox.Text = _receiveDir;
            }
        }

        // ─── Incoming request ─────────────────────────────────────────────

        private void OnIncomingRequest(DeviceInfo device, string manifestJson)
        {
            _pendingDevice   = device;
            _pendingManifest = manifestJson;

            // Try to extract file count / size from manifest JSON
            string summary = "wants to send files";
            try
            {
                using var doc = JsonDocument.Parse(manifestJson);
                if (doc.RootElement.TryGetProperty("files", out var filesEl))
                {
                    int count = filesEl.GetArrayLength();
                    long total = 0;
                    foreach (var f in filesEl.EnumerateArray())
                        if (f.TryGetProperty("size", out var sz)) total += sz.GetInt64();
                    summary = $"wants to send {count} file(s) ({FormatBytes(total)})";
                }
            }
            catch { }

            RequestSenderText.Text = device.Name;
            RequestFilesText.Text  = summary;
            NoIncomingBanner.Visibility = Visibility.Collapsed;
            RequestCard.Visibility      = Visibility.Visible;
        }

        private void BtnAccept_Click(object sender, RoutedEventArgs e)
        {
            RequestCard.Visibility      = Visibility.Collapsed;
            NoIncomingBanner.Visibility = Visibility.Visible;

            if (_pendingDevice != null && _ktm != null)
                Frame.Navigate(typeof(TransferView),
                    (_ktm, _pendingDevice, Array.Empty<string>()));
        }

        private void BtnDecline_Click(object sender, RoutedEventArgs e)
        {
            _pendingDevice   = null;
            _pendingManifest = "";
            RequestCard.Visibility      = Visibility.Collapsed;
            NoIncomingBanner.Visibility = Visibility.Visible;
        }

        // ─── Helpers ──────────────────────────────────────────────────────

        private static string FormatBytes(long bytes) =>
            bytes >= 1_000_000_000 ? $"{bytes / 1_000_000_000.0:F1} GB" :
            bytes >= 1_000_000     ? $"{bytes / 1_000_000.0:F1} MB"     :
            bytes >= 1_000         ? $"{bytes / 1_000.0:F1} KB"         :
            $"{bytes} B";
    }
}

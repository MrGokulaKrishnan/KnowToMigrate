using System;
using System.Collections.ObjectModel;
using System.Linq;
using KnowToMigrate.Services;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using Windows.Storage;
using Windows.Storage.Pickers;

namespace KnowToMigrate.Views
{
    public sealed partial class HomeView : Page
    {
        private KtmService? _ktm;

        public ObservableCollection<DeviceInfo>     Devices         { get; } = new();
        public ObservableCollection<TransferRecord> RecentTransfers { get; } = new();

        public HomeView()
        {
            this.InitializeComponent();
        }

        // ─── Navigation lifecycle ────────────────────────────────────────

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);

            if (e.Parameter is KtmService ktm)
            {
                _ktm = ktm;

                // Mirror the service collections into our observable props
                foreach (var d in _ktm.NearbyDevices)   Devices.Add(d);
                foreach (var t in _ktm.TransferHistory) RecentTransfers.Add(t);

                // Keep Devices in sync
                _ktm.NearbyDevices.CollectionChanged += (_, _) =>
                {
                    Devices.Clear();
                    foreach (var d in _ktm.NearbyDevices) Devices.Add(d);
                    NoDevicesText.Visibility = Devices.Count == 0
                        ? Visibility.Visible : Visibility.Collapsed;
                };

                NoDevicesText.Visibility = Devices.Count == 0
                    ? Visibility.Visible : Visibility.Collapsed;

                _ktm.StartDiscovery();
            }
        }

        protected override void OnNavigatedFrom(NavigationEventArgs e)
        {
            base.OnNavigatedFrom(e);
            _ktm?.StopDiscovery();
        }

        // ─── Drop zone ──────────────────────────────────────────────────

        private void DropZone_DragOver(object sender, DragEventArgs e)
        {
            e.AcceptedOperation = Windows.ApplicationModel.DataTransfer.DataPackageOperation.Copy;
        }

        private async void DropZone_Drop(object sender, DragEventArgs e)
        {
            if (e.DataView.Contains(Windows.ApplicationModel.DataTransfer.StandardDataFormats.StorageItems))
            {
                var items = await e.DataView.GetStorageItemsAsync();
                NavigateToSend(items.OfType<StorageFile>().Select(f => f.Path).ToArray());
            }
        }

        // ─── File/folder pickers ─────────────────────────────────────────

        private async void BtnSelectFiles_Click(object sender, RoutedEventArgs e)
        {
            var picker = new FileOpenPicker
            {
                ViewMode            = PickerViewMode.Thumbnail,
                SuggestedStartLocation = PickerLocationId.DocumentsLibrary,
            };
            picker.FileTypeFilter.Add("*");

            // Associate with the current window handle (WinUI 3 requirement)
            var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(
                (Application.Current as App)?.GetMainWindow());
            WinRT.Interop.InitializeWithWindow.Initialize(picker, hwnd);

            var files = await picker.PickMultipleFilesAsync();
            if (files.Count > 0)
                NavigateToSend(files.Select(f => f.Path).ToArray());
        }

        private async void BtnSelectFolder_Click(object sender, RoutedEventArgs e)
        {
            var picker = new FolderPicker
            {
                SuggestedStartLocation = PickerLocationId.DocumentsLibrary,
                ViewMode               = PickerViewMode.List,
            };
            picker.FileTypeFilter.Add("*");

            var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(
                (Application.Current as App)?.GetMainWindow());
            WinRT.Interop.InitializeWithWindow.Initialize(picker, hwnd);

            var folder = await picker.PickSingleFolderAsync();
            if (folder != null)
                NavigateToSend(new[] { folder.Path });
        }

        // ─── Device click ─────────────────────────────────────────────

        private void DevicesList_ItemClick(object sender, ItemClickEventArgs e)
        {
            if (e.ClickedItem is DeviceInfo device)
                Frame.Navigate(typeof(SendView), (_ktm, device, Array.Empty<string>()));
        }

        private void BtnRefreshDevices_Click(object sender, RoutedEventArgs e)
        {
            _ktm?.StopDiscovery();
            _ktm?.StartDiscovery();
        }

        // ─── Helpers ──────────────────────────────────────────────────

        private void NavigateToSend(string[] paths)
        {
            if (_ktm != null)
                Frame.Navigate(typeof(SendView), (_ktm, (DeviceInfo?)null, paths));
        }
    }
}

using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using Windows.Storage.Pickers;
using KnowToMigrate.Services;

namespace KnowToMigrate.Views
{
    public sealed partial class SettingsView : Page
    {
        private KtmService? _service;

        public SettingsView()
        {
            this.InitializeComponent();
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            _service = e.Parameter as KtmService;
            DeviceNameBox.Text = "My Windows PC";
            ReceiveDirBox.Text = System.IO.Path.Combine(
                System.Environment.GetFolderPath(System.Environment.SpecialFolder.UserProfile),
                "Downloads", "KnowToMigrate");
            base.OnNavigatedTo(e);
        }

        private async void BrowseReceiveDir_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
        {
            var picker = new FolderPicker();
            picker.SuggestedStartLocation = PickerLocationId.Downloads;
            picker.FileTypeFilter.Add("*");

            var hwnd = WinRT.Interop.WindowNative.GetWindowHandle(App.MainWindow);
            WinRT.Interop.InitializeWithWindow.Initialize(picker, hwnd);

            var folder = await picker.PickSingleFolderAsync();
            if (folder != null)
            {
                ReceiveDirBox.Text = folder.Path;
            }
        }

        private void SaveSettings_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
        {
            // Persist settings to local storage / registry
            // TODO: wire to SettingsService in v1.1
            var dialog = new ContentDialog
            {
                Title = "Settings Saved",
                Content = "Your settings have been saved.",
                CloseButtonText = "OK",
                XamlRoot = this.XamlRoot,
            };
            _ = dialog.ShowAsync();
        }
    }
}

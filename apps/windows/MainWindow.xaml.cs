using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using KnowToMigrate.Services;
using KnowToMigrate.Views;

namespace KnowToMigrate
{
    public sealed partial class MainWindow : Window
    {
        private readonly KtmService _ktmService;

        public MainWindow()
        {
            this.InitializeComponent();
            this.Title = "KnowToMigrate";
            this.ExtendsContentIntoTitleBar = true;

            _ktmService = new KtmService();
            _ktmService.Initialize();

            // Navigate to home on startup
            MainFrame.Navigate(typeof(HomeView), _ktmService);

            // Wire sidebar navigation
            NavHome.Click      += (_, _) => NavigateTo(typeof(HomeView));
            NavSend.Click      += (_, _) => NavigateTo(typeof(SendView));
            NavReceive.Click   += (_, _) => NavigateTo(typeof(ReceiveView));
            NavHistory.Click   += (_, _) => NavigateTo(typeof(HistoryView));
            NavMigration.Click += (_, _) => NavigateTo(typeof(MigrationView));
            NavSettings.Click  += (_, _) => NavigateTo(typeof(SettingsView));
        }

        private void NavigateTo(Type pageType)
        {
            if (MainFrame.CurrentSourcePageType != pageType)
                MainFrame.Navigate(pageType, _ktmService);
        }

        protected override void OnClosed(EventArgs args)
        {
            _ktmService?.Dispose();
            base.OnClosed(args);
        }
    }
}

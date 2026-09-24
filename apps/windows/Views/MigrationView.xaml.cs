using System;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;
using KnowToMigrate.Services;

namespace KnowToMigrate.Views
{
    public sealed partial class MigrationView : Page
    {
        private KtmService? _service;

        public MigrationView()
        {
            this.InitializeComponent();
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            _service = e.Parameter as KtmService;
            base.OnNavigatedTo(e);
        }

        private void BtnStartMigration_Click(object sender, RoutedEventArgs e)
        {
            StorageText.Text = "Migration plan initialized. Ready to transfer.";
            ScanProgress.IsIndeterminate = false;
            ScanProgress.Value = 100;
        }
    }
}

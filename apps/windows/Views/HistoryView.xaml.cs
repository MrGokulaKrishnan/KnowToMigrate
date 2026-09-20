using Microsoft.UI.Xaml.Controls;
using KnowToMigrate.Services;

namespace KnowToMigrate.Views
{
    public sealed partial class HistoryView : Page
    {
        private KtmService? _service;

        public HistoryView()
        {
            this.InitializeComponent();
        }

        protected override void OnNavigatedTo(Microsoft.UI.Xaml.Navigation.NavigationEventArgs e)
        {
            _service = e.Parameter as KtmService;
            base.OnNavigatedTo(e);
        }
    }
}

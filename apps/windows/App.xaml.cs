using Microsoft.UI.Xaml;

namespace KnowToMigrate
{
    public partial class App : Application
    {
        public static MainWindow? MainWindow { get; set; }
        public MainWindow? GetMainWindow() => MainWindow;

        private MainWindow? _window;

        public App()
        {
            this.InitializeComponent();
        }

        protected override void OnLaunched(Microsoft.UI.Xaml.LaunchActivatedEventArgs args)
        {
            MainWindow = new MainWindow();
            _window = MainWindow;
            _window.Activate();
        }
    }
}

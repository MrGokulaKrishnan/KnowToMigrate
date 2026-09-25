using System;
using Microsoft.UI.Windowing;
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

            // 1. Enable custom title bar extension and set drag area
            this.ExtendsContentIntoTitleBar = true;
            this.SetTitleBar(AppTitleBar);

            // 2. Apply safe window bounds validation & work-area positioning
            WindowBoundsManager.ApplySavedBoundsOrSafeDefaults(this);

            // 3. Track presenter window state changes to toggle Maximize vs Restore icon
            if (this.AppWindow != null)
            {
                this.AppWindow.Changed += AppWindow_Changed;
            }

            _ktmService = new KtmService();
            _ktmService.Initialize();

            // Navigate to home page
            MainFrame.Navigate(typeof(HomeView), _ktmService);

            // Wire navigation
            NavHome.Click      += (_, _) => NavigateTo(typeof(HomeView));
            NavSend.Click      += (_, _) => NavigateTo(typeof(SendView));
            NavReceive.Click   += (_, _) => NavigateTo(typeof(ReceiveView));
            NavHistory.Click   += (_, _) => NavigateTo(typeof(HistoryView));
            NavMigration.Click += (_, _) => NavigateTo(typeof(MigrationView));
            NavSettings.Click  += (_, _) => NavigateTo(typeof(SettingsView));

            // Custom Title Bar Control Actions
            BtnMinimize.Click += (_, _) => MinimizeWindow();
            BtnMaximize.Click += (_, _) => ToggleMaximizeRestoreWindow();
            BtnClose.Click    += (_, _) => this.Close();

            // Save state on close
            this.Closed += (_, _) =>
            {
                WindowBoundsManager.SaveWindowState(this);
                _ktmService?.Dispose();
            };
        }

        private void AppWindow_Changed(AppWindow sender, AppWindowChangedEventArgs args)
        {
            if (args.DidPresenterChange || args.DidPositionChange || args.DidSizeChange)
            {
                UpdateTitleBarButtons();
            }
        }

        private void UpdateTitleBarButtons()
        {
            if (this.AppWindow?.Presenter is OverlappedPresenter presenter)
            {
                bool isMaximized = (presenter.State == OverlappedPresenterState.Maximized);
                GlyphMaximize.Visibility = isMaximized ? Visibility.Collapsed : Visibility.Visible;
                GlyphRestore.Visibility = isMaximized ? Visibility.Visible : Visibility.Collapsed;
                ToolTipService.SetToolTip(BtnMaximize, isMaximized ? "Restore" : "Maximize");
            }
        }

        private void MinimizeWindow()
        {
            if (this.AppWindow?.Presenter is OverlappedPresenter presenter)
            {
                presenter.Minimize();
            }
        }

        private void ToggleMaximizeRestoreWindow()
        {
            if (this.AppWindow?.Presenter is OverlappedPresenter presenter)
            {
                if (presenter.State == OverlappedPresenterState.Maximized)
                {
                    presenter.Restore();
                }
                else
                {
                    presenter.Maximize();
                }
                UpdateTitleBarButtons();
            }
        }

        private void NavigateTo(Type pageType)
        {
            if (MainFrame.CurrentSourcePageType != pageType)
            {
                MainFrame.Navigate(pageType, _ktmService);
            }
        }
    }
}

using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using Microsoft.Win32;
using KnowToMigrate.Services;

namespace KnowToMigrate
{
    public partial class MainWindow : Window
    {
        private readonly List<string> _selectedFiles = new();
        private CancellationTokenSource? _transferCts;
        private string? _forcedTransport = null;

        public MainWindow()
        {
            InitializeComponent();
            Loaded += MainWindow_Loaded;
            Closing += MainWindow_Closing;
        }

        private void MainWindow_Loaded(object sender, RoutedEventArgs e)
        {
            // Safely set window icon
            try
            {
                var iconStream = Application.GetResourceStream(new Uri("pack://application:,,,/KnowToMigrate;component/Assets/KnowToMigrate.ico", UriKind.Absolute));
                if (iconStream != null)
                {
                    this.Icon = System.Windows.Media.Imaging.BitmapFrame.Create(iconStream.Stream);
                }
            }
            catch { }

            // Safely set header and migration logos
            try
            {
                var logoStream = Application.GetResourceStream(new Uri("pack://application:,,,/KnowToMigrate;component/Assets/logo.jpg", UriKind.Absolute));
                if (logoStream != null)
                {
                    var bmp = new System.Windows.Media.Imaging.BitmapImage();
                    bmp.BeginInit();
                    bmp.StreamSource = logoStream.Stream;
                    bmp.CacheOption = System.Windows.Media.Imaging.BitmapCacheOption.OnLoad;
                    bmp.EndInit();
                    LogoHeaderBrush.ImageSource = bmp;
                    LogoMigrationBrush.ImageSource = bmp;
                }
            }
            catch
            {
                try
                {
                    string localPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory ?? "", "Assets", "logo.jpg");
                    if (File.Exists(localPath))
                    {
                        var bmp = new System.Windows.Media.Imaging.BitmapImage(new Uri(localPath, UriKind.Absolute));
                        LogoHeaderBrush.ImageSource = bmp;
                        LogoMigrationBrush.ImageSource = bmp;
                    }
                }
                catch { }
            }

            try
            {
                KtmManager.Instance.Start();
                ListDevices.ItemsSource = KtmManager.Instance.NearbyDevices;
                ListHistory.ItemsSource = KtmManager.Instance.TransferHistory;
                TxtDownloadDir.Text = KtmManager.Instance.DownloadDirectory;
                TxtLocalInfo.Text = $"Device: {KtmManager.Instance.LocalDeviceName} ({KtmManager.Instance.LocalDeviceId})";

                KtmManager.Instance.OnProgress += OnTransferProgress;
                KtmManager.Instance.OnTransferDone += OnTransferCompleted;
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to initialize network services: {ex.Message}", "KnowToMigrate Error", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void MainWindow_Closing(object? sender, System.ComponentModel.CancelEventArgs e)
        {
            _transferCts?.Cancel();
            KtmManager.Instance.Stop();
        }

        private void OnTransferProgress(TransferProgressInfo info)
        {
            Dispatcher.Invoke(() =>
            {
                if (info.IsCompleted)
                {
                    PanelProgress.Visibility = Visibility.Collapsed;
                    ShowNotificationBanner(
                        title: "✓ Transfer Complete",
                        body: $"{info.CurrentFileName} ({KtmFormatting.FormatBytes(info.TotalBytes)}) · Verified",
                        isSuccess: true
                    );
                }
                else if (!string.IsNullOrEmpty(info.ErrorMessage))
                {
                    PanelProgress.Visibility = Visibility.Collapsed;
                    ShowNotificationBanner(
                        title: "Transfer Failed",
                        body: info.ErrorMessage,
                        isSuccess: false
                    );
                }
                else
                {
                    PanelProgress.Visibility = Visibility.Visible;
                    ProgressBarTransfer.Value = info.Percentage;
                    TxtProgressTitle.Text = string.IsNullOrEmpty(info.CurrentFileName)
                        ? "Preparing Transfer..."
                        : $"{info.CurrentFileName} ({info.CurrentFileIndex}/{info.TotalFiles})";

                    TxtActiveTransport.Text = info.TransportType;
                    TxtDirection.Text = info.Direction == TransferDirection.Sending ? "Sending" : "Receiving";

                    TxtTransferredSize.Text = info.FormattedTransferredSize;
                    TxtPercentage.Text = $"{info.Percentage:0}%";
                    TxtSpeed.Text = KtmFormatting.FormatSpeed(info.SpeedMBps);
                    TxtEta.Text = info.FormattedEta;
                    TxtElapsed.Text = info.FormattedElapsedTime;
                    BtnCancelTransfer.Visibility = Visibility.Visible;
                }
            });
        }

        private void ShowNotificationBanner(string title, string body, bool isSuccess)
        {
            TxtNotificationTitle.Text = title;
            TxtNotificationBody.Text = body;
            PanelNotificationBanner.Background = new System.Windows.Media.SolidColorBrush(isSuccess ? System.Windows.Media.Color.FromRgb(7, 28, 15) : System.Windows.Media.Color.FromRgb(35, 9, 9));
            PanelNotificationBanner.BorderBrush = new System.Windows.Media.SolidColorBrush(isSuccess ? System.Windows.Media.Color.FromRgb(34, 197, 94) : System.Windows.Media.Color.FromRgb(239, 68, 68));
            TxtNotificationTitle.Foreground = isSuccess ? new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(34, 197, 94)) : new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(239, 68, 68));
            PanelNotificationBanner.Visibility = Visibility.Visible;
        }

        private void DismissNotification_Click(object sender, RoutedEventArgs e)
        {
            PanelNotificationBanner.Visibility = Visibility.Collapsed;
        }

        private void OnTransferCompleted(string sessionId, bool success, string message)
        {
            Dispatcher.Invoke(() =>
            {
                if (success)
                {
                    KtmManager.Instance.TransferHistory.Add(new TransferProgressInfo
                    {
                        SessionId = sessionId,
                        CurrentFileName = $"{_selectedFiles.Count} items transferred",
                        PeerName = "Completed",
                        IsCompleted = true
                    });
                }
            });
        }

        private void Nav_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string tag)
            {
                ViewHome.Visibility = Visibility.Collapsed;
                ViewReceive.Visibility = Visibility.Collapsed;
                ViewMigration.Visibility = Visibility.Collapsed;
                ViewHistory.Visibility = Visibility.Collapsed;

                switch (tag)
                {
                    case "Home":
                        ViewHome.Visibility = Visibility.Visible;
                        HighlightNav(BtnNavHome);
                        break;
                    case "Receive":
                        ViewReceive.Visibility = Visibility.Visible;
                        HighlightNav(BtnNavReceive);
                        break;
                    case "Migration":
                        ViewMigration.Visibility = Visibility.Visible;
                        HighlightNav(BtnNavMigration);
                        break;
                    case "History":
                        ViewHistory.Visibility = Visibility.Visible;
                        HighlightNav(BtnNavHistory);
                        break;
                }
            }
        }

        private void HighlightNav(Button active)
        {
            Button[] buttons = { BtnNavHome, BtnNavReceive, BtnNavMigration, BtnNavHistory };
            foreach (var b in buttons)
            {
                if (b == active)
                {
                    b.Background = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(26, 13, 0));
                    b.Foreground = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(255, 138, 0));
                    b.BorderBrush = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(255, 90, 0));
                    b.BorderThickness = new Thickness(1);
                }
                else
                {
                    b.Background = System.Windows.Media.Brushes.Transparent;
                    b.Foreground = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(204, 204, 204));
                    b.BorderThickness = new Thickness(0);
                }
            }
        }

        private void DropZone_DragOver(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent(DataFormats.FileDrop))
                e.Effects = DragDropEffects.Copy;
            else
                e.Effects = DragDropEffects.None;
            e.Handled = true;
        }

        private void DropZone_Drop(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent(DataFormats.FileDrop))
            {
                var files = (string[])e.Data.GetData(DataFormats.FileDrop);
                if (files != null && files.Length > 0)
                {
                    _selectedFiles.Clear();
                    _selectedFiles.AddRange(files);
                    UpdateFileSelectionUI();
                }
            }
        }

        private void ChooseFiles_Click(object sender, RoutedEventArgs e)
        {
            var dlg = new OpenFileDialog
            {
                Multiselect = true,
                Title = "Select Files to Transfer"
            };
            if (dlg.ShowDialog() == true)
            {
                _selectedFiles.Clear();
                _selectedFiles.AddRange(dlg.FileNames);
                UpdateFileSelectionUI();
            }
        }

        private void ChooseFolder_Click(object sender, RoutedEventArgs e)
        {
            var dlg = new OpenFolderDialog
            {
                Title = "Select Folder to Transfer"
            };
            if (dlg.ShowDialog() == true)
            {
                _selectedFiles.Clear();
                _selectedFiles.Add(dlg.FolderName);
                UpdateFileSelectionUI();
            }
        }

        private void UpdateFileSelectionUI()
        {
            long totalBytes = 0;
            foreach (var f in _selectedFiles)
            {
                if (File.Exists(f)) totalBytes += new FileInfo(f).Length;
                else if (Directory.Exists(f))
                {
                    foreach (var sf in Directory.EnumerateFiles(f, "*", SearchOption.AllDirectories))
                    {
                        try { totalBytes += new FileInfo(sf).Length; } catch { }
                    }
                }
            }

            TxtStatus.Text = $"{_selectedFiles.Count} item(s) staged for transfer";
            TxtFileInfo.Text = $"{FormatBytes(totalBytes)} staged · Select a target device and click Transfer Now";
        }

        private void AddManualDevice_Click(object sender, RoutedEventArgs e)
        {
            string ip = TxtManualIp.Text.Trim();
            if (string.IsNullOrEmpty(ip) || ip == "192.168.1.")
            {
                MessageBox.Show("Please enter a valid IP address.", "KnowToMigrate", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }
            KtmManager.Instance.DiscoveryService.AddManualDevice(ip);
        }

        private async void TransferNow_Click(object sender, RoutedEventArgs e)
        {
            if (_selectedFiles.Count == 0)
            {
                MessageBox.Show("Please choose files or a folder to transfer first.", "KnowToMigrate", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            var targetDevice = ListDevices.SelectedItem as DiscoveredDevice;
            if (targetDevice == null)
            {
                var onlineDevices = KtmManager.Instance.DiscoveryService.GetOnlineDevices();
                if (onlineDevices.Count == 1)
                {
                    targetDevice = onlineDevices[0];
                }
                else if (onlineDevices.Count > 1)
                {
                    MessageBox.Show("Multiple devices discovered. Please select the destination device from the list on the right.", "KnowToMigrate", MessageBoxButton.OK, MessageBoxImage.Information);
                    return;
                }
                else
                {
                    string manualIp = TxtManualIp.Text.Trim();
                    if (!string.IsNullOrEmpty(manualIp) && manualIp != "192.168.1.")
                    {
                        KtmManager.Instance.DiscoveryService.AddManualDevice(manualIp);
                        targetDevice = new DiscoveredDevice
                        {
                            DeviceName = manualIp,
                            IpAddress = manualIp,
                            TransferPort = KtmConstants.TransferPort
                        };
                    }
                    else
                    {
                        MessageBox.Show("No destination device selected. Please wait for nearby devices to be discovered, or enter the target device IP directly.", "KnowToMigrate", MessageBoxButton.OK, MessageBoxImage.Information);
                        return;
                    }
                }
            }

            _transferCts = new CancellationTokenSource();
            BtnTransfer.IsEnabled = false;
            PanelProgress.Visibility = Visibility.Visible;
            ProgressBarTransfer.Value = 0;
            string transportLabel = _forcedTransport == null ? "Auto (Best)" : KtmTransportCodes.GetDisplayName(_forcedTransport);
            TxtProgressTitle.Text = $"Connecting to {targetDevice.DeviceName} ({targetDevice.IpAddress})...";
            TxtActiveTransport.Text = transportLabel;
            TxtDirection.Text = "Connecting";
            TxtTransferredSize.Text = "Performing mutual cryptographic handshake...";

            try
            {
                bool success = await Task.Run(() => KtmManager.Instance.SendFilesAsync(targetDevice, _selectedFiles, _forcedTransport, _transferCts.Token));
                if (success)
                {
                    MessageBox.Show("Transfer completed and cryptographically verified!", "KnowToMigrate Success", MessageBoxButton.OK, MessageBoxImage.Information);
                }
                else
                {
                    MessageBox.Show("Transfer failed or was rejected by recipient.", "KnowToMigrate Transfer", MessageBoxButton.OK, MessageBoxImage.Warning);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Transfer failed: {ex.Message}", "KnowToMigrate Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
            finally
            {
                BtnTransfer.IsEnabled = true;
            }
        }

        private void TransportSelect_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string tag)
            {
                Button[] buttons = { BtnTransportAuto, BtnTransportWifi, BtnTransportDirect, BtnTransportBt };
                foreach (var b in buttons)
                {
                    b.Style = (Style)FindResource("KmGlassButton");
                }
                btn.Style = (Style)FindResource("KmPrimaryButton");

                _forcedTransport = tag == "AUTO" ? null : tag;

                if (ListDevices.SelectedItem is DiscoveredDevice dev)
                {
                    string chosen = _forcedTransport ?? dev.BestTransport;
                    TxtBestTransportBadge.Text = _forcedTransport == null
                        ? $"Best: {KtmTransportCodes.GetDisplayName(chosen)} ({KtmTransportCodes.GetSpeedRating(chosen)})"
                        : $"Manual: {KtmTransportCodes.GetDisplayName(chosen)} ({KtmTransportCodes.GetSpeedRating(chosen)})";
                }
                else
                {
                    string chosen = _forcedTransport ?? KtmTransportCodes.WifiLan;
                    TxtBestTransportBadge.Text = _forcedTransport == null
                        ? $"Best: {KtmTransportCodes.GetDisplayName(chosen)} ({KtmTransportCodes.GetSpeedRating(chosen)})"
                        : $"Manual: {KtmTransportCodes.GetDisplayName(chosen)} ({KtmTransportCodes.GetSpeedRating(chosen)})";
                }
            }
        }

        private async void ListDevices_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (ListDevices.SelectedItem is DiscoveredDevice dev)
            {
                string best = await KtmTransportManager.Instance.EvaluateAndSelectBestTransportAsync(dev);
                string chosen = _forcedTransport ?? best;
                TxtBestTransportBadge.Text = _forcedTransport == null
                    ? $"Best: {KtmTransportCodes.GetDisplayName(chosen)} ({KtmTransportCodes.GetSpeedRating(chosen)})"
                    : $"Manual: {KtmTransportCodes.GetDisplayName(chosen)} ({KtmTransportCodes.GetSpeedRating(chosen)})";
            }
        }

        private void CancelTransfer_Click(object sender, RoutedEventArgs e)
        {
            _transferCts?.Cancel();
            TxtProgressTitle.Text = "Transfer cancelled by user";
            BtnCancelTransfer.Visibility = Visibility.Collapsed;
        }

        private void ChangeDownloadFolder_Click(object sender, RoutedEventArgs e)
        {
            var dlg = new OpenFolderDialog
            {
                Title = "Select Default Download Directory",
                InitialDirectory = KtmManager.Instance.DownloadDirectory
            };
            if (dlg.ShowDialog() == true)
            {
                KtmManager.Instance.DownloadDirectory = dlg.FolderName;
                TxtDownloadDir.Text = dlg.FolderName;
            }
        }

        private void ScanMigration_Click(object sender, RoutedEventArgs e)
        {
            var pathsToScan = new List<string>();
            if (ChkDocs.IsChecked == true) pathsToScan.Add(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments));
            if (ChkDesktop.IsChecked == true) pathsToScan.Add(Environment.GetFolderPath(Environment.SpecialFolder.Desktop));
            if (ChkPictures.IsChecked == true) pathsToScan.Add(Environment.GetFolderPath(Environment.SpecialFolder.MyPictures));
            if (ChkMusic.IsChecked == true) pathsToScan.Add(Environment.GetFolderPath(Environment.SpecialFolder.MyMusic));
            if (ChkVideos.IsChecked == true) pathsToScan.Add(Environment.GetFolderPath(Environment.SpecialFolder.MyVideos));

            long totalBytes = 0;
            int fileCount = 0;

            foreach (var p in pathsToScan)
            {
                if (Directory.Exists(p))
                {
                    foreach (var f in Directory.EnumerateFiles(p, "*", SearchOption.AllDirectories))
                    {
                        try
                        {
                            totalBytes += new FileInfo(f).Length;
                            fileCount++;
                        }
                        catch { }
                    }
                }
            }

            MessageBox.Show($"Migration Scan Complete!\n\nFound {fileCount} files across selected categories.\nTotal Size: {FormatBytes(totalBytes)}\n\nClick 'Start Migration Transfer' to send to your target device.", "KnowToMigrate Migration Scan", MessageBoxButton.OK, MessageBoxImage.Information);
        }

        private void StartMigration_Click(object sender, RoutedEventArgs e)
        {
            _selectedFiles.Clear();
            if (ChkDocs.IsChecked == true) _selectedFiles.Add(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments));
            if (ChkDesktop.IsChecked == true) _selectedFiles.Add(Environment.GetFolderPath(Environment.SpecialFolder.Desktop));
            if (ChkPictures.IsChecked == true) _selectedFiles.Add(Environment.GetFolderPath(Environment.SpecialFolder.MyPictures));
            if (ChkMusic.IsChecked == true) _selectedFiles.Add(Environment.GetFolderPath(Environment.SpecialFolder.MyMusic));
            if (ChkVideos.IsChecked == true) _selectedFiles.Add(Environment.GetFolderPath(Environment.SpecialFolder.MyVideos));

            ViewMigration.Visibility = Visibility.Collapsed;
            ViewHome.Visibility = Visibility.Visible;
            HighlightNav(BtnNavHome);
            UpdateFileSelectionUI();
        }

        private static string FormatBytes(long bytes)
        {
            string[] suffixes = { "B", "KB", "MB", "GB", "TB" };
            int i = 0;
            double d = bytes;
            while (d >= 1024 && i < suffixes.Length - 1)
            {
                d /= 1024;
                i++;
            }
            return $"{d:0.##} {suffixes[i]}";
        }

        private void MainWindow_StateChanged(object? sender, EventArgs e)
        {
            if (this.WindowState == WindowState.Maximized)
            {
                RootBorder.Margin = new Thickness(7);
                BtnMaximize.ToolTip = "Restore Down";
                try
                {
                    MaximizePath.Data = Geometry.Parse("M2,0 H10 V8 H2 Z M0,2 H8 V10 H0 Z");
                }
                catch { }
            }
            else
            {
                RootBorder.Margin = new Thickness(0);
                BtnMaximize.ToolTip = "Maximize";
                try
                {
                    MaximizePath.Data = Geometry.Parse("M0,0 H10 V10 H0 Z");
                }
                catch { }
            }
        }

        private void TitleBar_MouseDown(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            if (e.ChangedButton == System.Windows.Input.MouseButton.Left)
            {
                if (e.ClickCount == 2)
                {
                    BtnMaximize_Click(sender, e);
                }
                else
                {
                    try
                    {
                        this.DragMove();
                    }
                    catch { }
                }
            }
        }

        private void BtnMinimize_Click(object sender, RoutedEventArgs e)
        {
            this.WindowState = WindowState.Minimized;
        }

        private void BtnMaximize_Click(object sender, RoutedEventArgs e)
        {
            this.WindowState = this.WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
        }

        private void BtnClose_Click(object sender, RoutedEventArgs e)
        {
            this.Close();
        }
    }
}

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
            // CRITICAL FIX: Apply saved/validated window bounds in Loaded (after Activate),
            // never in the constructor. This prevents clipping caused by CenterScreen
            // resolving to wrong coordinates before the window is fully shown.
            WindowBoundsManager.ApplyToWindow(this);

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
                TxtUpdatedOn.Text = "Updated On: " + DateTime.Now.ToString("dd MMMM yyyy", System.Globalization.CultureInfo.InvariantCulture);

                KtmManager.Instance.NearbyDevices.CollectionChanged += (s, e) => UpdateDiscoveryStatus();
                UpdateDiscoveryStatus();

                KtmManager.Instance.OnProgress += OnTransferProgress;
                KtmManager.Instance.OnTransferDone += OnTransferCompleted;
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to initialize network services: {ex.Message}", "KnowToMigrate Error", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void UpdateDiscoveryStatus()
        {
            Dispatcher.Invoke(() =>
            {
                int count = KtmManager.Instance.NearbyDevices.Count;
                if (count == 0)
                {
                    DotDiscoveryStatus.Fill = new SolidColorBrush(Color.FromRgb(0xFF, 0x5A, 0x00));
                    TxtDiscoveryStatus.Text = "Scanning";
                    TxtDiscoveryStatus.Foreground = new SolidColorBrush(Color.FromRgb(0xFF, 0x8A, 0x00));
                }
                else if (count == 1)
                {
                    DotDiscoveryStatus.Fill = new SolidColorBrush(Color.FromRgb(0x22, 0xC5, 0x5E));
                    TxtDiscoveryStatus.Text = "1 Target";
                    TxtDiscoveryStatus.Foreground = new SolidColorBrush(Color.FromRgb(0x22, 0xC5, 0x5E));
                }
                else
                {
                    DotDiscoveryStatus.Fill = new SolidColorBrush(Color.FromRgb(0x22, 0xC5, 0x5E));
                    TxtDiscoveryStatus.Text = $"{count} Targets";
                    TxtDiscoveryStatus.Foreground = new SolidColorBrush(Color.FromRgb(0x22, 0xC5, 0x5E));
                }
            });
        }

        private void MainWindow_Closing(object? sender, System.ComponentModel.CancelEventArgs e)
        {
            // Save window state on close so we can restore it correctly next launch
            WindowBoundsManager.SaveWindowState(this);
            _transferCts?.Cancel();
            KtmManager.Instance.Stop();
        }

        private string? _lastReceivedFilePath;

        private void OnTransferProgress(TransferProgressInfo info)
        {
            Dispatcher.Invoke(() =>
            {
                if (info.IsCompleted)
                {
                    PanelProgress.Visibility = Visibility.Collapsed;
                    if (info.Direction == TransferDirection.Receiving)
                    {
                        ShowReceiveCompletionModal(info);
                    }
                    else
                    {
                        ShowNotificationBanner(
                            title: "✓ Transfer Complete",
                            body: $"{info.CurrentFileName} ({KtmFormatting.FormatBytes(info.TotalBytes)}) · Verified",
                            isSuccess: true
                        );
                    }
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

        private void ShowReceiveCompletionModal(TransferProgressInfo info)
        {
            _lastReceivedFilePath = !string.IsNullOrEmpty(info.FinalizedFilePath) && File.Exists(info.FinalizedFilePath)
                ? info.FinalizedFilePath
                : Path.Combine(KtmManager.Instance.DownloadDirectory, info.CurrentFileName);

            string displayName = !string.IsNullOrEmpty(info.FinalizedFileName)
                ? info.FinalizedFileName
                : (string.IsNullOrEmpty(info.CurrentFileName) ? "Received File" : info.CurrentFileName);

            TxtReceivedFileName.Text = displayName;
            TxtReceivedFileSize.Text = KtmFormatting.FormatBytes(info.TotalBytes);
            TxtReceivedSender.Text = string.IsNullOrEmpty(info.PeerName) ? "Nearby Device" : info.PeerName;
            TxtReceivedSavedTo.Text = "Downloads / KnowToMigrate";
            TxtCopyFeedback.Visibility = Visibility.Collapsed;

            try
            {
                string ext = Path.GetExtension(displayName).ToLowerInvariant();
                if (ext is ".jpg" or ".jpeg" or ".png" or ".gif" or ".webp" or ".bmp" or ".svg")
                {
                    IconReceivedFile.Data = Geometry.Parse("M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z");
                }
                else if (ext is ".mp4" or ".mkv" or ".avi" or ".mov" or ".webm" or ".wmv")
                {
                    IconReceivedFile.Data = Geometry.Parse("M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z");
                }
                else if (ext is ".mp3" or ".wav" or ".flac" or ".aac" or ".m4a" or ".ogg")
                {
                    IconReceivedFile.Data = Geometry.Parse("M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z");
                }
                else if (ext is ".pdf")
                {
                    IconReceivedFile.Data = Geometry.Parse("M20 2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8.5 7.5c0 .83-.67 1.5-1.5 1.5H9v2H7.5V7H10c.83 0 1.5.67 1.5 1.5v1zm5 2c0 .83-.67 1.5-1.5 1.5h-2.5V7H15c.83 0 1.5.67 1.5 1.5v3zm4-3H19v1h1.5V11H19v2h-1.5V7h3v1.5zM9 9.5h1v-1H9v1zm4.5 1.5h1v-2.5h-1V11z");
                }
                else if (ext is ".zip" or ".rar" or ".7z" or ".tar" or ".gz")
                {
                    IconReceivedFile.Data = Geometry.Parse("M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-6 10h-2v-2h2v2zm0-4h-2v-2h2v2z");
                }
                else
                {
                    IconReceivedFile.Data = Geometry.Parse("M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z");
                }
            }
            catch { }

            ModalReceiveCompletion.Visibility = Visibility.Visible;
        }

        private void BtnCloseReceiveModal_Click(object sender, RoutedEventArgs e)
        {
            ModalReceiveCompletion.Visibility = Visibility.Collapsed;
        }

        private void BtnOpenReceivedFile_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                if (!string.IsNullOrEmpty(_lastReceivedFilePath) && File.Exists(_lastReceivedFilePath))
                {
                    System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
                    {
                        FileName = _lastReceivedFilePath,
                        UseShellExecute = true
                    });
                }
                else
                {
                    System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
                    {
                        FileName = KtmManager.Instance.DownloadDirectory,
                        UseShellExecute = true
                    });
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Could not open file: {ex.Message}", "KnowToMigrate", MessageBoxButton.OK, MessageBoxImage.Information);
            }
        }

        private void BtnShowInFolder_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                if (!string.IsNullOrEmpty(_lastReceivedFilePath) && File.Exists(_lastReceivedFilePath))
                {
                    System.Diagnostics.Process.Start("explorer.exe", $"/select,\"{_lastReceivedFilePath}\"");
                }
                else
                {
                    System.Diagnostics.Process.Start("explorer.exe", $"\"{KtmManager.Instance.DownloadDirectory}\"");
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Could not open folder: {ex.Message}", "KnowToMigrate", MessageBoxButton.OK, MessageBoxImage.Information);
            }
        }

        private void BtnCopyReceivedFile_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                if (!string.IsNullOrEmpty(_lastReceivedFilePath) && File.Exists(_lastReceivedFilePath))
                {
                    var fileList = new System.Collections.Specialized.StringCollection { _lastReceivedFilePath };
                    Clipboard.SetFileDropList(fileList);
                    TxtCopyFeedback.Text = "✓ Copied to clipboard";
                    TxtCopyFeedback.Visibility = Visibility.Visible;
                }
                else if (!string.IsNullOrEmpty(_lastReceivedFilePath))
                {
                    Clipboard.SetText(_lastReceivedFilePath);
                    TxtCopyFeedback.Text = "✓ Copied path to clipboard";
                    TxtCopyFeedback.Visibility = Visibility.Visible;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Could not copy file: {ex.Message}", "KnowToMigrate", MessageBoxButton.OK, MessageBoxImage.Information);
            }
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
            string transportLabel = _forcedTransport == null ? "Adaptive Route" : KtmTransportCodes.GetDisplayName(_forcedTransport);
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

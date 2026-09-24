using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using Microsoft.Win32;
using KnowToMigrate.Services;

namespace KnowToMigrate
{
    public partial class MainWindow : Window
    {
        private readonly List<string> _selectedFiles = new();
        private CancellationTokenSource? _transferCts;

        public MainWindow()
        {
            InitializeComponent();
            Loaded += MainWindow_Loaded;
            Closing += MainWindow_Closing;
        }

        private void MainWindow_Loaded(object sender, RoutedEventArgs e)
        {
            try
            {
                var iconUri = new Uri("pack://application:,,,/Assets/KnowToMigrate.ico", UriKind.RelativeOrAbsolute);
                this.Icon = System.Windows.Media.Imaging.BitmapFrame.Create(iconUri);
            }
            catch { }

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
                PanelProgress.Visibility = Visibility.Visible;
                ProgressBarTransfer.Value = info.Percentage;
                TxtProgressTitle.Text = $"Transferring: {info.CurrentFileName} ({info.CurrentFileIndex}/{info.TotalFiles})";
                TxtProgressDetail.Text = $"{FormatBytes(info.BytesTransferred)} / {FormatBytes(info.TotalBytes)} ({info.Percentage:0.0}%) · {info.SpeedMBps:0.0} MB/s";

                if (info.IsCompleted)
                {
                    TxtProgressTitle.Text = "✓ Transfer Complete and Verified!";
                    TxtProgressDetail.Text = "All files cryptographically verified with SHA-256.";
                    BtnCancelTransfer.Visibility = Visibility.Collapsed;
                }
                else if (!string.IsNullOrEmpty(info.ErrorMessage))
                {
                    TxtProgressTitle.Text = "Transfer Failed";
                    TxtProgressDetail.Text = info.ErrorMessage;
                    BtnCancelTransfer.Visibility = Visibility.Collapsed;
                }
                else
                {
                    BtnCancelTransfer.Visibility = Visibility.Visible;
                }
            });
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

            TxtStatus.Text = $"✓ {_selectedFiles.Count} item(s) staged for transfer";
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
            TxtProgressTitle.Text = $"Connecting to {targetDevice.DeviceName} ({targetDevice.IpAddress})...";
            TxtProgressDetail.Text = "Performing mutual cryptographic handshake...";

            try
            {
                bool success = await Task.Run(() => KtmManager.Instance.SendFilesAsync(targetDevice, _selectedFiles, _transferCts.Token));
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
    }
}

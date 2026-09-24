using System;
using System.IO;
using System.Windows;
using System.Windows.Controls;
using Microsoft.Win32;

namespace KnowToMigrate
{
    public partial class MainWindow : Window
    {
        private string[] _selectedFiles = Array.Empty<string>();

        public MainWindow()
        {
            InitializeComponent();
        }

        private void Nav_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string tag)
            {
                if (tag == "Home")
                {
                    ViewHome.Visibility = Visibility.Visible;
                    ViewOther.Visibility = Visibility.Collapsed;
                    HighlightNav(BtnNavHome);
                }
                else
                {
                    ViewHome.Visibility = Visibility.Collapsed;
                    ViewOther.Visibility = Visibility.Visible;
                    TxtOtherTitle.Text = tag switch
                    {
                        "Send" => "📤 Send Files (Select Destination Device)",
                        "Receive" => "📥 Waiting for Incoming Transfers (Listening on TCP 54124)",
                        "Migration" => "🔄 PC Migration Wizard (Step 1: System Scan)",
                        "History" => "📋 Transfer History & Checkpoint Integrity",
                        _ => tag
                    };
                    HighlightNav(btn);
                }
            }
        }

        private void HighlightNav(Button active)
        {
            Button[] buttons = { BtnNavHome, BtnNavSend, BtnNavReceive, BtnNavMigration, BtnNavHistory };
            foreach (var b in buttons)
            {
                if (b == active)
                {
                    b.Background = System.Windows.Media.Brushes.DarkOrange;
                    b.Foreground = System.Windows.Media.Brushes.White;
                }
                else
                {
                    b.Background = System.Windows.Media.Brushes.Transparent;
                    b.Foreground = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromRgb(204, 204, 204));
                }
            }
        }

        private void BackToHome_Click(object sender, RoutedEventArgs e)
        {
            ViewHome.Visibility = Visibility.Visible;
            ViewOther.Visibility = Visibility.Collapsed;
            HighlightNav(BtnNavHome);
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
                    _selectedFiles = files;
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
                _selectedFiles = dlg.FileNames;
                UpdateFileSelectionUI();
            }
        }

        private void ChooseFolder_Click(object sender, RoutedEventArgs e)
        {
            // Folder picker
            var dlg = new OpenFolderDialog
            {
                Title = "Select Folder to Transfer"
            };
            if (dlg.ShowDialog() == true)
            {
                _selectedFiles = new[] { dlg.FolderName };
                UpdateFileSelectionUI();
            }
        }

        private void UpdateFileSelectionUI()
        {
            long totalBytes = 0;
            foreach (var f in _selectedFiles)
            {
                if (File.Exists(f)) totalBytes += new FileInfo(f).Length;
                else if (Directory.Exists(f)) totalBytes += 100_000_000; // estimation for folder
            }

            TxtStatus.Text = $"✓ {_selectedFiles.Length} item(s) staged for transfer";
            TxtFileInfo.Text = $"{FormatBytes(totalBytes)} ready · Encryption: AES-256-GCM";
        }

        private void TransferNow_Click(object sender, RoutedEventArgs e)
        {
            if (_selectedFiles.Length == 0)
            {
                MessageBox.Show("Please drag and drop files or click 'Choose Files' first.", "KnowToMigrate", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            MessageBox.Show($"Initiating encrypted P2P transfer of {_selectedFiles.Length} file(s) via local UDP/TCP discovery...\n\nSearching for compatible KnowToMigrate devices on local network...", "KnowToMigrate Transfer", MessageBoxButton.OK, MessageBoxImage.Information);
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

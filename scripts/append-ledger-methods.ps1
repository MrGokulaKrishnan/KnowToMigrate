$csPath = "c:\KnowToMigrate\apps\windows-wpf\MainWindow.xaml.cs"
$cs = Get-Content $csPath -Raw

$ledgerMethods = @"
        #region Migration Ledger Helpers

        private void UpdateLedgerUI()
        {
            Dispatcher.Invoke(() =>
            {
                if (PanelLedgerEmpty != null)
                    PanelLedgerEmpty.Visibility = _ledgerEntries.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
                if (ListHistory != null)
                    ListHistory.Visibility = _ledgerEntries.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
            });
        }

        private void BtnClearLedger_Click(object sender, RoutedEventArgs e)
        {
            if (MessageBox.Show("Are you sure you want to clear your Migration Ledger history?", "Clear Ledger", MessageBoxButton.YesNo, MessageBoxImage.Question) == MessageBoxResult.Yes)
            {
                UserSettingsManager.ClearHistory();
                _ledgerEntries.Clear();
                UpdateLedgerUI();
                ShowNotificationBanner("Ledger Cleared", "Transfer history records have been cleared.", true);
            }
        }

        private void BtnExportLedger_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                string report = UserSettingsManager.ExportHistoryText();
                string folder = KtmManager.Instance.DownloadDirectory;
                if (!Directory.Exists(folder)) Directory.CreateDirectory(folder);
                string logPath = Path.Combine(folder, "Migration_Ledger_" + DateTime.Now.ToString("yyyyMMdd_HHmmss") + ".txt");
                File.WriteAllText(logPath, report, Encoding.UTF8);
                MessageBox.Show("Migration Ledger audit log exported successfully!\r\n\r\nLocation: " + logPath, "Audit Log Exported", MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (Exception ex)
            {
                MessageBox.Show("Failed to export audit log: " + ex.Message, "Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private void BtnOpenDownloadsFolder_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                string folder = KtmManager.Instance.DownloadDirectory;
                if (!Directory.Exists(folder)) Directory.CreateDirectory(folder);
                Process.Start(new ProcessStartInfo
                {
                    FileName = folder,
                    UseShellExecute = true
                });
            }
            catch { }
        }

        private void BtnShowHistoryFile_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is string path && !string.IsNullOrEmpty(path))
            {
                try
                {
                    if (File.Exists(path))
                    {
                        Process.Start("explorer.exe", "/select,\"" + path + "\"");
                        return;
                    }
                }
                catch { }
            }
            BtnOpenDownloadsFolder_Click(sender, e);
        }

        #endregion
"@

$target = "        #endregion`r`n    }`r`n}"
if ($cs.Contains($target)) {
    $cs = $cs.Replace($target, "$ledgerMethods`r`n`r`n        #endregion`r`n    }`r`n}")
} else {
    $targetLF = "        #endregion`n    }`n}"
    $cs = $cs.Replace($targetLF, "$ledgerMethods`n`n        #endregion`n    }`n}")
}

Set-Content -Path $csPath -Value $cs -Encoding utf8
Write-Host "Appended ledger methods."

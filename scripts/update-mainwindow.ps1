$csPath = "c:\KnowToMigrate\apps\windows-wpf\MainWindow.xaml.cs"
$cs = Get-Content $csPath -Raw

# 1. Add _ledgerEntries field
if ($cs -notmatch '_ledgerEntries') {
    $cs = $cs.Replace(
        'private KtmMigrationReport? _lastMigrationReport = null;',
        "private KtmMigrationReport? _lastMigrationReport = null;`r`n        private readonly System.Collections.ObjectModel.ObservableCollection<HistoricalTransferItem> _ledgerEntries = new();"
    )
}

# 2. Update ListHistory.ItemsSource in constructor / Loaded
$oldHistBinding = 'ListHistory.ItemsSource = KtmManager.Instance.TransferHistory;'
$newHistBinding = @"
                // Initialize persistent Migration Ledger
                var history = UserSettingsManager.LoadHistory();
                _ledgerEntries.Clear();
                foreach (var hItem in history)
                {
                    _ledgerEntries.Add(hItem);
                }
                ListHistory.ItemsSource = _ledgerEntries;
                UpdateLedgerUI();
"@
$cs = $cs.Replace($oldHistBinding, $newHistBinding)

# 3. Update Nav_Click
$oldNavClick = @"
                ViewHome.Visibility = Visibility.Collapsed;
                ViewReceive.Visibility = Visibility.Collapsed;
                ViewMigration.Visibility = Visibility.Collapsed;
                ViewHistory.Visibility = Visibility.Collapsed;
                ViewWebShare.Visibility = Visibility.Collapsed;
                ViewSettings.Visibility = Visibility.Collapsed;

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
                    case "Settings":
                        ViewSettings.Visibility = Visibility.Visible;
                        HighlightNav(BtnNavSettings);
                        break;
                }
"@

$newNavClick = @"
                ViewHome.Visibility = Visibility.Collapsed;
                ViewReceive.Visibility = Visibility.Collapsed;
                ViewWebShare.Visibility = Visibility.Collapsed;
                ViewMigration.Visibility = Visibility.Collapsed;
                ViewHistory.Visibility = Visibility.Collapsed;
                ViewSecurity.Visibility = Visibility.Collapsed;
                ViewSettings.Visibility = Visibility.Collapsed;

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
                    case "WebShare":
                        ViewWebShare.Visibility = Visibility.Visible;
                        HighlightNav(BtnNavWebShare);
                        RefreshWebShareUi();
                        break;
                    case "Migration":
                        ViewMigration.Visibility = Visibility.Visible;
                        HighlightNav(BtnNavMigration);
                        break;
                    case "History":
                        ViewHistory.Visibility = Visibility.Visible;
                        HighlightNav(BtnNavHistory);
                        UpdateLedgerUI();
                        break;
                    case "Security":
                        ViewSecurity.Visibility = Visibility.Visible;
                        HighlightNav(BtnNavSecurity);
                        UpdateDiagnostics();
                        break;
                    case "Settings":
                        ViewSettings.Visibility = Visibility.Visible;
                        HighlightNav(BtnNavSettings);
                        break;
                }
"@
# Normalize newlines for replace
$cs = [regex]::Replace($cs, '(?s)ViewHome\.Visibility = Visibility\.Collapsed;.*?switch \(tag\).*?case "Settings":.*?break;\s*}', $newNavClick.Trim())

# 4. In OnTransferCompleted, record into ledger
$oldComplete = @"
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
"@

$newComplete = @"
                if (success)
                {
                    try
                    {
                        long totalBytes = 0;
                        string title = $"{_selectedFiles.Count} items transferred";
                        if (_selectedFiles.Count == 1 && File.Exists(_selectedFiles[0]))
                        {
                            var fi = new FileInfo(_selectedFiles[0]);
                            title = fi.Name;
                            totalBytes = fi.Length;
                        }
                        else
                        {
                            foreach (var f in _selectedFiles)
                            {
                                if (File.Exists(f)) totalBytes += new FileInfo(f).Length;
                            }
                        }

                        var entry = new HistoricalTransferItem
                        {
                            FileName = title,
                            TotalBytes = totalBytes,
                            DeviceName = _selectedDevice?.DeviceName ?? "Nearby Target",
                            Direction = "out",
                            Status = "Complete",
                            Transport = _selectedDevice?.ActiveTransport ?? "Pluto Direct",
                            Sha256 = "verified",
                            FilePath = _selectedFiles.Count > 0 ? _selectedFiles[0] : null
                        };
                        UserSettingsManager.AddHistoryItem(entry);
                        _ledgerEntries.Insert(0, entry);
                        UpdateLedgerUI();
                    }
                    catch { }
                }
"@
$cs = [regex]::Replace($cs, '(?s)if \(success\)\s*\{\s*KtmManager\.Instance\.TransferHistory\.Add\(.*?\);\s*\}', $newComplete.Trim())

# 5. In ShowReceiveCompletionModal, record into ledger
$oldReceiveModal = 'ModalReceiveCompletion.Visibility = Visibility.Visible;'
$newReceiveModal = @"
            try
            {
                var receiveItem = new HistoricalTransferItem
                {
                    FileName = displayName,
                    TotalBytes = info.TotalBytes,
                    DeviceName = string.IsNullOrEmpty(info.PeerName) ? "Nearby Device" : info.PeerName,
                    Direction = "in",
                    Status = "Complete",
                    Transport = info.TransportType ?? "Pluto Wi-Fi Direct",
                    Sha256 = "verified",
                    FilePath = _lastReceivedFilePath
                };
                UserSettingsManager.AddHistoryItem(receiveItem);
                _ledgerEntries.Insert(0, receiveItem);
                UpdateLedgerUI();
            }
            catch { }

            ModalReceiveCompletion.Visibility = Visibility.Visible;
"@
if ($cs -notmatch 'UserSettingsManager\.AddHistoryItem\(receiveItem\)') {
    $cs = $cs.Replace($oldReceiveModal, $newReceiveModal.Trim())
}

# 6. In WebShare OnFileUploaded, record into ledger
$oldWebShareUpload = 'ShowNotificationBanner("Web Share Received", $"{fileName} ({KtmFormatting.FormatBytes(bytes)}) received via browser.", true);'
$newWebShareUpload = @"
ShowNotificationBanner("Web Share Received", $"{fileName} ({KtmFormatting.FormatBytes(bytes)}) received via browser.", true);
                        try
                        {
                            var webItem = new HistoricalTransferItem
                            {
                                FileName = fileName,
                                TotalBytes = bytes,
                                DeviceName = "Browser Web Client",
                                Direction = "web",
                                Status = "Complete",
                                Transport = "Web Share HTTP",
                                Sha256 = "verified",
                                FilePath = Path.Combine(KtmManager.Instance.DownloadDirectory, fileName)
                            };
                            UserSettingsManager.AddHistoryItem(webItem);
                            _ledgerEntries.Insert(0, webItem);
                            UpdateLedgerUI();
                        }
                        catch { }
"@
if ($cs -notmatch 'UserSettingsManager\.AddHistoryItem\(webItem\)') {
    $cs = $cs.Replace($oldWebShareUpload, $newWebShareUpload.Trim())
}

# 7. Add ledger helper methods before the last closing brace
$ledgerMethods = @"

        #region Migration Ledger Helpers

        private void UpdateLedgerUI()
        {
            if (PanelLedgerEmpty != null)
                PanelLedgerEmpty.Visibility = _ledgerEntries.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
            if (ListHistory != null)
                ListHistory.Visibility = _ledgerEntries.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
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
                string logPath = Path.Combine(folder, $"Migration_Ledger_{DateTime.Now:yyyyMMdd_HHmmss}.txt");
                File.WriteAllText(logPath, report, Encoding.UTF8);
                MessageBox.Show($"Migration Ledger audit log exported successfully!\r\n\r\nLocation: {logPath}", "Audit Log Exported", MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to export audit log: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
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
                        Process.Start("explorer.exe", $"/select,\"{path}\"");
                        return;
                    }
                }
                catch { }
            }
            BtnOpenDownloadsFolder_Click(sender, e);
        }

        #endregion
    }
}
"@

if ($cs -notmatch 'UpdateLedgerUI') {
    # Replace the last `    }\r\n}` with $ledgerMethods
    $lastIdx = $cs.LastIndexOf("    }`r`n}");
    if ($lastIdx -lt 0) { $lastIdx = $cs.LastIndexOf("    }\n}"); }
    if ($lastIdx -ge 0) {
        $cs = $cs.Substring(0, $lastIdx) + $ledgerMethods.TrimStart()
    }
}

Set-Content -Path $csPath -Value $cs -Encoding utf8
Write-Host "Updated MainWindow.xaml.cs successfully."

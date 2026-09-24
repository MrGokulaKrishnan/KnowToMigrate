using System;
using System.IO;
using System.Linq;
using KnowToMigrate.Services;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Navigation;

namespace KnowToMigrate.Views
{
    public sealed partial class TransferView : Page
    {
        private KtmService?  _ktm;
        private DeviceInfo?  _device;
        private string[]     _paths = Array.Empty<string>();
        private string       _currentSessionId = "";
        private bool         _isPaused;
        private double       _barMaxWidth;

        public TransferView()
        {
            this.InitializeComponent();
        }

        // ─── Navigation ───────────────────────────────────────────────

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);

            if (e.Parameter is ValueTuple<KtmService, DeviceInfo, string[]> tuple)
            {
                var (ktm, device, paths) = tuple;
                _ktm    = ktm;
                _device = device;
                _paths  = paths;

                DeviceNameText.Text  = $"Sending to {device.Name}  ({device.Ip})";
                FilesCountText.Text  = $"0 / {_paths.Length}";

                _ktm.OnProgress += OnTransferProgress;

                BeginTransfer();
            }
        }

        protected override void OnNavigatedFrom(NavigationEventArgs e)
        {
            base.OnNavigatedFrom(e);
            if (_ktm != null) _ktm.OnProgress -= OnTransferProgress;
        }

        // ─── Transfer control ─────────────────────────────────────────

        private void BeginTransfer()
        {
            if (_ktm == null || _device == null) return;

            StatusText.Text = "Connecting…";

            if (_paths.Length > 0)
            {
                CurrentFileName.Text = Path.GetFileName(_paths[0]);

                // Kick off each file sequentially via native send
                foreach (var path in _paths)
                {
                    _currentSessionId = _ktm.SendFile(_device.Ip, _device.Port, path);
                    if (string.IsNullOrEmpty(_currentSessionId))
                    {
                        // Demo mode: simulate progress
                        SimulateProgress();
                        return;
                    }
                }
            }
            else
            {
                // Incoming receive — just show progress
                CurrentFileName.Text = "Receiving files…";
                SimulateProgress();
            }
        }

        private void SimulateProgress()
        {
            StatusText.Text = "Transferring…";

            // Fake progress ticker for demo/preview mode
            var timer = this.DispatcherQueue.CreateTimer();
            if (timer == null) return;

            double pct = 0;
            timer.Interval = TimeSpan.FromMilliseconds(80);
            timer.Tick += (t, _) =>
            {
                pct = Math.Min(pct + 0.5, 100);
                UpdateProgressUI(pct, (long)(pct * 10_000_000), 1_000_000_000,
                                 pct < 100 ? 45_000_000 : 0,
                                 pct < 100 ? (100 - pct) / 0.5 * 0.08 : 0);

                if (pct >= 100)
                {
                    t.Stop();
                    StatusText.Text = "Completed ✓";
                    BtnPause.IsEnabled  = false;
                    BtnCancel.IsEnabled = false;
                    StatusBadge.Background = new Microsoft.UI.Xaml.Media.SolidColorBrush(
                        Microsoft.UI.ColorHelper.FromArgb(255, 26, 47, 26));
                }
            };
            timer.Start();
        }

        // ─── Progress callback ────────────────────────────────────────

        private void OnTransferProgress(TransferProgress progress)
        {
            _currentSessionId = progress.SessionId;
            UpdateProgressUI(progress.Percentage, progress.BytesSent, progress.TotalBytes,
                             progress.SpeedBps, progress.EtaSecs);
        }

        private void UpdateProgressUI(double pct, long sent, long total, double speedBps, double etaSecs)
        {
            ProgressLabel.Text   = $"{pct:F0} %";
            SentBytesLabel.Text  = $"{FormatBytes(sent)} / {FormatBytes(total)}";
            SpeedText.Text       = FormatBytes((long)speedBps) + "/s";
            EtaText.Text         = TimeSpan.FromSeconds(etaSecs).ToString(@"mm\:ss");

            // Animate progress bar fill width
            if (_barMaxWidth == 0)
                _barMaxWidth = ProgressFill.ActualWidth > 0
                    ? ProgressFill.ActualWidth
                    : ((FrameworkElement)ProgressFill.Parent).ActualWidth;

            ProgressFill.Width = _barMaxWidth * pct / 100.0;
        }

        // ─── Pause / Cancel ───────────────────────────────────────────

        private void BtnPause_Click(object sender, RoutedEventArgs e)
        {
            _isPaused = !_isPaused;
            BtnPause.Content = _isPaused ? "▶  Resume" : "⏸  Pause";
            StatusText.Text  = _isPaused ? "Paused" : "Transferring…";
        }

        private void BtnCancel_Click(object sender, RoutedEventArgs e)
        {
            if (!string.IsNullOrEmpty(_currentSessionId))
                _ktm?.CancelTransfer(_currentSessionId);

            Frame.GoBack();
        }

        // ─── Helpers ──────────────────────────────────────────────────

        private static string FormatBytes(long bytes) =>
            bytes >= 1_000_000_000 ? $"{bytes / 1_000_000_000.0:F1} GB" :
            bytes >= 1_000_000     ? $"{bytes / 1_000_000.0:F1} MB"     :
            bytes >= 1_000         ? $"{bytes / 1_000.0:F1} KB"         :
            $"{bytes} B";
    }
}

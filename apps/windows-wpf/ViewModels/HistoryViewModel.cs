using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO;
using System.Runtime.CompilerServices;
using System.Text;
using System.Windows;
using System.Windows.Data;
using System.Windows.Input;
using KnowToMigrate.Services;

namespace KnowToMigrate.ViewModels
{
    public class RelayCommand : ICommand
    {
        private readonly Action _execute;
        private readonly Func<bool>? _canExecute;

        public RelayCommand(Action execute, Func<bool>? canExecute = null)
        {
            _execute = execute ?? throw new ArgumentNullException(nameof(execute));
            _canExecute = canExecute;
        }

        public bool CanExecute(object? parameter) => _canExecute?.Invoke() ?? true;
        public void Execute(object? parameter) => _execute();

        public event EventHandler? CanExecuteChanged
        {
            add => CommandManager.RequerySuggested += value;
            remove => CommandManager.RequerySuggested -= value;
        }
    }

    public class HistoryViewModel : INotifyPropertyChanged
    {
        private readonly ObservableCollection<HistoricalTransferItem> _entries = new();
        private ICollectionView? _historyView;
        private string _searchFilter = string.Empty;
        private TransferDirection? _directionFilter = null;
        private bool _hasEntries;
        private bool _isEmpty = true;

        public ObservableCollection<HistoricalTransferItem> Entries => _entries;

        public ICollectionView? HistoryView
        {
            get => _historyView;
            private set
            {
                _historyView = value;
                OnPropertyChanged();
            }
        }

        public string SearchFilter
        {
            get => _searchFilter;
            set
            {
                if (_searchFilter != value)
                {
                    _searchFilter = value;
                    OnPropertyChanged();
                    _historyView?.Refresh();
                    UpdateEmptyState();
                }
            }
        }

        public TransferDirection? DirectionFilter
        {
            get => _directionFilter;
            set
            {
                if (_directionFilter != value)
                {
                    _directionFilter = value;
                    OnPropertyChanged();
                    _historyView?.Refresh();
                    UpdateEmptyState();
                }
            }
        }

        public bool HasEntries
        {
            get => _hasEntries;
            private set
            {
                if (_hasEntries != value)
                {
                    _hasEntries = value;
                    OnPropertyChanged();
                }
            }
        }

        public bool IsEmpty
        {
            get => _isEmpty;
            private set
            {
                if (_isEmpty != value)
                {
                    _isEmpty = value;
                    OnPropertyChanged();
                }
            }
        }

        public ICommand ClearHistoryCommand { get; }
        public ICommand ExportHistoryCommand { get; }
        public ICommand RefreshCommand { get; }

        public event Action<string, string, bool>? NotificationRequested;

        public HistoryViewModel()
        {
            ClearHistoryCommand = new RelayCommand(ClearHistory);
            ExportHistoryCommand = new RelayCommand(ExportHistory);
            RefreshCommand = new RelayCommand(Load);

            Load();
        }

        public void Load()
        {
            var history = UserSettingsManager.LoadHistory();
            _entries.Clear();
            foreach (var item in history)
            {
                _entries.Add(item);
            }

            var view = CollectionViewSource.GetDefaultView(_entries);
            if (view != null && view.GroupDescriptions.Count == 0)
            {
                view.GroupDescriptions.Add(new PropertyGroupDescription("DayGroup"));
            }

            if (view != null)
            {
                view.Filter = FilterItem;
            }
            HistoryView = view;
            UpdateEmptyState();
        }

        private bool FilterItem(object obj)
        {
            if (obj is not HistoricalTransferItem item) return false;

            if (_directionFilter.HasValue && item.Direction != _directionFilter.Value)
                return false;

            if (!string.IsNullOrWhiteSpace(_searchFilter))
            {
                string query = _searchFilter.Trim();
                bool matchesFile = item.FileName != null && item.FileName.IndexOf(query, StringComparison.OrdinalIgnoreCase) >= 0;
                bool matchesDevice = item.DeviceName != null && item.DeviceName.IndexOf(query, StringComparison.OrdinalIgnoreCase) >= 0;
                if (!matchesFile && !matchesDevice) return false;
            }

            return true;
        }

        public void AddItem(HistoricalTransferItem item)
        {
            _entries.Insert(0, item);
            UpdateEmptyState();
        }

        public void ClearHistory()
        {
            if (MessageBox.Show("Are you sure you want to clear your transfer history?", "Clear History", MessageBoxButton.YesNo, MessageBoxImage.Question) == MessageBoxResult.Yes)
            {
                UserSettingsManager.ClearHistory();
                _entries.Clear();
                UpdateEmptyState();
                NotificationRequested?.Invoke("History Cleared", "Transfer history records have been cleared.", true);
            }
        }

        public void ExportHistory()
        {
            try
            {
                string report = UserSettingsManager.ExportHistoryText();
                string folder = KtmManager.Instance.DownloadDirectory;
                if (!Directory.Exists(folder)) Directory.CreateDirectory(folder);

                string logPath = Path.Combine(folder, "Transfer_History_" + DateTime.Now.ToString("yyyyMMdd_HHmmss") + ".txt");
                File.WriteAllText(logPath, report, Encoding.UTF8);

                MessageBox.Show("Transfer history exported successfully!\r\n\r\nLocation: " + logPath, "History Exported", MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (Exception ex)
            {
                MessageBox.Show("Failed to export audit log: " + ex.Message, "Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        public void UpdateEmptyState()
        {
            HasEntries = _entries.Count > 0;
            IsEmpty = _entries.Count == 0;
        }

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? propertyName = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }
    }
}

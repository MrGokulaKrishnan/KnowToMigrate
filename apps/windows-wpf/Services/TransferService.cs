using System;
using System.Collections.Generic;
using System.Linq;

namespace KnowToMigrate.Services
{
    /// <summary>
    /// Single writer service for transfer history.
    /// Deduplicates transfer records by session ID so that exactly one history row is written per transfer session.
    /// </summary>
    public class TransferService
    {
        private static readonly TransferService _instance = new();
        public static TransferService Instance => _instance;

        private readonly HashSet<string> _recordedSessionIds = new(StringComparer.OrdinalIgnoreCase);
        private readonly object _lock = new();

        /// <summary>
        /// Records a transfer item into history, deduplicating by SessionId.
        /// Returns true if recorded, false if ignored as duplicate.
        /// </summary>
        public bool Record(HistoricalTransferItem record)
        {
            if (record == null) return false;

            lock (_lock)
            {
                if (!string.IsNullOrEmpty(record.SessionId))
                {
                    if (_recordedSessionIds.Contains(record.SessionId))
                    {
                        return false;
                    }

                    // Check if already in persistent history
                    var existing = UserSettingsManager.LoadHistory();
                    if (existing.Any(e => !string.IsNullOrEmpty(e.SessionId) && string.Equals(e.SessionId, record.SessionId, StringComparison.OrdinalIgnoreCase)))
                    {
                        _recordedSessionIds.Add(record.SessionId);
                        return false;
                    }

                    _recordedSessionIds.Add(record.SessionId);
                }

                UserSettingsManager.AddHistoryItem(record);
                return true;
            }
        }

        public void ClearSessionCache()
        {
            lock (_lock)
            {
                _recordedSessionIds.Clear();
            }
        }
    }
}

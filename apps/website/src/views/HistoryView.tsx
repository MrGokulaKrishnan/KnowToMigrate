import React, { useState } from 'react';
import {
  ShieldCheck, ArrowUpRight, ArrowDownLeft, Folder, Search,
  Trash2, Filter, CheckCircle2, History as HistoryIcon
} from 'lucide-react';
import { useTransfer } from '../context/TransferContext';
import { useToast } from '../components/Toast';

export const HistoryView: React.FC = () => {
  const { history, clearHistory, settings } = useTransfer();
  const toast = useToast();
  const [search, setSearch] = useState('');
  const [directionFilter, setDirectionFilter] = useState<'all' | 'send' | 'receive'>('all');

  const filtered = history.filter((item) => {
    const matchesSearch =
      item.title.toLowerCase().includes(search.toLowerCase()) ||
      item.device.toLowerCase().includes(search.toLowerCase());
    const matchesDirection = directionFilter === 'all' || item.direction === directionFilter;
    return matchesSearch && matchesDirection;
  });

  const handleClear = () => {
    clearHistory();
    toast.info('History Cleared', 'All local transfer audit records have been removed.');
  };

  const handleOpenFolder = (title: string) => {
    toast.info('Inbound Folder', `Storage location: ${settings.saveDirectory}\\${title}`);
  };

  return (
    <div className="glass-panel p-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-white/10">
        <div>
          <h2 className="text-2xl font-black text-white">Transfer History &amp; Cryptographic Audit</h2>
          <p className="text-sm text-gray-400">
            Records of verified peer-to-peer transfers. No file contents are stored in history logs.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Direction filters */}
          <div className="flex bg-white/5 rounded-xl p-0.5 border border-white/10 text-xs">
            {(['all', 'send', 'receive'] as const).map((dir) => (
              <button
                key={dir}
                type="button"
                onClick={() => setDirectionFilter(dir)}
                className={`px-3 py-1 rounded-lg capitalize transition ${
                  directionFilter === dir
                    ? 'bg-[#FF5A00] text-white font-bold'
                    : 'text-gray-400 hover:text-white'
                }`}
              >
                {dir}
              </button>
            ))}
          </div>

          <div className="relative">
            <Search className="w-4 h-4 text-gray-500 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search transfers..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="bg-white/5 border border-white/10 rounded-xl pl-9 pr-3.5 py-1.5 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-[#FF5A00]"
            />
          </div>

          {history.length > 0 && (
            <button
              type="button"
              onClick={handleClear}
              className="km-glass-btn px-3 py-1.5 text-xs text-gray-400 hover:text-red-400 hover:border-red-500/30 gap-1"
            >
              <Trash2 className="w-3.5 h-3.5" /> Clear History
            </button>
          )}
        </div>
      </div>

      {/* History Items List */}
      <div className="space-y-3">
        {filtered.map((item) => (
          <div
            key={item.id}
            className="glass-card p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 group"
          >
            <div className="flex items-center gap-3.5 min-w-0">
              <div
                className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                  item.direction === 'send'
                    ? 'bg-[#FF5A00]/15 border border-[#FF5A00]/30 text-[#FF5A00]'
                    : 'bg-[#22C55E]/15 border border-[#22C55E]/30 text-[#22C55E]'
                }`}
              >
                {item.direction === 'send' ? (
                  <ArrowUpRight className="w-5 h-5" />
                ) : (
                  <ArrowDownLeft className="w-5 h-5" />
                )}
              </div>
              <div className="min-w-0">
                <h4 className="font-bold text-white text-sm truncate max-w-xs sm:max-w-md">
                  {item.title}
                </h4>
                <p className="text-xs text-gray-400">
                  {item.direction === 'send' ? 'Sent to' : 'Received from'}{' '}
                  <strong className="text-gray-200">{item.device}</strong> · {item.dateFormatted} ·{' '}
                  {item.speed} ({item.transport})
                </p>
                {item.merkleRootHash && (
                  <p className="text-[10px] text-gray-500 font-mono mt-0.5 truncate max-w-sm">
                    Merkle: {item.merkleRootHash.slice(0, 24)}…
                  </p>
                )}
              </div>
            </div>

            <div className="flex items-center justify-between sm:justify-end gap-4 shrink-0">
              <div className="text-right">
                <span className="font-mono text-sm font-bold text-white">{item.sizeFormatted}</span>
                <p className="text-[11px] text-[#22C55E] flex items-center justify-end gap-1">
                  <ShieldCheck className="w-3.5 h-3.5" /> SHA-256 Verified
                </p>
              </div>
              <div className="flex gap-1.5">
                <button
                  type="button"
                  onClick={() => handleOpenFolder(item.title)}
                  className="km-glass-btn px-3 py-1.5 text-xs gap-1 hover:border-[#FF5A00]"
                  title="Locate folder"
                >
                  <Folder className="w-3.5 h-3.5" /> Open
                </button>
              </div>
            </div>
          </div>
        ))}

        {filtered.length === 0 && (
          <div className="text-center py-16 text-gray-500 text-sm space-y-3">
            <HistoryIcon className="w-10 h-10 mx-auto text-gray-600" />
            <p>No transfer records found matching your filters.</p>
          </div>
        )}
      </div>
    </div>
  );
};

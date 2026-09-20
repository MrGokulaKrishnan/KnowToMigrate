import React, { useState } from 'react';
import { ShieldCheck, ArrowUpRight, ArrowDownLeft, Folder, Search, Trash2, RotateCcw } from 'lucide-react';

export const HistoryView: React.FC = () => {
  const [search, setSearch] = useState('');
  const [historyItems, setHistoryItems] = useState([
    {
      id: '1',
      title: 'Design_System_Figma_Assets.zip',
      device: "Krish's Laptop",
      direction: 'send',
      size: '4.20 GB',
      date: 'Today, 18:42',
      speed: '86.4 MB/s',
      transport: 'Direct LAN',
      verified: true
    },
    {
      id: '2',
      title: 'GoPro_Vacation_Footage/ (84 items)',
      device: 'Studio Tablet',
      direction: 'receive',
      size: '32.80 GB',
      date: 'Yesterday, 14:10',
      speed: '91.2 MB/s',
      transport: 'Direct Wi-Fi 6',
      verified: true
    },
    {
      id: '3',
      title: 'Full_Phone_Migration_Backup (Photos, Videos, Docs)',
      device: "Krish's Phone",
      direction: 'receive',
      size: '94.20 GB',
      date: '15 Sep 2026',
      speed: '88.5 MB/s',
      transport: 'Direct Wi-Fi 6',
      verified: true
    }
  ]);

  const filtered = historyItems.filter((i) =>
    i.title.toLowerCase().includes(search.toLowerCase()) ||
    i.device.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="glass-panel p-8 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-white/10">
        <div>
          <h2 className="text-2xl font-black text-white">Transfer History &amp; Cryptographic Audit</h2>
          <p className="text-sm text-gray-400">
            Records of verified peer-to-peer transfers. No file contents are stored in history logs.
          </p>
        </div>
        <div className="flex items-center gap-2">
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
          <button
            onClick={() => setHistoryItems([])}
            className="km-glass-btn px-3 py-1.5 text-xs text-gray-400 hover:text-white gap-1"
          >
            <Trash2 className="w-3.5 h-3.5" /> Clear
          </button>
        </div>
      </div>

      <div className="space-y-3">
        {filtered.map((item) => (
          <div key={item.id} className="glass-card p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3.5">
              <div
                className={`w-10 h-10 rounded-xl flex items-center justify-center ${
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
              <div>
                <h4 className="font-bold text-white text-sm">{item.title}</h4>
                <p className="text-xs text-gray-400">
                  {item.direction === 'send' ? 'Sent to' : 'Received from'}{' '}
                  <strong className="text-gray-200">{item.device}</strong> · {item.date} · {item.speed} ({item.transport})
                </p>
              </div>
            </div>

            <div className="flex items-center justify-between sm:justify-end gap-4">
              <div className="text-right">
                <span className="font-mono text-sm font-bold text-white">{item.size}</span>
                <p className="text-[11px] text-[#22C55E] flex items-center justify-end gap-1">
                  <ShieldCheck className="w-3.5 h-3.5" /> SHA-256 Verified
                </p>
              </div>
              <div className="flex gap-1.5">
                <button
                  onClick={() => alert(`Opening save directory for: ${item.title}`)}
                  className="km-glass-btn px-3 py-1.5 text-xs gap-1 hover:border-[#FF5A00]"
                >
                  <Folder className="w-3.5 h-3.5" /> Open
                </button>
              </div>
            </div>
          </div>
        ))}

        {filtered.length === 0 && (
          <div className="text-center py-12 text-gray-500 text-sm">
            No transfer records found.
          </div>
        )}
      </div>
    </div>
  );
};

import React from 'react';
import { Upload, FolderUp, Smartphone, Laptop, Tablet, QrCode, RefreshCw, ShieldCheck, HardDrive, Wifi, Zap } from 'lucide-react';

interface HomeViewProps {
  onSelectDevice: (device: { name: string; platform: string; type: string }) => void;
  onOpenMigration: () => void;
  onOpenWebReceiver: () => void;
}

export const HomeView: React.FC<HomeViewProps> = ({ onSelectDevice, onOpenMigration, onOpenWebReceiver }) => {
  return (
    <div className="space-y-6">
      {/* Top Hero & System Health Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Dropzone (8 cols) */}
        <div className="lg:col-span-8 glass-panel p-8 relative overflow-hidden flex flex-col items-center justify-center text-center border-dashed border-2 border-[#FF5A00]/40 hover:border-[#FF5A00] transition group cursor-pointer">
          <div className="absolute inset-0 bg-gradient-to-b from-[#FF5A00]/5 to-transparent pointer-events-none"></div>

          <div className="w-20 h-20 rounded-2xl bg-[#FF5A00]/10 border border-[#FF5A00]/30 flex items-center justify-center mb-4 group-hover:scale-110 transition duration-300 shadow-[0_0_30px_rgba(255,90,0,0.2)]">
            <Upload className="w-10 h-10 text-[#FF5A00]" />
          </div>

          <h2 className="text-2xl font-bold text-white mb-1">Drag and Drop Files or Folders Here</h2>
          <p className="text-sm text-gray-400 max-w-md mb-6">
            Preserves recursive folder structures, original timestamps, and uncompressed 4K/8K quality. No arbitrary size limits.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-3">
            <button
              onClick={() => onSelectDevice({ name: "Krish's Phone", platform: "Android", type: "phone" })}
              className="km-glossy-btn px-6 py-2.5 text-sm gap-2"
            >
              <Upload className="w-4 h-4" /> Select Files
            </button>
            <button
              onClick={() => onSelectDevice({ name: "Krish's Phone", platform: "Android", type: "phone" })}
              className="km-glass-btn px-5 py-2.5 text-sm gap-2"
            >
              <FolderUp className="w-4 h-4 text-[#FF8A00]" /> Select Entire Folder
            </button>
            <button
              onClick={onOpenMigration}
              className="km-glass-btn px-5 py-2.5 text-sm text-[#FF8A00] border-[#FF5A00]/30 hover:border-[#FF5A00] gap-2"
            >
              <Zap className="w-4 h-4 text-[#FF5A00]" /> Full Device Migration
            </button>
          </div>
        </div>

        {/* Local Transfer Health Card (4 cols) */}
        <div className="lg:col-span-4 glass-panel p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-sm text-gray-300">Local Transfer Engine</h3>
              <span className="text-xs px-2 py-0.5 rounded-full bg-[#22C55E]/15 text-[#22C55E] border border-[#22C55E]/30 font-medium">
                P2P Ready
              </span>
            </div>

            <div className="space-y-3.5 text-sm">
              <div className="flex justify-between items-center py-2 border-b border-white/5">
                <span className="text-gray-400">Current Device</span>
                <span className="font-medium text-white flex items-center gap-1.5">
                  <Laptop className="w-4 h-4 text-blue-400" /> My Windows PC
                </span>
              </div>
              <div className="flex justify-between items-center py-2 border-b border-white/5">
                <span className="text-gray-400">Network Transport</span>
                <span className="font-medium text-white flex items-center gap-1.5">
                  <Wifi className="w-4 h-4 text-emerald-400" /> Direct Wi-Fi 6 (1.2 Gbps)
                </span>
              </div>
              <div className="flex justify-between items-center py-2 border-b border-white/5">
                <span className="text-gray-400">Encryption</span>
                <span className="font-medium text-[#22C55E] flex items-center gap-1">
                  <ShieldCheck className="w-4 h-4 text-[#22C55E]" /> TLS 1.3 + AES-GCM
                </span>
              </div>
              <div className="flex justify-between items-center py-2">
                <span className="text-gray-400">Available Storage</span>
                <span className="font-medium text-white flex items-center gap-1.5">
                  <HardDrive className="w-4 h-4 text-purple-400" /> 482.4 GB Free (NVMe)
                </span>
              </div>
            </div>
          </div>

          <div className="p-3.5 rounded-xl bg-white/5 border border-white/10 mt-4 flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-[#FF5A00]/20 flex items-center justify-center text-[#FF5A00]">
              <QrCode className="w-5 h-5" />
            </div>
            <div className="text-xs">
              <p className="font-semibold text-white">Zero-Install Web Receiver</p>
              <p className="text-gray-400">Drop files to any browser instantly</p>
            </div>
            <button
              onClick={onOpenWebReceiver}
              className="ml-auto text-xs px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 text-[#FF8A00] font-medium transition"
            >
              QR Code
            </button>
          </div>
        </div>
      </div>

      {/* Nearby Devices Section */}
      <div className="glass-panel p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2.5">
            <div className="w-3 h-3 rounded-full bg-[#FF5A00] shadow-[0_0_12px_#FF5A00]"></div>
            <h3 className="text-lg font-bold text-white">Nearby Devices Available for Transfer</h3>
            <span className="text-xs text-gray-400">(4 discovered via BLE &amp; mDNS)</span>
          </div>
          <button className="km-glass-btn px-3 py-1.5 text-xs gap-1.5 hover:border-[#FF5A00]/50">
            <RefreshCw className="w-3.5 h-3.5 text-[#FF5A00] animate-spin" />
            Rescan Radar
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          
          {/* Device 1 */}
          <div
            onClick={() => onSelectDevice({ name: "Krish's Phone", platform: "Android", type: "phone" })}
            className="glass-card p-5 cursor-pointer relative group"
          >
            <div className="flex items-start justify-between mb-3">
              <div className="w-12 h-12 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 group-hover:scale-105 transition">
                <Smartphone className="w-6 h-6" />
              </div>
              <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-[#22C55E]/15 text-[#22C55E] border border-[#22C55E]/30 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-[#22C55E]"></span> Trusted
              </span>
            </div>
            <h4 className="font-bold text-white text-base group-hover:text-[#FF8A00] transition">Krish's Phone</h4>
            <p className="text-xs text-gray-400">Google Pixel 8 Pro · Android 15</p>
            <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between text-xs">
              <span className="text-emerald-400 font-medium">Direct P2P Ready</span>
              <button className="km-glossy-btn px-3 py-1 text-xs">Send</button>
            </div>
          </div>

          {/* Device 2 */}
          <div
            onClick={() => onSelectDevice({ name: "Gokul's Laptop", platform: "macOS", type: "laptop" })}
            className="glass-card p-5 cursor-pointer relative group"
          >
            <div className="flex items-start justify-between mb-3">
              <div className="w-12 h-12 rounded-xl bg-blue-500/20 border border-blue-500/30 flex items-center justify-center text-blue-400 group-hover:scale-105 transition">
                <Laptop className="w-6 h-6" />
              </div>
              <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-blue-500/15 text-blue-400 border border-blue-500/30">
                Local LAN
              </span>
            </div>
            <h4 className="font-bold text-white text-base group-hover:text-[#FF8A00] transition">Gokul's Laptop</h4>
            <p className="text-xs text-gray-400">MacBook Pro M3 · macOS</p>
            <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between text-xs">
              <span className="text-gray-400">Signal: 98% (Wi-Fi 6)</span>
              <button className="km-glass-btn px-3 py-1 text-xs hover:border-[#FF5A00]">Send</button>
            </div>
          </div>

          {/* Device 3 */}
          <div
            onClick={() => onSelectDevice({ name: "Studio Tablet", platform: "Android", type: "tablet" })}
            className="glass-card p-5 cursor-pointer relative group"
          >
            <div className="flex items-start justify-between mb-3">
              <div className="w-12 h-12 rounded-xl bg-purple-500/20 border border-purple-500/30 flex items-center justify-center text-purple-400 group-hover:scale-105 transition">
                <Tablet className="w-6 h-6" />
              </div>
              <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-[#22C55E]/15 text-[#22C55E] border border-[#22C55E]/30 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-[#22C55E]"></span> Trusted
              </span>
            </div>
            <h4 className="font-bold text-white text-base group-hover:text-[#FF8A00] transition">Studio Tablet</h4>
            <p className="text-xs text-gray-400">Samsung Galaxy Tab S9</p>
            <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between text-xs">
              <span className="text-emerald-400 font-medium">Auto-Accept ON</span>
              <button className="km-glossy-btn px-3 py-1 text-xs">Send</button>
            </div>
          </div>

          {/* Device 4 (QR Web Receiver) */}
          <div
            onClick={onOpenWebReceiver}
            className="glass-card p-5 cursor-pointer relative group border-dashed border-[#FF5A00]/30 hover:border-[#FF5A00]"
          >
            <div className="flex items-start justify-between mb-3">
              <div className="w-12 h-12 rounded-xl bg-[#FF5A00]/20 border border-[#FF5A00]/30 flex items-center justify-center text-[#FF5A00] group-hover:scale-105 transition">
                <QrCode className="w-6 h-6" />
              </div>
              <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-[#FF5A00]/15 text-[#FF8A00] border border-[#FF5A00]/30">
                Zero-Install
              </span>
            </div>
            <h4 className="font-bold text-white text-base group-hover:text-[#FF8A00] transition">Instant QR Drop</h4>
            <p className="text-xs text-gray-400">Send to any browser receiver</p>
            <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between text-xs">
              <span className="text-[#FF8A00]">Generate QR</span>
              <button className="km-glass-btn px-3 py-1 text-xs">Scan</button>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};

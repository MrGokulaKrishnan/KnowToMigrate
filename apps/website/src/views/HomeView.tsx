import React, { useRef, useState } from 'react';
import {
  Upload, FolderUp, Smartphone, Laptop, Tablet, QrCode,
  RefreshCw, ShieldCheck, HardDrive, Wifi, Zap, CheckCircle2
} from 'lucide-react';
import { useTransfer } from '../context/TransferContext';
import { useToast } from '../components/Toast';

interface HomeViewProps {
  onSelectDevice: (device: { name: string; platform: string; type: string }) => void;
  onOpenMigration: () => void;
  onOpenWebReceiver: () => void;
}

export const HomeView: React.FC<HomeViewProps> = ({
  onSelectDevice,
  onOpenMigration,
  onOpenWebReceiver,
}) => {
  const { addFiles, setTargetDevice, settings } = useTransfer();
  const toast = useToast();
  const [isDragging, setIsDragging] = useState(false);
  const [isScanning, setIsScanning] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const folderInputRef = useRef<HTMLInputElement>(null);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      addFiles(e.dataTransfer.files);
      toast.success(
        'Files Added',
        `${e.dataTransfer.files.length} item(s) staged with SHA-256 chunk validation.`
      );
      onSelectDevice({ name: "Krish's Phone", platform: 'Android', type: 'phone' });
    }
  };

  const handleFilesSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      addFiles(e.target.files);
      toast.success(
        'Files Selected',
        `${e.target.files.length} item(s) added to transfer queue.`
      );
      onSelectDevice({ name: "Krish's Phone", platform: 'Android', type: 'phone' });
    }
  };

  const handleRescanRadar = () => {
    setIsScanning(true);
    setTimeout(() => {
      setIsScanning(false);
      toast.info('Radar Refreshed', 'Found 3 active P2P devices on local subnet.');
    }, 1200);
  };

  const handlePickDevice = (dev: { name: string; platform: string; type: string }) => {
    setTargetDevice(dev.name);
    onSelectDevice(dev);
  };

  return (
    <div className="space-y-6">
      {/* Hidden inputs for real file/folder browsing */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFilesSelected}
        multiple
        className="hidden"
        aria-hidden="true"
      />
      <input
        type="file"
        ref={folderInputRef}
        onChange={handleFilesSelected}
        multiple
        {...({ webkitdirectory: '' } as any)}
        className="hidden"
        aria-hidden="true"
      />

      {/* Top Hero & System Health Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Dropzone (8 cols) */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          role="region"
          aria-label="File dropzone"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') fileInputRef.current?.click();
          }}
          className={`lg:col-span-8 glass-panel p-8 relative overflow-hidden flex flex-col items-center justify-center text-center border-dashed border-2 transition cursor-pointer group focus:outline-none focus:ring-2 focus:ring-[#FF5A00] ${
            isDragging
              ? 'border-[#FF5A00] bg-[#FF5A00]/15 scale-[1.01]'
              : 'border-[#FF5A00]/40 hover:border-[#FF5A00]'
          }`}
        >
          <div className="absolute inset-0 bg-gradient-to-b from-[#FF5A00]/5 to-transparent pointer-events-none"></div>

          <div className="w-20 h-20 rounded-2xl bg-[#FF5A00]/10 border border-[#FF5A00]/30 flex items-center justify-center mb-4 group-hover:scale-110 transition duration-300 shadow-[0_0_30px_rgba(255,90,0,0.2)]">
            <Upload className="w-10 h-10 text-[#FF5A00]" />
          </div>

          <h2 className="text-2xl font-bold text-white mb-1">
            {isDragging ? 'Release to Stage Files' : 'Drag and Drop Files or Folders Here'}
          </h2>
          <p className="text-sm text-gray-400 max-w-md mb-6">
            Preserves recursive folder structures, original timestamps, and uncompressed 4K/8K quality. No arbitrary size limits.
          </p>

          <div
            className="flex flex-wrap items-center justify-center gap-3"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="km-glossy-btn px-6 py-2.5 text-sm gap-2"
            >
              <Upload className="w-4 h-4" /> Select Files
            </button>
            <button
              type="button"
              onClick={() => folderInputRef.current?.click()}
              className="km-glass-btn px-5 py-2.5 text-sm gap-2"
            >
              <FolderUp className="w-4 h-4 text-[#FF8A00]" /> Select Entire Folder
            </button>
            <button
              type="button"
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
              <span className="text-xs px-2 py-0.5 rounded-full bg-[#22C55E]/15 text-[#22C55E] border border-[#22C55E]/30 font-medium flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3 text-[#22C55E]" /> P2P Ready
              </span>
            </div>

            <div className="space-y-3.5 text-sm">
              <div className="flex justify-between items-center py-2 border-b border-white/5">
                <span className="text-gray-400">Current Device</span>
                <span className="font-medium text-white flex items-center gap-1.5">
                  <Laptop className="w-4 h-4 text-blue-400" /> {settings.deviceName}
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
              type="button"
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
            <span className="text-xs text-gray-400">(3 discovered via BLE &amp; mDNS)</span>
          </div>
          <button
            type="button"
            onClick={handleRescanRadar}
            className="km-glass-btn px-3 py-1.5 text-xs gap-1.5 hover:border-[#FF5A00]/50"
          >
            <RefreshCw
              className={`w-3.5 h-3.5 text-[#FF5A00] ${isScanning ? 'animate-spin' : ''}`}
            />
            {isScanning ? 'Scanning...' : 'Rescan Radar'}
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Device 1 */}
          <div
            onClick={() => handlePickDevice({ name: "Krish's Phone", platform: 'Android', type: 'phone' })}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handlePickDevice({ name: "Krish's Phone", platform: 'Android', type: 'phone' });
            }}
            className="glass-card p-5 cursor-pointer relative group focus:outline-none focus:ring-1 focus:ring-[#FF5A00]"
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
              <button type="button" className="km-glossy-btn px-3 py-1 text-xs">Send</button>
            </div>
          </div>

          {/* Device 2 */}
          <div
            onClick={() => handlePickDevice({ name: "Gokul's Laptop", platform: 'macOS', type: 'laptop' })}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handlePickDevice({ name: "Gokul's Laptop", platform: 'macOS', type: 'laptop' });
            }}
            className="glass-card p-5 cursor-pointer relative group focus:outline-none focus:ring-1 focus:ring-[#FF5A00]"
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
              <button type="button" className="km-glass-btn px-3 py-1 text-xs hover:border-[#FF5A00]">Send</button>
            </div>
          </div>

          {/* Device 3 */}
          <div
            onClick={() => handlePickDevice({ name: 'Studio Tablet', platform: 'Android', type: 'tablet' })}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handlePickDevice({ name: 'Studio Tablet', platform: 'Android', type: 'tablet' });
            }}
            className="glass-card p-5 cursor-pointer relative group focus:outline-none focus:ring-1 focus:ring-[#FF5A00]"
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
              <button type="button" className="km-glossy-btn px-3 py-1 text-xs">Send</button>
            </div>
          </div>

          {/* Device 4 (QR Web Receiver) */}
          <div
            onClick={onOpenWebReceiver}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter') onOpenWebReceiver();
            }}
            className="glass-card p-5 cursor-pointer relative group border-dashed border-[#FF5A00]/30 hover:border-[#FF5A00] focus:outline-none focus:ring-1 focus:ring-[#FF5A00]"
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
              <button type="button" className="km-glass-btn px-3 py-1 text-xs">Scan</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

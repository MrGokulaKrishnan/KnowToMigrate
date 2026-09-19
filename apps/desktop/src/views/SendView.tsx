import React, { useState } from 'react';
import { Upload, FolderUp, Smartphone, FileText, Film, Image as ImageIcon, Music, CheckCircle2, ShieldCheck, ArrowRight } from 'lucide-react';

interface SendViewProps {
  onStartTransfer: (device: string, files: any[]) => void;
}

export const SendView: React.FC<SendViewProps> = ({ onStartTransfer }) => {
  const [selectedDevice, setSelectedDevice] = useState("Krish's Phone");
  const [mockFiles, setMockFiles] = useState([
    { name: '4K_Drone_Cinematic_Footage.mp4', size: '2.84 GB', type: 'video' },
    { name: 'Production_Soundtrack_Masters.flac', size: '480 MB', type: 'audio' },
    { name: 'KnowToMigrate_Brand_Guidelines.pdf', size: '42 MB', type: 'doc' },
    { name: 'RAW_Camera_Shoot_Asset_Folder/', size: '8.40 GB', type: 'folder' }
  ]);

  return (
    <div className="glass-panel p-8 space-y-6">
      <div className="border-b border-white/10 pb-4">
        <h2 className="text-2xl font-black text-white">Send Content to Nearby Device</h2>
        <p className="text-sm text-gray-400">
          Streaming directly over local Wi-Fi 6 with chunked backpressure and SHA-256 integrity verification.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* File Selection & Manifest List (7 cols) */}
        <div className="lg:col-span-7 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-gray-300">Selected Items ({mockFiles.length})</h3>
            <div className="flex gap-2">
              <button className="km-glass-btn px-3 py-1 text-xs gap-1.5">
                <Upload className="w-3.5 h-3.5" /> Add Files
              </button>
              <button className="km-glass-btn px-3 py-1 text-xs gap-1.5">
                <FolderUp className="w-3.5 h-3.5 text-[#FF8A00]" /> Add Folder
              </button>
            </div>
          </div>

          <div className="space-y-2.5 max-h-80 overflow-y-auto pr-1">
            {mockFiles.map((file, idx) => (
              <div key={idx} className="glass-card p-3.5 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-[#FF5A00]/15 flex items-center justify-center text-[#FF5A00]">
                    {file.type === 'video' && <Film className="w-4 h-4" />}
                    {file.type === 'audio' && <Music className="w-4 h-4" />}
                    {file.type === 'doc' && <FileText className="w-4 h-4" />}
                    {file.type === 'folder' && <FolderUp className="w-4 h-4" />}
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-white truncate max-w-xs">{file.name}</p>
                    <p className="text-xs text-gray-400 font-mono">{file.size}</p>
                  </div>
                </div>
                <span className="text-xs text-[#22C55E] flex items-center gap-1 font-medium">
                  <CheckCircle2 className="w-3.5 h-3.5" /> Ready
                </span>
              </div>
            ))}
          </div>

          <div className="p-3.5 rounded-xl bg-white/5 border border-white/10 flex justify-between text-xs text-gray-300">
            <span>Total Payload Size: <strong className="text-white font-mono">11.76 GB</strong></span>
            <span>Total 8MB Chunks: <strong className="text-[#FF8A00] font-mono">1,505 Chunks</strong></span>
          </div>
        </div>

        {/* Target Recipient & Pre-flight Summary (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <h3 className="text-sm font-bold text-gray-300">Choose Target Recipient</h3>
          
          <div className="space-y-2">
            {["Krish's Phone", "Gokul's Laptop", "Studio Tablet"].map((dev) => (
              <div
                key={dev}
                onClick={() => setSelectedDevice(dev)}
                className={`p-3.5 rounded-xl border cursor-pointer transition flex items-center justify-between ${
                  selectedDevice === dev
                    ? 'border-[#FF5A00] bg-[#FF5A00]/10 shadow-[0_0_15px_rgba(255,90,0,0.2)]'
                    : 'border-white/10 bg-white/5 hover:border-white/20'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
                    <Smartphone className="w-4 h-4" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-white">{dev}</h4>
                    <p className="text-xs text-gray-400">Direct Wi-Fi 6 · TLS 1.3</p>
                  </div>
                </div>
                {selectedDevice === dev && (
                  <span className="w-2.5 h-2.5 rounded-full bg-[#FF5A00]"></span>
                )}
              </div>
            ))}
          </div>

          {/* Pre-flight Security & Storage Check */}
          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-xs text-emerald-300 space-y-2">
            <div className="flex items-center gap-1.5 font-bold">
              <ShieldCheck className="w-4 h-4 text-[#22C55E]" />
              Pre-flight Destination Storage &amp; Safety Check
            </div>
            <p className="text-gray-300">
              Target has <strong>112.4 GB</strong> free space available. Payload requires <strong>11.76 GB</strong>.
              Sufficient space confirmed.
            </p>
          </div>

          <button
            onClick={() => onStartTransfer(selectedDevice, mockFiles)}
            className="w-full km-glossy-btn py-3 text-sm gap-2"
          >
            Start Direct Transfer to {selectedDevice}
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>

      </div>
    </div>
  );
};

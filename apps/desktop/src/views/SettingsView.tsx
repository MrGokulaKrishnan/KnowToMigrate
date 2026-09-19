import React, { useState } from 'react';
import { Settings, Shield, Folder, Eye, Zap, Cpu, Bell, HardDrive } from 'lucide-react';

export const SettingsView: React.FC = () => {
  const [deviceName, setDeviceName] = useState("My Windows PC");
  const [saveDir, setSaveDir] = useState("C:\\Users\\Krish\\Downloads\\KnowToMigrate");
  const [autoAcceptTrusted, setAutoAcceptTrusted] = useState(true);
  const [encryptTransfers, setEncryptTransfers] = useState(true);
  const [chunkSizeMB, setChunkSizeMB] = useState(8);

  return (
    <div className="glass-panel p-8 space-y-8">
      <div className="border-b border-white/10 pb-4">
        <h2 className="text-2xl font-black text-white">KnowToMigrate Settings</h2>
        <p className="text-sm text-gray-400">
          Configure identity, discovery visibility, security controls, and transfer engine parameters.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
        
        {/* Section 1: Device Identity */}
        <div className="glass-card p-5 space-y-4">
          <div className="flex items-center gap-2 text-white font-bold">
            <Cpu className="w-4 h-4 text-[#FF5A00]" />
            <h3>Device Identity &amp; Naming</h3>
          </div>
          <div>
            <label className="text-xs text-gray-400 block mb-1.5">Public Device Display Name</label>
            <input
              type="text"
              value={deviceName}
              onChange={(e) => setDeviceName(e.target.value)}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2 text-white focus:outline-none focus:border-[#FF5A00]"
            />
          </div>
          <div className="text-xs text-gray-400">
            Device ID: <code className="text-gray-300">KTM-78AF-92C1-NODE22</code>
          </div>
        </div>

        {/* Section 2: Storage & Sandbox */}
        <div className="glass-card p-5 space-y-4">
          <div className="flex items-center gap-2 text-white font-bold">
            <Folder className="w-4 h-4 text-emerald-400" />
            <h3>Destination Directory &amp; Sandbox</h3>
          </div>
          <div>
            <label className="text-xs text-gray-400 block mb-1.5">Default Inbound Storage Path</label>
            <input
              type="text"
              value={saveDir}
              onChange={(e) => setSaveDir(e.target.value)}
              className="w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2 text-white font-mono text-xs focus:outline-none focus:border-[#FF5A00]"
            />
          </div>
          <p className="text-[11px] text-gray-400">
            PathGuard strictly prevents relative path traversal attacks outside this folder.
          </p>
        </div>

        {/* Section 3: Security & Cryptography */}
        <div className="glass-card p-5 space-y-4">
          <div className="flex items-center gap-2 text-white font-bold">
            <Shield className="w-4 h-4 text-blue-400" />
            <h3>Security &amp; Cryptography</h3>
          </div>
          <label className="flex items-center justify-between cursor-pointer">
            <div>
              <p className="font-semibold text-white">TLS 1.3 &amp; AES-256-GCM</p>
              <p className="text-xs text-gray-400">Enforce end-to-end authenticated encryption</p>
            </div>
            <input
              type="checkbox"
              checked={encryptTransfers}
              onChange={(e) => setEncryptTransfers(e.target.checked)}
              className="w-5 h-5 accent-[#FF5A00] rounded cursor-pointer"
            />
          </label>
          <label className="flex items-center justify-between cursor-pointer">
            <div>
              <p className="font-semibold text-white">Auto-Accept for Trusted Devices</p>
              <p className="text-xs text-gray-400">Bypass confirmation prompt for verified personal devices</p>
            </div>
            <input
              type="checkbox"
              checked={autoAcceptTrusted}
              onChange={(e) => setAutoAcceptTrusted(e.target.checked)}
              className="w-5 h-5 accent-[#FF5A00] rounded cursor-pointer"
            />
          </label>
        </div>

        {/* Section 4: Transfer Engine Parameters */}
        <div className="glass-card p-5 space-y-4">
          <div className="flex items-center gap-2 text-white font-bold">
            <Zap className="w-4 h-4 text-[#FF8A00]" />
            <h3>Engine Tuning</h3>
          </div>
          <div>
            <div className="flex justify-between text-xs mb-1.5">
              <span className="text-gray-400">Streaming Chunk Size</span>
              <span className="font-bold text-[#FF8A00] font-mono">{chunkSizeMB} MB</span>
            </div>
            <input
              type="range"
              min={1}
              max={32}
              step={1}
              value={chunkSizeMB}
              onChange={(e) => setChunkSizeMB(Number(e.target.value))}
              className="w-full accent-[#FF5A00] cursor-pointer"
            />
            <span className="text-[11px] text-gray-500 block mt-1">
              Recommended: 8 MB for 1 Gbps LAN with low CPU memory overhead.
            </span>
          </div>
        </div>

      </div>

      <div className="pt-4 border-t border-white/10 flex justify-end">
        <button
          onClick={() => alert("Settings saved successfully.")}
          className="km-glossy-btn px-6 py-2.5 text-sm"
        >
          Save Configuration
        </button>
      </div>
    </div>
  );
};

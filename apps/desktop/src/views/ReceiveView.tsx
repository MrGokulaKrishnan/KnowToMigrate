import React, { useState } from 'react';
import { QrCode, Radio, FolderCheck, ShieldAlert, Check, X, Shield, Lock } from 'lucide-react';

interface ReceiveViewProps {
  onSimulateIncoming: () => void;
}

export const ReceiveView: React.FC<ReceiveViewProps> = ({ onSimulateIncoming }) => {
  const [showIncomingModal, setShowIncomingModal] = useState(false);
  const [savePath, setSavePath] = useState('C:\\Users\\Krish\\Downloads\\KnowToMigrate');

  return (
    <div className="glass-panel p-8 space-y-8 relative">
      <div className="border-b border-white/10 pb-4 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-black text-white">Receive Files &amp; Migrations</h2>
          <p className="text-sm text-gray-400">
            This device is actively broadcasting discovery beacons via BLE &amp; UDP multicast.
          </p>
        </div>
        <button
          onClick={() => setShowIncomingModal(true)}
          className="km-glass-btn px-3.5 py-1.5 text-xs text-[#FF8A00] border-[#FF5A00]/30 hover:border-[#FF5A00]"
        >
          Simulate Incoming Request
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
        
        {/* Radar Waiting Animation (6 cols) */}
        <div className="lg:col-span-6 flex flex-col items-center justify-center p-8 glass-card text-center relative overflow-hidden">
          <div className="relative w-48 h-48 flex items-center justify-center mb-6">
            <div className="absolute w-44 h-44 rounded-full border border-[#FF5A00]/20 animate-ping"></div>
            <div className="absolute w-32 h-32 rounded-full border border-[#FF5A00]/30 animate-pulse"></div>
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-[#FF4D00] to-[#FF8A00] flex items-center justify-center p-0.5 shadow-[0_0_30px_rgba(255,90,0,0.5)]">
              <div className="w-full h-full bg-black rounded-[14px] flex items-center justify-center">
                <Radio className="w-8 h-8 text-[#FF5A00] animate-pulse" />
              </div>
            </div>
          </div>

          <h3 className="text-lg font-bold text-white mb-1">Waiting for Sender Connection</h3>
          <p className="text-xs text-gray-400 max-w-sm">
            Make sure the sending device is connected to the same Wi-Fi network or scan the QR code to connect directly.
          </p>

          <div className="mt-6 pt-4 border-t border-white/5 w-full flex items-center justify-between text-xs">
            <span className="text-gray-400 flex items-center gap-1.5">
              <FolderCheck className="w-4 h-4 text-emerald-400" /> Save Location:
            </span>
            <span className="font-mono text-white text-[11px] truncate max-w-xs">{savePath}</span>
          </div>
        </div>

        {/* QR Code & Pairing PIN (6 cols) */}
        <div className="lg:col-span-6 p-8 glass-card space-y-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#FF5A00]/20 flex items-center justify-center text-[#FF5A00]">
              <QrCode className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Instant QR Direct Pairing</h3>
              <p className="text-xs text-gray-400">Scan with sender camera or enter 6-digit code</p>
            </div>
          </div>

          {/* QR Simulation Block */}
          <div className="flex flex-col sm:flex-row items-center gap-6 p-4 rounded-xl bg-black/60 border border-white/5">
            <div className="w-32 h-32 bg-white rounded-xl p-2.5 flex items-center justify-center shadow-[0_0_20px_rgba(255,90,0,0.2)]">
              {/* High-contrast QR pattern SVG */}
              <svg viewBox="0 0 100 100" className="w-full h-full fill-black">
                <rect x="10" y="10" width="25" height="25" />
                <rect x="15" y="15" width="15" height="15" fill="white" />
                <rect x="18" y="18" width="9" height="9" />
                <rect x="65" y="10" width="25" height="25" />
                <rect x="70" y="15" width="15" height="15" fill="white" />
                <rect x="73" y="18" width="9" height="9" />
                <rect x="10" y="65" width="25" height="25" />
                <rect x="15" y="70" width="15" height="15" fill="white" />
                <rect x="18" y="73" width="9" height="9" />
                <rect x="45" y="45" width="10" height="10" fill="#FF5A00" />
                <rect x="40" y="15" width="8" height="8" />
                <rect x="50" y="25" width="8" height="8" />
                <rect x="40" y="75" width="8" height="8" />
                <rect x="75" y="45" width="8" height="8" />
                <rect x="65" y="75" width="8" height="8" />
              </svg>
            </div>

            <div className="space-y-2 text-center sm:text-left">
              <span className="text-[10px] uppercase font-bold tracking-widest text-[#FF8A00]">Ephemeral Pairing PIN</span>
              <div className="text-3xl font-mono font-black text-white tracking-wider">
                842 - 916
              </div>
              <p className="text-xs text-gray-400">Session token expires in <strong className="text-white">09:24</strong></p>
              <span className="inline-block text-[11px] px-2 py-0.5 rounded bg-emerald-500/15 text-emerald-300 font-medium">
                End-to-End Encrypted Session
              </span>
            </div>
          </div>

          <div className="text-xs text-gray-400 flex items-center gap-2">
            <Lock className="w-4 h-4 text-blue-400 shrink-0" />
            <span>Unknown devices require explicit one-tap approval before writing any bytes to disk.</span>
          </div>
        </div>

      </div>

      {/* Simulated Incoming Transfer Modal */}
      {showIncomingModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="glass-panel p-6 max-w-md w-full border border-[#FF5A00]/50 shadow-[0_0_50px_rgba(255,90,0,0.3)] space-y-5">
            <div className="flex items-start justify-between">
              <div>
                <span className="text-xs font-bold uppercase tracking-wider text-[#FF5A00]">Incoming Transfer Request</span>
                <h3 className="text-xl font-bold text-white mt-1">From: Krish's Phone</h3>
                <p className="text-xs text-gray-400">Google Pixel 8 Pro · Direct Wi-Fi 6</p>
              </div>
              <button
                onClick={() => setShowIncomingModal(false)}
                className="text-gray-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-white/5 border border-white/10 space-y-2 text-xs">
              <div className="flex justify-between text-gray-300">
                <span>Items:</span>
                <strong className="text-white">4 Files (Video, FLAC, PDF, Folder)</strong>
              </div>
              <div className="flex justify-between text-gray-300">
                <span>Total Payload:</span>
                <strong className="text-white font-mono">11.76 GB</strong>
              </div>
              <div className="flex justify-between text-gray-300">
                <span>Verification:</span>
                <strong className="text-[#22C55E]">SHA-256 Merkle Enforced</strong>
              </div>
            </div>

            <div className="flex gap-2.5 pt-2">
              <button
                onClick={() => {
                  setShowIncomingModal(false);
                  onSimulateIncoming();
                }}
                className="flex-1 km-glossy-btn py-2.5 text-sm gap-1.5"
              >
                <Check className="w-4 h-4" /> Accept &amp; Receive
              </button>
              <button
                onClick={() => setShowIncomingModal(false)}
                className="km-glass-btn px-4 py-2.5 text-sm text-gray-300 hover:text-white"
              >
                Decline
              </button>
              <button
                onClick={() => {
                  alert("Device blocked successfully.");
                  setShowIncomingModal(false);
                }}
                className="km-glass-btn px-3 py-2.5 text-sm text-red-400 hover:border-red-500/50"
                title="Block Device"
              >
                <ShieldAlert className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

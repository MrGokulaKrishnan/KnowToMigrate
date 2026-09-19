import React from 'react';
import { Smartphone, Laptop, Settings, ShieldCheck, Wifi, Radio } from 'lucide-react';

interface NavbarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  isTransferActive: boolean;
}

export const Navbar: React.FC<NavbarProps> = ({ activeTab, setActiveTab, isTransferActive }) => {
  return (
    <header className="sticky top-0 z-50 border-b border-white/10 bg-black/85 backdrop-blur-2xl px-6 py-3.5 flex items-center justify-between">
      {/* Brand & Monogram Logo */}
      <div className="flex items-center gap-3.5 cursor-pointer" onClick={() => setActiveTab('home')}>
        <div className="relative flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-br from-[#FF4D00] to-[#FF8A00] p-0.5 shadow-[0_0_20px_rgba(255,90,0,0.4)]">
          <div className="w-full h-full bg-black rounded-[10px] flex items-center justify-center overflow-hidden">
            <img src="/logo.jpg" alt="KM Logo" className="w-full h-full object-cover" />
          </div>
        </div>
        <div>
          <div className="flex items-center gap-2">
            <h1 className="font-extrabold tracking-wider text-lg text-white">
              KNOW<span className="text-[#FF5A00]">TO</span>MIGRATE
            </h1>
            <span className="text-[10px] uppercase font-bold tracking-widest px-2 py-0.5 rounded-full bg-[#FF5A00]/15 text-[#FF8A00] border border-[#FF5A00]/30">
              V2.0 Core
            </span>
          </div>
          <p className="text-xs text-gray-400 -mt-0.5">Move Anything. Anywhere. Seamlessly.</p>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex items-center bg-white/5 border border-white/10 rounded-xl p-1 gap-1 text-sm">
        <button
          onClick={() => setActiveTab('home')}
          className={`px-3.5 py-1.5 rounded-lg font-medium transition-all ${
            activeTab === 'home'
              ? 'bg-[#FF5A00] text-white shadow-[0_2px_12px_rgba(255,90,0,0.3)]'
              : 'text-gray-400 hover:text-white'
          }`}
        >
          Overview
        </button>
        <button
          onClick={() => setActiveTab('send')}
          className={`px-3.5 py-1.5 rounded-lg font-medium transition-all ${
            activeTab === 'send'
              ? 'bg-[#FF5A00] text-white shadow-[0_2px_12px_rgba(255,90,0,0.3)]'
              : 'text-gray-400 hover:text-white'
          }`}
        >
          Send
        </button>
        <button
          onClick={() => setActiveTab('receive')}
          className={`px-3.5 py-1.5 rounded-lg font-medium transition-all ${
            activeTab === 'receive'
              ? 'bg-[#FF5A00] text-white shadow-[0_2px_12px_rgba(255,90,0,0.3)]'
              : 'text-gray-400 hover:text-white'
          }`}
        >
          Receive
        </button>
        <button
          onClick={() => setActiveTab('transfer')}
          className={`px-3.5 py-1.5 rounded-lg font-medium transition-all flex items-center gap-1.5 ${
            activeTab === 'transfer'
              ? 'bg-[#FF5A00] text-white shadow-[0_2px_12px_rgba(255,90,0,0.3)]'
              : 'text-gray-400 hover:text-white'
          }`}
        >
          Active Transfer
          {isTransferActive && (
            <span className="w-2 h-2 rounded-full bg-[#22C55E] animate-ping"></span>
          )}
        </button>
        <button
          onClick={() => setActiveTab('migration')}
          className={`px-3.5 py-1.5 rounded-lg font-medium transition-all text-[#FF8A00] ${
            activeTab === 'migration'
              ? 'bg-[#FF5A00] text-white shadow-[0_2px_12px_rgba(255,90,0,0.3)]'
              : 'hover:text-white'
          }`}
        >
          ⚡ Migration Wizard
        </button>
        <button
          onClick={() => setActiveTab('history')}
          className={`px-3.5 py-1.5 rounded-lg font-medium transition-all ${
            activeTab === 'history'
              ? 'bg-[#FF5A00] text-white shadow-[0_2px_12px_rgba(255,90,0,0.3)]'
              : 'text-gray-400 hover:text-white'
          }`}
        >
          History
        </button>
      </div>

      {/* Visibility & Settings */}
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2 bg-white/5 border border-white/10 rounded-xl px-3 py-1.5 text-xs text-gray-300">
          <span className="w-2 h-2 rounded-full bg-[#22C55E] animate-pulse"></span>
          <span className="hidden sm:inline">Visibility:</span>
          <select className="bg-transparent text-[#FF8A00] font-semibold outline-none cursor-pointer">
            <option className="bg-[#111]">Everyone (10m)</option>
            <option className="bg-[#111]">Trusted Devices Only</option>
            <option className="bg-[#111]">Invisible</option>
          </select>
        </div>
        <button
          onClick={() => setActiveTab('settings')}
          className="w-9 h-9 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center hover:bg-white/10 text-gray-300 hover:text-white transition"
          title="Settings"
        >
          <Settings className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
};

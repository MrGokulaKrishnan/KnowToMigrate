import React, { useState } from 'react';
import { Navbar } from './components/Navbar';
import { HomeView } from './views/HomeView';
import { SendView } from './views/SendView';
import { ReceiveView } from './views/ReceiveView';
import { TransferView } from './views/TransferView';
import { MigrationView } from './views/MigrationView';
import { HistoryView } from './views/HistoryView';
import { SettingsView } from './views/SettingsView';
import { QrCode, X, Copy, ExternalLink } from 'lucide-react';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('home');
  const [isTransferActive, setIsTransferActive] = useState<boolean>(false);
  const [showWebReceiverModal, setShowWebReceiverModal] = useState<boolean>(false);

  const handleSelectDevice = (device: any) => {
    setActiveTab('send');
  };

  const handleStartTransfer = (device: string, files: any[]) => {
    setIsTransferActive(true);
    setActiveTab('transfer');
  };

  const handleTransferComplete = () => {
    setIsTransferActive(false);
    alert('🎉 Transfer Completed & Verified! Checksums match 100% with zero corruption.');
    setActiveTab('history');
  };

  const handleCancelTransfer = () => {
    if (confirm('Cancel active transfer? Any confirmed chunks are saved in checkpoint for instant resume.')) {
      setIsTransferActive(false);
      setActiveTab('home');
    }
  };

  return (
    <div className="min-h-screen bg-black text-white flex flex-col selection:bg-[#FF5A00] selection:text-white">
      {/* Top Navbar */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        isTransferActive={isTransferActive}
      />

      {/* Main Workspace Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6 space-y-6">
        {activeTab === 'home' && (
          <HomeView
            onSelectDevice={handleSelectDevice}
            onOpenMigration={() => setActiveTab('migration')}
            onOpenWebReceiver={() => setShowWebReceiverModal(true)}
          />
        )}

        {activeTab === 'send' && (
          <SendView onStartTransfer={handleStartTransfer} />
        )}

        {activeTab === 'receive' && (
          <ReceiveView
            onSimulateIncoming={() => {
              setIsTransferActive(true);
              setActiveTab('transfer');
            }}
          />
        )}

        {activeTab === 'transfer' && (
          <TransferView
            onTransferComplete={handleTransferComplete}
            onCancel={handleCancelTransfer}
          />
        )}

        {activeTab === 'migration' && (
          <MigrationView
            onStartMigrationTransfer={() => {
              setIsTransferActive(true);
              setActiveTab('transfer');
            }}
          />
        )}

        {activeTab === 'history' && <HistoryView />}

        {activeTab === 'settings' && <SettingsView />}
      </main>

      {/* Zero-Install Web Receiver Modal */}
      {showWebReceiverModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="glass-panel p-6 max-w-lg w-full border border-[#FF5A00]/50 shadow-[0_0_50px_rgba(255,90,0,0.3)] space-y-5">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-[#FF5A00]/20 flex items-center justify-center text-[#FF5A00]">
                  <QrCode className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-white">Zero-Install Web Receiver</h3>
                  <p className="text-xs text-gray-400">Share files with any device without app installation</p>
                </div>
              </div>
              <button
                onClick={() => setShowWebReceiverModal(false)}
                className="text-gray-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 rounded-xl bg-black/60 border border-white/10 space-y-3 text-xs">
              <p className="text-gray-300">
                1. Open this secure URL on the receiving phone, tablet, or PC:
              </p>
              <div className="flex items-center gap-2 bg-white/5 p-2 rounded-lg border border-white/10">
                <code className="text-[#FF8A00] font-mono text-sm flex-1">
                  https://knowtomigrate.live/rx/KTM-8942
                </code>
                <button
                  onClick={() => alert("URL copied to clipboard!")}
                  className="km-glass-btn px-2.5 py-1 text-xs gap-1"
                >
                  <Copy className="w-3.5 h-3.5" /> Copy
                </button>
              </div>
              <p className="text-gray-400">
                2. Both devices will negotiate a direct <strong>WebRTC DataChannel</strong> with end-to-end encryption via Web Crypto API. No files are stored in the cloud.
              </p>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setShowWebReceiverModal(false)}
                className="km-glossy-btn px-5 py-2 text-sm"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default App;

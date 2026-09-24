import React, { useState } from 'react';
import {
  Zap, Check, ChevronRight, ChevronLeft, Camera, Film,
  FileText, Download, Music, UserCheck, HardDrive, ShieldCheck,
  Smartphone, Laptop, RefreshCw, Key, FolderOpen, ArrowRight
} from 'lucide-react';
import { useTransfer } from '../context/TransferContext';
import { useToast } from '../components/Toast';

interface MigrationViewProps {
  onStartMigrationTransfer: () => void;
}

export const MigrationView: React.FC<MigrationViewProps> = ({ onStartMigrationTransfer }) => {
  const { startTransfer, targetDevice, setTargetDevice, settings } = useTransfer();
  const toast = useToast();

  const [currentStep, setCurrentStep] = useState(1);
  const [sourceDevice, setSourceDevice] = useState(settings.deviceName);
  const [isScanning, setIsScanning] = useState(false);
  const [scanProgress, setScanProgress] = useState(0);

  const [categories, setCategories] = useState([
    { id: 'photos', name: 'Photos & Camera Roll', count: '14,240 items', size: '42.8 GB', sizeNum: 42.8, selected: true, icon: Camera },
    { id: 'videos', name: '4K / 8K Video Library', count: '312 videos', size: '68.4 GB', sizeNum: 68.4, selected: true, icon: Film },
    { id: 'documents', name: 'Documents & Workspaces', count: '1,840 files', size: '8.2 GB', sizeNum: 8.2, selected: true, icon: FileText },
    { id: 'downloads', name: 'Downloads & Saved Media', count: '420 files', size: '12.5 GB', sizeNum: 12.5, selected: true, icon: Download },
    { id: 'music', name: 'Music & Audio Recordings', count: '1,200 tracks', size: '14.1 GB', sizeNum: 14.1, selected: false, icon: Music },
    { id: 'contacts', name: 'Contacts & Calendars', count: '840 contacts', size: '42 MB', sizeNum: 0.04, selected: true, icon: UserCheck },
  ]);

  const toggleCategory = (id: string) => {
    setCategories((prev) =>
      prev.map((cat) => (cat.id === id ? { ...cat, selected: !cat.selected } : cat))
    );
  };

  const totalSelectedGB = categories
    .filter((c) => c.selected)
    .reduce((sum, c) => sum + c.sizeNum, 0)
    .toFixed(2);

  const totalSelectedItems = categories
    .filter((c) => c.selected)
    .map((c) => c.count)
    .join(', ');

  const targetAvailableGB = 482.4;
  const hasEnoughSpace = targetAvailableGB > parseFloat(totalSelectedGB);

  const stepTitles = [
    'Source Device',
    'Target Destination',
    'Media Scan',
    'Category Selection',
    'Storage Headroom',
    'Secure Pairing',
    'Stream Preparation',
    'Batch Transfer',
    'Merkle Verification',
    'Migration Complete',
  ];

  const handleRunScan = () => {
    setIsScanning(true);
    setScanProgress(0);
    const interval = setInterval(() => {
      setScanProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          setIsScanning(false);
          setCurrentStep(4);
          toast.success('Scan Completed', 'Found 6 data categories (18,052 total items).');
          return 100;
        }
        return prev + 20;
      });
    }, 250);
  };

  const handleExecuteMigration = async () => {
    await startTransfer(targetDevice);
    onStartMigrationTransfer();
  };

  return (
    <div className="glass-panel p-8 space-y-6">
      {/* Wizard Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-white/10">
        <div>
          <div className="flex items-center gap-2">
            <Zap className="w-6 h-6 text-[#FF5A00]" />
            <h2 className="text-2xl font-black text-white">Smart Device Migration Wizard</h2>
          </div>
          <p className="text-sm text-gray-400 mt-0.5">
            Migrate entire device categories without cloud storage, file-size limits, or compression loss.
          </p>
        </div>
        <span className="self-start sm:self-auto px-3.5 py-1 rounded-full bg-[#FF5A00]/15 text-[#FF8A00] border border-[#FF5A00]/30 text-xs font-bold uppercase tracking-wider">
          Step {currentStep} of 10: {stepTitles[currentStep - 1]}
        </span>
      </div>

      {/* 10-Step Progress Stepper */}
      <div className="space-y-2">
        <div className="grid grid-cols-5 sm:grid-cols-10 gap-1 text-[11px] text-gray-400 font-medium text-center">
          {stepTitles.map((lbl, idx) => {
            const stepNum = idx + 1;
            const isDone = stepNum < currentStep;
            const isCurrent = stepNum === currentStep;
            return (
              <div
                key={lbl}
                onClick={() => {
                  if (stepNum < currentStep) setCurrentStep(stepNum);
                }}
                className={`truncate cursor-pointer transition ${
                  isCurrent
                    ? 'text-white font-bold underline decoration-[#FF5A00] decoration-2'
                    : isDone
                    ? 'text-[#FF8A00] hover:text-white'
                    : 'text-gray-500'
                }`}
                title={`Step ${stepNum}: ${lbl}`}
              >
                {stepNum}. {lbl}
              </div>
            );
          })}
        </div>
        <div className="w-full h-2 bg-white/5 rounded-full overflow-hidden border border-white/10">
          <div
            className="h-full bg-gradient-to-r from-[#FF4D00] to-[#FF8A00] rounded-full transition-all duration-300"
            style={{ width: `${(currentStep / 10) * 100}%` }}
          ></div>
        </div>
      </div>

      {/* Step 1: Source Device Selection */}
      {currentStep === 1 && (
        <div className="space-y-6 pt-2">
          <h3 className="text-base font-bold text-white">1. Choose Migration Source Device</h3>
          <p className="text-sm text-gray-400">
            Select the device you are migrating data FROM.
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {[
              { name: settings.deviceName, platform: 'This Device (Windows)', icon: Laptop },
              { name: "Krish's Phone", platform: 'Android 15', icon: Smartphone },
              { name: 'Studio Tablet', platform: 'Android Tablet', icon: Smartphone },
            ].map((d) => (
              <div
                key={d.name}
                onClick={() => setSourceDevice(d.name)}
                role="button"
                tabIndex={0}
                className={`glass-card p-5 cursor-pointer border transition flex flex-col justify-between ${
                  sourceDevice === d.name
                    ? 'border-[#FF5A00] bg-[#FF5A00]/10 shadow-[0_0_20px_rgba(255,90,0,0.15)]'
                    : 'border-white/5 hover:border-white/15'
                }`}
              >
                <div className="w-10 h-10 rounded-xl bg-[#FF5A00]/20 text-[#FF5A00] flex items-center justify-center mb-3">
                  <d.icon className="w-5 h-5" />
                </div>
                <h4 className="font-bold text-white text-sm">{d.name}</h4>
                <p className="text-xs text-gray-400 mt-1">{d.platform}</p>
              </div>
            ))}
          </div>
          <div className="flex justify-end pt-4">
            <button
              type="button"
              onClick={() => setCurrentStep(2)}
              className="km-glossy-btn px-6 py-2.5 text-sm gap-2"
            >
              Continue to Target Device <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 2: Target Device Selection */}
      {currentStep === 2 && (
        <div className="space-y-6 pt-2">
          <h3 className="text-base font-bold text-white">2. Choose Migration Destination Device</h3>
          <p className="text-sm text-gray-400">
            Select the destination device that will receive the migrated files.
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {[
              { name: "Krish's Phone", platform: 'Android 15', freeSpace: '112.4 GB', icon: Smartphone },
              { name: "Gokul's Laptop", platform: 'macOS M3', freeSpace: '482.4 GB', icon: Laptop },
              { name: 'Studio Tablet', platform: 'Android', freeSpace: '64.0 GB', icon: Smartphone },
            ].map((d) => (
              <div
                key={d.name}
                onClick={() => setTargetDevice(d.name)}
                role="button"
                tabIndex={0}
                className={`glass-card p-5 cursor-pointer border transition flex flex-col justify-between ${
                  targetDevice === d.name
                    ? 'border-[#FF5A00] bg-[#FF5A00]/10 shadow-[0_0_20px_rgba(255,90,0,0.15)]'
                    : 'border-white/5 hover:border-white/15'
                }`}
              >
                <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center mb-3">
                  <d.icon className="w-5 h-5" />
                </div>
                <h4 className="font-bold text-white text-sm">{d.name}</h4>
                <p className="text-xs text-gray-400 mt-1">{d.platform}</p>
                <span className="text-xs text-emerald-400 font-medium mt-2">{d.freeSpace} Free</span>
              </div>
            ))}
          </div>
          <div className="flex justify-between pt-4">
            <button
              type="button"
              onClick={() => setCurrentStep(1)}
              className="km-glass-btn px-4 py-2.5 text-sm gap-1.5"
            >
              <ChevronLeft className="w-4 h-4" /> Back
            </button>
            <button
              type="button"
              onClick={() => setCurrentStep(3)}
              className="km-glossy-btn px-6 py-2.5 text-sm gap-2"
            >
              Start Media Scan <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 3: Media Scan */}
      {currentStep === 3 && (
        <div className="space-y-6 pt-2 text-center py-8">
          <div className="w-16 h-16 rounded-2xl bg-[#FF5A00]/20 text-[#FF5A00] flex items-center justify-center mx-auto mb-4">
            <RefreshCw className={`w-8 h-8 ${isScanning ? 'animate-spin' : ''}`} />
          </div>
          <h3 className="text-xl font-bold text-white">Scanning Media Libraries on {sourceDevice}</h3>
          <p className="text-sm text-gray-400 max-w-md mx-auto">
            Deep recursive scan examining Camera DCIM, Videos, Documents, Downloads, Audio, and App Data.
          </p>

          <div className="max-w-md mx-auto space-y-2">
            <div className="flex justify-between text-xs text-gray-300">
              <span>Scanning Progress</span>
              <span className="font-mono text-[#FF8A00]">{scanProgress}%</span>
            </div>
            <div className="w-full h-2.5 bg-white/10 rounded-full overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-[#FF4D00] to-[#FF8A00] rounded-full transition-all duration-300"
                style={{ width: `${scanProgress}%` }}
              ></div>
            </div>
          </div>

          <div className="pt-4 flex justify-center gap-3">
            <button
              type="button"
              onClick={handleRunScan}
              disabled={isScanning}
              className="km-glossy-btn px-8 py-3 text-sm gap-2 disabled:opacity-50"
            >
              {isScanning ? 'Scanning Libraries...' : 'Run Automated Scan'}
            </button>
          </div>
        </div>
      )}

      {/* Step 4: Category Filter */}
      {currentStep === 4 && (
        <div className="space-y-6 pt-2">
          <div className="flex justify-between items-center">
            <h3 className="text-base font-bold text-white">4. Select Data Categories to Migrate</h3>
            <span className="text-xs text-[#FF8A00]">
              Total Selected: <strong className="font-mono text-white">{totalSelectedGB} GB</strong>
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
            {categories.map((cat) => {
              const IconComp = cat.icon;
              return (
                <div
                  key={cat.id}
                  onClick={() => toggleCategory(cat.id)}
                  role="button"
                  tabIndex={0}
                  className={`glass-card p-4 flex items-center justify-between cursor-pointer border transition ${
                    cat.selected
                      ? 'border-[#FF5A00]/50 bg-[#FF5A00]/5 shadow-[0_0_20px_rgba(255,90,0,0.12)]'
                      : 'border-white/5 hover:border-white/15'
                  }`}
                >
                  <div className="flex items-center gap-3.5">
                    <input
                      type="checkbox"
                      checked={cat.selected}
                      onChange={() => {}}
                      className="w-5 h-5 accent-[#FF5A00] rounded cursor-pointer"
                    />
                    <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center text-[#FF5A00]">
                      <IconComp className="w-5 h-5" />
                    </div>
                    <div>
                      <h4 className="font-bold text-white text-sm">{cat.name}</h4>
                      <p className="text-xs text-gray-400">{cat.count}</p>
                    </div>
                  </div>
                  <span className="font-mono text-sm font-bold text-[#FF8A00]">{cat.size}</span>
                </div>
              );
            })}
          </div>

          <div className="flex justify-between pt-4">
            <button
              type="button"
              onClick={() => setCurrentStep(3)}
              className="km-glass-btn px-4 py-2.5 text-sm gap-1.5"
            >
              <ChevronLeft className="w-4 h-4" /> Back
            </button>
            <button
              type="button"
              onClick={() => setCurrentStep(5)}
              className="km-glossy-btn px-6 py-2.5 text-sm gap-2"
            >
              Check Headroom <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 5: Storage Headroom Check */}
      {currentStep === 5 && (
        <div className="space-y-6 pt-2">
          <h3 className="text-base font-bold text-white">5. Storage Pre-Flight Headroom Verification</h3>
          <div className="p-6 rounded-2xl bg-white/5 border border-white/10 space-y-4">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <span className="text-xs text-gray-400 uppercase tracking-wider font-semibold">
                  Required Space on {targetDevice}
                </span>
                <div className="text-3xl font-black text-white mt-1">{totalSelectedGB} GB</div>
                <p className="text-xs text-gray-400 mt-1">Payload includes: {totalSelectedItems}</p>
              </div>

              <div className="md:text-right">
                <span className="text-xs text-gray-400 uppercase tracking-wider font-semibold">
                  Available on Destination Drive
                </span>
                <div className="text-3xl font-black text-[#22C55E] mt-1">{targetAvailableGB} GB</div>
                <p className="text-xs text-emerald-400 mt-1 flex items-center md:justify-end gap-1">
                  <ShieldCheck className="w-4 h-4" />
                  {hasEnoughSpace ? 'Headroom Verified (+350 GB Safe Buffer)' : 'Insufficient Storage'}
                </p>
              </div>
            </div>

            <div className="w-full h-3 bg-white/10 rounded-full overflow-hidden flex">
              <div
                className="h-full bg-[#FF5A00]"
                style={{ width: `${(parseFloat(totalSelectedGB) / targetAvailableGB) * 100}%` }}
                title="Migrated Payload"
              ></div>
              <div
                className="h-full bg-emerald-500/40"
                style={{ width: `${100 - (parseFloat(totalSelectedGB) / targetAvailableGB) * 100}%` }}
                title="Free Headroom"
              ></div>
            </div>
          </div>

          <div className="flex justify-between pt-4">
            <button
              type="button"
              onClick={() => setCurrentStep(4)}
              className="km-glass-btn px-4 py-2.5 text-sm gap-1.5"
            >
              <ChevronLeft className="w-4 h-4" /> Back
            </button>
            <button
              type="button"
              onClick={() => setCurrentStep(6)}
              className="km-glossy-btn px-6 py-2.5 text-sm gap-2"
            >
              Confirm Pairing <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 6: Secure Pairing */}
      {currentStep === 6 && (
        <div className="space-y-6 pt-2">
          <h3 className="text-base font-bold text-white">6. Cryptographic Device Pairing</h3>
          <div className="p-6 rounded-2xl glass-card border space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-[#FF5A00]/20 text-[#FF5A00] flex items-center justify-center">
                <Key className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-bold text-white text-sm">X25519 Ephemeral Key Agreement</h4>
                <p className="text-xs text-gray-400">Zero cloud exchange. Direct local link.</p>
              </div>
            </div>
            <div className="p-4 rounded-xl bg-black/60 border border-white/5 flex items-center justify-between text-xs">
              <span className="text-gray-400">Session Security Token:</span>
              <span className="font-mono text-[#22C55E] font-bold">KTM-SESSION-TLS1.3-VERIFIED</span>
            </div>
          </div>

          <div className="flex justify-between pt-4">
            <button
              type="button"
              onClick={() => setCurrentStep(5)}
              className="km-glass-btn px-4 py-2.5 text-sm gap-1.5"
            >
              <ChevronLeft className="w-4 h-4" /> Back
            </button>
            <button
              type="button"
              onClick={() => setCurrentStep(7)}
              className="km-glossy-btn px-6 py-2.5 text-sm gap-2"
            >
              Setup Stream <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 7: Stream Preparation & Checkpoints */}
      {currentStep === 7 && (
        <div className="space-y-6 pt-2">
          <h3 className="text-base font-bold text-white">7. Stream Preparation &amp; Checkpoint Initialization</h3>
          <div className="p-6 rounded-2xl glass-card border space-y-3 text-xs text-gray-300">
            <div className="flex justify-between py-1 border-b border-white/5">
              <span>Streaming Chunker:</span>
              <strong className="text-white font-mono">{settings.chunkSizeMB} MB Streaming Buffers</strong>
            </div>
            <div className="flex justify-between py-1 border-b border-white/5">
              <span>Checkpoint Manifest:</span>
              <strong className="text-emerald-400 font-mono">.ktm-checkpoint.json (Resumable)</strong>
            </div>
            <div className="flex justify-between py-1 border-b border-white/5">
              <span>Integrity Verification:</span>
              <strong className="text-white font-mono">Dual-Layer SHA-256 Merkle Roots</strong>
            </div>
          </div>

          <div className="flex justify-between pt-4">
            <button
              type="button"
              onClick={() => setCurrentStep(6)}
              className="km-glass-btn px-4 py-2.5 text-sm gap-1.5"
            >
              <ChevronLeft className="w-4 h-4" /> Back
            </button>
            <button
              type="button"
              onClick={handleExecuteMigration}
              className="km-glossy-btn px-8 py-3 text-sm gap-2 font-bold shadow-[0_0_30px_rgba(255,90,0,0.3)]"
            >
              <Zap className="w-4 h-4 text-white" />
              Execute Migration Pipeline
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

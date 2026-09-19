import React, { useState } from 'react';
import {
  Zap,
  Check,
  ChevronRight,
  ChevronLeft,
  Camera,
  Film,
  FileText,
  Download,
  Music,
  UserCheck,
  HardDrive,
  ShieldCheck,
  Smartphone,
  Laptop
} from 'lucide-react';

interface MigrationViewProps {
  onStartMigrationTransfer: () => void;
}

export const MigrationView: React.FC<MigrationViewProps> = ({ onStartMigrationTransfer }) => {
  const [currentStep, setCurrentStep] = useState(4);
  const [categories, setCategories] = useState([
    { id: 'photos', name: 'Photos & Camera Roll', count: '14,240 items', size: '42.8 GB', sizeNum: 42.8, selected: true, icon: Camera },
    { id: 'videos', name: '4K / 8K Video Library', count: '312 videos', size: '68.4 GB', sizeNum: 68.4, selected: true, icon: Film },
    { id: 'documents', name: 'Documents & Workspaces', count: '1,840 files', size: '8.2 GB', sizeNum: 8.2, selected: true, icon: FileText },
    { id: 'downloads', name: 'Downloads & Saved Media', count: '420 files', size: '12.5 GB', sizeNum: 12.5, selected: true, icon: Download },
    { id: 'music', name: 'Music & Audio Recordings', count: '1,200 tracks', size: '14.1 GB', sizeNum: 14.1, selected: false, icon: Music },
    { id: 'contacts', name: 'Contacts & Calendars', count: '840 contacts', size: '42 MB', sizeNum: 0.04, selected: true, icon: UserCheck }
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

  const stepLabels = [
    'Source', 'Target', 'Scan', 'Categories', 'Storage',
    'Pairing', 'Prepare', 'Transfer', 'Verify', 'Done'
  ];

  return (
    <div className="glass-panel p-8 space-y-6">
      {/* Wizard Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-white/10">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-[#FF5A00] font-black text-2xl">⚡</span>
            <h2 className="text-2xl font-black text-white">Smart Device Migration Wizard</h2>
          </div>
          <p className="text-sm text-gray-400 mt-0.5">
            Migrate entire device categories without cloud storage, file-size limits, or compression loss.
          </p>
        </div>
        <span className="self-start sm:self-auto px-3.5 py-1 rounded-full bg-[#FF5A00]/15 text-[#FF8A00] border border-[#FF5A00]/30 text-xs font-bold uppercase tracking-wider">
          Step {currentStep} of 10
        </span>
      </div>

      {/* 10-Step Progress Stepper */}
      <div className="space-y-2">
        <div className="grid grid-cols-5 sm:grid-cols-10 gap-1 text-[11px] text-gray-400 font-medium text-center">
          {stepLabels.map((lbl, idx) => {
            const stepNum = idx + 1;
            const isDone = stepNum < currentStep;
            const isCurrent = stepNum === currentStep;
            return (
              <div
                key={lbl}
                className={`truncate ${
                  isCurrent
                    ? 'text-white font-bold underline decoration-[#FF5A00] decoration-2'
                    : isDone
                    ? 'text-[#FF8A00]'
                    : 'text-gray-500'
                }`}
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

      {/* Step Content */}
      {currentStep === 4 && (
        <div className="space-y-6 pt-2">
          <div className="flex justify-between items-center">
            <h3 className="text-base font-bold text-white">Select Data Categories to Migrate</h3>
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

          {/* Pre-flight Storage Check Card */}
          <div className="p-5 rounded-2xl bg-white/5 border border-white/10 flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                Destination Storage Pre-Check
              </span>
              <div className="flex items-baseline gap-2 mt-1">
                <span className="text-2xl font-black text-white">{totalSelectedGB} GB</span>
                <span className="text-sm text-gray-400">required vs</span>
                <span className="text-xl font-bold text-[#22C55E]">482.40 GB</span>
                <span className="text-sm text-gray-400">available on target</span>
              </div>
              <p className="text-xs text-[#22C55E] mt-1 flex items-center gap-1">
                <ShieldCheck className="w-4 h-4" />
                Storage verified. Est. transfer time: <strong>23m 15s</strong> over Direct Wi-Fi 6.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={() => setCurrentStep(3)}
                className="km-glass-btn px-4 py-2.5 text-sm gap-1.5"
              >
                <ChevronLeft className="w-4 h-4" /> Back
              </button>
              <button
                onClick={() => {
                  setCurrentStep(8);
                  onStartMigrationTransfer();
                }}
                className="km-glossy-btn px-6 py-2.5 text-sm gap-2"
              >
                Execute Migration Pipeline
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}

      {currentStep > 4 && (
        <div className="text-center py-10 space-y-4">
          <div className="w-16 h-16 rounded-2xl bg-emerald-500/20 text-[#22C55E] flex items-center justify-center mx-auto shadow-[0_0_30px_rgba(34,197,94,0.3)]">
            <Check className="w-8 h-8" />
          </div>
          <h3 className="text-2xl font-bold text-white">Migration Session Initialized</h3>
          <p className="text-sm text-gray-400 max-w-md mx-auto">
            The active batch transfer pipeline is now executing with parallel chunking and SHA-256 Merkle root verification.
          </p>
          <button
            onClick={() => setCurrentStep(4)}
            className="km-glass-btn px-5 py-2 text-sm text-[#FF8A00] border-[#FF5A00]/30"
          >
            Configure Categories Again
          </button>
        </div>
      )}
    </div>
  );
};

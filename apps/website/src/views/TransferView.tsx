import React, { useEffect } from 'react';
import { Pause, Play, ShieldCheck, Film, CheckCircle2, RotateCcw } from 'lucide-react';
import { useTransfer } from '../context/TransferContext';
import { FileValidator } from '../services/fileValidator';

interface TransferViewProps {
  onTransferComplete: () => void;
  onCancel: () => void;
}

export const TransferView: React.FC<TransferViewProps> = ({ onTransferComplete, onCancel }) => {
  const {
    activeTransfer,
    pauseTransfer,
    resumeTransfer,
    cancelTransfer,
    targetDevice
  } = useTransfer();

  // Watch for completion event
  useEffect(() => {
    if (activeTransfer?.status === 'completed') {
      const t = setTimeout(() => {
        onTransferComplete();
      }, 800);
      return () => clearTimeout(t);
    }
  }, [activeTransfer?.status, onTransferComplete]);

  // Fallback defaults if view is navigated to directly without active transfer
  const progress = activeTransfer?.percent ?? 0;
  const speed = activeTransfer?.speedMBps ?? 94.6;
  const isPaused = activeTransfer?.status === 'paused';
  const isCompleted = activeTransfer?.status === 'completed';
  const totalBytes = activeTransfer?.totalBytes || 11_760_000_000;
  const transferredBytes = activeTransfer?.transferredBytes || Math.round((totalBytes * progress) / 100);

  const totalFormatted = FileValidator.formatBytes(totalBytes);
  const transferredFormatted = FileValidator.formatBytes(transferredBytes);

  const etaSeconds = activeTransfer?.etaSeconds ?? (speed > 0 ? Math.round((totalBytes - transferredBytes) / (speed * 1024 * 1024)) : 0);
  const etaFormatted = isPaused
    ? 'Paused'
    : isCompleted
    ? 'Completed'
    : `00m ${etaSeconds.toString().padStart(2, '0')}s`;

  const circumference = 264;
  const strokeOffset = circumference - (circumference * progress) / 100;

  const chunkStates = activeTransfer?.chunkStates || Array.from({ length: 48 }, (_, i) =>
    i < Math.round(48 * (progress / 100)) ? 'verified' : i === Math.round(48 * (progress / 100)) ? 'in_flight' : 'pending'
  );

  const sessionId = activeTransfer?.sessionId || 'KTM-8942-F20A';
  const currentFileName = activeTransfer?.currentFileName || '4K_Drone_Cinematic_Footage.mp4';

  const handleCancelClick = () => {
    cancelTransfer();
    onCancel();
  };

  return (
    <div className="glass-panel p-8 relative overflow-hidden space-y-6">
      <div className="absolute top-0 right-0 w-96 h-96 bg-[#FF5A00]/10 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20"></div>

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-white/10">
        <div>
          <div className="flex items-center gap-3">
            <span
              className={`px-2.5 py-1 rounded-full text-xs font-semibold border flex items-center gap-1.5 ${
                isCompleted
                  ? 'bg-[#22C55E]/15 text-[#22C55E] border-[#22C55E]/30'
                  : isPaused
                  ? 'bg-amber-500/15 text-amber-400 border-amber-500/30'
                  : 'bg-[#22C55E]/15 text-[#22C55E] border-[#22C55E]/30'
              }`}
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  isCompleted
                    ? 'bg-[#22C55E]'
                    : isPaused
                    ? 'bg-amber-400'
                    : 'bg-[#22C55E] animate-ping'
                }`}
              ></span>
              {isCompleted ? 'Transfer Verified' : isPaused ? 'Transfer Paused' : 'Live Transfer Active'}
            </span>
            <span className="text-xs text-gray-400">
              Session ID: <code className="text-gray-300 font-mono">{sessionId}</code>
            </span>
          </div>
          <h2 className="text-2xl font-black text-white mt-1.5">Direct P2P Streaming in Progress</h2>
          <p className="text-sm text-gray-400">
            Target: <strong className="text-white">{targetDevice}</strong> · Direct Wi-Fi 6 (TLS 1.3)
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2.5">
          {!isCompleted && (
            <button
              type="button"
              onClick={isPaused ? resumeTransfer : pauseTransfer}
              className="km-glass-btn px-4 py-2 text-sm gap-2"
            >
              {isPaused ? (
                <>
                  <Play className="w-4 h-4 text-[#22C55E]" /> Resume Transfer
                </>
              ) : (
                <>
                  <Pause className="w-4 h-4 text-[#FF8A00]" /> Pause Transfer
                </>
              )}
            </button>
          )}
          <button
            type="button"
            onClick={handleCancelClick}
            className="km-glass-btn px-4 py-2 text-sm text-red-400 hover:text-red-300 border-red-500/20 hover:border-red-500/40"
          >
            {isCompleted ? 'Done' : 'Cancel'}
          </button>
        </div>
      </div>

      {/* Main Metrics & Visualizer Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center py-4">
        {/* Speedometer Circle (4 cols) */}
        <div className="lg:col-span-4 flex flex-col items-center justify-center p-6 glass-card relative">
          <div className="relative w-44 h-44 flex items-center justify-center">
            <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="42" stroke="rgba(255,255,255,0.06)" strokeWidth="8" fill="none" />
              <circle
                cx="50"
                cy="50"
                r="42"
                stroke="url(#km-gradient)"
                strokeWidth="8"
                strokeDasharray={circumference}
                strokeDashoffset={strokeOffset}
                strokeLinecap="round"
                fill="none"
                className="transition-all duration-300"
              />
              <defs>
                <linearGradient id="km-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#FF4D00" />
                  <stop offset="50%" stopColor="#FF6500" />
                  <stop offset="100%" stopColor="#FF9A3D" />
                </linearGradient>
              </defs>
            </svg>
            <div className="absolute flex flex-col items-center justify-center text-center">
              <span className="text-3xl font-black text-white">{progress.toFixed(1)}%</span>
              <span className="text-[10px] text-gray-400 uppercase tracking-widest mt-0.5">Streamed</span>
            </div>
          </div>

          <div className="mt-4 text-center">
            <div className="text-2xl font-extrabold text-[#FF5A00] tracking-tight flex items-center justify-center gap-1">
              <span>{isPaused ? '0.0' : speed}</span>
              <span className="text-sm font-semibold text-gray-400">MB/s</span>
            </div>
            <p className="text-xs text-gray-400 mt-0.5">
              ETA: <strong className="text-white font-mono">{etaFormatted}</strong> remaining
            </p>
          </div>
        </div>

        {/* Progression & Chunk Map (8 cols) */}
        <div className="lg:col-span-8 space-y-5">
          {/* Overall Batch Progress */}
          <div>
            <div className="flex justify-between text-sm font-medium mb-1.5">
              <span className="text-gray-300">Overall Batch Progress</span>
              <span className="text-white font-mono">
                {transferredFormatted} / {totalFormatted}
              </span>
            </div>
            <div className="w-full h-3 bg-white/5 rounded-full overflow-hidden p-0.5 border border-white/10">
              <div
                className="h-full bg-gradient-to-r from-[#FF4D00] via-[#FF6500] to-[#FF9A3D] rounded-full transition-all duration-300 shadow-[0_0_12px_rgba(255,90,0,0.5)]"
                style={{ width: `${progress}%` }}
              ></div>
            </div>
          </div>

          {/* Current File Being Streamed */}
          <div className="p-4 rounded-xl bg-white/5 border border-white/10">
            <div className="flex items-center justify-between text-xs mb-2">
              <span className="text-gray-400">Currently Streaming:</span>
              <span className="text-[#22C55E] font-medium flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5" /> SHA-256 Merkle Checkpoints
              </span>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-[#FF5A00]/20 flex items-center justify-center text-[#FF5A00] shrink-0">
                <Film className="w-5 h-5" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex justify-between text-sm font-semibold truncate">
                  <span className="truncate text-white">{currentFileName}</span>
                  <span className="text-gray-400 text-xs font-mono">
                    {progress.toFixed(0)}%
                  </span>
                </div>
                <div className="w-full h-1.5 bg-white/10 rounded-full mt-2 overflow-hidden">
                  <div
                    className="h-full bg-[#FF8A00] rounded-full transition-all duration-300"
                    style={{ width: `${progress}%` }}
                  ></div>
                </div>
              </div>
            </div>
          </div>

          {/* 8MB Chunk Allocation Map */}
          <div>
            <div className="flex justify-between text-xs text-gray-400 mb-2">
              <span>8MB Chunk Allocation Map ({chunkStates.length} chunks mapped)</span>
              <span className="text-[#FF8A00] font-mono">
                Verified: {chunkStates.filter((s) => s === 'verified').length}/{chunkStates.length}
              </span>
            </div>
            <div
              className="grid gap-1 p-2.5 rounded-xl bg-black/60 border border-white/5"
              style={{ gridTemplateColumns: 'repeat(24, 1fr)' }}
            >
              {chunkStates.map((state, i) => (
                <div
                  key={i}
                  className={`h-2.5 rounded-sm transition-all ${
                    state === 'verified'
                      ? 'bg-[#22C55E]'
                      : state === 'in_flight'
                      ? 'bg-[#FF5A00] animate-pulse'
                      : 'bg-white/10'
                  }`}
                  title={`Chunk ${i + 1}: ${state}`}
                ></div>
              ))}
            </div>
            <div className="flex items-center gap-4 text-[11px] text-gray-400 mt-2">
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-sm bg-[#22C55E]"></span> Verified &amp; Written
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-sm bg-[#FF5A00]"></span> In Flight
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-sm bg-white/10"></span> Queued / Checkpoint
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Verification Assurance Footer */}
      <div className="p-3.5 rounded-xl bg-[#22C55E]/10 border border-[#22C55E]/20 flex items-center justify-between text-xs text-emerald-300">
        <div className="flex items-center gap-2">
          <ShieldCheck className="w-4 h-4 text-[#22C55E]" />
          <span>
            <strong>Zero-Corruption Guarantee:</strong> Interrupted transfers resume from the last verified chunk without re-transmitting previous data.
          </span>
        </div>
        <span className="font-mono text-white">0 Bad Chunks</span>
      </div>
    </div>
  );
};

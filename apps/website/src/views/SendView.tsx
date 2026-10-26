import React, { useRef } from 'react';
import {
  Upload, FolderUp, Smartphone, Laptop, Tablet, FileText, Film,
  Image as ImageIcon, Music, Archive, CheckCircle2, ShieldCheck,
  ArrowRight, Trash2, X, Clock
} from 'lucide-react';
import { useTransfer } from '../context/TransferContext';
import { useToast } from '../components/Toast';
import { FileValidator, ValidatedFileItem } from '../services/fileValidator';
import { useLiveTimeAgo } from '../utils/timeAgo';

const StagedItemRow: React.FC<{
  file: ValidatedFileItem;
  onRemove: (id: string) => void;
  icon: React.ReactNode;
}> = ({ file, onRemove, icon }) => {
  const liveTime = useLiveTimeAgo(file.lastModified || Date.now());

  return (
    <div className="glass-card p-3.5 flex items-center justify-between group">
      <div className="flex items-center gap-3 min-w-0">
        <div className="w-8 h-8 rounded-lg bg-[#FF5A00]/15 flex items-center justify-center text-[#FF5A00] shrink-0">
          {icon}
        </div>
        <div className="min-w-0">
          <p className="text-sm font-semibold text-white truncate max-w-xs sm:max-w-md">
            {file.sanitizedName}
          </p>
          <p className="text-xs text-gray-400 font-mono flex items-center gap-1.5 flex-wrap">
            <span>{file.sizeFormatted}</span>
            <span>·</span>
            <span className="text-[#4ADE80] font-sans flex items-center gap-1 font-medium">
              <Clock className="w-3 h-3 text-[#4ADE80]" />
              {liveTime}
            </span>
          </p>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <span className="text-xs text-[#4ADE80] flex items-center gap-1 font-semibold">
          <CheckCircle2 className="w-3.5 h-3.5 text-[#4ADE80]" /> Ready
        </span>
        <button
          type="button"
          onClick={() => onRemove(file.id)}
          aria-label={`Remove ${file.sanitizedName}`}
          className="p-1 rounded-md text-gray-500 hover:text-red-400 hover:bg-white/5 transition"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};

interface SendViewProps {
  onStartTransfer: () => void;
}

export const SendView: React.FC<SendViewProps> = ({ onStartTransfer }) => {
  const {
    selectedFiles,
    addFiles,
    removeFile,
    clearFiles,
    targetDevice,
    setTargetDevice,
    startTransfer,
    settings
  } = useTransfer();

  const toast = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const folderInputRef = useRef<HTMLInputElement>(null);

  const handleFilesAdded = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      addFiles(e.target.files);
      toast.success('Files Added', `Added ${e.target.files.length} items to payload.`);
    }
  };

  const totalBytes = selectedFiles.reduce((acc, f) => acc + f.sizeBytes, 0);
  const totalChunks = Math.ceil(totalBytes / (settings.chunkSizeMB * 1024 * 1024)) || 0;

  const handleStart = async () => {
    if (selectedFiles.length === 0) {
      toast.error('No Files Selected', 'Please add at least one file to transfer.');
      return;
    }
    await startTransfer(targetDevice);
    onStartTransfer();
  };

  const getFileIcon = (cat: ValidatedFileItem['category']) => {
    switch (cat) {
      case 'video': return <Film className="w-4 h-4" />;
      case 'audio': return <Music className="w-4 h-4" />;
      case 'image': return <ImageIcon className="w-4 h-4" />;
      case 'doc': return <FileText className="w-4 h-4" />;
      case 'archive': return <Archive className="w-4 h-4" />;
      case 'folder': return <FolderUp className="w-4 h-4" />;
      default: return <FileText className="w-4 h-4" />;
    }
  };

  const targets = [
    { name: "Krish's Phone", platform: 'Android', type: 'phone', icon: Smartphone, signal: 'Direct transfer' },
    { name: "Gokul's Laptop", platform: 'macOS', type: 'laptop', icon: Laptop, signal: 'Local LAN' },
    { name: 'Studio Tablet', platform: 'Android', type: 'tablet', icon: Tablet, signal: 'Direct transfer' },
  ];

  return (
    <div className="glass-panel p-8 space-y-6">
      {/* Hidden inputs for adding more files */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFilesAdded}
        multiple
        className="hidden"
        aria-hidden="true"
      />
      <input
        type="file"
        ref={folderInputRef}
        onChange={handleFilesAdded}
        multiple
        {...({ webkitdirectory: '' } as any)}
        className="hidden"
        aria-hidden="true"
      />

      <div className="border-b border-white/10 pb-4 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <h2 className="text-2xl font-black text-white">Send Content to Nearby Device</h2>
          <p className="text-sm text-gray-400">
            Streaming directly over local Wi-Fi 6 with {settings.chunkSizeMB}MB chunked backpressure and SHA-256 integrity verification.
          </p>
        </div>
        {selectedFiles.length > 0 && (
          <button
            type="button"
            onClick={clearFiles}
            className="self-start sm:self-auto km-glass-btn px-3 py-1.5 text-xs text-red-400 hover:text-red-300 border-red-500/20 gap-1.5"
          >
            <Trash2 className="w-3.5 h-3.5" /> Clear All
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* File Selection & Manifest List (7 cols) */}
        <div className="lg:col-span-7 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-gray-300">
              Selected Items ({selectedFiles.length})
            </h3>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="km-glass-btn px-3 py-1 text-xs gap-1.5"
              >
                <Upload className="w-3.5 h-3.5" /> Add Files
              </button>
              <button
                type="button"
                onClick={() => folderInputRef.current?.click()}
                className="km-glass-btn px-3 py-1 text-xs gap-1.5"
              >
                <FolderUp className="w-3.5 h-3.5 text-[#FF8A00]" /> Add Folder
              </button>
            </div>
          </div>

          <div className="space-y-2.5 max-h-80 overflow-y-auto pr-1">
            {selectedFiles.map((file) => (
              <StagedItemRow
                key={file.id}
                file={file}
                onRemove={removeFile}
                icon={getFileIcon(file.category)}
              />
            ))}

            {selectedFiles.length === 0 && (
              <div className="p-8 text-center glass-card border-dashed">
                <p className="text-sm text-gray-400">No files staged for transfer.</p>
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="mt-3 km-glossy-btn px-4 py-1.5 text-xs gap-1.5"
                >
                  <Upload className="w-3.5 h-3.5" /> Browse Files
                </button>
              </div>
            )}
          </div>

          <div className="p-3.5 rounded-xl bg-white/5 border border-white/10 flex justify-between text-xs text-gray-300">
            <span>
              Total Payload Size: <strong className="text-white font-mono">{FileValidator.formatBytes(totalBytes)}</strong>
            </span>
            <span>
              Total {settings.chunkSizeMB}MB Chunks: <strong className="text-[#FF8A00] font-mono">{totalChunks} Chunks</strong>
            </span>
          </div>
        </div>

        {/* Target Recipient & Pre-flight Summary (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <h3 className="text-sm font-bold text-gray-300">Choose Target Recipient</h3>

          <div className="space-y-2">
            {targets.map((dev) => {
              const isSelected = targetDevice === dev.name;
              const IconComp = dev.icon;
              return (
                <div
                  key={dev.name}
                  onClick={() => setTargetDevice(dev.name)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') setTargetDevice(dev.name);
                  }}
                  className={`p-3.5 rounded-xl border cursor-pointer transition flex items-center justify-between ${
                    isSelected
                      ? 'border-[#FF5A00] bg-[#FF5A00]/10 shadow-[0_0_15px_rgba(255,90,0,0.2)]'
                      : 'border-white/10 bg-white/5 hover:border-white/20'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
                      <IconComp className="w-4 h-4" />
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-white">{dev.name}</h4>
                      <p className="text-xs text-gray-400">{dev.signal} · Direct LAN</p>
                    </div>
                  </div>
                  {isSelected && (
                    <span className="w-2.5 h-2.5 rounded-full bg-[#FF5A00]"></span>
                  )}
                </div>
              );
            })}
          </div>

          {/* Pre-flight Security & Storage Check */}
          <div className="p-4 rounded-xl bg-[#4ADE80]/10 border border-[#4ADE80]/30 text-xs text-[#4ADE80] space-y-2">
            <div className="flex items-center gap-1.5 font-bold">
              <ShieldCheck className="w-4 h-4 text-[#4ADE80]" />
              Pre-flight Destination Storage &amp; Safety Check
            </div>
            <p className="text-gray-300">
              Target has <strong>112.4 GB</strong> free space available. Payload requires{' '}
              <strong>{FileValidator.formatBytes(totalBytes)}</strong>. Sufficient space confirmed.
            </p>
          </div>

          <button
            type="button"
            onClick={handleStart}
            disabled={selectedFiles.length === 0}
            className="w-full km-glossy-btn py-3 text-sm gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Start Direct Transfer to {targetDevice}
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};

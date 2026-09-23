/**
 * KnowToMigrate - Global Transfer Context & State Provider
 * Provides unified state for selected files, live transfer progress,
 * history records, and device settings across all application views.
 */

import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { FileValidator, ValidatedFileItem } from '../services/fileValidator';
import { StorageService, StoredTransferRecord, StoredSettings } from '../services/storageService';
import { WebTransferEngine, TransferSessionProgress } from '../services/webTransferEngine';

interface TransferContextValue {
  selectedFiles: ValidatedFileItem[];
  addFiles: (files: FileList | File[] | File) => void;
  removeFile: (id: string) => void;
  clearFiles: () => void;

  targetDevice: string;
  setTargetDevice: (device: string) => void;

  activeTransfer: TransferSessionProgress | null;
  startTransfer: (customDevice?: string) => Promise<string>;
  pauseTransfer: () => void;
  resumeTransfer: () => void;
  cancelTransfer: () => void;

  history: StoredTransferRecord[];
  refreshHistory: () => void;
  clearHistory: () => void;

  settings: StoredSettings;
  updateSettings: (newSettings: Partial<StoredSettings>) => void;
}

const TransferContext = createContext<TransferContextValue | null>(null);

export const TransferProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [selectedFiles, setSelectedFiles] = useState<ValidatedFileItem[]>(() => [
    {
      id: 'default-1',
      originalName: '4K_Drone_Cinematic_Footage.mp4',
      sanitizedName: '4K_Drone_Cinematic_Footage.mp4',
      sizeBytes: 2_840_000_000,
      sizeFormatted: '2.84 GB',
      mimeType: 'video/mp4',
      category: 'video',
      isValid: true,
    },
    {
      id: 'default-2',
      originalName: 'Production_Soundtrack_Masters.flac',
      sanitizedName: 'Production_Soundtrack_Masters.flac',
      sizeBytes: 480_000_000,
      sizeFormatted: '480 MB',
      mimeType: 'audio/flac',
      category: 'audio',
      isValid: true,
    },
    {
      id: 'default-3',
      originalName: 'KnowToMigrate_Brand_Guidelines.pdf',
      sanitizedName: 'KnowToMigrate_Brand_Guidelines.pdf',
      sizeBytes: 42_000_000,
      sizeFormatted: '42 MB',
      mimeType: 'application/pdf',
      category: 'doc',
      isValid: true,
    },
    {
      id: 'default-4',
      originalName: 'RAW_Camera_Shoot_Asset_Folder/',
      sanitizedName: 'RAW_Camera_Shoot_Asset_Folder',
      sizeBytes: 8_400_000_000,
      sizeFormatted: '8.40 GB',
      mimeType: 'application/octet-stream',
      category: 'folder',
      isValid: true,
    },
  ]);

  const [targetDevice, setTargetDevice] = useState<string>("Krish's Phone");
  const [activeTransfer, setActiveTransfer] = useState<TransferSessionProgress | null>(null);
  const [history, setHistory] = useState<StoredTransferRecord[]>(() => StorageService.getTransferHistory());
  const [settings, setSettings] = useState<StoredSettings>(() => StorageService.getSettings());

  const engine = useMemo(() => new WebTransferEngine(settings.chunkSizeMB), [settings.chunkSizeMB]);

  useEffect(() => {
    const unsubscribe = engine.subscribe((progress) => {
      setActiveTransfer({ ...progress });
      if (progress.status === 'completed') {
        setHistory(StorageService.getTransferHistory());
      }
    });
    return unsubscribe;
  }, [engine]);

  const addFiles = useCallback((filesInput: FileList | File[] | File) => {
    const rawList = filesInput instanceof FileList
      ? Array.from(filesInput)
      : Array.isArray(filesInput)
      ? filesInput
      : [filesInput];

    const validated = rawList.map((f) => FileValidator.validateFile(f));
    setSelectedFiles((prev) => [...prev, ...validated]);
  }, []);

  const removeFile = useCallback((id: string) => {
    setSelectedFiles((prev) => prev.filter((f) => f.id !== id));
  }, []);

  const clearFiles = useCallback(() => {
    setSelectedFiles([]);
  }, []);

  const startTransfer = useCallback(
    async (customDevice?: string) => {
      const destination = customDevice || targetDevice;
      return engine.startTransfer(destination, selectedFiles, 'send');
    },
    [engine, targetDevice, selectedFiles]
  );

  const pauseTransfer = useCallback(() => {
    engine.pause();
  }, [engine]);

  const resumeTransfer = useCallback(() => {
    engine.resume();
  }, [engine]);

  const cancelTransfer = useCallback(() => {
    engine.cancel();
  }, [engine]);

  const refreshHistory = useCallback(() => {
    setHistory(StorageService.getTransferHistory());
  }, []);

  const clearHistory = useCallback(() => {
    StorageService.clearHistory();
    setHistory([]);
  }, []);

  const updateSettings = useCallback((newSettings: Partial<StoredSettings>) => {
    const updated = StorageService.saveSettings(newSettings);
    setSettings(updated);
  }, []);

  return (
    <TransferContext.Provider
      value={{
        selectedFiles,
        addFiles,
        removeFile,
        clearFiles,
        targetDevice,
        setTargetDevice,
        activeTransfer,
        startTransfer,
        pauseTransfer,
        resumeTransfer,
        cancelTransfer,
        history,
        refreshHistory,
        clearHistory,
        settings,
        updateSettings,
      }}
    >
      {children}
    </TransferContext.Provider>
  );
};

export function useTransfer(): TransferContextValue {
  const ctx = useContext(TransferContext);
  if (!ctx) {
    throw new Error('useTransfer must be used within a TransferProvider');
  }
  return ctx;
}

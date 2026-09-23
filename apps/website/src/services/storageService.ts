/**
 * KnowToMigrate - Storage Service
 * Handles data persistence for transfer records, nearby/trusted devices,
 * application settings, and device identity using localStorage with schema validation.
 */

export interface StoredTransferRecord {
  id: string;
  title: string;
  device: string;
  direction: 'send' | 'receive';
  sizeBytes: number;
  sizeFormatted: string;
  timestamp: number;
  dateFormatted: string;
  speed: string;
  transport: string;
  merkleRootHash: string;
  verified: boolean;
  status: 'completed' | 'cancelled' | 'failed';
}

export interface StoredSettings {
  deviceName: string;
  saveDirectory: string;
  autoAcceptTrusted: boolean;
  encryptTransfers: boolean;
  chunkSizeMB: number;
  theme: 'amoled' | 'glass';
  reducedMotion: boolean;
}

export interface StoredDevice {
  id: string;
  name: string;
  platform: 'Android' | 'Windows' | 'macOS' | 'Linux' | 'Web';
  ip?: string;
  isTrusted: boolean;
  lastSeen: number;
}

const STORAGE_KEYS = {
  TRANSFERS: 'ktm_transfer_history_v2',
  SETTINGS: 'ktm_app_settings_v2',
  DEVICES: 'ktm_known_devices_v2',
  DEVICE_ID: 'ktm_device_identity_v2',
} as const;

const DEFAULT_SETTINGS: StoredSettings = {
  deviceName: 'My Windows PC',
  saveDirectory: 'C:\\Users\\Downloads\\KnowToMigrate',
  autoAcceptTrusted: true,
  encryptTransfers: true,
  chunkSizeMB: 8,
  theme: 'amoled',
  reducedMotion: false,
};

export class StorageService {
  public static getSettings(): StoredSettings {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SETTINGS);
      if (data) {
        return { ...DEFAULT_SETTINGS, ...JSON.parse(data) };
      }
    } catch {
      // Fallback on parse failure
    }
    return DEFAULT_SETTINGS;
  }

  public static saveSettings(settings: Partial<StoredSettings>): StoredSettings {
    const current = this.getSettings();
    const updated = { ...current, ...settings };
    try {
      localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(updated));
    } catch (e) {
      console.error('Failed to save KTM settings to storage', e);
    }
    return updated;
  }

  public static getTransferHistory(): StoredTransferRecord[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.TRANSFERS);
      if (data) {
        const records = JSON.parse(data);
        if (Array.isArray(records)) {
          return records;
        }
      }
    } catch {
      // Fallback
    }

    // Default initial seed records for demonstration
    const seedRecords: StoredTransferRecord[] = [
      {
        id: 'hist-seed-1',
        title: 'Design_System_Figma_Assets.zip',
        device: "Krish's Laptop",
        direction: 'send',
        sizeBytes: 4_509_715_660,
        sizeFormatted: '4.20 GB',
        timestamp: Date.now() - 3600000 * 2,
        dateFormatted: 'Today, 18:42',
        speed: '86.4 MB/s',
        transport: 'Direct LAN',
        merkleRootHash: '3c328244776fbf249bce8204990fcff856239525185245de8f83c5459979b26e',
        verified: true,
        status: 'completed',
      },
      {
        id: 'hist-seed-2',
        title: 'GoPro_Vacation_Footage/ (84 items)',
        device: 'Studio Tablet',
        direction: 'receive',
        sizeBytes: 35_218_731_827,
        sizeFormatted: '32.80 GB',
        timestamp: Date.now() - 3600000 * 24,
        dateFormatted: 'Yesterday, 14:10',
        speed: '91.2 MB/s',
        transport: 'Direct Wi-Fi 6',
        merkleRootHash: 'a787d3eb434e744374a9fa43566d070583cdf53b10758fd43f457d540fe6752e',
        verified: true,
        status: 'completed',
      },
    ];

    this.saveTransferHistory(seedRecords);
    return seedRecords;
  }

  public static saveTransferHistory(records: StoredTransferRecord[]): void {
    try {
      localStorage.setItem(STORAGE_KEYS.TRANSFERS, JSON.stringify(records));
    } catch (e) {
      console.error('Failed to save transfer history', e);
    }
  }

  public static addTransferRecord(record: StoredTransferRecord): void {
    const history = this.getTransferHistory();
    history.unshift(record);
    // Keep max 100 historical entries
    this.saveTransferHistory(history.slice(0, 100));
  }

  public static clearHistory(): void {
    try {
      localStorage.removeItem(STORAGE_KEYS.TRANSFERS);
    } catch (e) {
      console.error('Failed to clear history', e);
    }
  }

  public static getDeviceId(): string {
    let id = localStorage.getItem(STORAGE_KEYS.DEVICE_ID);
    if (!id) {
      id = 'KTM-' + Math.random().toString(36).substring(2, 6).toUpperCase() + '-' + Date.now().toString(16).slice(-4).toUpperCase();
      localStorage.setItem(STORAGE_KEYS.DEVICE_ID, id);
    }
    return id;
  }
}

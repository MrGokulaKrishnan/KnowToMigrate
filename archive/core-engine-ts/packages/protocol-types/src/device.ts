/**
 * KnowToMigrate - Device Models & Interfaces
 */

export type PlatformType = 'android' | 'windows' | 'macos' | 'linux' | 'web' | 'ios';

export type DeviceType = 'phone' | 'desktop' | 'laptop' | 'tablet' | 'browser';

export type DeviceStatus = 
  | 'offline'
  | 'discovering'
  | 'discovered'
  | 'connecting'
  | 'authenticating'
  | 'authenticated'
  | 'ready'
  | 'transferring'
  | 'paused'
  | 'verifying'
  | 'completed'
  | 'error';

export type VisibilityMode = 
  | 'nobody' 
  | 'trusted_only' 
  | 'contacts' 
  | 'everyone_temporary' 
  | 'everyone';

export interface DeviceInfo {
  id: string;                      // Unique cryptographic device identifier
  name: string;                    // User-friendly display name (e.g. "Krish's Phone")
  platform: PlatformType;          // Target OS
  deviceType: DeviceType;          // Hardware category
  model?: string;                  // Specific hardware model (e.g. "Pixel 8 Pro")
  publicKey: string;               // Ephemeral or identity public key (hex)
  address?: string;                // IP address on local subnet
  port?: number;                   // TCP listening port
  status: DeviceStatus;
  connectionQuality: number;       // 0.0 to 1.0
  isTrusted: boolean;              // Auto-accept transfers
  autoAccept: boolean;
  visibility: VisibilityMode;
  visibilityExpiresAt?: number;    // Epoch ms for temporary visibility
  lastSeen: number;                // Epoch ms
  capabilities: string[];          // e.g. ["chunking_v2", "webrtc", "aes_gcm", "migration_wizard"]
}

export interface TrustedDeviceRecord {
  deviceId: string;
  name: string;
  platform: PlatformType;
  publicKey: string;
  trustedAt: number;
  autoAcceptFiles: boolean;
  autoAcceptFolders: boolean;
  autoAcceptClipboard: boolean;
  blocked: boolean;
}

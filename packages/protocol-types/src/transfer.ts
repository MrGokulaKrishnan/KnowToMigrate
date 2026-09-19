/**
 * KnowToMigrate - Transfer, Chunking & Checkpoint Models
 */

export type ConnectionTransport = 'direct_lan' | 'direct_internet_p2p' | 'encrypted_relay' | 'mock_simulation';

export type SessionState = 
  | 'idle'
  | 'preparing'
  | 'connecting'
  | 'authenticating'
  | 'preflight'
  | 'streaming'
  | 'paused'
  | 'verifying'
  | 'completed'
  | 'failed'
  | 'cancelled';

export interface ChunkInfo {
  index: number;
  offset: number;
  length: number;
  sha256: string;
  verified: boolean;
  attempts: number;
}

export interface FileItemManifest {
  index: number;
  relativePath: string;       // Normalized path e.g. "Photos/2026/IMG_001.JPG"
  absolutePath?: string;      // Source filesystem path
  size: number;               // Bytes
  sha256: string;             // Full file SHA-256
  isDir: boolean;
  mimeType?: string;
  mtime: number;              // Modified timestamp
  mode?: number;              // File permissions
  chunks: ChunkInfo[];
  status: 'pending' | 'transferring' | 'verified' | 'failed' | 'skipped';
}

export interface TransferProgress {
  sessionId: string;
  state: SessionState;
  transport: ConnectionTransport;
  currentFileIndex: number;
  currentFileName: string;
  currentFileProgress: number; // 0.0 to 1.0
  totalFiles: number;
  completedFiles: number;
  totalBytes: number;
  transferredBytes: number;
  speedBytesPerSec: number;
  speedMbps: number;
  etaSeconds: number;
  chunksTotal: number;
  chunksVerified: number;
  failedChunks: number;
}

export interface TransferCheckpoint {
  sessionId: string;
  sourceDeviceId: string;
  targetDeviceId: string;
  createdAt: number;
  updatedAt: number;
  totalBytes: number;
  transferredBytes: number;
  files: {
    index: number;
    relativePath: string;
    verifiedChunks: number[]; // Array of chunk indices successfully written & verified
  }[];
  isComplete: boolean;
}

export interface VerificationReport {
  sessionId: string;
  allVerified: boolean;
  totalFiles: number;
  verifiedFiles: number;
  failedFiles: { path: string; expectedHash: string; actualHash: string }[];
  merkleRootHash: string;
  completedAt: number;
}

export interface TransferHistoryRecord {
  id: string;
  timestamp: number;
  sourceDevice: string;
  targetDevice: string;
  direction: 'send' | 'receive';
  fileCount: number;
  totalBytes: number;
  status: 'completed' | 'failed' | 'cancelled' | 'interrupted';
  durationSeconds: number;
  averageSpeedMbps: number;
  transport: ConnectionTransport;
  verified: boolean;
  filesPreview: string[];
}

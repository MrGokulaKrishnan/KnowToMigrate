/**
 * KnowToMigrate - Protocol Message Definitions & Framing
 */

export const KTM_MAGIC = 'KTM2';
export const KTM_PROTOCOL_VERSION = 2;
export const DEFAULT_CHUNK_SIZE = 8 * 1024 * 1024; // 8 MB default chunk size

export enum MessageType {
  // Discovery & Handshake
  BEACON = 0x01,
  HELLO = 0x02,
  HELLO_ACK = 0x03,
  AUTH_CHALLENGE = 0x04,
  AUTH_RESPONSE = 0x05,

  // Pre-flight & Manifest
  PREFLIGHT_REQ = 0x10,
  PREFLIGHT_RESP = 0x11,
  MANIFEST_HEADER = 0x12,
  FILE_METADATA = 0x13,
  MANIFEST_COMPLETE = 0x14,

  // Streaming Data Chunks
  CHUNK_REQ = 0x20,
  CHUNK_DATA = 0x21,
  CHUNK_ACK = 0x22,

  // Session Control
  SESSION_PAUSE = 0x30,
  SESSION_RESUME = 0x31,
  SESSION_CANCEL = 0x32,

  // Verification & Completion
  FILE_COMPLETE = 0x40,
  TRANSFER_VERIFY = 0x41,
  TRANSFER_COMPLETE = 0x42,

  // Diagnostics & Error
  HEARTBEAT = 0x50,
  ERROR = 0xFF
}

export interface KTMHeader {
  magic: string;            // Must be "KTM2"
  version: number;          // Protocol version (2)
  type: MessageType;        // Message identifier
  seq: number;              // Monotonic sequence number
  sessionId: string;        // 32-char hex session token
  payloadLength: number;    // Length of the following encrypted or raw payload
  crc32: number;            // Header + payload checksum
}

export interface HelloPayload {
  protocolVersion: number;
  deviceId: string;
  deviceName: string;
  platform: string;
  ephemeralPublicKey: string; // X25519 public key in hex
  preferredChunkSize: number;
  capabilities: string[];
}

export interface PreflightReqPayload {
  sessionId: string;
  fileCount: number;
  totalBytes: number;
  isMigration: boolean;
  folderStructurePreserved: boolean;
  manifestHash: string; // SHA-256 of full manifest JSON
}

export interface PreflightRespPayload {
  sessionId: string;
  accepted: boolean;
  rejectionReason?: 'user_declined' | 'storage_full' | 'unauthorized' | 'rate_limited';
  destinationAvailableBytes: number;
  savePath?: string;
}

export interface ChunkDataHeader {
  sessionId: string;
  fileIndex: number;
  chunkIndex: number;
  offset: number;
  length: number;
  sha256: string; // SHA-256 of this 8MB chunk
}

export interface ChunkAckPayload {
  sessionId: string;
  fileIndex: number;
  chunkIndex: number;
  verified: boolean;
  error?: string;
}

export interface SessionResumePayload {
  sessionId: string;
  verifiedChunkBitset: string; // Compressed bitset or index list
}

export interface TransferErrorPayload {
  code: number;
  message: string;
  recoverable: boolean;
}

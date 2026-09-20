//! JSON-serializable payload structs for every KTM v2 message type.
//!
//! Each struct maps 1:1 to a [`crate::packet::PacketType`] variant and is
//! serialised with `serde_json` before being placed into the
//! [`crate::packet::KtmPacket`] payload field.

use serde::{Deserialize, Serialize};

// ---------------------------------------------------------------------------
// DeviceInfo  (Discovery / PairRequest)
// ---------------------------------------------------------------------------

/// Information broadcast by a device during discovery and pairing.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct DeviceInfo {
    /// Stable device UUID (generated once, persisted to `~/.ktm/identity.json`).
    pub device_id: String,
    /// Human-readable device name (e.g. "Rahul's iPhone").
    pub name: String,
    /// High-level platform: `"android"`, `"ios"`, `"windows"`, `"macos"`, `"linux"`.
    pub platform: String,
    /// OS version string (e.g. `"Android 14"`, `"Windows 11 22H2"`).
    pub os_version: String,
    /// KnowToMigrate application version (e.g. `"1.0.0"`).
    pub app_version: String,
    /// TCP port on which the transfer server is listening.
    pub transfer_port: u16,
    /// Optional list of feature flags this device supports.
    /// Known flags: `"aes256gcm"`, `"resume"`, `"compression"`.
    #[serde(default)]
    pub features: Vec<String>,
}

// ---------------------------------------------------------------------------
// TransferManifest  (TransferManifest)
// ---------------------------------------------------------------------------

/// Sent by the sender before starting a transfer; describes all files.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct TransferManifest {
    /// Unique transfer ID (UUID v4).
    pub transfer_id: String,
    /// Ordered list of files to be transferred.
    pub files: Vec<FileEntry>,
    /// Sum of all file sizes in bytes.
    pub total_size: u64,
    /// Number of files (convenience field — equals `files.len()`).
    pub total_files: u32,
    /// Compression algorithm in use, e.g. `"none"`, `"lz4"`, `"zstd"`.
    pub compression: String,
}

/// Metadata for a single file in a [`TransferManifest`].
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct FileEntry {
    /// Relative path inside the transfer (POSIX separators, no leading `/`).
    pub path: String,
    /// File size in bytes.
    pub size: u64,
    /// Lower-case SHA-256 hex digest of the complete file.
    pub sha256: String,
    /// Last-modified timestamp (Unix seconds, UTC).
    pub modified_at: i64,
}

// ---------------------------------------------------------------------------
// ChunkData header  (ChunkData)
// ---------------------------------------------------------------------------

/// Binary prefix embedded at the start of each `ChunkData` payload.
///
/// Layout: `transfer_id_len (2B LE) || transfer_id_bytes || chunk_header_json || \n || chunk_data`
///
/// For simplicity the chunk header is encoded as a JSON struct followed by a
/// newline, then the raw chunk bytes.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChunkHeader {
    /// Transfer this chunk belongs to.
    pub transfer_id: String,
    /// Index of the file within the [`TransferManifest::files`] list.
    pub file_index: u32,
    /// Zero-based chunk index within the file.
    pub chunk_index: u64,
    /// Byte offset of this chunk within the file.
    pub offset: u64,
    /// Length of the raw chunk data that follows (bytes).
    pub data_len: u32,
    /// Lower-case SHA-256 hex digest of this chunk's raw data.
    pub sha256: String,
    /// `true` if this is the final chunk of the file.
    pub is_last: bool,
}

// ---------------------------------------------------------------------------
// ChunkAck  (ChunkAck)
// ---------------------------------------------------------------------------

/// Sent by the receiver to acknowledge (or NAK) a single chunk.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct ChunkAck {
    /// Transfer this acknowledgement refers to.
    pub transfer_id: String,
    /// Index of the file within the manifest.
    pub file_index: u32,
    /// Chunk index being acknowledged.
    pub chunk_index: u64,
    /// `true` if the chunk was received and verified; `false` if it should be
    /// retransmitted.
    pub received: bool,
    /// Optional reason for NAK (e.g. `"sha256_mismatch"`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

// ---------------------------------------------------------------------------
// TransferComplete  (TransferComplete)
// ---------------------------------------------------------------------------

/// Sent by the sender once all chunks have been transmitted.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct TransferComplete {
    /// Transfer ID.
    pub transfer_id: String,
    /// Hex-encoded Merkle root of all chunk SHA-256 hashes.
    pub merkle_root: String,
    /// Number of files for which the receiver confirmed all chunks.
    pub files_verified: u32,
    /// Total bytes transferred (uncompressed).
    pub total_bytes: u64,
}

// ---------------------------------------------------------------------------
// ResumeRequest  (ResumeRequest)
// ---------------------------------------------------------------------------

/// Sent by the receiver to resume an interrupted transfer.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct ResumeRequest {
    /// Transfer to resume.
    pub transfer_id: String,
    /// Flat list of (file_index << 32 | chunk_index) tuples for chunks that
    /// have already been received and verified — the sender skips these.
    pub received_chunks: Vec<u64>,
}

// ---------------------------------------------------------------------------
// CancelMessage  (Cancel)
// ---------------------------------------------------------------------------

/// Sent by either peer to abort a transfer.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct CancelMessage {
    /// Transfer being cancelled, or `""` to cancel all.
    pub transfer_id: String,
    /// Human-readable reason (for logging only).
    pub reason: String,
}

// ---------------------------------------------------------------------------
// HeartbeatMessage  (Heartbeat)
// ---------------------------------------------------------------------------

/// Periodic keepalive packet.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct HeartbeatMessage {
    /// Unix timestamp (seconds) when this heartbeat was sent.
    pub timestamp: i64,
}

// ---------------------------------------------------------------------------
// Serialisation helpers
// ---------------------------------------------------------------------------

impl DeviceInfo {
    pub fn to_json(&self) -> serde_json::Result<Vec<u8>> {
        serde_json::to_vec(self)
    }
    pub fn from_json(data: &[u8]) -> serde_json::Result<Self> {
        serde_json::from_slice(data)
    }
}

impl TransferManifest {
    pub fn to_json(&self) -> serde_json::Result<Vec<u8>> {
        serde_json::to_vec(self)
    }
    pub fn from_json(data: &[u8]) -> serde_json::Result<Self> {
        serde_json::from_slice(data)
    }
}

impl ChunkAck {
    pub fn to_json(&self) -> serde_json::Result<Vec<u8>> {
        serde_json::to_vec(self)
    }
    pub fn from_json(data: &[u8]) -> serde_json::Result<Self> {
        serde_json::from_slice(data)
    }
}

impl TransferComplete {
    pub fn to_json(&self) -> serde_json::Result<Vec<u8>> {
        serde_json::to_vec(self)
    }
    pub fn from_json(data: &[u8]) -> serde_json::Result<Self> {
        serde_json::from_slice(data)
    }
}

impl ResumeRequest {
    pub fn to_json(&self) -> serde_json::Result<Vec<u8>> {
        serde_json::to_vec(self)
    }
    pub fn from_json(data: &[u8]) -> serde_json::Result<Self> {
        serde_json::from_slice(data)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn device_info_round_trip() {
        let di = DeviceInfo {
            device_id: "uuid-1234".into(),
            name: "Test Phone".into(),
            platform: "android".into(),
            os_version: "Android 14".into(),
            app_version: "1.0.0".into(),
            transfer_port: 54124,
            features: vec!["aes256gcm".into(), "resume".into()],
        };
        let json = di.to_json().unwrap();
        let di2 = DeviceInfo::from_json(&json).unwrap();
        assert_eq!(di, di2);
    }

    #[test]
    fn transfer_manifest_round_trip() {
        let m = TransferManifest {
            transfer_id: "tid-abc".into(),
            files: vec![FileEntry {
                path: "photos/img.jpg".into(),
                size: 1024,
                sha256: "a".repeat(64),
                modified_at: 1_700_000_000,
            }],
            total_size: 1024,
            total_files: 1,
            compression: "none".into(),
        };
        let json = m.to_json().unwrap();
        let m2 = TransferManifest::from_json(&json).unwrap();
        assert_eq!(m, m2);
    }
}

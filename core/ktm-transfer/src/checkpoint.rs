//! Resume checkpoint manager.
//!
//! Persists a JSON checkpoint file in the destination directory so that an
//! interrupted transfer can be resumed without re-sending already-verified
//! chunks.
//!
//! Checkpoint file location: `{dest_dir}/.ktm-checkpoint-{transfer_id}.json`
//!
//! The checkpoint records:
//! - `transfer_id` — unique transfer UUID.
//! - `received_chunks` — a sorted list of `(file_index, chunk_index)` pairs
//!   that have been received **and** SHA-256 verified.

use serde::{Deserialize, Serialize};
use std::collections::HashSet;
use std::io;
use std::path::{Path, PathBuf};

// ---------------------------------------------------------------------------
// On-disk format
// ---------------------------------------------------------------------------

/// Opaque chunk key stored in the checkpoint.
///
/// Encodes `(file_index u32, chunk_index u64)` as a single `u64` using
/// `(file_index as u64) << 32 | chunk_index`.  This matches the encoding used
/// in [`ktm_protocol::messages::ResumeRequest::received_chunks`].
type ChunkKey = u64;

fn make_key(file_index: u32, chunk_index: u64) -> ChunkKey {
    ((file_index as u64) << 32) | (chunk_index & 0xFFFF_FFFF)
}

#[derive(Debug, Serialize, Deserialize)]
struct CheckpointFile {
    transfer_id: String,
    /// Sorted list of encoded chunk keys.
    received_chunks: Vec<ChunkKey>,
}

// ---------------------------------------------------------------------------
// CheckpointManager
// ---------------------------------------------------------------------------

/// Manages the checkpoint file for a single transfer.
pub struct CheckpointManager {
    transfer_id: String,
    path: PathBuf,
    received: HashSet<ChunkKey>,
}

impl CheckpointManager {
    /// Create (or reload) the checkpoint for `transfer_id` in `dest_dir`.
    pub fn new(dest_dir: &Path, transfer_id: &str) -> io::Result<Self> {
        let path = checkpoint_path(dest_dir, transfer_id);
        let received = if path.exists() {
            load_checkpoint(&path)?
        } else {
            HashSet::new()
        };
        Ok(Self {
            transfer_id: transfer_id.to_owned(),
            path,
            received,
        })
    }

    /// Mark a chunk as received and persist the checkpoint to disk.
    pub fn mark_chunk_received(&mut self, file_index: u32, chunk_index: u64) -> io::Result<()> {
        self.received.insert(make_key(file_index, chunk_index));
        self.flush()
    }

    /// Return `true` if the chunk has already been received and verified.
    pub fn is_chunk_received(&self, file_index: u32, chunk_index: u64) -> bool {
        self.received.contains(&make_key(file_index, chunk_index))
    }

    /// Return a list of missing chunk keys for the given total chunk count.
    ///
    /// `chunks_per_file` maps `file_index → total_chunks_for_that_file`.
    pub fn get_missing_chunks(&self, chunks_per_file: &[(u32, u64)]) -> Vec<u64> {
        let mut missing = Vec::new();
        for &(file_index, total_chunks) in chunks_per_file {
            for chunk_index in 0..total_chunks {
                if !self.is_chunk_received(file_index, chunk_index) {
                    missing.push(make_key(file_index, chunk_index));
                }
            }
        }
        missing
    }

    /// Return all received chunk keys (for sending in a [`ResumeRequest`]).
    pub fn received_chunks(&self) -> Vec<u64> {
        let mut v: Vec<u64> = self.received.iter().copied().collect();
        v.sort_unstable();
        v
    }

    /// Delete the checkpoint file (call on successful transfer completion).
    pub fn delete_checkpoint(&self) -> io::Result<()> {
        if self.path.exists() {
            std::fs::remove_file(&self.path)?;
        }
        Ok(())
    }

    /// Flush the current in-memory state to disk atomically.
    fn flush(&self) -> io::Result<()> {
        let mut sorted: Vec<ChunkKey> = self.received.iter().copied().collect();
        sorted.sort_unstable();

        let file = CheckpointFile {
            transfer_id: self.transfer_id.clone(),
            received_chunks: sorted,
        };

        let json = serde_json::to_vec_pretty(&file)
            .map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e))?;

        // Atomic write via temp file + rename.
        let tmp = self.path.with_extension("json.tmp");
        std::fs::write(&tmp, &json)?;
        std::fs::rename(&tmp, &self.path)?;
        Ok(())
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

fn checkpoint_path(dest_dir: &Path, transfer_id: &str) -> PathBuf {
    dest_dir.join(format!(".ktm-checkpoint-{transfer_id}.json"))
}

fn load_checkpoint(path: &Path) -> io::Result<HashSet<ChunkKey>> {
    let data = std::fs::read(path)?;
    let file: CheckpointFile = serde_json::from_slice(&data)
        .map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e))?;
    Ok(file.received_chunks.into_iter().collect())
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use std::env;

    fn tmp_dir() -> PathBuf {
        env::temp_dir()
    }

    #[test]
    fn mark_and_check_chunk() {
        let dir = tmp_dir();
        let tid = format!("tid-{}", std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH).unwrap().subsec_nanos());
        let mut mgr = CheckpointManager::new(&dir, &tid).unwrap();
        assert!(!mgr.is_chunk_received(0, 5));
        mgr.mark_chunk_received(0, 5).unwrap();
        assert!(mgr.is_chunk_received(0, 5));
        assert!(!mgr.is_chunk_received(0, 6));
        mgr.delete_checkpoint().unwrap();
    }

    #[test]
    fn persists_across_reload() {
        let dir = tmp_dir();
        let tid = format!("tid-reload-{}", std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH).unwrap().subsec_nanos());
        {
            let mut mgr = CheckpointManager::new(&dir, &tid).unwrap();
            mgr.mark_chunk_received(0, 0).unwrap();
            mgr.mark_chunk_received(0, 1).unwrap();
            mgr.mark_chunk_received(1, 0).unwrap();
        }
        let mgr = CheckpointManager::new(&dir, &tid).unwrap();
        assert!(mgr.is_chunk_received(0, 0));
        assert!(mgr.is_chunk_received(0, 1));
        assert!(mgr.is_chunk_received(1, 0));
        assert!(!mgr.is_chunk_received(1, 1));
        mgr.delete_checkpoint().unwrap();
    }

    #[test]
    fn missing_chunks_reported_correctly() {
        let dir = tmp_dir();
        let tid = format!("tid-miss-{}", std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH).unwrap().subsec_nanos());
        let mut mgr = CheckpointManager::new(&dir, &tid).unwrap();
        mgr.mark_chunk_received(0, 0).unwrap();
        // File 0 has 3 chunks, file 1 has 2 chunks.
        let missing = mgr.get_missing_chunks(&[(0, 3), (1, 2)]);
        // Expect (0,1), (0,2), (1,0), (1,1) — 4 missing.
        assert_eq!(missing.len(), 4);
        mgr.delete_checkpoint().unwrap();
    }
}

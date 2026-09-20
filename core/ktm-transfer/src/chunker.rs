//! Streaming 8 MiB file chunker.
//!
//! Reads a file in fixed-size chunks without loading the entire file into
//! memory.  Each [`Chunk`] carries its data, SHA-256 hash, byte offset within
//! the file, and a flag indicating whether it is the last chunk.

use sha2::{Digest, Sha256};
use std::fs::File;
use std::io::{self, BufReader, Read};
use std::path::Path;

/// Default chunk size: 8 MiB.
pub const CHUNK_SIZE: usize = 8 * 1024 * 1024;

// ---------------------------------------------------------------------------
// Chunk
// ---------------------------------------------------------------------------

/// A single chunk of file data produced by [`FileChunker`].
#[derive(Debug, Clone)]
pub struct Chunk {
    /// Zero-based chunk index within the file.
    pub index: u64,
    /// Raw chunk bytes.
    pub data: Vec<u8>,
    /// SHA-256 digest of `data`.
    pub hash: [u8; 32],
    /// Byte offset of this chunk's first byte within the source file.
    pub offset: u64,
    /// `true` if this is the last chunk of the file (may be smaller than `CHUNK_SIZE`).
    pub is_last: bool,
}

impl Chunk {
    /// Return the hash as a lower-case hex string.
    pub fn hash_hex(&self) -> String {
        self.hash.iter().map(|b| format!("{b:02x}")).collect()
    }
}

// ---------------------------------------------------------------------------
// FileChunker — iterator
// ---------------------------------------------------------------------------

/// An iterator that yields [`Chunk`]s for a single file.
///
/// Uses a [`BufReader`] so the OS page cache is not bypassed and I/O is
/// efficient even on low-memory devices.
pub struct FileChunker {
    reader: BufReader<File>,
    index: u64,
    offset: u64,
    file_size: u64,
    chunk_size: usize,
    exhausted: bool,
}

impl FileChunker {
    /// Open `path` and prepare to yield chunks of `chunk_size` bytes.
    ///
    /// Pass [`CHUNK_SIZE`] for `chunk_size` in production.
    pub fn open(path: &Path, chunk_size: usize) -> io::Result<Self> {
        let file = File::open(path)?;
        let file_size = file.metadata()?.len();
        let reader = BufReader::with_capacity(chunk_size, file);
        Ok(Self {
            reader,
            index: 0,
            offset: 0,
            file_size,
            chunk_size,
            exhausted: false,
        })
    }

    /// Total file size in bytes.
    pub fn file_size(&self) -> u64 {
        self.file_size
    }

    /// Total number of chunks that will be produced (rounded up).
    pub fn total_chunks(&self) -> u64 {
        if self.file_size == 0 {
            1
        } else {
            (self.file_size + self.chunk_size as u64 - 1) / self.chunk_size as u64
        }
    }
}

impl Iterator for FileChunker {
    type Item = io::Result<Chunk>;

    fn next(&mut self) -> Option<Self::Item> {
        if self.exhausted {
            return None;
        }

        let mut buf = vec![0u8; self.chunk_size];
        let mut bytes_read = 0usize;

        // Fill buf up to chunk_size bytes, tolerating short reads from the OS.
        while bytes_read < self.chunk_size {
            match self.reader.read(&mut buf[bytes_read..]) {
                Ok(0) => break, // EOF
                Ok(n) => bytes_read += n,
                Err(e) if e.kind() == io::ErrorKind::Interrupted => continue,
                Err(e) => return Some(Err(e)),
            }
        }

        if bytes_read == 0 {
            // Truly at EOF and we haven't emitted a "last" chunk yet —
            // this can happen for empty files.
            self.exhausted = true;
            let hash = Sha256::digest(&[] as &[u8]).into();
            return Some(Ok(Chunk {
                index: self.index,
                data: vec![],
                hash,
                offset: self.offset,
                is_last: true,
            }));
        }

        buf.truncate(bytes_read);
        let hash: [u8; 32] = Sha256::digest(&buf).into();
        let offset = self.offset;
        let index = self.index;

        self.index += 1;
        self.offset += bytes_read as u64;

        let is_last = self.offset >= self.file_size || bytes_read < self.chunk_size;
        if is_last {
            self.exhausted = true;
        }

        Some(Ok(Chunk {
            index,
            data: buf,
            hash,
            offset,
            is_last,
        }))
    }
}

// ---------------------------------------------------------------------------
// Async streaming helper
// ---------------------------------------------------------------------------

/// Read all chunks from `path` and call `callback` for each one.
///
/// Suitable for async callers that want to yield between chunks via
/// `tokio::task::spawn_blocking`.
pub async fn stream_chunks<F>(path: &Path, chunk_size: usize, mut callback: F) -> io::Result<()>
where
    F: FnMut(Chunk) -> io::Result<()> + Send + 'static,
{
    let path = path.to_path_buf();
    tokio::task::spawn_blocking(move || {
        let chunker = FileChunker::open(&path, chunk_size)?;
        for result in chunker {
            let chunk = result?;
            callback(chunk)?;
        }
        Ok(())
    })
    .await
    .map_err(|e| io::Error::new(io::ErrorKind::Other, e))?
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use tempfile::NamedTempFile; // Not a dep — test uses std tmp

    fn write_temp(data: &[u8]) -> std::path::PathBuf {
        let mut path = std::env::temp_dir();
        path.push(format!("ktm_chunker_test_{}", data.len()));
        std::fs::write(&path, data).unwrap();
        path
    }

    #[test]
    fn empty_file_yields_one_empty_chunk() {
        let path = write_temp(b"");
        let chunker = FileChunker::open(&path, CHUNK_SIZE).unwrap();
        let chunks: Vec<_> = chunker.map(|r| r.unwrap()).collect();
        assert_eq!(chunks.len(), 1);
        assert!(chunks[0].data.is_empty());
        assert!(chunks[0].is_last);
        let _ = std::fs::remove_file(path);
    }

    #[test]
    fn small_file_is_single_chunk() {
        let data = b"Hello, World!";
        let path = write_temp(data);
        let chunker = FileChunker::open(&path, CHUNK_SIZE).unwrap();
        let chunks: Vec<_> = chunker.map(|r| r.unwrap()).collect();
        assert_eq!(chunks.len(), 1);
        assert_eq!(&chunks[0].data, data);
        assert!(chunks[0].is_last);
        let _ = std::fs::remove_file(path);
    }

    #[test]
    fn exact_multiple_of_chunk_size() {
        let chunk_size = 64usize;
        let data = vec![0xAB_u8; chunk_size * 3];
        let path = write_temp(&data);
        let chunker = FileChunker::open(&path, chunk_size).unwrap();
        let chunks: Vec<_> = chunker.map(|r| r.unwrap()).collect();
        assert_eq!(chunks.len(), 3);
        assert!(!chunks[0].is_last);
        assert!(!chunks[1].is_last);
        assert!(chunks[2].is_last);
        for (i, c) in chunks.iter().enumerate() {
            assert_eq!(c.index, i as u64);
            assert_eq!(c.offset, (i * chunk_size) as u64);
            assert_eq!(c.data.len(), chunk_size);
        }
        let _ = std::fs::remove_file(path);
    }

    #[test]
    fn hash_is_sha256_of_data() {
        let data = b"KTM chunk data";
        let path = write_temp(data);
        let chunker = FileChunker::open(&path, CHUNK_SIZE).unwrap();
        let chunk = chunker.map(|r| r.unwrap()).next().unwrap();
        let expected: [u8; 32] = sha2::Sha256::digest(data).into();
        assert_eq!(chunk.hash, expected);
        let _ = std::fs::remove_file(path);
    }
}

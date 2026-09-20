//! SHA-256 Merkle tree.
//!
//! Accepts a list of 32-byte leaf hashes (one per file chunk) and builds a
//! binary Merkle tree.  Odd layers are padded by duplicating the last node,
//! matching Bitcoin's construction.
//!
//! # Example
//! ```
//! use ktm_crypto::merkle::MerkleTree;
//!
//! let chunk_hashes: Vec<[u8; 32]> = (0u8..4)
//!     .map(|i| {
//!         let mut h = [0u8; 32];
//!         h[0] = i;
//!         h
//!     })
//!     .collect();
//!
//! let tree = MerkleTree::new(chunk_hashes);
//! let root = tree.root();
//! // verify a leaf is present at the expected index
//! let mut leaf = [0u8; 32];
//! leaf[0] = 2;
//! assert!(tree.verify_chunk(&leaf, 2).is_ok());
//! ```

use sha2::{Digest, Sha256};
use std::fmt;

/// Error returned when [`MerkleTree::verify_chunk`] fails.
#[derive(Debug, PartialEq, Eq)]
pub enum MerkleVerifyError {
    /// The supplied chunk index is out of bounds.
    IndexOutOfBounds { index: usize, len: usize },
    /// The stored leaf hash does not match the supplied chunk hash.
    HashMismatch,
    /// The tree was built with zero leaves (no chunks).
    EmptyTree,
}

impl fmt::Display for MerkleVerifyError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::IndexOutOfBounds { index, len } => {
                write!(f, "Chunk index {index} out of bounds (tree has {len} leaves)")
            }
            Self::HashMismatch => write!(f, "Chunk hash does not match stored Merkle leaf"),
            Self::EmptyTree => write!(f, "Merkle tree contains no leaves"),
        }
    }
}

impl std::error::Error for MerkleVerifyError {}

/// A complete binary Merkle tree over SHA-256 chunk hashes.
pub struct MerkleTree {
    /// All tree nodes in level-order: leaves are at the end.
    /// Index 0 = root (when there is more than one node).
    /// For a single leaf, `nodes[0]` is simultaneously leaf and root.
    nodes: Vec<[u8; 32]>,
    /// Number of original leaves supplied by the caller.
    leaf_count: usize,
}

impl MerkleTree {
    /// Build a Merkle tree from a slice of 32-byte chunk hashes.
    ///
    /// Panics if `leaves` is empty — callers must ensure at least one chunk.
    pub fn new(leaves: Vec<[u8; 32]>) -> Self {
        assert!(!leaves.is_empty(), "MerkleTree requires at least one leaf");
        let leaf_count = leaves.len();
        let nodes = build_tree(leaves);
        Self { nodes, leaf_count }
    }

    /// Return the 32-byte Merkle root hash.
    pub fn root(&self) -> [u8; 32] {
        self.nodes[0]
    }

    /// Return the Merkle root as a lower-case hex string (64 chars).
    pub fn root_hex(&self) -> String {
        hex_encode(&self.root())
    }

    /// Verify that the given `chunk_hash` matches the leaf at `chunk_index`.
    pub fn verify_chunk(
        &self,
        chunk_hash: &[u8; 32],
        chunk_index: usize,
    ) -> Result<(), MerkleVerifyError> {
        if self.leaf_count == 0 {
            return Err(MerkleVerifyError::EmptyTree);
        }
        if chunk_index >= self.leaf_count {
            return Err(MerkleVerifyError::IndexOutOfBounds {
                index: chunk_index,
                len: self.leaf_count,
            });
        }

        // Locate the leaf node.  The nodes Vec is built bottom-up, so leaves
        // start at the offset determined by the next power-of-two.
        let leaf_offset = leaf_start_offset(self.leaf_count);
        let stored = self.nodes[leaf_offset + chunk_index];

        if &stored == chunk_hash {
            Ok(())
        } else {
            Err(MerkleVerifyError::HashMismatch)
        }
    }

    /// Number of leaves in the tree (equals number of chunks).
    pub fn leaf_count(&self) -> usize {
        self.leaf_count
    }
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

/// Hash two 32-byte child nodes into a parent node.
fn hash_pair(left: &[u8; 32], right: &[u8; 32]) -> [u8; 32] {
    let mut hasher = Sha256::new();
    hasher.update(left);
    hasher.update(right);
    hasher.finalize().into()
}

/// Find the smallest power of two ≥ n.
fn next_power_of_two(n: usize) -> usize {
    if n == 0 {
        return 1;
    }
    let mut p = 1usize;
    while p < n {
        p <<= 1;
    }
    p
}

/// Return the index of the first leaf in a level-order tree whose capacity is
/// `next_power_of_two(leaf_count)`.
fn leaf_start_offset(leaf_count: usize) -> usize {
    next_power_of_two(leaf_count) - 1
}

/// Build the full node array in level-order (root at index 0).
fn build_tree(leaves: Vec<[u8; 32]>) -> Vec<[u8; 32]> {
    let capacity = next_power_of_two(leaves.len());
    // Total nodes in a perfect binary tree of depth log2(capacity).
    let total = 2 * capacity - 1;
    let mut nodes = vec![[0u8; 32]; total];

    // Place leaves (pad with duplicates if needed).
    let offset = capacity - 1; // == leaf_start_offset(leaves.len()) when len == capacity
    for (i, leaf) in leaves.iter().enumerate() {
        nodes[offset + i] = *leaf;
    }
    // Pad any missing leaves by duplicating the last real leaf.
    let last = *leaves.last().unwrap();
    for i in leaves.len()..capacity {
        nodes[offset + i] = last;
    }

    // Build internal nodes bottom-up.
    // For a node at index `i`, its children are at `2*i+1` and `2*i+2`.
    if total > 1 {
        for i in (0..offset).rev() {
            let left = nodes[2 * i + 1];
            let right = nodes[2 * i + 2];
            nodes[i] = hash_pair(&left, &right);
        }
    }

    nodes
}

/// Lower-case hex encoding of a byte slice.
fn hex_encode(bytes: &[u8]) -> String {
    bytes.iter().map(|b| format!("{b:02x}")).collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use sha2::{Digest, Sha256};

    fn sha256(data: &[u8]) -> [u8; 32] {
        Sha256::digest(data).into()
    }

    #[test]
    fn single_leaf_root_equals_leaf() {
        let leaf = sha256(b"chunk0");
        let tree = MerkleTree::new(vec![leaf]);
        assert_eq!(tree.root(), leaf);
    }

    #[test]
    fn two_leaves_root_is_hash_of_pair() {
        let l0 = sha256(b"chunk0");
        let l1 = sha256(b"chunk1");
        let expected = hash_pair(&l0, &l1);
        let tree = MerkleTree::new(vec![l0, l1]);
        assert_eq!(tree.root(), expected);
    }

    #[test]
    fn verify_chunk_succeeds_for_correct_index() {
        let leaves: Vec<[u8; 32]> = (0u8..4).map(|i| sha256(&[i])).collect();
        let tree = MerkleTree::new(leaves.clone());
        for (i, leaf) in leaves.iter().enumerate() {
            assert!(tree.verify_chunk(leaf, i).is_ok());
        }
    }

    #[test]
    fn verify_chunk_fails_for_wrong_hash() {
        let leaves: Vec<[u8; 32]> = (0u8..4).map(|i| sha256(&[i])).collect();
        let tree = MerkleTree::new(leaves);
        let wrong_hash = sha256(b"wrong");
        assert_eq!(tree.verify_chunk(&wrong_hash, 0), Err(MerkleVerifyError::HashMismatch));
    }

    #[test]
    fn verify_chunk_out_of_bounds() {
        let leaves = vec![sha256(b"a"), sha256(b"b")];
        let tree = MerkleTree::new(leaves);
        let h = sha256(b"x");
        assert!(matches!(
            tree.verify_chunk(&h, 5),
            Err(MerkleVerifyError::IndexOutOfBounds { .. })
        ));
    }

    #[test]
    fn root_hex_is_64_chars() {
        let tree = MerkleTree::new(vec![sha256(b"abc")]);
        assert_eq!(tree.root_hex().len(), 64);
    }
}

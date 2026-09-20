//! ktm-crypto — Cryptographic primitives for KnowToMigrate
//!
//! Provides X25519 ECDH key agreement, AES-256-GCM authenticated encryption,
//! SHA-256 Merkle trees, and device identity management.

pub mod aead;
pub mod ecdh;
pub mod identity;
pub mod merkle;

/// Re-export commonly used types for convenience.
pub use aead::{CryptoError, KtmCipher};
pub use ecdh::KtmKeyPair;
pub use identity::{DeviceIdentity, IdentityError};
pub use merkle::{MerkleTree, MerkleVerifyError};

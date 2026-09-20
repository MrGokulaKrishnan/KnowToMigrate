//! AES-256-GCM authenticated encryption.
//!
//! Each `encrypt` call prepends a freshly generated 96-bit (12-byte) random
//! nonce so the output is self-contained.  `decrypt` expects the nonce to be
//! present at the start of the ciphertext buffer.
//!
//! Additional Authenticated Data (AAD) is bound to the ciphertext but not
//! encrypted; callers should include the session-ID or packet sequence number
//! so that packets cannot be replayed into a different context.

use aes_gcm::aead::{Aead, AeadCore, KeyInit, OsRng, Payload};
use aes_gcm::{Aes256Gcm, Key, Nonce};
use std::error::Error;
use std::fmt;

/// Errors that can occur during cryptographic operations.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CryptoError {
    /// AES-GCM encryption failed (e.g. nonce reuse would be detected at a
    /// higher level; here it typically means an internal library error).
    EncryptionFailed,
    /// Decryption or authentication-tag verification failed.
    DecryptionFailed,
    /// A key slice supplied to a constructor was not exactly 32 bytes.
    InvalidKeyLength,
    /// The ciphertext buffer was too short to contain a 12-byte nonce.
    InvalidNonceLength,
}

impl fmt::Display for CryptoError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::EncryptionFailed => write!(f, "AES-256-GCM encryption failed"),
            Self::DecryptionFailed => {
                write!(f, "AES-256-GCM decryption / tag verification failed")
            }
            Self::InvalidKeyLength => write!(f, "Key must be exactly 32 bytes"),
            Self::InvalidNonceLength => {
                write!(f, "Ciphertext buffer too short (< 12 bytes nonce)")
            }
        }
    }
}

impl Error for CryptoError {}

/// An AES-256-GCM cipher bound to a specific 256-bit key.
///
/// Instances are cheaply cloneable via [`KtmCipher::clone_key`] if the same
/// key needs to be shared across tasks.
pub struct KtmCipher {
    cipher: Aes256Gcm,
}

impl KtmCipher {
    /// Create a cipher from a 32-byte key slice.
    pub fn new(key_bytes: &[u8; 32]) -> Self {
        let key = Key::<Aes256Gcm>::from_slice(key_bytes);
        let cipher = Aes256Gcm::new(key);
        Self { cipher }
    }

    /// Create a cipher from a shared-secret produced by ECDH.
    ///
    /// Converts the raw 32-byte `SharedSecret` into a cipher key directly —
    /// it is the caller's responsibility to have agreed on proper key
    /// derivation (e.g. HKDF) for production use if multiple keys are needed.
    pub fn from_shared_secret(secret_bytes: &[u8; 32]) -> Self {
        Self::new(secret_bytes)
    }

    /// Encrypt `plaintext` with this cipher.
    ///
    /// A random 12-byte nonce is generated and prepended to the output.
    /// `aad` is authenticated but not encrypted (may be empty).
    ///
    /// Returns `nonce (12 B) || ciphertext || tag (16 B)`.
    pub fn encrypt(&self, plaintext: &[u8], aad: &[u8]) -> Result<Vec<u8>, CryptoError> {
        let nonce = Aes256Gcm::generate_nonce(&mut OsRng {});
        let ciphertext = self
            .cipher
            .encrypt(&nonce, Payload { msg: plaintext, aad })
            .map_err(|_| CryptoError::EncryptionFailed)?;

        let mut result = Vec::with_capacity(12 + ciphertext.len());
        result.extend_from_slice(&nonce);
        result.extend_from_slice(&ciphertext);
        Ok(result)
    }

    /// Decrypt a buffer produced by [`KtmCipher::encrypt`].
    ///
    /// Expects `nonce (12 B) || ciphertext || tag (16 B)` layout.
    /// `aad` must match exactly what was passed during encryption.
    pub fn decrypt(
        &self,
        ciphertext_with_nonce: &[u8],
        aad: &[u8],
    ) -> Result<Vec<u8>, CryptoError> {
        if ciphertext_with_nonce.len() < 12 {
            return Err(CryptoError::InvalidNonceLength);
        }
        let (nonce_bytes, ciphertext) = ciphertext_with_nonce.split_at(12);
        let nonce = Nonce::from_slice(nonce_bytes);
        self.cipher
            .decrypt(nonce, Payload { msg: ciphertext, aad })
            .map_err(|_| CryptoError::DecryptionFailed)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn test_key() -> [u8; 32] {
        [0xAB_u8; 32]
    }

    #[test]
    fn round_trip_no_aad() {
        let cipher = KtmCipher::new(&test_key());
        let plaintext = b"Hello, KnowToMigrate!";
        let encrypted = cipher.encrypt(plaintext, b"").unwrap();
        let decrypted = cipher.decrypt(&encrypted, b"").unwrap();
        assert_eq!(decrypted, plaintext);
    }

    #[test]
    fn round_trip_with_aad() {
        let cipher = KtmCipher::new(&test_key());
        let plaintext = b"secret payload";
        let aad = b"session:abc123";
        let encrypted = cipher.encrypt(plaintext, aad).unwrap();
        let decrypted = cipher.decrypt(&encrypted, aad).unwrap();
        assert_eq!(decrypted, plaintext);
    }

    #[test]
    fn wrong_aad_fails_decryption() {
        let cipher = KtmCipher::new(&test_key());
        let encrypted = cipher.encrypt(b"data", b"correct-aad").unwrap();
        assert_eq!(
            cipher.decrypt(&encrypted, b"wrong-aad"),
            Err(CryptoError::DecryptionFailed)
        );
    }

    #[test]
    fn tampered_ciphertext_fails() {
        let cipher = KtmCipher::new(&test_key());
        let mut encrypted = cipher.encrypt(b"data", b"").unwrap();
        // Flip a byte inside the ciphertext region (after the 12-byte nonce).
        let last = encrypted.len() - 1;
        encrypted[last] ^= 0xFF;
        assert_eq!(
            cipher.decrypt(&encrypted, b""),
            Err(CryptoError::DecryptionFailed)
        );
    }

    #[test]
    fn too_short_buffer_returns_error() {
        let cipher = KtmCipher::new(&test_key());
        assert_eq!(
            cipher.decrypt(b"short", b""),
            Err(CryptoError::InvalidNonceLength)
        );
    }

    #[test]
    fn nonces_are_unique_across_encryptions() {
        let cipher = KtmCipher::new(&test_key());
        let e1 = cipher.encrypt(b"msg", b"").unwrap();
        let e2 = cipher.encrypt(b"msg", b"").unwrap();
        // The first 12 bytes are the nonces — they must differ.
        assert_ne!(&e1[..12], &e2[..12]);
    }
}

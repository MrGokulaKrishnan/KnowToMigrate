//! X25519 Elliptic Curve Diffie-Hellman key agreement.
//!
//! Generates ephemeral keypairs and performs ECDH to produce a 32-byte shared
//! secret that is subsequently used as an AES-256-GCM key.

use rand::rngs::OsRng;
use x25519_dalek::{EphemeralSecret, PublicKey, SharedSecret};

/// A single-use X25519 keypair.
///
/// The private scalar is consumed during [`KtmKeyPair::diffie_hellman`] to
/// enforce forward secrecy — an `EphemeralSecret` can only be used once.
pub struct KtmKeyPair {
    secret: EphemeralSecret,
    /// The corresponding public key, derived from the ephemeral secret.
    pub public: PublicKey,
}

impl KtmKeyPair {
    /// Generate a new ephemeral X25519 keypair using the OS CSPRNG.
    pub fn generate() -> Self {
        let secret = EphemeralSecret::random_from_rng(OsRng);
        let public = PublicKey::from(&secret);
        Self { secret, public }
    }

    /// Return the raw 32-byte encoding of the public key.
    pub fn public_bytes(&self) -> [u8; 32] {
        self.public.to_bytes()
    }

    /// Perform Diffie-Hellman with the remote party's public key bytes.
    ///
    /// Consumes `self` — the `EphemeralSecret` is dropped after use.
    pub fn diffie_hellman(self, their_public_bytes: &[u8; 32]) -> SharedSecret {
        let their_public = PublicKey::from(*their_public_bytes);
        self.secret.diffie_hellman(&their_public)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ecdh_produces_same_shared_secret_on_both_sides() {
        let alice = KtmKeyPair::generate();
        let bob = KtmKeyPair::generate();

        let alice_pub = alice.public_bytes();
        let bob_pub = bob.public_bytes();

        let alice_shared = alice.diffie_hellman(&bob_pub);
        let bob_shared = bob.diffie_hellman(&alice_pub);

        assert_eq!(alice_shared.as_bytes(), bob_shared.as_bytes());
    }

    #[test]
    fn public_key_is_32_bytes() {
        let kp = KtmKeyPair::generate();
        assert_eq!(kp.public_bytes().len(), 32);
    }
}

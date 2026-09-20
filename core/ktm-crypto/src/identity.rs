//! Device identity — persistent X25519 keypair stored in `~/.ktm/identity.json`.
//!
//! The identity file holds:
//! - A stable `device_id` (UUID v4, generated once).
//! - The X25519 static public key bytes (hex-encoded).
//! - The X25519 static private key scalar (hex-encoded, zeroized on drop).
//!
//! Static keys are used for *pairing* authentication (the ephemeral ECDH keys
//! in [`crate::ecdh`] handle per-session key agreement).  In production the
//! private scalar should be protected by OS keychain / Android Keystore; for
//! this reference implementation it is stored in a file with restricted
//! permissions.

use serde::{Deserialize, Serialize};
use std::error::Error;
use std::fmt;
use std::fs;
use std::path::{Path, PathBuf};
use x25519_dalek::{PublicKey, StaticSecret};
use zeroize::Zeroize;

// ---------------------------------------------------------------------------
// Error type
// ---------------------------------------------------------------------------

/// Errors that can occur while loading or creating a device identity.
#[derive(Debug)]
pub enum IdentityError {
    /// The identity file could not be read.
    Io(std::io::Error),
    /// The identity file contained invalid JSON or malformed keys.
    Corrupt(String),
    /// Could not determine the home directory.
    NoHomeDir,
}

impl fmt::Display for IdentityError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Io(e) => write!(f, "Identity I/O error: {e}"),
            Self::Corrupt(msg) => write!(f, "Identity file corrupt: {msg}"),
            Self::NoHomeDir => write!(f, "Cannot determine home directory"),
        }
    }
}

impl Error for IdentityError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        if let Self::Io(e) = self {
            Some(e)
        } else {
            None
        }
    }
}

impl From<std::io::Error> for IdentityError {
    fn from(e: std::io::Error) -> Self {
        Self::Io(e)
    }
}

// ---------------------------------------------------------------------------
// On-disk representation (zeroized on drop)
// ---------------------------------------------------------------------------

/// Raw JSON format stored in `~/.ktm/identity.json`.
#[derive(Serialize, Deserialize)]
struct IdentityFile {
    device_id: String,
    public_key_hex: String,
    private_key_hex: String,
}

impl Drop for IdentityFile {
    fn drop(&mut self) {
        self.private_key_hex.zeroize();
    }
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/// A device's long-term identity keypair.
///
/// The private scalar is wrapped in [`StaticSecret`] which implements
/// [`Zeroize`] on drop.
pub struct DeviceIdentity {
    /// A stable UUID v4 that identifies this device to peers.
    pub device_id: String,
    /// The X25519 static public key.
    pub public_key: PublicKey,
    /// The X25519 static private key (zeroized on drop).
    secret: StaticSecret,
}

impl DeviceIdentity {
    /// Generate a brand-new identity (keypair + random UUID).
    pub fn generate() -> Self {
        use rand::rngs::OsRng;
        let secret = StaticSecret::random_from_rng(OsRng);
        let public_key = PublicKey::from(&secret);
        let device_id = uuid_v4();
        Self {
            device_id,
            public_key,
            secret,
        }
    }

    /// Return the public key as a 32-byte array.
    pub fn public_key_bytes(&self) -> [u8; 32] {
        self.public_key.to_bytes()
    }

    /// Return the private key scalar as a 32-byte array (zeroize after use).
    pub fn private_key_bytes(&self) -> [u8; 32] {
        self.secret.to_bytes()
    }

    /// Perform X25519 DH with a remote static public key.
    ///
    /// Used during the pairing handshake to authenticate the remote device.
    pub fn diffie_hellman(&self, their_public: &[u8; 32]) -> [u8; 32] {
        let remote = PublicKey::from(*their_public);
        self.secret.diffie_hellman(&remote).to_bytes()
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    /// Default path: `~/.ktm/identity.json`.
    pub fn default_path() -> Result<PathBuf, IdentityError> {
        let home = home_dir().ok_or(IdentityError::NoHomeDir)?;
        Ok(home.join(".ktm").join("identity.json"))
    }

    /// Load the identity from `path`, or generate and save a new one if it
    /// does not exist.
    pub fn load_or_create(path: &Path) -> Result<Self, IdentityError> {
        if path.exists() {
            Self::load(path)
        } else {
            let identity = Self::generate();
            identity.save(path)?;
            Ok(identity)
        }
    }

    /// Convenience wrapper that uses [`DeviceIdentity::default_path`].
    pub fn load_or_create_default() -> Result<Self, IdentityError> {
        let path = Self::default_path()?;
        Self::load_or_create(&path)
    }

    /// Persist the identity to `path`, creating parent directories as needed.
    pub fn save(&self, path: &Path) -> Result<(), IdentityError> {
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)?;
        }

        let file = IdentityFile {
            device_id: self.device_id.clone(),
            public_key_hex: hex::encode(self.public_key_bytes()),
            private_key_hex: hex::encode(self.private_key_bytes()),
        };

        let json = serde_json::to_string_pretty(&file)
            .map_err(|e| IdentityError::Corrupt(e.to_string()))?;

        // Write atomically via a temp file.
        let tmp = path.with_extension("json.tmp");
        fs::write(&tmp, json.as_bytes())?;
        fs::rename(&tmp, path)?;

        // Set restrictive permissions on Unix.
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            let perms = fs::Permissions::from_mode(0o600);
            fs::set_permissions(path, perms)?;
        }

        Ok(())
    }

    /// Load the identity from `path`.
    pub fn load(path: &Path) -> Result<Self, IdentityError> {
        let json = fs::read_to_string(path)?;
        let file: IdentityFile = serde_json::from_str(&json)
            .map_err(|e| IdentityError::Corrupt(e.to_string()))?;

        let priv_bytes: [u8; 32] = hex::decode(&file.private_key_hex)
            .map_err(|e| IdentityError::Corrupt(format!("private key hex: {e}")))?
            .try_into()
            .map_err(|_| IdentityError::Corrupt("private key must be 32 bytes".into()))?;

        let pub_bytes: [u8; 32] = hex::decode(&file.public_key_hex)
            .map_err(|e| IdentityError::Corrupt(format!("public key hex: {e}")))?
            .try_into()
            .map_err(|_| IdentityError::Corrupt("public key must be 32 bytes".into()))?;

        let secret = StaticSecret::from(priv_bytes);
        let public_key = PublicKey::from(pub_bytes);

        Ok(Self {
            device_id: file.device_id,
            public_key,
            secret,
        })
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/// Platform-independent home directory.
fn home_dir() -> Option<PathBuf> {
    #[cfg(target_os = "windows")]
    {
        std::env::var("USERPROFILE")
            .ok()
            .map(PathBuf::from)
            .or_else(|| {
                let drive = std::env::var("HOMEDRIVE").ok()?;
                let path = std::env::var("HOMEPATH").ok()?;
                Some(PathBuf::from(format!("{drive}{path}")))
            })
    }
    #[cfg(not(target_os = "windows"))]
    {
        std::env::var("HOME").ok().map(PathBuf::from)
    }
}

/// Generate a UUID v4 without the `uuid` crate.
///
/// Uses the OS CSPRNG for all 128 bits then sets the version / variant bits.
fn uuid_v4() -> String {
    use rand::Rng;
    let mut rng = rand::rngs::OsRng;
    let mut bytes = [0u8; 16];
    rng.fill(&mut bytes);
    // RFC 4122 version 4.
    bytes[6] = (bytes[6] & 0x0F) | 0x40;
    // RFC 4122 variant 1.
    bytes[8] = (bytes[8] & 0x3F) | 0x80;
    format!(
        "{:08x}-{:04x}-{:04x}-{:04x}-{:012x}",
        u32::from_be_bytes(bytes[0..4].try_into().unwrap()),
        u16::from_be_bytes(bytes[4..6].try_into().unwrap()),
        u16::from_be_bytes(bytes[6..8].try_into().unwrap()),
        u16::from_be_bytes(bytes[8..10].try_into().unwrap()),
        {
            let mut v = 0u64;
            for b in &bytes[10..16] {
                v = (v << 8) | (*b as u64);
            }
            v
        }
    )
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::env;

    #[test]
    fn generate_produces_unique_device_ids() {
        let a = DeviceIdentity::generate();
        let b = DeviceIdentity::generate();
        assert_ne!(a.device_id, b.device_id);
    }

    #[test]
    fn save_and_load_round_trip() {
        let tmp = env::temp_dir().join(format!("ktm_identity_test_{}.json", uuid_v4()));
        let identity = DeviceIdentity::generate();
        identity.save(&tmp).unwrap();
        let loaded = DeviceIdentity::load(&tmp).unwrap();
        assert_eq!(identity.device_id, loaded.device_id);
        assert_eq!(identity.public_key_bytes(), loaded.public_key_bytes());
        assert_eq!(identity.private_key_bytes(), loaded.private_key_bytes());
        let _ = fs::remove_file(&tmp);
    }

    #[test]
    fn load_or_create_creates_then_loads() {
        let tmp = env::temp_dir().join(format!("ktm_loc_test_{}.json", uuid_v4()));
        let a = DeviceIdentity::load_or_create(&tmp).unwrap();
        let b = DeviceIdentity::load_or_create(&tmp).unwrap();
        assert_eq!(a.device_id, b.device_id);
        let _ = fs::remove_file(&tmp);
    }

    #[test]
    fn dh_is_consistent() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let ab = alice.diffie_hellman(&bob.public_key_bytes());
        let ba = bob.diffie_hellman(&alice.public_key_bytes());
        assert_eq!(ab, ba);
    }
}

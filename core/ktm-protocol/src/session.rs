//! Session ID generation utilities.
//!
//! A `SessionId` is a 32-byte cryptographically-random identifier used to
//! associate all packets belonging to a single transfer session.

use rand::RngCore;
use rand::rngs::OsRng;

/// A 32-byte opaque session identifier.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct SessionId(pub [u8; 32]);

impl SessionId {
    /// Generate a new random session ID using the OS CSPRNG.
    pub fn generate() -> Self {
        let mut bytes = [0u8; 32];
        OsRng.fill_bytes(&mut bytes);
        Self(bytes)
    }

    /// Create a `SessionId` from raw bytes.
    pub fn from_bytes(bytes: [u8; 32]) -> Self {
        Self(bytes)
    }

    /// Return the raw byte representation.
    pub fn as_bytes(&self) -> &[u8; 32] {
        &self.0
    }

    /// Return a lower-case hex string representation (64 chars).
    pub fn to_hex(&self) -> String {
        self.0.iter().map(|b| format!("{b:02x}")).collect()
    }

    /// Parse from a 64-character hex string.
    pub fn from_hex(s: &str) -> Result<Self, &'static str> {
        if s.len() != 64 {
            return Err("SessionId hex must be exactly 64 characters");
        }
        let mut bytes = [0u8; 32];
        for (i, chunk) in s.as_bytes().chunks(2).enumerate() {
            let hi = hex_digit(chunk[0])?;
            let lo = hex_digit(chunk[1])?;
            bytes[i] = (hi << 4) | lo;
        }
        Ok(Self(bytes))
    }
}

fn hex_digit(b: u8) -> Result<u8, &'static str> {
    match b {
        b'0'..=b'9' => Ok(b - b'0'),
        b'a'..=b'f' => Ok(b - b'a' + 10),
        b'A'..=b'F' => Ok(b - b'A' + 10),
        _ => Err("Invalid hex character in SessionId"),
    }
}

impl std::fmt::Display for SessionId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.to_hex())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn generate_is_unique() {
        let a = SessionId::generate();
        let b = SessionId::generate();
        assert_ne!(a, b);
    }

    #[test]
    fn hex_round_trip() {
        let id = SessionId::generate();
        let hex = id.to_hex();
        assert_eq!(hex.len(), 64);
        let recovered = SessionId::from_hex(&hex).unwrap();
        assert_eq!(id, recovered);
    }

    #[test]
    fn invalid_hex_rejected() {
        assert!(SessionId::from_hex("not hex").is_err());
    }
}

//! KTM v2 binary packet framing.
//!
//! ## Header layout (50 bytes)
//!
//! | Offset | Length | Field        | Description                          |
//! |--------|--------|--------------|--------------------------------------|
//! | 0      | 4      | magic        | ASCII `KTM2`                         |
//! | 4      | 1      | version      | Protocol version (currently `2`)      |
//! | 5      | 1      | msg_type     | [`PacketType`] discriminant (u8)      |
//! | 6      | 4      | seq          | Sequence number (big-endian u32)      |
//! | 10     | 32     | session_id   | Session UUID bytes                   |
//! | 42     | 4      | payload_len  | Payload byte length (big-endian u32) |
//! | 46     | 4      | crc32        | CRC-32 over header[0..46] + payload  |
//!
//! Total header = 50 bytes.  Payload follows immediately.

use std::fmt;
use crc32fast::Hasher as Crc32Hasher;

/// Wire magic bytes — `KTM2`.
pub const MAGIC: &[u8; 4] = b"KTM2";
/// Current protocol version.
pub const PROTOCOL_VERSION: u8 = 2;
/// Fixed header size in bytes.
pub const HEADER_SIZE: usize = 50;

// ---------------------------------------------------------------------------
// PacketType
// ---------------------------------------------------------------------------

/// Discriminants for every KTM v2 message type.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[repr(u8)]
pub enum PacketType {
    Discovery       = 0x01,
    PairRequest     = 0x02,
    PairAccept      = 0x03,
    PairReject      = 0x04,
    TransferManifest= 0x05,
    ChunkData       = 0x06,
    ChunkAck        = 0x07,
    TransferComplete= 0x08,
    ResumeRequest   = 0x09,
    Cancel          = 0x0A,
    Heartbeat       = 0x0B,
}

impl PacketType {
    /// Decode a raw byte into a [`PacketType`], returning an error if unknown.
    pub fn from_u8(v: u8) -> Result<Self, PacketError> {
        match v {
            0x01 => Ok(Self::Discovery),
            0x02 => Ok(Self::PairRequest),
            0x03 => Ok(Self::PairAccept),
            0x04 => Ok(Self::PairReject),
            0x05 => Ok(Self::TransferManifest),
            0x06 => Ok(Self::ChunkData),
            0x07 => Ok(Self::ChunkAck),
            0x08 => Ok(Self::TransferComplete),
            0x09 => Ok(Self::ResumeRequest),
            0x0A => Ok(Self::Cancel),
            0x0B => Ok(Self::Heartbeat),
            other => Err(PacketError::UnknownPacketType(other)),
        }
    }
}

// ---------------------------------------------------------------------------
// Error type
// ---------------------------------------------------------------------------

/// Errors that can occur during packet encoding/decoding.
#[derive(Debug, PartialEq, Eq)]
pub enum PacketError {
    /// The buffer was too short to contain a full header.
    BufferTooShort,
    /// The magic bytes `KTM2` were not found.
    InvalidMagic,
    /// The protocol version byte is not supported.
    UnsupportedVersion(u8),
    /// The `msg_type` byte does not correspond to any known [`PacketType`].
    UnknownPacketType(u8),
    /// The CRC-32 computed over the packet did not match the stored checksum.
    CrcMismatch { expected: u32, actual: u32 },
    /// The `payload_len` field exceeds an internal sanity limit (128 MiB).
    PayloadTooLarge(u32),
}

impl fmt::Display for PacketError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::BufferTooShort => write!(f, "Buffer is shorter than the 50-byte KTM2 header"),
            Self::InvalidMagic => write!(f, "Magic bytes are not `KTM2`"),
            Self::UnsupportedVersion(v) => write!(f, "Unsupported protocol version {v}"),
            Self::UnknownPacketType(t) => write!(f, "Unknown packet type 0x{t:02X}"),
            Self::CrcMismatch { expected, actual } => {
                write!(f, "CRC-32 mismatch: stored={expected:#010x} computed={actual:#010x}")
            }
            Self::PayloadTooLarge(n) => write!(f, "Payload length {n} exceeds 128 MiB limit"),
        }
    }
}

impl std::error::Error for PacketError {}

// ---------------------------------------------------------------------------
// KtmPacket
// ---------------------------------------------------------------------------

/// Maximum allowed payload size: 128 MiB.
const MAX_PAYLOAD: u32 = 128 * 1024 * 1024;

/// A decoded KTM v2 packet.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct KtmPacket {
    /// Protocol version (should be [`PROTOCOL_VERSION`]).
    pub version: u8,
    /// The message type of this packet.
    pub msg_type: PacketType,
    /// Monotonically increasing sequence number for ordered delivery.
    pub seq: u32,
    /// 32-byte session identifier.
    pub session_id: [u8; 32],
    /// Raw payload bytes (may be empty for control packets).
    pub payload: Vec<u8>,
}

impl KtmPacket {
    /// Construct a new packet with the current protocol version.
    pub fn new(
        msg_type: PacketType,
        seq: u32,
        session_id: [u8; 32],
        payload: Vec<u8>,
    ) -> Self {
        Self {
            version: PROTOCOL_VERSION,
            msg_type,
            seq,
            session_id,
            payload,
        }
    }

    /// Encode the packet into a byte vector (header + payload).
    ///
    /// The CRC-32 is computed over `header[0..46] || payload` and written into
    /// `header[46..50]`.
    pub fn encode(&self) -> Vec<u8> {
        let payload_len = self.payload.len() as u32;
        let total = HEADER_SIZE + self.payload.len();
        let mut buf = vec![0u8; total];

        // Magic
        buf[0..4].copy_from_slice(MAGIC);
        // Version
        buf[4] = self.version;
        // msg_type
        buf[5] = self.msg_type as u8;
        // seq (big-endian)
        buf[6..10].copy_from_slice(&self.seq.to_be_bytes());
        // session_id
        buf[10..42].copy_from_slice(&self.session_id);
        // payload_len (big-endian)
        buf[42..46].copy_from_slice(&payload_len.to_be_bytes());

        // Copy payload
        if !self.payload.is_empty() {
            buf[HEADER_SIZE..].copy_from_slice(&self.payload);
        }

        // CRC-32 over bytes 0..46 and the payload.
        let crc = compute_crc32(&buf[..46], &self.payload);
        buf[46..50].copy_from_slice(&crc.to_be_bytes());

        buf
    }

    /// Decode a packet from a byte slice.
    ///
    /// The slice must contain exactly one complete packet
    /// (`HEADER_SIZE + payload_len` bytes).
    pub fn decode(data: &[u8]) -> Result<Self, PacketError> {
        if data.len() < HEADER_SIZE {
            return Err(PacketError::BufferTooShort);
        }

        // Magic
        if &data[0..4] != MAGIC {
            return Err(PacketError::InvalidMagic);
        }

        // Version
        let version = data[4];
        if version != PROTOCOL_VERSION {
            return Err(PacketError::UnsupportedVersion(version));
        }

        // msg_type
        let msg_type = PacketType::from_u8(data[5])?;

        // seq
        let seq = u32::from_be_bytes(data[6..10].try_into().unwrap());

        // session_id
        let mut session_id = [0u8; 32];
        session_id.copy_from_slice(&data[10..42]);

        // payload_len
        let payload_len = u32::from_be_bytes(data[42..46].try_into().unwrap());
        if payload_len > MAX_PAYLOAD {
            return Err(PacketError::PayloadTooLarge(payload_len));
        }

        // CRC check — stored CRC is at bytes 46..50.
        let stored_crc = u32::from_be_bytes(data[46..50].try_into().unwrap());

        let expected_total = HEADER_SIZE + payload_len as usize;
        if data.len() < expected_total {
            return Err(PacketError::BufferTooShort);
        }

        let payload = data[HEADER_SIZE..HEADER_SIZE + payload_len as usize].to_vec();
        let computed_crc = compute_crc32(&data[..46], &payload);

        if stored_crc != computed_crc {
            return Err(PacketError::CrcMismatch {
                expected: stored_crc,
                actual: computed_crc,
            });
        }

        Ok(Self {
            version,
            msg_type,
            seq,
            session_id,
            payload,
        })
    }

    /// Total wire size of this packet.
    pub fn wire_size(&self) -> usize {
        HEADER_SIZE + self.payload.len()
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/// Compute CRC-32 over `header_prefix` (first 46 bytes) concatenated with
/// `payload`.
fn compute_crc32(header_prefix: &[u8], payload: &[u8]) -> u32 {
    let mut h = Crc32Hasher::new();
    h.update(header_prefix);
    h.update(payload);
    h.finalize()
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    fn session() -> [u8; 32] {
        [0xBE_u8; 32]
    }

    #[test]
    fn encode_decode_round_trip_no_payload() {
        let pkt = KtmPacket::new(PacketType::Heartbeat, 1, session(), vec![]);
        let encoded = pkt.encode();
        assert_eq!(encoded.len(), HEADER_SIZE);
        let decoded = KtmPacket::decode(&encoded).unwrap();
        assert_eq!(decoded.msg_type, PacketType::Heartbeat);
        assert_eq!(decoded.seq, 1);
        assert_eq!(decoded.session_id, session());
        assert!(decoded.payload.is_empty());
    }

    #[test]
    fn encode_decode_with_payload() {
        let payload = b"hello world".to_vec();
        let pkt = KtmPacket::new(PacketType::ChunkData, 42, session(), payload.clone());
        let encoded = pkt.encode();
        let decoded = KtmPacket::decode(&encoded).unwrap();
        assert_eq!(decoded.payload, payload);
        assert_eq!(decoded.seq, 42);
    }

    #[test]
    fn bad_magic_rejected() {
        let pkt = KtmPacket::new(PacketType::Heartbeat, 0, session(), vec![]);
        let mut encoded = pkt.encode();
        encoded[0] = b'X';
        assert_eq!(KtmPacket::decode(&encoded), Err(PacketError::InvalidMagic));
    }

    #[test]
    fn crc_mismatch_detected() {
        let pkt = KtmPacket::new(PacketType::Heartbeat, 0, session(), b"data".to_vec());
        let mut encoded = pkt.encode();
        // Corrupt a payload byte.
        let last = encoded.len() - 1;
        encoded[last] ^= 0xFF;
        assert!(matches!(
            KtmPacket::decode(&encoded),
            Err(PacketError::CrcMismatch { .. })
        ));
    }

    #[test]
    fn buffer_too_short_rejected() {
        assert_eq!(KtmPacket::decode(b"short"), Err(PacketError::BufferTooShort));
    }

    #[test]
    fn all_packet_types_round_trip() {
        let types = [
            PacketType::Discovery,
            PacketType::PairRequest,
            PacketType::PairAccept,
            PacketType::PairReject,
            PacketType::TransferManifest,
            PacketType::ChunkData,
            PacketType::ChunkAck,
            PacketType::TransferComplete,
            PacketType::ResumeRequest,
            PacketType::Cancel,
            PacketType::Heartbeat,
        ];
        for t in types {
            let pkt = KtmPacket::new(t, 0, session(), vec![]);
            let enc = pkt.encode();
            let dec = KtmPacket::decode(&enc).unwrap();
            assert_eq!(dec.msg_type, t);
        }
    }
}

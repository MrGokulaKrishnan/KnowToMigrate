//! ktm-protocol — KTM v2 wire protocol, packet codec, and message payloads.
//!
//! Provides:
//! - Binary packet framing with 50-byte header (magic, version, type, seq,
//!   session-id, payload length, CRC-32).
//! - JSON-serializable message payload structs for every message type.
//! - Session ID generation utilities.

pub mod messages;
pub mod packet;
pub mod session;

pub use messages::*;
pub use packet::{KtmPacket, PacketType};
pub use session::SessionId;

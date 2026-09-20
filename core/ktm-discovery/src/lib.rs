//! ktm-discovery — LAN peer discovery via UDP broadcast.
//!
//! Provides:
//! - [`udp_discovery`] — broadcast sender/receiver and device registry.
//! - [`beacon`] — beacon packet structure.

pub mod beacon;
pub mod udp_discovery;

pub use beacon::DiscoveryBeacon;
pub use udp_discovery::{DiscoveredDevice, DiscoveryService};

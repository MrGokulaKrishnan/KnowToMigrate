//! Beacon packet — the JSON payload broadcast during UDP discovery.

use ktm_protocol::messages::DeviceInfo;
use serde::{Deserialize, Serialize};

/// The JSON object broadcast on port 54123.
///
/// Wraps [`DeviceInfo`] with an additional schema version field so future
/// protocol changes can be detected without breaking existing receivers.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiscoveryBeacon {
    /// Beacon schema version — currently `1`.
    pub beacon_version: u8,
    /// Embedded device information.
    pub device: DeviceInfo,
}

impl DiscoveryBeacon {
    /// Create a beacon for the local device.
    pub fn new(device: DeviceInfo) -> Self {
        Self {
            beacon_version: 1,
            device,
        }
    }

    /// Serialise to a compact JSON byte vector.
    pub fn to_bytes(&self) -> serde_json::Result<Vec<u8>> {
        serde_json::to_vec(self)
    }

    /// Deserialise from a JSON byte slice.
    pub fn from_bytes(data: &[u8]) -> serde_json::Result<Self> {
        serde_json::from_slice(data)
    }
}

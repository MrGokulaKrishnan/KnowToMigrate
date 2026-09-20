//! UDP broadcast discovery service.
//!
//! Sends a [`DiscoveryBeacon`] to `255.255.255.255:54123` every 2 seconds and
//! listens on `0.0.0.0:54123` for beacons from other devices.
//!
//! Discovered devices are tracked in an in-memory [`DeviceRegistry`] and
//! expired after 10 seconds of silence.  New or updated device events are
//! delivered through a [`tokio::sync::mpsc`] channel.

use crate::beacon::DiscoveryBeacon;
use ktm_protocol::messages::DeviceInfo;
use std::collections::HashMap;
use std::net::SocketAddr;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::net::UdpSocket;
use tokio::sync::{mpsc, Mutex};
use tokio::time;

/// UDP port used for discovery broadcasts.
pub const DISCOVERY_PORT: u16 = 54123;
/// Broadcast address.
const BROADCAST_ADDR: &str = "255.255.255.255";
/// Interval between outgoing beacons.
const BEACON_INTERVAL: Duration = Duration::from_secs(2);
/// How long without a beacon before a device is considered gone.
const DEVICE_EXPIRY: Duration = Duration::from_secs(10);
/// Maximum UDP datagram size we read (1500-byte Ethernet MTU is typical).
const MAX_DATAGRAM: usize = 4096;

// ---------------------------------------------------------------------------
// DiscoveredDevice
// ---------------------------------------------------------------------------

/// A device seen on the local network.
#[derive(Debug, Clone)]
pub struct DiscoveredDevice {
    /// Device information extracted from the beacon.
    pub info: DeviceInfo,
    /// The source IP address of the beacon.
    pub addr: SocketAddr,
    /// When this device was last seen.
    pub last_seen: Instant,
}

// ---------------------------------------------------------------------------
// DeviceRegistry
// ---------------------------------------------------------------------------

/// Thread-safe map of `device_id → DiscoveredDevice`.
#[derive(Default)]
pub struct DeviceRegistry {
    inner: Mutex<HashMap<String, DiscoveredDevice>>,
}

impl DeviceRegistry {
    pub fn new() -> Arc<Self> {
        Arc::new(Self {
            inner: Mutex::new(HashMap::new()),
        })
    }

    /// Insert or update a device; returns `true` if this is a new device.
    pub async fn upsert(&self, device: DiscoveredDevice) -> bool {
        let mut map = self.inner.lock().await;
        let is_new = !map.contains_key(&device.info.device_id);
        map.insert(device.info.device_id.clone(), device);
        is_new
    }

    /// Remove devices whose last-seen timestamp is older than [`DEVICE_EXPIRY`].
    pub async fn expire(&self) -> Vec<String> {
        let mut map = self.inner.lock().await;
        let now = Instant::now();
        let expired: Vec<String> = map
            .iter()
            .filter(|(_, d)| now.duration_since(d.last_seen) > DEVICE_EXPIRY)
            .map(|(id, _)| id.clone())
            .collect();
        for id in &expired {
            map.remove(id);
        }
        expired
    }

    /// Return a snapshot of all currently known devices.
    pub async fn snapshot(&self) -> Vec<DiscoveredDevice> {
        let map = self.inner.lock().await;
        map.values().cloned().collect()
    }
}

// ---------------------------------------------------------------------------
// DiscoveryService
// ---------------------------------------------------------------------------

/// Manages the UDP broadcast sender and receiver tasks.
pub struct DiscoveryService {
    registry: Arc<DeviceRegistry>,
    shutdown_tx: tokio::sync::watch::Sender<bool>,
}

impl DiscoveryService {
    /// Start the discovery service.
    ///
    /// Returns a `(DiscoveryService, Receiver<DiscoveredDevice>)` pair.
    /// The receiver yields each newly-seen or updated device.
    ///
    /// The service keeps running until [`DiscoveryService::stop`] is called or
    /// the handle is dropped.
    pub async fn start(
        local_device: DeviceInfo,
    ) -> std::io::Result<(Self, mpsc::Receiver<DiscoveredDevice>)> {
        let registry = DeviceRegistry::new();
        let (event_tx, event_rx) = mpsc::channel::<DiscoveredDevice>(64);
        let (shutdown_tx, shutdown_rx) = tokio::sync::watch::channel(false);

        // --- Sender task ---
        {
            let beacon = DiscoveryBeacon::new(local_device.clone());
            let mut rx = shutdown_rx.clone();
            tokio::spawn(async move {
                if let Err(e) = run_sender(beacon, &mut rx).await {
                    eprintln!("[ktm-discovery] sender error: {e}");
                }
            });
        }

        // --- Receiver task ---
        {
            let registry = Arc::clone(&registry);
            let local_id = local_device.device_id.clone();
            let mut rx = shutdown_rx.clone();
            tokio::spawn(async move {
                if let Err(e) = run_receiver(registry, local_id, event_tx, &mut rx).await {
                    eprintln!("[ktm-discovery] receiver error: {e}");
                }
            });
        }

        // --- Expiry task ---
        {
            let registry = Arc::clone(&registry);
            let mut rx = shutdown_rx.clone();
            tokio::spawn(async move {
                run_expiry(registry, &mut rx).await;
            });
        }

        Ok((
            Self {
                registry,
                shutdown_tx,
            },
            event_rx,
        ))
    }

    /// Signal all background tasks to stop.
    pub fn stop(&self) {
        let _ = self.shutdown_tx.send(true);
    }

    /// Return a snapshot of all currently live devices.
    pub async fn known_devices(&self) -> Vec<DiscoveredDevice> {
        self.registry.snapshot().await
    }
}

impl Drop for DiscoveryService {
    fn drop(&mut self) {
        self.stop();
    }
}

// ---------------------------------------------------------------------------
// Background task implementations
// ---------------------------------------------------------------------------

async fn run_sender(
    beacon: DiscoveryBeacon,
    shutdown: &mut tokio::sync::watch::Receiver<bool>,
) -> std::io::Result<()> {
    // Bind to any port so we can send broadcasts.
    let socket = UdpSocket::bind("0.0.0.0:0").await?;
    socket.set_broadcast(true)?;

    let dest: SocketAddr = format!("{BROADCAST_ADDR}:{DISCOVERY_PORT}").parse().unwrap();
    let payload = beacon
        .to_bytes()
        .map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e))?;

    let mut interval = time::interval(BEACON_INTERVAL);

    loop {
        tokio::select! {
            _ = interval.tick() => {
                if let Err(e) = socket.send_to(&payload, dest).await {
                    eprintln!("[ktm-discovery] send_to error: {e}");
                }
            }
            _ = shutdown.changed() => {
                if *shutdown.borrow() {
                    break;
                }
            }
        }
    }
    Ok(())
}

async fn run_receiver(
    registry: Arc<DeviceRegistry>,
    local_device_id: String,
    event_tx: mpsc::Sender<DiscoveredDevice>,
    shutdown: &mut tokio::sync::watch::Receiver<bool>,
) -> std::io::Result<()> {
    let socket = UdpSocket::bind(format!("0.0.0.0:{DISCOVERY_PORT}")).await?;
    socket.set_broadcast(true)?;

    let mut buf = vec![0u8; MAX_DATAGRAM];

    loop {
        tokio::select! {
            result = socket.recv_from(&mut buf) => {
                match result {
                    Ok((len, src)) => {
                        let data = &buf[..len];
                        match DiscoveryBeacon::from_bytes(data) {
                            Ok(beacon) => {
                                // Ignore our own broadcasts.
                                if beacon.device.device_id == local_device_id {
                                    continue;
                                }
                                let discovered = DiscoveredDevice {
                                    info: beacon.device,
                                    addr: src,
                                    last_seen: Instant::now(),
                                };
                                registry.upsert(discovered.clone()).await;
                                // Best-effort send to the event channel.
                                let _ = event_tx.try_send(discovered);
                            }
                            Err(e) => {
                                eprintln!("[ktm-discovery] malformed beacon from {src}: {e}");
                            }
                        }
                    }
                    Err(e) => {
                        eprintln!("[ktm-discovery] recv_from error: {e}");
                    }
                }
            }
            _ = shutdown.changed() => {
                if *shutdown.borrow() {
                    break;
                }
            }
        }
    }
    Ok(())
}

async fn run_expiry(
    registry: Arc<DeviceRegistry>,
    shutdown: &mut tokio::sync::watch::Receiver<bool>,
) {
    let mut interval = time::interval(Duration::from_secs(5));
    loop {
        tokio::select! {
            _ = interval.tick() => {
                let expired = registry.expire().await;
                for id in expired {
                    eprintln!("[ktm-discovery] device expired: {id}");
                }
            }
            _ = shutdown.changed() => {
                if *shutdown.borrow() {
                    break;
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Convenience free function matching the task spec
// ---------------------------------------------------------------------------

/// Start the discovery service and return the event receiver.
///
/// Equivalent to [`DiscoveryService::start`] but discards the service handle
/// — useful for FFI or quick integrations where lifecycle is managed externally.
pub async fn start_discovery(
    device_info: DeviceInfo,
) -> std::io::Result<mpsc::Receiver<DiscoveredDevice>> {
    let (_service, rx) = DiscoveryService::start(device_info).await?;
    // NOTE: `_service` is intentionally leaked here so the background tasks
    // keep running.  Callers that want clean shutdown should use
    // `DiscoveryService::start` directly.
    std::mem::forget(_service);
    Ok(rx)
}

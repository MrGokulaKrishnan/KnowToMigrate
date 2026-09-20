use std::ffi::{CStr, CString, c_char, c_int};
use std::panic::catch_unwind;
use std::ptr;

// ============================================================
// KTM Handle â€” opaque pointer exposed to C callers
// ============================================================

pub struct KtmHandle {
    pub device_id: String,
    pub version: String,
    pub runtime: Option<tokio::runtime::Runtime>,
}

impl KtmHandle {
    fn new() -> Self {
        let device_id = generate_device_id();
        let runtime = tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .worker_threads(4)
            .thread_name("ktm-worker")
            .build()
            .ok();
        Self {
            device_id,
            version: env!("CARGO_PKG_VERSION").to_string(),
            runtime,
        }
    }
}

fn generate_device_id() -> String {
    use std::time::{SystemTime, UNIX_EPOCH};
    let ts = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_nanos();
    format!("ktm-{:x}", ts & 0xFFFFFFFFFFFF)
}

// ============================================================
// Lifecycle
// ============================================================

/// Initialize the KTM core. Returns an opaque handle.
/// Caller must call `ktm_destroy` when done.
#[no_mangle]
pub extern "C" fn ktm_init() -> *mut KtmHandle {
    catch_unwind(|| {
        let handle = Box::new(KtmHandle::new());
        Box::into_raw(handle)
    })
    .unwrap_or(ptr::null_mut())
}

/// Destroy the KTM handle and free all resources.
#[no_mangle]
pub unsafe extern "C" fn ktm_destroy(handle: *mut KtmHandle) {
    if handle.is_null() {
        return;
    }
    let _ = catch_unwind(|| {
        drop(Box::from_raw(handle));
    });
}

// ============================================================
// Device Discovery
// ============================================================

pub type DeviceDiscoveredCallback = unsafe extern "C" fn(device_json: *const c_char);

/// Start broadcasting device beacons and listening for nearby devices.
/// `callback` is called on the calling thread for each device discovered.
#[no_mangle]
pub unsafe extern "C" fn ktm_start_discovery(
    handle: *mut KtmHandle,
    callback: DeviceDiscoveredCallback,
) -> c_int {
    if handle.is_null() {
        return -1;
    }
    catch_unwind(|| {
        let h = &*handle;
        if let Some(rt) = &h.runtime {
            let device_id = h.device_id.clone();
            rt.spawn(async move {
                use std::time::Duration;
                use tokio::net::UdpSocket;

                let sock = match UdpSocket::bind("0.0.0.0:0").await {
                    Ok(s) => s,
                    Err(_) => return,
                };
                let _ = sock.set_broadcast(true);

                let hostname = hostname::get()
                    .unwrap_or_default()
                    .to_string_lossy()
                    .to_string();

                let beacon = serde_json::json!({
                    "device_id": device_id,
                    "name": hostname,
                    "platform": std::env::consts::OS,
                    "app_version": env!("CARGO_PKG_VERSION"),
                    "transfer_port": 54124u16,
                    "discovery_version": 2,
                });
                let beacon_bytes = serde_json::to_vec(&beacon).unwrap_or_default();

                loop {
                    let _ = sock.send_to(&beacon_bytes, "255.255.255.255:54123").await;
                    tokio::time::sleep(Duration::from_secs(2)).await;
                }
            });

            // Listener task
            let cb_ptr = callback as usize;
            rt.spawn(async move {
                use tokio::net::UdpSocket;

                let listener = match UdpSocket::bind("0.0.0.0:54123").await {
                    Ok(s) => s,
                    Err(_) => return,
                };
                let _ = listener.set_broadcast(true);

                let mut buf = vec![0u8; 4096];
                loop {
                    let (len, addr) = match listener.recv_from(&mut buf).await {
                        Ok(r) => r,
                        Err(_) => continue,
                    };
                    let data = &buf[..len];
                    if let Ok(mut val) = serde_json::from_slice::<serde_json::Value>(data) {
                        val["ip"] = serde_json::json!(addr.ip().to_string());
                        if let Ok(json_str) = serde_json::to_string(&val) {
                            if let Ok(c_str) = CString::new(json_str) {
                                let cb: DeviceDiscoveredCallback = std::mem::transmute(cb_ptr);
                                unsafe { cb(c_str.as_ptr()) };
                            }
                        }
                    }
                }
            });
            0
        } else {
            -2
        }
    })
    .unwrap_or(-1)
}

/// Stop broadcasting and listening.
#[no_mangle]
pub unsafe extern "C" fn ktm_stop_discovery(handle: *mut KtmHandle) {
    // Discovery tasks are stopped by dropping the runtime on ktm_destroy.
    // For a finer-grained cancel, use a cancellation token (future enhancement).
    let _ = handle;
}

// ============================================================
// File Transfer â€” Send
// ============================================================

pub type TransferProgressCallback = unsafe extern "C" fn(
    session_id: *const c_char,
    bytes_sent: u64,
    total_bytes: u64,
    speed_bps: f64,
    eta_secs: f64,
);

/// Send a single file to a remote device.
/// Returns session_id string (caller must free with `ktm_free_string`).
/// Returns null on error.
#[no_mangle]
pub unsafe extern "C" fn ktm_send_file(
    handle: *mut KtmHandle,
    target_ip: *const c_char,
    target_port: u16,
    file_path: *const c_char,
    progress_callback: TransferProgressCallback,
) -> *mut c_char {
    if handle.is_null() || target_ip.is_null() || file_path.is_null() {
        return ptr::null_mut();
    }

    let result = catch_unwind(|| {
        let h = &*handle;
        let ip = CStr::from_ptr(target_ip).to_string_lossy().to_string();
        let path_str = CStr::from_ptr(file_path).to_string_lossy().to_string();
        let device_id = h.device_id.clone();
        let session_id = format!("sess-{:x}", std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis());
        let sid_clone = session_id.clone();
        let cb_ptr = progress_callback as usize;

        if let Some(rt) = &h.runtime {
            let sid2 = sid_clone.clone();
            rt.spawn(async move {
                use ktm_transfer::client::TransferClient;
                use std::path::Path;

                let client = TransferClient::new(
                    &ip,
                    target_port,
                    &sid2,
                    &device_id,
                    "KnowToMigrate",
                );

                let _ = client.send_files(&[Path::new(&path_str)], move |prog| {
                    if let (Ok(sid_c), ) = (CString::new(prog.session_id.as_str()), ) {
                        let cb: TransferProgressCallback = unsafe { std::mem::transmute(cb_ptr) };
                        unsafe {
                            cb(
                                sid_c.as_ptr(),
                                prog.bytes_sent,
                                prog.total_bytes,
                                prog.speed_bps,
                                prog.eta_secs,
                            )
                        };
                    }
                }).await;
            });
        }

        CString::new(session_id).map(|s| s.into_raw()).unwrap_or(ptr::null_mut())
    });

    result.unwrap_or(ptr::null_mut())
}

// ============================================================
// File Transfer â€” Receive
// ============================================================

pub type TransferRequestCallback = unsafe extern "C" fn(
    device_json: *const c_char,
    manifest_json: *const c_char,
) -> c_int; // return 1 = accept, 0 = reject

/// Start the receive server. Blocks until handle is destroyed.
#[no_mangle]
pub unsafe extern "C" fn ktm_receive_start(
    handle: *mut KtmHandle,
    receive_dir: *const c_char,
    request_callback: TransferRequestCallback,
    progress_callback: TransferProgressCallback,
) -> c_int {
    if handle.is_null() || receive_dir.is_null() {
        return -1;
    }

    catch_unwind(|| {
        let h = &*handle;
        let dir = CStr::from_ptr(receive_dir).to_string_lossy().to_string();
        let req_cb_ptr = request_callback as usize;
        let prog_cb_ptr = progress_callback as usize;

        if let Some(rt) = &h.runtime {
            rt.spawn(async move {
                use ktm_transfer::server::TransferServer;
                use ktm_transfer::server::TransferRequest;

                let server = TransferServer::new(
                    54124,
                    dir,
                    move |req: TransferRequest| {
                        if let (Ok(dev_c), Ok(man_c)) = (
                            CString::new(serde_json::json!({"device_id": req.sender_device_id, "name": req.sender_name}).to_string()),
                            CString::new(req.manifest_json),
                        ) {
                            let cb: TransferRequestCallback = unsafe { std::mem::transmute(req_cb_ptr) };
                            unsafe { cb(dev_c.as_ptr(), man_c.as_ptr()) == 1 }
                        } else {
                            false
                        }
                    },
                    move |prog| {
                        if let Ok(sid_c) = CString::new(prog.session_id) {
                            let cb: TransferProgressCallback = unsafe { std::mem::transmute(prog_cb_ptr) };
                            unsafe {
                                cb(
                                    sid_c.as_ptr(),
                                    prog.bytes_received,
                                    prog.total_bytes,
                                    prog.speed_bps,
                                    prog.eta_secs,
                                )
                            };
                        }
                    },
                );

                let _ = server.listen().await;
            });
            0
        } else {
            -2
        }
    })
    .unwrap_or(-1)
}

// ============================================================
// Session Control
// ============================================================

/// Cancel a transfer session by ID.
#[no_mangle]
pub unsafe extern "C" fn ktm_cancel(
    _handle: *mut KtmHandle,
    _session_id: *const c_char,
) {
    // TODO: implement per-session cancellation tokens in v1.1
}

// ============================================================
// Utility
// ============================================================

/// Returns the device ID string. Caller must free with `ktm_free_string`.
#[no_mangle]
pub unsafe extern "C" fn ktm_get_device_id(handle: *mut KtmHandle) -> *mut c_char {
    if handle.is_null() {
        return ptr::null_mut();
    }
    let h = &*handle;
    CString::new(h.device_id.as_str())
        .map(|s| s.into_raw())
        .unwrap_or(ptr::null_mut())
}

/// Returns the library version string. Caller must free with `ktm_free_string`.
#[no_mangle]
pub extern "C" fn ktm_get_version() -> *mut c_char {
    CString::new(env!("CARGO_PKG_VERSION"))
        .map(|s| s.into_raw())
        .unwrap_or(ptr::null_mut())
}

/// Free a string returned by any ktm_* function.
#[no_mangle]
pub unsafe extern "C" fn ktm_free_string(ptr: *mut c_char) {
    if ptr.is_null() {
        return;
    }
    drop(CString::from_raw(ptr));
}


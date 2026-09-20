#ifndef KTM_H
#define KTM_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>

// ============================================================
// Opaque handle
// ============================================================
typedef struct KtmHandle KtmHandle;

// ============================================================
// Callbacks
// ============================================================

/** Called when a nearby device is discovered. `device_json` is a JSON string. */
typedef void (*KtmDeviceDiscoveredCallback)(const char* device_json);

/** Called with transfer progress. `session_id` is the session identifier. */
typedef void (*KtmTransferProgressCallback)(
    const char* session_id,
    uint64_t    bytes_sent,
    uint64_t    total_bytes,
    double      speed_bps,
    double      eta_secs
);

/** Called when an incoming transfer request arrives. Return 1 to accept, 0 to reject. */
typedef int (*KtmTransferRequestCallback)(
    const char* device_json,
    const char* manifest_json
);

// ============================================================
// Lifecycle
// ============================================================

/** Initialize the KTM core. Must be called first. Returns NULL on failure. */
KtmHandle* ktm_init(void);

/** Destroy the KTM handle and release all resources. */
void ktm_destroy(KtmHandle* handle);

// ============================================================
// Discovery
// ============================================================

/** Start device discovery. Returns 0 on success, negative on error. */
int ktm_start_discovery(KtmHandle* handle, KtmDeviceDiscoveredCallback callback);

/** Stop device discovery. */
void ktm_stop_discovery(KtmHandle* handle);

// ============================================================
// Send
// ============================================================

/**
 * Send a file to a remote device.
 * Returns a session_id string — caller must free with ktm_free_string().
 * Returns NULL on error.
 */
char* ktm_send_file(
    KtmHandle*                   handle,
    const char*                  target_ip,
    uint16_t                     target_port,
    const char*                  file_path,
    KtmTransferProgressCallback  progress_callback
);

// ============================================================
// Receive
// ============================================================

/**
 * Start the receive server listening on port 54124.
 * Returns 0 on success, negative on error.
 */
int ktm_receive_start(
    KtmHandle*                   handle,
    const char*                  receive_dir,
    KtmTransferRequestCallback   request_callback,
    KtmTransferProgressCallback  progress_callback
);

// ============================================================
// Session Control
// ============================================================

/** Cancel a transfer session by ID. */
void ktm_cancel(KtmHandle* handle, const char* session_id);

// ============================================================
// Utility
// ============================================================

/** Returns device ID. Caller must free with ktm_free_string(). */
char* ktm_get_device_id(KtmHandle* handle);

/** Returns library version string. Caller must free with ktm_free_string(). */
char* ktm_get_version(void);

/** Free a string returned by any ktm_* function. */
void ktm_free_string(char* ptr);

#ifdef __cplusplus
}
#endif

#endif /* KTM_H */

#include "ktm.h"
#include <stdlib.h>
#include <string.h>

struct KtmHandle {
    int active;
};

KtmHandle* ktm_init(void) {
    KtmHandle* h = (KtmHandle*)malloc(sizeof(KtmHandle));
    if (h) h->active = 1;
    return h;
}

void ktm_destroy(KtmHandle* handle) {
    if (handle) free(handle);
}

int ktm_start_discovery(KtmHandle* handle, KtmDeviceDiscoveredCallback callback) {
    (void)handle;
    (void)callback;
    return 0;
}

void ktm_stop_discovery(KtmHandle* handle) {
    (void)handle;
}

char* ktm_send_file(
    KtmHandle* handle,
    const char* target_ip,
    uint16_t target_port,
    const char* file_path,
    KtmTransferProgressCallback progress_callback
) {
    (void)handle;
    (void)target_ip;
    (void)target_port;
    (void)file_path;
    (void)progress_callback;
    return strdup("ktm-session-001");
}

int ktm_receive_start(
    KtmHandle* handle,
    const char* receive_dir,
    KtmTransferRequestCallback request_callback,
    KtmTransferProgressCallback progress_callback
) {
    (void)handle;
    (void)receive_dir;
    (void)request_callback;
    (void)progress_callback;
    return 0;
}

void ktm_cancel(KtmHandle* handle, const char* session_id) {
    (void)handle;
    (void)session_id;
}

char* ktm_get_device_id(KtmHandle* handle) {
    (void)handle;
    return strdup("ktm-android-device");
}

char* ktm_get_version(void) {
    return strdup("1.0.0");
}

void ktm_free_string(char* ptr) {
    if (ptr) free(ptr);
}

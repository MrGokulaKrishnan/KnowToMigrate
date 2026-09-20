#include <jni.h>
#include <android/log.h>
#include <string>
#include "ktm.h"

#define LOG_TAG "KtmJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================
// Lifecycle
// ============================================================

extern "C" JNIEXPORT jlong JNICALL
Java_com_knowtomigrate_app_jni_KtmJni_ktmInit(JNIEnv* env, jobject) {
    KtmHandle* h = ktm_init();
    LOGI("ktm_init -> handle=%p", h);
    return reinterpret_cast<jlong>(h);
}

extern "C" JNIEXPORT void JNICALL
Java_com_knowtomigrate_app_jni_KtmJni_ktmDestroy(JNIEnv* env, jobject, jlong handle) {
    ktm_destroy(reinterpret_cast<KtmHandle*>(handle));
}

// ============================================================
// Discovery
// ============================================================

static JavaVM* gVm = nullptr;
static jobject gDiscoveryCallback = nullptr;

static void onDeviceDiscovered(const char* deviceJson) {
    JNIEnv* env = nullptr;
    bool attached = false;
    int ret = gVm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (ret == JNI_EDETACHED) {
        gVm->AttachCurrentThread(&env, nullptr);
        attached = true;
    }
    if (!env || !gDiscoveryCallback) return;

    jclass cbClass = env->GetObjectClass(gDiscoveryCallback);
    jmethodID mid = env->GetMethodID(cbClass, "onDeviceDiscovered", "(Ljava/lang/String;)V");
    if (mid) {
        jstring jStr = env->NewStringUTF(deviceJson);
        env->CallVoidMethod(gDiscoveryCallback, mid, jStr);
        env->DeleteLocalRef(jStr);
    }
    if (attached) gVm->DetachCurrentThread();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_knowtomigrate_app_jni_KtmJni_ktmStartDiscovery(JNIEnv* env, jobject, jlong handle, jobject callback) {
    if (gDiscoveryCallback) env->DeleteGlobalRef(gDiscoveryCallback);
    gDiscoveryCallback = env->NewGlobalRef(callback);
    env->GetJavaVM(&gVm);
    return ktm_start_discovery(reinterpret_cast<KtmHandle*>(handle), onDeviceDiscovered);
}

extern "C" JNIEXPORT void JNICALL
Java_com_knowtomigrate_app_jni_KtmJni_ktmStopDiscovery(JNIEnv* env, jobject, jlong handle) {
    ktm_stop_discovery(reinterpret_cast<KtmHandle*>(handle));
}

// ============================================================
// Send
// ============================================================

static jobject gProgressCallback = nullptr;

static void onProgress(const char* sessionId, uint64_t bytesSent, uint64_t totalBytes, double speedBps, double etaSecs) {
    JNIEnv* env = nullptr;
    bool attached = false;
    int ret = gVm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (ret == JNI_EDETACHED) {
        gVm->AttachCurrentThread(&env, nullptr);
        attached = true;
    }
    if (!env || !gProgressCallback) return;

    jclass cbClass = env->GetObjectClass(gProgressCallback);
    jmethodID mid = env->GetMethodID(cbClass, "onProgress", "(Ljava/lang/String;JJDD)V");
    if (mid) {
        jstring jSid = env->NewStringUTF(sessionId);
        env->CallVoidMethod(gProgressCallback, mid, jSid,
            (jlong)bytesSent, (jlong)totalBytes, (jdouble)speedBps, (jdouble)etaSecs);
        env->DeleteLocalRef(jSid);
    }
    if (attached) gVm->DetachCurrentThread();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_knowtomigrate_app_jni_KtmJni_ktmSendFile(
        JNIEnv* env, jobject,
        jlong handle, jstring targetIp, jint targetPort, jstring filePath, jobject progressCb) {
    if (gProgressCallback) env->DeleteGlobalRef(gProgressCallback);
    gProgressCallback = env->NewGlobalRef(progressCb);
    env->GetJavaVM(&gVm);

    const char* ip = env->GetStringUTFChars(targetIp, nullptr);
    const char* path = env->GetStringUTFChars(filePath, nullptr);

    char* sessionId = ktm_send_file(
        reinterpret_cast<KtmHandle*>(handle),
        ip, (uint16_t)targetPort, path,
        onProgress
    );

    env->ReleaseStringUTFChars(targetIp, ip);
    env->ReleaseStringUTFChars(filePath, path);

    jstring result = nullptr;
    if (sessionId) {
        result = env->NewStringUTF(sessionId);
        ktm_free_string(sessionId);
    }
    return result;
}

// ============================================================
// Receive
// ============================================================

static jobject gRequestCallback = nullptr;

static int onTransferRequest(const char* deviceJson, const char* manifestJson) {
    JNIEnv* env = nullptr;
    bool attached = false;
    int ret = gVm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (ret == JNI_EDETACHED) {
        gVm->AttachCurrentThread(&env, nullptr);
        attached = true;
    }
    if (!env || !gRequestCallback) return 0;

    jclass cbClass = env->GetObjectClass(gRequestCallback);
    jmethodID mid = env->GetMethodID(cbClass, "onRequest", "(Ljava/lang/String;Ljava/lang/String;)Z");
    jboolean accepted = JNI_FALSE;
    if (mid) {
        jstring jDev = env->NewStringUTF(deviceJson);
        jstring jMan = env->NewStringUTF(manifestJson);
        accepted = env->CallBooleanMethod(gRequestCallback, mid, jDev, jMan);
        env->DeleteLocalRef(jDev);
        env->DeleteLocalRef(jMan);
    }
    if (attached) gVm->DetachCurrentThread();
    return accepted ? 1 : 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_knowtomigrate_app_jni_KtmJni_ktmReceiveStart(
        JNIEnv* env, jobject,
        jlong handle, jstring receiveDir, jobject requestCb, jobject progressCb) {
    if (gRequestCallback) env->DeleteGlobalRef(gRequestCallback);
    gRequestCallback = env->NewGlobalRef(requestCb);
    if (gProgressCallback) env->DeleteGlobalRef(gProgressCallback);
    gProgressCallback = env->NewGlobalRef(progressCb);
    env->GetJavaVM(&gVm);

    const char* dir = env->GetStringUTFChars(receiveDir, nullptr);
    int result = ktm_receive_start(
        reinterpret_cast<KtmHandle*>(handle),
        dir, onTransferRequest, onProgress
    );
    env->ReleaseStringUTFChars(receiveDir, dir);
    return result;
}

// ============================================================
// Session / Utility
// ============================================================

extern "C" JNIEXPORT void JNICALL
Java_com_knowtomigrate_app_jni_KtmJni_ktmCancel(JNIEnv* env, jobject, jlong handle, jstring sessionId) {
    const char* sid = env->GetStringUTFChars(sessionId, nullptr);
    ktm_cancel(reinterpret_cast<KtmHandle*>(handle), sid);
    env->ReleaseStringUTFChars(sessionId, sid);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_knowtomigrate_app_jni_KtmJni_ktmGetDeviceId(JNIEnv* env, jobject, jlong handle) {
    char* id = ktm_get_device_id(reinterpret_cast<KtmHandle*>(handle));
    jstring result = id ? env->NewStringUTF(id) : nullptr;
    if (id) ktm_free_string(id);
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_knowtomigrate_app_jni_KtmJni_ktmGetVersion(JNIEnv* env, jobject) {
    char* ver = ktm_get_version();
    jstring result = ver ? env->NewStringUTF(ver) : nullptr;
    if (ver) ktm_free_string(ver);
    return result;
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*) {
    gVm = vm;
    LOGI("KTM JNI loaded");
    return JNI_VERSION_1_6;
}

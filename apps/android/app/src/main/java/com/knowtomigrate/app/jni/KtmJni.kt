package com.knowtomigrate.app.jni

object KtmJni {
    init {
        try { System.loadLibrary("ktm_jni") } catch (_: Throwable) { /* pure Kotlin mode */ }
    }

    // Lifecycle
    external fun ktmInit(): Long
    external fun ktmDestroy(handle: Long)

    // Discovery
    external fun ktmStartDiscovery(handle: Long, callback: DeviceDiscoveredCallback): Int
    external fun ktmStopDiscovery(handle: Long)

    // Send
    external fun ktmSendFile(
        handle: Long,
        targetIp: String,
        targetPort: Int,
        filePath: String,
        progressCallback: TransferProgressCallback
    ): String?

    // Receive
    external fun ktmReceiveStart(
        handle: Long,
        receiveDir: String,
        requestCallback: TransferRequestCallback,
        progressCallback: TransferProgressCallback
    ): Int

    // Session
    external fun ktmCancel(handle: Long, sessionId: String)

    // Utility
    external fun ktmGetDeviceId(handle: Long): String?
    external fun ktmGetVersion(): String?

    // Callback interfaces
    fun interface DeviceDiscoveredCallback {
        fun onDeviceDiscovered(deviceJson: String)
    }

    fun interface TransferProgressCallback {
        fun onProgress(
            sessionId: String,
            bytesSent: Long,
            totalBytes: Long,
            speedBps: Double,
            etaSecs: Double
        )
    }

    fun interface TransferRequestCallback {
        /** Return true to accept the incoming transfer, false to reject */
        fun onRequest(deviceJson: String, manifestJson: String): Boolean
    }
}

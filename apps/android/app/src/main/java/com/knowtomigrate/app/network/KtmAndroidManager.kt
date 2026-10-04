package com.knowtomigrate.app.network

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.knowtomigrate.app.data.KtmPreferences
import com.knowtomigrate.app.data.TransferHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class KtmAndroidManager private constructor(private val context: Context) {

    val preferences = KtmPreferences.getInstance(context)
    val historyRepository = TransferHistoryRepository.getInstance(context)

    val localDeviceName: String
        get() = preferences.deviceName.value

    val localDeviceId: String by lazy {
        val base = localDeviceName.replace(Regex("[^a-zA-Z0-9-]"), "-").lowercase().take(20)
        "ktm-and-$base-${UUID.randomUUID().toString().take(6)}"
    }

    val downloadDirectory: File by lazy {
        try {
            val appSpecific = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (appSpecific != null && (appSpecific.exists() || appSpecific.mkdirs())) {
                val ktmDir = File(appSpecific, "KnowToMigrate")
                if (!ktmDir.exists()) ktmDir.mkdirs()
                return@lazy ktmDir
            }
        } catch (_: Exception) {}

        try {
            val fallback = File(context.filesDir, "KnowToMigrate")
            if (!fallback.exists()) fallback.mkdirs()
            return@lazy fallback
        } catch (_: Exception) {}

        File(context.cacheDir, "KnowToMigrate").apply { mkdirs() }
    }

    val transportManager = KtmTransportManager.getInstance(context)
    val discoveryService = KtmDiscoveryService(context, localDeviceId, localDeviceName)
    val transferServer by lazy { KtmTransferServer(downloadDirectory, KtmConstants.TRANSFER_PORT, context) }
    val transferClient = KtmTransferClient(context)

    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = discoveryService.devices
    val serverProgress: StateFlow<TransferProgressInfo> get() = transferServer.progress
    val clientProgress: StateFlow<TransferProgressInfo> = transferClient.progress

    fun start() {
        try {
            discoveryService.start()
            transferServer.start()
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            discoveryService.stop()
            transferServer.stop()
        } catch (_: Exception) {}
    }

    suspend fun evaluateTargetTransport(target: DiscoveredDevice): KtmTransportType {
        return transportManager.evaluateAndSelectBest(target)
    }

    suspend fun sendUris(target: DiscoveredDevice, uris: List<Uri>, forcedTransport: String? = null): Boolean {
        val selected = forcedTransport ?: target.bestTransport
        return transferClient.sendUris(
            targetIp = target.ipAddress,
            targetPort = target.transferPort,
            targetDeviceName = target.deviceName,
            localDeviceId = localDeviceId,
            localDeviceName = localDeviceName,
            uris = uris,
            selectedTransport = selected
        )
    }

    suspend fun sendClipboardText(targetIp: String, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("http://$targetIp:${KtmConstants.WEB_SHARE_PORT}/clipboard")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val jsonPayload = org.json.JSONObject().apply {
                put("text", text)
            }.toString()
            conn.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
            }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchClipboardText(targetIp: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("http://$targetIp:${KtmConstants.WEB_SHARE_PORT}/clipboard")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = org.json.JSONObject(response)
                if (obj.has("text")) obj.getString("text") else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: KtmAndroidManager? = null

        fun getInstance(context: Context): KtmAndroidManager {
            return instance ?: synchronized(this) {
                instance ?: KtmAndroidManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

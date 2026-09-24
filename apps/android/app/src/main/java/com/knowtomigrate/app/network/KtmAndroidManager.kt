package com.knowtomigrate.app.network

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

class KtmAndroidManager private constructor(private val context: Context) {

    val localDeviceId: String = "ktm-and-" + Build.MODEL.replace(" ", "-").lowercase() + "-" + UUID.randomUUID().toString().take(6)
    val localDeviceName: String = Build.MODEL ?: "Android Device"

    val downloadDirectory: File by lazy {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val ktmDir = File(publicDownloads, "KnowToMigrate")
        if (!ktmDir.exists()) ktmDir.mkdirs()
        if (ktmDir.canWrite()) ktmDir else File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "KnowToMigrate").apply { mkdirs() }
    }

    val discoveryService = KtmDiscoveryService(context, localDeviceId, localDeviceName)
    val transferServer = KtmTransferServer(downloadDirectory)
    val transferClient = KtmTransferClient(context)

    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = discoveryService.devices
    val serverProgress: StateFlow<TransferProgressInfo> = transferServer.progress
    val clientProgress: StateFlow<TransferProgressInfo> = transferClient.progress

    fun start() {
        discoveryService.start()
        transferServer.start()
    }

    fun stop() {
        discoveryService.stop()
        transferServer.stop()
    }

    suspend fun sendUris(target: DiscoveredDevice, uris: List<Uri>): Boolean {
        return transferClient.sendUris(
            targetIp = target.ipAddress,
            targetPort = target.transferPort,
            localDeviceId = localDeviceId,
            localDeviceName = localDeviceName,
            uris = uris
        )
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

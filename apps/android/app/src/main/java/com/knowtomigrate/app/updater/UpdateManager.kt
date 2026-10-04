package com.knowtomigrate.app.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.knowtomigrate.app.BuildConfig
import com.knowtomigrate.app.network.KtmAndroidManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.max

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val manifest: AndroidUpdateManifest) : UpdateState()
    data class Downloading(
        val manifest: AndroidUpdateManifest,
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedMBps: Double,
        val etaSecs: Int
    ) : UpdateState()
    data class Verifying(val manifest: AndroidUpdateManifest) : UpdateState()
    data class ReadyToInstall(val apkFile: File, val manifest: AndroidUpdateManifest) : UpdateState()
    data class QueuedAfterTransfer(val apkFile: File, val manifest: AndroidUpdateManifest) : UpdateState()
    data class Installing(val manifest: AndroidUpdateManifest) : UpdateState()
    data class Error(val message: String) : UpdateState()
    object UpToDate : UpdateState()
}

class UpdateManager private constructor(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null

    val updatesDir: File by lazy {
        File(context.filesDir, "updates").apply {
            if (!exists()) mkdirs()
        }
    }

    fun checkForUpdates() {
        if (_updateState.value is UpdateState.Checking || _updateState.value is UpdateState.Downloading) return

        _updateState.value = UpdateState.Checking
        scope.launch {
            try {
                val manifest = fetchManifest()
                if (manifest == null) {
                    _updateState.value = UpdateState.Error("Could not retrieve update manifest.")
                    return@launch
                }

                // Integer comparison of versionCode is strictly enforced
                if (manifest.versionCode > BuildConfig.VERSION_CODE) {
                    // Check if APK was already downloaded and verified
                    val existingApk = File(updatesDir, "KnowToMigrate-${manifest.versionName}.apk")
                    if (existingApk.exists() && existingApk.length() > 0 && verifySha256(existingApk, manifest.sha256)) {
                        _updateState.value = UpdateState.ReadyToInstall(existingApk, manifest)
                    } else {
                        _updateState.value = UpdateState.UpdateAvailable(manifest)
                    }
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Failed to check for updates.")
            }
        }
    }

    private suspend fun fetchManifest(): AndroidUpdateManifest? = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://knowtomigrate.web.app/updates/android/stable.json",
            "https://knowtomigrate.web.app/update-manifest.json"
        )

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "KnowToMigrate-Android/${BuildConfig.VERSION_NAME}")
                conn.setRequestProperty("Cache-Control", "no-cache")

                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val manifest = AndroidUpdateManifest.fromJson(body)
                    if (manifest != null && manifest.versionCode > 0) {
                        return@withContext manifest
                    }
                }
            } catch (_: Exception) {}
        }
        null
    }

    fun startDownload(manifest: AndroidUpdateManifest) {
        if (_updateState.value is UpdateState.Downloading) return

        downloadJob?.cancel()
        downloadJob = scope.launch {
            val partFile = File(updatesDir, "KnowToMigrate-${manifest.versionName}.apk.part")
            val targetFile = File(updatesDir, "KnowToMigrate-${manifest.versionName}.apk")

            try {
                if (partFile.exists()) partFile.delete()
                if (targetFile.exists()) targetFile.delete()

                val url = URL(manifest.apkUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("User-Agent", "KnowToMigrate-Android/${BuildConfig.VERSION_NAME}")

                if (conn.responseCode !in 200..299) {
                    _updateState.value = UpdateState.Error("Server returned HTTP ${conn.responseCode}")
                    return@launch
                }

                val totalBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else manifest.sizeBytes
                var downloadedBytes = 0L

                val buffer = ByteArray(64 * 1024)
                var lastSampleTime = System.currentTimeMillis()
                var lastSampleBytes = 0L
                var currentSpeedMBps = 0.0

                _updateState.value = UpdateState.Downloading(
                    manifest = manifest,
                    progress = 0f,
                    bytesDownloaded = 0,
                    totalBytes = totalBytes,
                    speedMBps = 0.0,
                    etaSecs = 0
                )

                conn.inputStream.use { input ->
                    FileOutputStream(partFile).use { output ->
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            ensureActive()
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val now = System.currentTimeMillis()
                            val elapsed = now - lastSampleTime
                            if (elapsed >= 500) {
                                val bytesInInterval = downloadedBytes - lastSampleBytes
                                currentSpeedMBps = (bytesInInterval.toDouble() / (1024.0 * 1024.0)) / (elapsed / 1000.0)
                                lastSampleTime = now
                                lastSampleBytes = downloadedBytes

                                val remainingBytes = max(0L, totalBytes - downloadedBytes)
                                val speedBytesPerSec = (currentSpeedMBps * 1024.0 * 1024.0).toLong()
                                val etaSecs = if (speedBytesPerSec > 0) (remainingBytes / speedBytesPerSec).toInt() else 0
                                val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

                                _updateState.value = UpdateState.Downloading(
                                    manifest = manifest,
                                    progress = progress,
                                    bytesDownloaded = downloadedBytes,
                                    totalBytes = totalBytes,
                                    speedMBps = currentSpeedMBps,
                                    etaSecs = etaSecs
                                )
                            }
                        }
                    }
                }

                // Download completed, verify SHA-256
                _updateState.value = UpdateState.Verifying(manifest)

                val verified = verifySha256(partFile, manifest.sha256)
                if (verified) {
                    if (partFile.renameTo(targetFile)) {
                        _updateState.value = UpdateState.ReadyToInstall(targetFile, manifest)
                    } else {
                        _updateState.value = UpdateState.ReadyToInstall(partFile, manifest)
                    }
                } else {
                    partFile.delete()
                    _updateState.value = UpdateState.Error("Cryptographic SHA-256 verification failed. File rejected.")
                }

            } catch (e: CancellationException) {
                partFile.delete()
                _updateState.value = UpdateState.UpdateAvailable(manifest)
            } catch (e: Exception) {
                partFile.delete()
                _updateState.value = UpdateState.Error(e.message ?: "Download failed.")
            }
        }
    }

    fun cancelDownload(manifest: AndroidUpdateManifest) {
        downloadJob?.cancel()
        downloadJob = null
        _updateState.value = UpdateState.UpdateAvailable(manifest)
    }

    private fun verifySha256(file: File, expectedHash: String): Boolean {
        if (expectedHash.isBlank()) return true // No hash provided on manifest
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                while (fis.read(buf).also { read = it } != -1) {
                    digest.update(buf, 0, read)
                }
            }
            val calculated = digest.digest().joinToString("") { "%02x".format(it) }
            calculated.equals(expectedHash.trim(), ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun isTransferActive(): Boolean {
        val manager = KtmAndroidManager.getInstance(context)
        val cp = manager.clientProgress.value
        val sp = manager.serverProgress.value

        val clientActive = !cp.isCompleted && cp.bytesTransferred > 0 && cp.bytesTransferred < cp.totalBytes
        val serverActive = !sp.isCompleted && sp.bytesTransferred > 0 && sp.bytesTransferred < sp.totalBytes
        return clientActive || serverActive
    }

    fun queueInstallAfterTransfer(file: File, manifest: AndroidUpdateManifest) {
        _updateState.value = UpdateState.QueuedAfterTransfer(file, manifest)
    }

    fun onTransferFinished() {
        val current = _updateState.value
        if (current is UpdateState.QueuedAfterTransfer) {
            installApk(current.apkFile, current.manifest)
        }
    }

    fun installApk(file: File, manifest: AndroidUpdateManifest) {
        if (!file.exists()) {
            _updateState.value = UpdateState.Error("APK file not found on disk.")
            return
        }

        // Android 8.0+ Unknown App Sources Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        try {
            _updateState.value = UpdateState.Installing(manifest)
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Failed to launch PackageInstaller: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var instance: UpdateManager? = null

        fun getInstance(context: Context): UpdateManager {
            return instance ?: synchronized(this) {
                instance ?: UpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

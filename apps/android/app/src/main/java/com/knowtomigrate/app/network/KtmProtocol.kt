package com.knowtomigrate.app.network

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object KtmConstants {
    const val DISCOVERY_PORT = 54123
    const val TRANSFER_PORT = 54124
    const val DISCOVERY_MAGIC = "KTM_DISCOVER"
    const val CHUNK_MAGIC = 0x4B544D43 // "KTMC"
    const val DEFAULT_CHUNK_SIZE = 256 * 1024 // 256 KB
    const val MAX_CHUNK_SIZE = 1024 * 1024 // 1 MB
    const val DISCOVERY_INTERVAL_MS = 2000L
    const val DEVICE_TIMEOUT_MS = 7000L
}

enum class KtmTransportType(val code: String, val displayName: String, val speedRating: String, val priority: Int) {
    WIFI_LAN("WIFI_LAN", "Wi-Fi LAN", "50–120+ MB/s", 1),
    WIFI_DIRECT("WIFI_DIRECT", "Wi-Fi Direct", "30–80 MB/s", 2),
    BLUETOOTH("BLUETOOTH", "Bluetooth", "1–2 MB/s", 3);

    companion object {
        fun fromCode(code: String): KtmTransportType {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: WIFI_LAN
        }
    }
}

data class DiscoveredDevice(
    val deviceId: String,
    val deviceName: String,
    val platform: String = "Android",
    val transferPort: Int = KtmConstants.TRANSFER_PORT,
    val version: String = "1.0.0",
    var ipAddress: String = "",
    var lastSeen: Long = System.currentTimeMillis(),
    val supportedTransports: List<String> = listOf("WIFI_LAN", "WIFI_DIRECT", "BLUETOOTH"),
    val wifiDirectName: String = "DIRECT-KM-" + deviceName.replace(Regex("[^a-zA-Z0-9-]"), "-").take(15),
    val wifiDirectPort: Int = KtmConstants.TRANSFER_PORT,
    val bluetoothAddress: String = "",
    var activeTransport: String = "WIFI_LAN",
    var bestTransport: String = "WIFI_LAN",
    var isWifiLanReachable: Boolean = false
) {
    val isOnline: Boolean
        get() = (System.currentTimeMillis() - lastSeen) < KtmConstants.DEVICE_TIMEOUT_MS

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("magic", KtmConstants.DISCOVERY_MAGIC)
        obj.put("deviceId", deviceId)
        obj.put("deviceName", deviceName)
        obj.put("platform", platform)
        obj.put("transferPort", transferPort)
        obj.put("version", version)
        val arr = JSONArray()
        supportedTransports.forEach { arr.put(it) }
        obj.put("supportedTransports", arr)
        obj.put("wifiDirectName", wifiDirectName)
        obj.put("wifiDirectPort", wifiDirectPort)
        obj.put("bluetoothAddress", bluetoothAddress)
        obj.put("bestTransport", bestTransport)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String, remoteIp: String): DiscoveredDevice? {
            return try {
                val obj = JSONObject(json)
                if (obj.optString("magic") != KtmConstants.DISCOVERY_MAGIC) return null
                val transports = mutableListOf<String>()
                val arr = obj.optJSONArray("supportedTransports")
                if (arr != null) {
                    for (i in 0 until arr.length()) transports.add(arr.getString(i))
                }
                if (transports.isEmpty()) {
                    transports.addAll(listOf("WIFI_LAN", "WIFI_DIRECT", "BLUETOOTH"))
                }

                val devName = obj.getString("deviceName")
                DiscoveredDevice(
                    deviceId = obj.getString("deviceId"),
                    deviceName = devName,
                    platform = obj.optString("platform", "Unknown"),
                    transferPort = obj.optInt("transferPort", KtmConstants.TRANSFER_PORT),
                    version = obj.optString("version", "1.0.0"),
                    ipAddress = remoteIp,
                    lastSeen = System.currentTimeMillis(),
                    supportedTransports = transports,
                    wifiDirectName = obj.optString("wifiDirectName", "DIRECT-KM-" + devName.take(15)),
                    wifiDirectPort = obj.optInt("wifiDirectPort", KtmConstants.TRANSFER_PORT),
                    bluetoothAddress = obj.optString("bluetoothAddress", ""),
                    bestTransport = obj.optString("bestTransport", "WIFI_LAN")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class KtmManifestItem(
    val fileIndex: Int,
    val relativePath: String,
    val size: Long,
    val sha256: String,
    val isFolder: Boolean = false
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("fileIndex", fileIndex)
        obj.put("relativePath", relativePath)
        obj.put("size", size)
        obj.put("sha256", sha256)
        obj.put("isFolder", isFolder)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): KtmManifestItem {
            return KtmManifestItem(
                fileIndex = obj.getInt("fileIndex"),
                relativePath = obj.getString("relativePath"),
                size = obj.getLong("size"),
                sha256 = obj.optString("sha256", ""),
                isFolder = obj.optBoolean("isFolder", false)
            )
        }
    }
}

data class KtmManifest(
    val sessionId: String = UUID.randomUUID().toString().replace("-", ""),
    val senderDeviceId: String,
    val senderDeviceName: String,
    val totalBytes: Long,
    val totalFiles: Int,
    val files: List<KtmManifestItem>
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("type", "MANIFEST")
        obj.put("sessionId", sessionId)
        obj.put("senderDeviceId", senderDeviceId)
        obj.put("senderDeviceName", senderDeviceName)
        obj.put("totalBytes", totalBytes)
        obj.put("totalFiles", totalFiles)
        val arr = JSONArray()
        files.forEach { arr.put(it.toJsonObject()) }
        obj.put("files", arr)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String): KtmManifest? {
            return try {
                val obj = JSONObject(json)
                if (obj.optString("type") != "MANIFEST") return null
                val arr = obj.getJSONArray("files")
                val list = mutableListOf<KtmManifestItem>()
                for (i in 0 until arr.length()) {
                    list.add(KtmManifestItem.fromJsonObject(arr.getJSONObject(i)))
                }
                KtmManifest(
                    sessionId = obj.getString("sessionId"),
                    senderDeviceId = obj.optString("senderDeviceId", ""),
                    senderDeviceName = obj.optString("senderDeviceName", ""),
                    totalBytes = obj.getLong("totalBytes"),
                    totalFiles = obj.getInt("totalFiles"),
                    files = list
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class KtmHandshake(
    val deviceId: String,
    val deviceName: String,
    val platform: String = "Android",
    val pin: String,
    val selectedTransport: String = "WIFI_LAN",
    val supportedTransports: List<String> = listOf("WIFI_LAN", "WIFI_DIRECT", "BLUETOOTH")
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("type", "HANDSHAKE")
        obj.put("deviceId", deviceId)
        obj.put("deviceName", deviceName)
        obj.put("platform", platform)
        obj.put("pin", pin)
        obj.put("selectedTransport", selectedTransport)
        val arr = JSONArray()
        supportedTransports.forEach { arr.put(it) }
        obj.put("supportedTransports", arr)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String): KtmHandshake? {
            return try {
                val obj = JSONObject(json)
                if (obj.optString("type") != "HANDSHAKE") return null
                val transports = mutableListOf<String>()
                val arr = obj.optJSONArray("supportedTransports")
                if (arr != null) {
                    for (i in 0 until arr.length()) transports.add(arr.getString(i))
                }
                if (transports.isEmpty()) {
                    transports.addAll(listOf("WIFI_LAN", "WIFI_DIRECT", "BLUETOOTH"))
                }
                KtmHandshake(
                    deviceId = obj.optString("deviceId"),
                    deviceName = obj.optString("deviceName"),
                    platform = obj.optString("platform", "Android"),
                    pin = obj.optString("pin"),
                    selectedTransport = obj.optString("selectedTransport", "WIFI_LAN"),
                    supportedTransports = transports
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class KtmHandshakeAck(
    val accepted: Boolean,
    val pin: String = "",
    val reason: String = "",
    val selectedTransport: String = "WIFI_LAN"
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("type", "HANDSHAKE_ACK")
        obj.put("accepted", accepted)
        obj.put("pin", pin)
        obj.put("reason", reason)
        obj.put("selectedTransport", selectedTransport)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String): KtmHandshakeAck? {
            return try {
                val obj = JSONObject(json)
                if (obj.optString("type") != "HANDSHAKE_ACK") return null
                KtmHandshakeAck(
                    accepted = obj.optBoolean("accepted", false),
                    pin = obj.optString("pin", ""),
                    reason = obj.optString("reason", ""),
                    selectedTransport = obj.optString("selectedTransport", "WIFI_LAN")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

enum class TransferDirection(val code: String, val displayName: String) {
    SENDING("SENDING", "Sending"),
    RECEIVING("RECEIVING", "Receiving")
}

enum class TransferStatus(val code: String, val displayName: String) {
    IDLE("IDLE", "Idle"),
    CONNECTING("CONNECTING", "Connecting"),
    TRANSFERRING("TRANSFERRING", "Transferring"),
    VERIFYING("VERIFYING", "Verifying SHA-256"),
    COMPLETED("COMPLETED", "Transfer Complete"),
    FAILED("FAILED", "Transfer Failed"),
    PAUSED("PAUSED", "Paused"),
    RECONNECTING("RECONNECTING", "Reconnecting")
}

object KtmFormatting {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return if (digitGroups == 0) "$bytes B" else "%.2f %s".format(java.util.Locale.US, value, units[digitGroups])
    }

    fun formatSpeed(speedMBps: Double): String {
        return if (speedMBps < 0.1) {
            "%.1f KB/s".format(java.util.Locale.US, speedMBps * 1024.0)
        } else {
            "%.1f MB/s".format(java.util.Locale.US, speedMBps)
        }
    }
}

data class TransferProgressInfo(
    var sessionId: String = "",
    var currentFileName: String = "",
    var currentFileIndex: Int = 0,
    var totalFiles: Int = 0,
    var bytesTransferred: Long = 0,
    var totalBytes: Long = 0,
    var speedMBps: Double = 0.0,
    var startTimeMs: Long = System.currentTimeMillis(),
    var elapsedTimeMs: Long = 0L,
    var peerName: String = "",
    var status: TransferStatus = TransferStatus.IDLE,
    var direction: TransferDirection = TransferDirection.SENDING,
    var isCompleted: Boolean = false,
    var isCancelled: Boolean = false,
    var errorMessage: String = "",
    var transportType: String = "Wi-Fi (LAN)"
) {
    val percentage: Double
        get() = if (totalBytes > 0) Math.min(100.0, (bytesTransferred.toDouble() / totalBytes) * 100.0) else 0.0

    val remainingBytes: Long
        get() = Math.max(0L, totalBytes - bytesTransferred)

    val etaSeconds: Long
        get() {
            if (speedMBps <= 0.001 || remainingBytes <= 0) return 0L
            val bytesPerSec = speedMBps * 1024.0 * 1024.0
            return (remainingBytes / bytesPerSec).toLong()
        }

    fun getFormattedEta(): String {
        return when {
            status == TransferStatus.PAUSED -> "Paused"
            status == TransferStatus.RECONNECTING -> "Reconnecting..."
            status == TransferStatus.VERIFYING -> "Verifying..."
            status == TransferStatus.COMPLETED -> "00:00"
            status == TransferStatus.FAILED -> "Failed"
            speedMBps <= 0.01 || bytesTransferred < 32 * 1024 -> "Calculating time remaining..."
            else -> {
                val secs = etaSeconds
                val m = secs / 60
                val s = secs % 60
                "ETA %02d:%02d".format(java.util.Locale.US, m, s)
            }
        }
    }

    fun getFormattedElapsedTime(): String {
        val secs = (elapsedTimeMs / 1000L).coerceAtLeast(0L)
        val m = secs / 60
        val s = secs % 60
        return "%02d:%02d".format(java.util.Locale.US, m, s)
    }

    fun getFormattedTransferredSize(): String {
        return "${KtmFormatting.formatBytes(bytesTransferred)} / ${KtmFormatting.formatBytes(totalBytes)}"
    }
}

object KtmSecurityUtils {
    /**
     * Sanitizes relative paths to prevent directory traversal attacks (e.g. ../, drive letters, leading slashes).
     */
    fun sanitizeRelativePath(relativePath: String): String {
        if (relativePath.isBlank()) return "unnamed_file"

        var clean = relativePath.replace('\\', '/')
        if (clean.length >= 2 && clean[0].isLetter() && clean[1] == ':') {
            clean = clean.substring(2)
        }
        clean = clean.trimStart('/')

        val parts = clean.split("/").filter { it.isNotBlank() && it != "." && it != ".." }
        val safeParts = parts.map { part ->
            part.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_").trim()
        }.filter { it.isNotBlank() }

        if (safeParts.isEmpty()) {
            return "safe_file_" + UUID.randomUUID().toString().take(8)
        }
        return safeParts.joinToString(File.separator)
    }

    fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(65536)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val hash = digest.digest()
        return hash.joinToString("") { "%02X".format(it) }
    }

    fun generate6DigitPin(): String {
        val random = SecureRandom()
        val num = 100000 + random.nextInt(900000)
        return num.toString()
    }
}

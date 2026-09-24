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

data class DiscoveredDevice(
    val deviceId: String,
    val deviceName: String,
    val platform: String = "Android",
    val transferPort: Int = KtmConstants.TRANSFER_PORT,
    val version: String = "1.0.0",
    var ipAddress: String = "",
    var lastSeen: Long = System.currentTimeMillis()
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
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String, remoteIp: String): DiscoveredDevice? {
            return try {
                val obj = JSONObject(json)
                if (obj.optString("magic") != KtmConstants.DISCOVERY_MAGIC) return null
                DiscoveredDevice(
                    deviceId = obj.getString("deviceId"),
                    deviceName = obj.getString("deviceName"),
                    platform = obj.optString("platform", "Unknown"),
                    transferPort = obj.optInt("transferPort", KtmConstants.TRANSFER_PORT),
                    version = obj.optString("version", "1.0.0"),
                    ipAddress = remoteIp,
                    lastSeen = System.currentTimeMillis()
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

data class TransferProgressInfo(
    var sessionId: String = "",
    var currentFileName: String = "",
    var currentFileIndex: Int = 0,
    var totalFiles: Int = 0,
    var bytesTransferred: Long = 0,
    var totalBytes: Long = 0,
    var speedMBps: Double = 0.0,
    var peerName: String = "",
    var isCompleted: Boolean = false,
    var isCancelled: Boolean = false,
    var errorMessage: String = ""
) {
    val percentage: Double
        get() = if (totalBytes > 0) Math.min(100.0, (bytesTransferred.toDouble() / totalBytes) * 100.0) else 0.0
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

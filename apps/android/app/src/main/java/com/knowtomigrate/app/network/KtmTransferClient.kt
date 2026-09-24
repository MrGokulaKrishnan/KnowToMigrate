package com.knowtomigrate.app.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest

class KtmTransferClient(private val context: Context) {

    private val _progress = MutableStateFlow(TransferProgressInfo())
    val progress: StateFlow<TransferProgressInfo> = _progress.asStateFlow()

    suspend fun sendUris(
        targetIp: String,
        targetPort: Int,
        localDeviceId: String,
        localDeviceName: String,
        uris: List<Uri>
    ): Boolean = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext true

        val prog = TransferProgressInfo(
            peerName = targetIp,
            totalFiles = uris.size
        )

        try {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(targetIp, targetPort), 8000)

                val inputStream = DataInputStream(BufferedInputStream(socket.getInputStream(), 64 * 1024))
                val outputStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream(), 256 * 1024))

                // 1. Build Manifest from Uris
                val manifestItems = mutableListOf<KtmManifestItem>()
                var totalBytes = 0L

                uris.forEachIndexed { index, uri ->
                    val (name, size) = queryFileInfo(uri)
                    val sha = computeUriSha256(uri)
                    manifestItems.add(
                        KtmManifestItem(
                            fileIndex = index,
                            relativePath = name,
                            size = size,
                            sha256 = sha,
                            isFolder = false
                        )
                    )
                    totalBytes += size
                }

                val manifest = KtmManifest(
                    senderDeviceId = localDeviceId,
                    senderDeviceName = localDeviceName,
                    totalBytes = totalBytes,
                    totalFiles = manifestItems.size,
                    files = manifestItems
                )

                prog.sessionId = manifest.sessionId
                prog.totalBytes = totalBytes
                _progress.value = prog.copy()

                // 2. Handshake with 6-digit confirmation PIN
                val pin = KtmSecurityUtils.generate6DigitPin()
                val handshakeObj = JSONObject().apply {
                    put("type", "HANDSHAKE")
                    put("deviceId", localDeviceId)
                    put("deviceName", localDeviceName)
                    put("platform", "Android")
                    put("pin", pin)
                }
                writeLengthPrefixedString(outputStream, handshakeObj.toString())

                val ackJson = readLengthPrefixedString(inputStream)
                val ackObj = JSONObject(ackJson)
                if (!ackObj.optBoolean("accepted", false)) {
                    throw IOException("Transfer rejected by recipient: " + ackObj.optString("reason"))
                }

                // 3. Send Manifest
                writeLengthPrefixedString(outputStream, manifest.toJson())

                val manifestAckJson = readLengthPrefixedString(inputStream)
                val manifestAckObj = JSONObject(manifestAckJson)
                if (!manifestAckObj.optBoolean("accepted", false)) {
                    throw IOException("Manifest rejected by recipient: " + manifestAckObj.optString("reason"))
                }

                val existingOffsets = manifestAckObj.optJSONObject("existingOffsets") ?: JSONObject()

                // 4. Stream Chunks with Resume Support
                val chunkBuffer = ByteArray(KtmConstants.DEFAULT_CHUNK_SIZE)
                var lastTimerTime = System.currentTimeMillis()
                var bytesSinceTimer = 0L

                manifest.files.forEachIndexed { index, item ->
                    val uri = uris[index]
                    val resumeOffset = existingOffsets.optLong(item.fileIndex.toString(), 0L)
                    var fileBytesSent = resumeOffset

                    prog.currentFileName = item.relativePath
                    prog.currentFileIndex = index + 1
                    _progress.value = prog.copy()

                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        if (resumeOffset > 0) {
                            var skipped = 0L
                            while (skipped < resumeOffset) {
                                val s = stream.skip(resumeOffset - skipped)
                                if (s <= 0) break
                                skipped += s
                            }
                            prog.bytesTransferred += resumeOffset
                        }

                        while (fileBytesSent < item.size) {
                            val toRead = Math.min(chunkBuffer.size.toLong(), item.size - fileBytesSent).toInt()
                            val read = stream.read(chunkBuffer, 0, toRead)
                            if (read <= 0) break

                            // Write 20-byte frame header
                            outputStream.writeInt(KtmConstants.CHUNK_MAGIC)
                            outputStream.writeInt(item.fileIndex)
                            outputStream.writeLong(fileBytesSent)
                            outputStream.writeInt(read)
                            outputStream.write(chunkBuffer, 0, read)
                            outputStream.flush()

                            fileBytesSent += read
                            prog.bytesTransferred += read
                            bytesSinceTimer += read

                            val now = System.currentTimeMillis()
                            if (now - lastTimerTime >= 500) {
                                val secs = (now - lastTimerTime) / 1000.0
                                prog.speedMBps = (bytesSinceTimer / (1024.0 * 1024.0)) / secs
                                lastTimerTime = now
                                bytesSinceTimer = 0L
                                _progress.value = prog.copy()
                            }
                        }
                    }

                    // Read FILE_COMPLETE ack
                    val compJson = readLengthPrefixedString(inputStream)
                    val compObj = JSONObject(compJson)
                    if (compObj.optString("status") != "OK") {
                        throw IOException("Checksum verification failed on recipient for ${item.relativePath}")
                    }
                }

                // 5. Transfer Complete
                val doneJson = readLengthPrefixedString(inputStream)
                val doneObj = JSONObject(doneJson)

                prog.isCompleted = doneObj.optBoolean("success", false)
                _progress.value = prog.copy()
                return@withContext prog.isCompleted
            }
        } catch (e: Exception) {
            prog.errorMessage = e.message ?: "Transfer error"
            _progress.value = prog.copy()
            return@withContext false
        }
    }

    private fun queryFileInfo(uri: Uri): Pair<String, Long> {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {}

        return Pair(name, size)
    }

    private fun computeUriSha256(uri: Uri): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buf = ByteArray(65536)
                var r: Int
                while (stream.read(buf).also { r = it } != -1) {
                    digest.update(buf, 0, r)
                }
            }
            digest.digest().joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun readLengthPrefixedString(dis: DataInputStream): String {
        val len = dis.readInt()
        if (len <= 0 || len > 10 * 1024 * 1024) throw IOException("Invalid string length: $len")
        val bytes = ByteArray(len)
        dis.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun writeLengthPrefixedString(dos: DataOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        dos.writeInt(bytes.size)
        dos.write(bytes)
        dos.flush()
    }
}

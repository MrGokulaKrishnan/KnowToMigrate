package com.knowtomigrate.app.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
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
        targetDeviceName: String = targetIp,
        localDeviceId: String,
        localDeviceName: String,
        uris: List<Uri>,
        selectedTransport: String = "WIFI_LAN"
    ): Boolean = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext true

        val friendlyTransport = KtmTransportType.fromCode(selectedTransport).displayName
        val historyRepo = com.knowtomigrate.app.data.TransferHistoryRepository.getInstance(context)
        val prog = TransferProgressInfo(
            peerName = targetDeviceName,
            totalFiles = uris.size,
            transportType = friendlyTransport
        )

        try {
            Log.i("KtmTransferClient", "[TRANSFER_CREATED] Target=$targetIp:$targetPort, Files=${uris.size}")
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(targetIp, targetPort), 10000)

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
                    Log.i("KtmTransferClient", "[MANIFEST_ITEM] #$index: '$name', size=$size bytes, SHA=$sha")
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

                val mainFileName = manifestItems.firstOrNull()?.relativePath ?: "Files"
                val displayTitle = if (manifestItems.size > 1) "$mainFileName (+${manifestItems.size - 1} more)" else mainFileName
                historyRepo.insert(
                    com.knowtomigrate.app.data.TransferRecord(
                        sessionId = manifest.sessionId,
                        fileName = displayTitle,
                        fileCount = manifestItems.size,
                        totalBytes = totalBytes,
                        bytesTransferred = 0L,
                        peerDeviceName = targetDeviceName,
                        direction = com.knowtomigrate.app.data.TransferDirection.SENT,
                        status = com.knowtomigrate.app.data.TransferRecordStatus.CONNECTING,
                        transport = selectedTransport
                    )
                )

                // 2. Handshake with 6-digit confirmation PIN
                val pin = KtmSecurityUtils.generate6DigitPin()
                val handshakeObj = JSONObject().apply {
                    put("type", "HANDSHAKE")
                    put("deviceId", localDeviceId)
                    put("deviceName", localDeviceName)
                    put("platform", "Android")
                    put("pin", pin)
                    put("selectedTransport", selectedTransport)
                }
                Log.i("KtmTransferClient", "[HANDSHAKE_SEND] PIN=$pin, Sender=$localDeviceName, Transport=$selectedTransport")
                writeLengthPrefixedString(outputStream, handshakeObj.toString())

                val ackJson = readLengthPrefixedString(inputStream)
                val ackObj = JSONObject(ackJson)
                if (!ackObj.optBoolean("accepted", false)) {
                    val reason = ackObj.optString("reason", "Rejected by recipient")
                    Log.w("KtmTransferClient", "[HANDSHAKE_REJECTED] Reason: $reason")
                    historyRepo.updateProgress(manifest.sessionId, com.knowtomigrate.app.data.TransferRecordStatus.CANCELLED, 0L, totalBytes, reason)
                    throw IOException("Transfer rejected by recipient: $reason")
                }
                Log.i("KtmTransferClient", "[HANDSHAKE_ACCEPTED] Recipient confirmed PIN")
                historyRepo.updateProgress(manifest.sessionId, com.knowtomigrate.app.data.TransferRecordStatus.TRANSFERRING, 0L, totalBytes)

                // 3. Send Manifest
                writeLengthPrefixedString(outputStream, manifest.toJson())

                val manifestAckJson = readLengthPrefixedString(inputStream)
                val manifestAckObj = JSONObject(manifestAckJson)
                if (!manifestAckObj.optBoolean("accepted", false)) {
                    val reason = manifestAckObj.optString("reason", "Manifest rejected")
                    Log.w("KtmTransferClient", "[MANIFEST_REJECTED] Reason: $reason")
                    throw IOException("Manifest rejected by recipient: $reason")
                }

                val existingOffsets = manifestAckObj.optJSONObject("existingOffsets") ?: JSONObject()
                Log.i("KtmTransferClient", "[MANIFEST_ACCEPTED] SessionId=${manifest.sessionId}, Offsets=$existingOffsets")

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

                    if (item.size > 0L) {
                        val stream = context.contentResolver.openInputStream(uri)
                            ?: throw IOException("Cannot open input stream for ${item.relativePath} (URI: $uri)")

                        stream.use { inStream ->
                            Log.i("KtmTransferClient", "[FILE_OPEN_SUCCESS] '${item.relativePath}', resumeOffset=$resumeOffset, targetSize=${item.size}")
                            if (resumeOffset > 0L) {
                                var skipped = 0L
                                while (skipped < resumeOffset) {
                                    val s = inStream.skip(resumeOffset - skipped)
                                    if (s <= 0L) break
                                    skipped += s
                                }
                                prog.bytesTransferred += resumeOffset
                            }

                            while (fileBytesSent < item.size) {
                                val toRead = Math.min(chunkBuffer.size.toLong(), item.size - fileBytesSent).toInt()
                                val read = inStream.read(chunkBuffer, 0, toRead)
                                if (read <= 0) break

                                // Write 20-byte frame header (Big-Endian)
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
                        Log.i("KtmTransferClient", "[BYTES_SENT] '${item.relativePath}': $fileBytesSent/${item.size} bytes sent")
                    } else {
                        Log.i("KtmTransferClient", "[ZERO_BYTE_FILE] '${item.relativePath}' is 0 bytes, skipping chunk streaming")
                    }

                    // Read FILE_COMPLETE ack from receiver
                    val compJson = readLengthPrefixedString(inputStream)
                    val compObj = JSONObject(compJson)
                    val status = compObj.optString("status", "")
                    if (status != "OK") {
                        val err = "Recipient failed verification for ${item.relativePath} (status: $status)"
                        Log.e("KtmTransferClient", "[FILE_VERIFY_FAILED] $err")
                        throw IOException(err)
                    }
                    Log.i("KtmTransferClient", "[FILE_VERIFIED] '${item.relativePath}' confirmed OK by recipient")
                }

                // 5. Transfer Complete
                val doneJson = readLengthPrefixedString(inputStream)
                val doneObj = JSONObject(doneJson)

                prog.isCompleted = doneObj.optBoolean("success", false)
                _progress.value = prog.copy()
                Log.i("KtmTransferClient", "[TRANSFER_COMPLETED] Success=${prog.isCompleted}")
                if (prog.isCompleted) {
                    historyRepo.updateProgress(manifest.sessionId, com.knowtomigrate.app.data.TransferRecordStatus.COMPLETED, totalBytes, totalBytes)
                } else {
                    historyRepo.updateProgress(manifest.sessionId, com.knowtomigrate.app.data.TransferRecordStatus.FAILED, prog.bytesTransferred, totalBytes, "Transfer failed at destination")
                }
                return@withContext prog.isCompleted
            }
        } catch (e: Exception) {
            Log.e("KtmTransferClient", "[TRANSFER_ERROR] ${e.message}", e)
            prog.errorMessage = e.message ?: "Transfer error"
            _progress.value = prog.copy()
            if (prog.sessionId.isNotBlank()) {
                historyRepo.updateProgress(prog.sessionId, com.knowtomigrate.app.data.TransferRecordStatus.FAILED, prog.bytesTransferred, prog.totalBytes, e.message)
            }
            return@withContext false
        }
    }

    private fun queryFileInfo(uri: Uri): Pair<String, Long> {
        var name = "file_${System.currentTimeMillis()}"
        var size = -1L

        // Primary: Query Android ContentResolver OpenableColumns
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        val n = cursor.getString(nameIndex)
                        if (!n.isNullOrBlank()) name = n
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        val s = cursor.getLong(sizeIndex)
                        if (s >= 0) size = s
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("KtmTransferClient", "Cursor query failed for $uri", e)
        }

        // Fallback 1: AssetFileDescriptor length
        if (size <= 0) {
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    val afdLen = afd.length
                    if (afdLen > 0) size = afdLen
                }
            } catch (_: Exception) {}
        }

        // Fallback 2: ParcelFileDescriptor statSize
        if (size <= 0) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val pfdSize = pfd.statSize
                    if (pfdSize > 0) size = pfdSize
                }
            } catch (_: Exception) {}
        }

        // Fallback 3: Measure stream length directly
        if (size <= 0) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    var count = 0L
                    val buf = ByteArray(65536)
                    var r: Int
                    while (stream.read(buf).also { r = it } != -1) {
                        count += r
                    }
                    size = count
                }
            } catch (e: Exception) {
                Log.w("KtmTransferClient", "Stream byte measurement failed for $uri", e)
            }
        }

        // Name fallback from URI path if generic
        if (name.startsWith("file_")) {
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
                val clean = lastSegment.substringAfterLast('/').substringAfterLast(':')
                if (clean.isNotBlank()) name = clean
            }
        }

        if (size < 0) size = 0L
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
            Log.w("KtmTransferClient", "Failed to compute SHA-256 for $uri", e)
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

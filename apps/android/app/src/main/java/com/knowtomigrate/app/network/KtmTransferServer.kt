package com.knowtomigrate.app.network

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class KtmTransferServer(
    private val downloadDir: File,
    private val port: Int = KtmConstants.TRANSFER_PORT,
    private val context: Context? = null
) {
    private var scope: CoroutineScope? = null
    private var serverSocket: ServerSocket? = null

    var onHandshakeReceived: ((deviceName: String, pin: String, transport: String) -> Boolean)? = null

    private val _progress = MutableStateFlow(TransferProgressInfo())
    val progress: StateFlow<TransferProgressInfo> = _progress.asStateFlow()

    init {
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
    }

    fun start() {
        if (scope != null) return
        val handler = CoroutineExceptionHandler { _, t ->
            Log.w("KtmTransferServer", "Handled server exception", t)
        }
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + handler)

        scope?.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(port))
                }
                Log.i("KtmTransferServer", "[SERVER_STARTED] Listening on TCP port $port, dir=${downloadDir.absolutePath}")
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    Log.i("KtmTransferServer", "[CLIENT_CONNECTED] Remote=${client.remoteSocketAddress}")
                    launch {
                        try {
                            handleClient(client)
                        } catch (t: Throwable) {
                            Log.e("KtmTransferServer", "[CLIENT_ERROR] ${t.message}", t)
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.w("KtmTransferServer", "[SERVER_STOPPED] ${t.message}")
            }
        }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        Log.i("KtmTransferServer", "[SERVER_STOPPED]")
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            s.tcpNoDelay = true
            val inputStream = DataInputStream(BufferedInputStream(s.getInputStream(), 256 * 1024))
            val outputStream = DataOutputStream(BufferedOutputStream(s.getOutputStream(), 64 * 1024))

            val prog = TransferProgressInfo()

            try {
                // 1. Handshake
                val handshakeJson = readLengthPrefixedString(inputStream)
                val handshakeObj = JSONObject(handshakeJson)
                val senderName = handshakeObj.optString("deviceName", "Remote Device")
                val pin = handshakeObj.optString("pin", "000000")
                val senderTransport = handshakeObj.optString("selectedTransport", "WIFI_LAN")
                val friendlyTransport = KtmTransportType.fromCode(senderTransport).displayName
                prog.transportType = friendlyTransport
                Log.i("KtmTransferServer", "[HANDSHAKE_RECEIVED] Sender='$senderName', PIN=$pin, Transport=$senderTransport")

                val accepted = onHandshakeReceived?.invoke(senderName, pin, friendlyTransport) ?: true
                val ackObj = JSONObject().apply {
                    put("type", "HANDSHAKE_ACK")
                    put("accepted", accepted)
                    put("pin", pin)
                    put("selectedTransport", senderTransport)
                    put("reason", if (accepted) "" else "Rejected by user")
                }
                writeLengthPrefixedString(outputStream, ackObj.toString())
                Log.i("KtmTransferServer", "[HANDSHAKE_ACK] Accepted=$accepted, ConfirmedTransport=$senderTransport")

                if (!accepted) return

                // 2. Manifest
                val manifestJson = readLengthPrefixedString(inputStream)
                val manifest = KtmManifest.fromJson(manifestJson) ?: throw IOException("Invalid manifest")

                prog.sessionId = manifest.sessionId
                prog.peerName = senderName
                prog.totalFiles = manifest.totalFiles
                prog.totalBytes = manifest.totalBytes
                _progress.value = prog.copy()
                Log.i("KtmTransferServer", "[MANIFEST_RECEIVED] Session=${manifest.sessionId}, Files=${manifest.totalFiles}, Bytes=${manifest.totalBytes}")

                // Storage preflight
                if (downloadDir.usableSpace < manifest.totalBytes) {
                    val rejectObj = JSONObject().apply {
                        put("type", "MANIFEST_ACK")
                        put("accepted", false)
                        put("reason", "Insufficient storage space on Android device")
                    }
                    writeLengthPrefixedString(outputStream, rejectObj.toString())
                    Log.w("KtmTransferServer", "[MANIFEST_REJECTED] Insufficient storage space")
                    return
                }

                // Check existing .part files for resume
                val existingOffsets = JSONObject()
                var totalAlready = 0L

                for (item in manifest.files) {
                    val safeRel = KtmSecurityUtils.sanitizeRelativePath(item.relativePath)
                    val targetFile = File(downloadDir, safeRel)
                    val partFile = File(downloadDir, "$safeRel.part")

                    if (partFile.exists() && partFile.length() < item.size) {
                        val len = partFile.length()
                        existingOffsets.put(item.fileIndex.toString(), len)
                        totalAlready += len
                    } else {
                        if (partFile.exists()) partFile.delete()
                        existingOffsets.put(item.fileIndex.toString(), 0L)
                    }
                }

                prog.bytesTransferred = totalAlready

                val historyRepo = context?.let { com.knowtomigrate.app.data.TransferHistoryRepository.getInstance(it) }
                val mainFileName = manifest.files.firstOrNull()?.relativePath ?: "Files"
                val displayTitle = if (manifest.totalFiles > 1) "$mainFileName (+${manifest.totalFiles - 1} more)" else mainFileName
                historyRepo?.insert(
                    com.knowtomigrate.app.data.TransferRecord(
                        sessionId = manifest.sessionId,
                        fileName = displayTitle,
                        fileCount = manifest.totalFiles,
                        totalBytes = manifest.totalBytes,
                        bytesTransferred = totalAlready,
                        peerDeviceName = senderName,
                        direction = com.knowtomigrate.app.data.TransferDirection.RECEIVED,
                        status = com.knowtomigrate.app.data.TransferRecordStatus.TRANSFERRING,
                        transport = senderTransport
                    )
                )

                val manifestAck = JSONObject().apply {
                    put("type", "MANIFEST_ACK")
                    put("sessionId", manifest.sessionId)
                    put("accepted", true)
                    put("existingOffsets", existingOffsets)
                }
                writeLengthPrefixedString(outputStream, manifestAck.toString())
                Log.i("KtmTransferServer", "[MANIFEST_ACK_SENT] Ready for chunk streaming")

                // 3. Receive Chunks
                val speedTimer = System.currentTimeMillis()
                var lastTimerTime = speedTimer
                var bytesSinceTimer = 0L

                for (item in manifest.files) {
                    val safeRel = KtmSecurityUtils.sanitizeRelativePath(item.relativePath)
                    val targetFile = File(downloadDir, safeRel)
                    targetFile.parentFile?.mkdirs()

                    val partFile = File(downloadDir, "$safeRel.part")
                    partFile.parentFile?.mkdirs()

                    val resumeOffset = existingOffsets.optLong(item.fileIndex.toString(), 0L)
                    var currentOffset = resumeOffset

                    prog.currentFileName = safeRel
                    prog.currentFileIndex = item.fileIndex + 1
                    _progress.value = prog.copy()
                    Log.i("KtmTransferServer", "[RECEIVING_FILE] #$prog.currentFileIndex: '$safeRel', targetSize=${item.size}, resumeOffset=$resumeOffset")

                    RandomAccessFile(partFile, "rw").use { raf ->
                        raf.seek(currentOffset)

                        while (currentOffset < item.size) {
                            val magic = inputStream.readInt()
                            if (magic != KtmConstants.CHUNK_MAGIC) {
                                throw IOException("Invalid frame magic: 0x${Integer.toHexString(magic)}")
                            }

                            val chunkFileIdx = inputStream.readInt()
                            val chunkOffset = inputStream.readLong()
                            val payloadLen = inputStream.readInt()

                            if (payloadLen <= 0 || payloadLen > KtmConstants.MAX_CHUNK_SIZE) {
                                throw IOException("Invalid payload length: $payloadLen")
                            }

                            val payload = ByteArray(payloadLen)
                            inputStream.readFully(payload)

                            raf.seek(chunkOffset)
                            raf.write(payload)

                            currentOffset += payloadLen
                            prog.bytesTransferred += payloadLen
                            bytesSinceTimer += payloadLen

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

                    Log.i("KtmTransferServer", "[CHUNKS_COMPLETE] '$safeRel' written to disk ($currentOffset bytes), verifying SHA-256")

                    // 4. Verify SHA-256
                    val computedSha = KtmSecurityUtils.computeFileSha256(partFile)
                    if (item.sha256.isNotBlank() && !computedSha.equals(item.sha256, ignoreCase = true)) {
                        partFile.delete()
                        val compObj = JSONObject().apply {
                            put("type", "FILE_COMPLETE")
                            put("fileIndex", item.fileIndex)
                            put("status", "HASH_MISMATCH")
                            put("sha256", computedSha)
                        }
                        writeLengthPrefixedString(outputStream, compObj.toString())
                        Log.e("KtmTransferServer", "[HASH_MISMATCH] '$safeRel' Expected=${item.sha256}, Got=$computedSha")
                        throw IOException("SHA-256 mismatch for ${item.relativePath}")
                    }

                    // 5. Finalize file: atomic rename with copy fallback
                    if (targetFile.exists()) targetFile.delete()
                    val renamed = partFile.renameTo(targetFile)
                    if (!renamed) {
                        Log.w("KtmTransferServer", "[RENAME_FALLBACK] Atomic rename returned false, copying bytes to target")
                        partFile.inputStream().use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        partFile.delete()
                    }

                    if (!targetFile.exists() || targetFile.length() != item.size) {
                        throw IOException("Failed to save complete file: ${targetFile.absolutePath}")
                    }

                    Log.i("KtmTransferServer", "[FILE_SAVED_SUCCESS] '${targetFile.absolutePath}', size=${targetFile.length()} bytes, SHA=$computedSha")

                    // 6. Export to Public Downloads and notify MediaScanner
                    exportToPublicDownloadsAndScan(targetFile, safeRel)

                    val compObj = JSONObject().apply {
                        put("type", "FILE_COMPLETE")
                        put("fileIndex", item.fileIndex)
                        put("status", "OK")
                        put("sha256", computedSha)
                    }
                    writeLengthPrefixedString(outputStream, compObj.toString())
                }

                // 7. Transfer Complete
                val doneObj = JSONObject().apply {
                    put("type", "TRANSFER_COMPLETE")
                    put("sessionId", manifest.sessionId)
                    put("success", true)
                }
                writeLengthPrefixedString(outputStream, doneObj.toString())

                prog.isCompleted = true
                _progress.value = prog.copy()
                Log.i("KtmTransferServer", "[TRANSFER_ALL_DONE] Session=${manifest.sessionId}, TotalBytes=${prog.bytesTransferred}")
                historyRepo?.updateProgress(manifest.sessionId, com.knowtomigrate.app.data.TransferRecordStatus.COMPLETED, prog.totalBytes, prog.totalBytes)
            } catch (e: Exception) {
                Log.e("KtmTransferServer", "[TRANSFER_SERVER_ERROR] ${e.message}", e)
                prog.errorMessage = e.message ?: "Transfer error"
                _progress.value = prog.copy()
                if (prog.sessionId.isNotBlank()) {
                    val historyRepo = context?.let { com.knowtomigrate.app.data.TransferHistoryRepository.getInstance(it) }
                    historyRepo?.updateProgress(prog.sessionId, com.knowtomigrate.app.data.TransferRecordStatus.FAILED, prog.bytesTransferred, prog.totalBytes, e.message)
                }
            }
        }
    }

    private fun exportToPublicDownloadsAndScan(savedFile: File, relativePath: String) {
        try {
            // Also save a copy to public Downloads/KnowToMigrate folder
            val publicDownloads = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "KnowToMigrate"
            )
            if (!publicDownloads.exists()) publicDownloads.mkdirs()

            val publicFile = File(publicDownloads, relativePath)
            publicFile.parentFile?.mkdirs()

            if (publicFile.canonicalPath != savedFile.canonicalPath) {
                try {
                    savedFile.copyTo(publicFile, overwrite = true)
                    Log.i("KtmTransferServer", "[PUBLIC_EXPORT_SUCCESS] '${publicFile.absolutePath}'")
                } catch (e: Exception) {
                    Log.w("KtmTransferServer", "Could not copy to public Downloads directory", e)
                }
            }

            // Notify Android MediaScanner so file appears in Gallery / Files app immediately
            if (context != null) {
                val pathsToScan = mutableListOf(savedFile.absolutePath)
                if (publicFile.exists()) pathsToScan.add(publicFile.absolutePath)

                MediaScannerConnection.scanFile(
                    context,
                    pathsToScan.toTypedArray(),
                    null
                ) { path, uri ->
                    Log.i("KtmTransferServer", "[MEDIA_SCAN_DONE] Path=$path, Uri=$uri")
                }

                try {
                    val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                        data = Uri.fromFile(if (publicFile.exists()) publicFile else savedFile)
                    }
                    context.sendBroadcast(scanIntent)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w("KtmTransferServer", "Error in exportToPublicDownloadsAndScan", e)
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

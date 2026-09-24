package com.knowtomigrate.app.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest

class KtmTransferServer(
    private val downloadDir: File,
    private val port: Int = KtmConstants.TRANSFER_PORT
) {
    private var scope: CoroutineScope? = null
    private var serverSocket: ServerSocket? = null

    var onHandshakeReceived: ((deviceName: String, pin: String) -> Boolean)? = null

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
            android.util.Log.w("KtmTransferServer", "Handled server exception", t)
        }
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + handler)

        scope?.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(port))
                }
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    launch {
                        try {
                            handleClient(client)
                        } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {
                // Server closed or port bound
            }
        }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
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

                val accepted = onHandshakeReceived?.invoke(senderName, pin) ?: true
                val ackObj = JSONObject()
                ackObj.put("type", "HANDSHAKE_ACK")
                ackObj.put("accepted", accepted)
                ackObj.put("pin", pin)
                ackObj.put("reason", if (accepted) "" else "Rejected by user")
                writeLengthPrefixedString(outputStream, ackObj.toString())

                if (!accepted) return

                // 2. Manifest
                val manifestJson = readLengthPrefixedString(inputStream)
                val manifest = KtmManifest.fromJson(manifestJson) ?: throw IOException("Invalid manifest")

                prog.sessionId = manifest.sessionId
                prog.peerName = senderName
                prog.totalFiles = manifest.totalFiles
                prog.totalBytes = manifest.totalBytes
                _progress.value = prog.copy()

                // Check free space
                if (downloadDir.usableSpace < manifest.totalBytes) {
                    val rejectObj = JSONObject()
                    rejectObj.put("type", "MANIFEST_ACK")
                    rejectObj.put("accepted", false)
                    rejectObj.put("reason", "Insufficient storage space on Android device")
                    writeLengthPrefixedString(outputStream, rejectObj.toString())
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

                val manifestAck = JSONObject()
                manifestAck.put("type", "MANIFEST_ACK")
                manifestAck.put("sessionId", manifest.sessionId)
                manifestAck.put("accepted", true)
                manifestAck.put("existingOffsets", existingOffsets)
                writeLengthPrefixedString(outputStream, manifestAck.toString())

                // 3. Receive Chunks
                val speedTimer = System.currentTimeMillis()
                var lastTimerTime = speedTimer
                var bytesSinceTimer = 0L

                for (item in manifest.files) {
                    val safeRel = KtmSecurityUtils.sanitizeRelativePath(item.relativePath)
                    val targetFile = File(downloadDir, safeRel)
                    targetFile.parentFile?.mkdirs()

                    val partFile = File(downloadDir, "$safeRel.part")
                    val resumeOffset = existingOffsets.optLong(item.fileIndex.toString(), 0L)
                    var currentOffset = resumeOffset

                    prog.currentFileName = safeRel
                    prog.currentFileIndex = item.fileIndex + 1
                    _progress.value = prog.copy()

                    RandomAccessFile(partFile, "rw").use { raf ->
                        raf.seek(currentOffset)

                        while (currentOffset < item.size) {
                            // Read 20-byte frame header
                            val magic = inputStream.readInt()
                            if (magic != KtmConstants.CHUNK_MAGIC) {
                                throw IOException("Invalid frame magic: $magic")
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

                    // 4. Verify SHA-256
                    val computedSha = KtmSecurityUtils.computeFileSha256(partFile)
                    if (item.sha256.isNotBlank() && !computedSha.equals(item.sha256, ignoreCase = true)) {
                        partFile.delete()
                        val compObj = JSONObject()
                        compObj.put("type", "FILE_COMPLETE")
                        compObj.put("fileIndex", item.fileIndex)
                        compObj.put("status", "HASH_MISMATCH")
                        writeLengthPrefixedString(outputStream, compObj.toString())
                        throw IOException("SHA-256 mismatch for ${item.relativePath}")
                    }

                    if (targetFile.exists()) targetFile.delete()
                    partFile.renameTo(targetFile)

                    val compObj = JSONObject()
                    compObj.put("type", "FILE_COMPLETE")
                    compObj.put("fileIndex", item.fileIndex)
                    compObj.put("status", "OK")
                    writeLengthPrefixedString(outputStream, compObj.toString())
                }

                // 5. Transfer Complete
                val doneObj = JSONObject()
                doneObj.put("type", "TRANSFER_COMPLETE")
                doneObj.put("sessionId", manifest.sessionId)
                doneObj.put("success", true)
                writeLengthPrefixedString(outputStream, doneObj.toString())

                prog.isCompleted = true
                _progress.value = prog.copy()
            } catch (e: Exception) {
                prog.errorMessage = e.message ?: "Transfer error"
                _progress.value = prog.copy()
            }
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

package com.knowtomigrate.app.data

import java.text.SimpleDateFormat
import java.util.*

enum class TransferDirection {
    SENT,
    RECEIVED
}

enum class TransferRecordStatus {
    PREPARING,
    CONNECTING,
    WAITING_ACCEPTANCE,
    TRANSFERRING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED;

    val displayName: String
        get() = when (this) {
            PREPARING -> "Preparing"
            CONNECTING -> "Connecting"
            WAITING_ACCEPTANCE -> "Waiting Acceptance"
            TRANSFERRING -> "Transferring"
            VERIFYING -> "Verifying"
            COMPLETED -> "Completed"
            FAILED -> "Failed"
            CANCELLED -> "Cancelled"
        }
}

data class TransferRecord(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "",
    val fileName: String,
    val fileCount: Int = 1,
    val totalBytes: Long = 0L,
    val bytesTransferred: Long = 0L,
    val peerDeviceName: String,
    val peerDeviceId: String = "",
    val direction: TransferDirection,
    val status: TransferRecordStatus,
    val transport: String = "WIFI_LAN",
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    val formattedSize: String
        get() = formatBytes(totalBytes)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val progressFraction: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1_073_741_824L -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
                bytes >= 1_048_576L     -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
                bytes >= 1_024L         -> String.format(Locale.US, "%.1f KB", bytes / 1_024.0)
                bytes > 0               -> "$bytes B"
                else                    -> "0 B"
            }
        }
    }
}

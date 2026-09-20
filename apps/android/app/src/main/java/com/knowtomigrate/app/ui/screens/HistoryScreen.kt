package com.knowtomigrate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.theme.*

data class TransferRecord(
    val id: String,
    val fileName: String,
    val sizeFormatted: String,
    val deviceName: String,
    val direction: TransferDirection,
    val status: TransferStatus,
    val dateFormatted: String,
)

enum class TransferDirection { SENT, RECEIVED }
enum class TransferStatus { COMPLETE, FAILED, IN_PROGRESS }

@Composable
fun HistoryScreen(navController: NavController) {
    // Sample data — in production this comes from Room DB
    val records = remember {
        listOf(
            TransferRecord("1", "Project_Assets.zip", "4.2 GB", "Krish's Laptop", TransferDirection.SENT, TransferStatus.COMPLETE, "Today, 14:32"),
            TransferRecord("2", "DCIM Photos", "1.8 GB", "Krish's Phone", TransferDirection.RECEIVED, TransferStatus.COMPLETE, "Today, 11:15"),
            TransferRecord("3", "Work Documents", "340 MB", "Home PC", TransferDirection.SENT, TransferStatus.COMPLETE, "Yesterday, 20:44"),
            TransferRecord("4", "Video_4K.mp4", "12.4 GB", "Krish's Laptop", TransferDirection.SENT, TransferStatus.FAILED, "Yesterday, 09:12"),
            TransferRecord("5", "Music Library", "6.1 GB", "Krish's Phone", TransferDirection.RECEIVED, TransferStatus.COMPLETE, "2 days ago"),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KmBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Transfer History",
                    color = KmTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = "${records.size} transfers",
                    color = KmTextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            if (records.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No transfers yet", color = KmTextMuted, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your transfer history will appear here", color = KmTextDisabled, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records) { record ->
                        TransferHistoryItem(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferHistoryItem(record: TransferRecord) {
    val statusColor = when (record.status) {
        TransferStatus.COMPLETE -> KmSuccess
        TransferStatus.FAILED -> KmError
        TransferStatus.IN_PROGRESS -> KmOrange
    }
    val statusLabel = when (record.status) {
        TransferStatus.COMPLETE -> "Complete"
        TransferStatus.FAILED -> "Failed"
        TransferStatus.IN_PROGRESS -> "In Progress"
    }
    val directionIcon = when (record.direction) {
        TransferDirection.SENT -> "↑"
        TransferDirection.RECEIVED -> "↓"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = KmGlassBackground,
        border = ButtonDefaults.outlinedButtonBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Direction indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KmOrangeGlow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = directionIcon,
                    color = KmOrange,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName,
                    color = KmTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.deviceName} · ${record.sizeFormatted}",
                    color = KmTextSecondary,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = record.dateFormatted,
                    color = KmTextMuted,
                    fontSize = 11.sp,
                )
            }

            // Status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

package com.knowtomigrate.app.ui.screens

/*
 * KnowToMigrate - HistoryScreen
 * Updated: 22 October 2026
 * AMOLED black, KM Orange accents, Liquid Glass surfaces, Grouped by day with verified SHA-256 badges
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.R
import com.knowtomigrate.app.data.TransferDirection
import com.knowtomigrate.app.data.TransferRecord
import com.knowtomigrate.app.data.TransferRecordStatus
import com.knowtomigrate.app.ui.components.KmBadge
import com.knowtomigrate.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val allRecords by manager.historyRepository.transfers.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Group records by day
    val groupedRecords = remember(allRecords) {
        groupRecordsByDay(allRecords)
    }

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.history_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = KmTextPrimary
                        )
                        Text(
                            text = stringResource(R.string.history_subtitle),
                            fontSize = 11.sp,
                            color = KmTextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = KmTextPrimary
                        )
                    }
                },
                actions = {
                    if (allRecords.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear history",
                                tint = KmTextMuted
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KmBlack,
                    titleContentColor = KmTextPrimary,
                    navigationIconContentColor = KmTextPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues)
        ) {
            if (allRecords.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = KmOrange,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No transfer history yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = KmTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.empty_recent_transfers),
                        fontSize = 13.sp,
                        color = KmTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            } else {
                // Records grouped by day
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedRecords.forEach { (dayLabel, recordsInDay) ->
                        item(key = "header_$dayLabel") {
                            Text(
                                text = dayLabel.uppercase(Locale.getDefault()),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KmOrange,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp)
                            )
                        }

                        items(recordsInDay, key = { it.id }) { record ->
                            HistoryRecordItem(record = record)
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for clearing transfer history
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear history?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = KmTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will remove all past transfer records from this device. Downloaded files will not be deleted.",
                    fontSize = 13.sp,
                    color = KmTextMuted,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        manager.historyRepository.clearAll()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text(
                        text = "Clear All",
                        color = KmOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = KmTextMuted
                    )
                }
            },
            containerColor = KmBlackCard,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(KmRadii.Card)
        )
    }
}

@Composable
private fun HistoryRecordItem(record: TransferRecord) {
    val isSent = record.direction == TransferDirection.SENT
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(record.timestamp) { timeFormatter.format(Date(record.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KmRadii.ListRow))
            .background(KmGlassBackground)
            .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.ListRow))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Direction Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isSent) Color(0x1AFF5A00) else Color(0x1A22C55E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSent) Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
                contentDescription = if (isSent) "Sent" else "Received",
                tint = if (isSent) KmOrange else KmOnline,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // File name & peer / size / time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.fileName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = KmTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val directionLabel = if (isSent) "To ${record.peerDeviceName}" else "From ${record.peerDeviceName}"
            Text(
                text = "$directionLabel · ${record.formattedSize} · $timeStr",
                fontSize = 12.sp,
                color = KmTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Status badge: Verified only when status == COMPLETED
        when (record.status) {
            TransferRecordStatus.COMPLETED -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x1F22C55E))
                        .border(1.dp, Color(0x4022C55E), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = KmOnline,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.label_verified),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KmOnline
                        )
                    }
                }
            }
            TransferRecordStatus.FAILED -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x1FEF4444))
                        .border(1.dp, Color(0x40EF4444), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Failed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = KmError
                    )
                }
            }
            TransferRecordStatus.CANCELLED -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Cancelled",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = KmTextMuted
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x1AFF5A00))
                        .border(1.dp, Color(0x40FF5A00), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = record.status.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = KmOrange
                    )
                }
            }
        }
    }
}

/**
 * Groups transfer records by day: "Today", "Yesterday", or "d MMMM yyyy".
 */
private fun groupRecordsByDay(records: List<TransferRecord>): Map<String, List<TransferRecord>> {
    val result = linkedMapOf<String, MutableList<TransferRecord>>()
    val now = Calendar.getInstance()
    val todayYear = now.get(Calendar.YEAR)
    val todayDay = now.get(Calendar.DAY_OF_YEAR)

    val itemCal = Calendar.getInstance()
    val fullDateFormatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

    for (rec in records) {
        itemCal.timeInMillis = rec.timestamp
        val dayLabel = when {
            itemCal.get(Calendar.YEAR) == todayYear && itemCal.get(Calendar.DAY_OF_YEAR) == todayDay -> "Today"
            itemCal.get(Calendar.YEAR) == todayYear && itemCal.get(Calendar.DAY_OF_YEAR) == todayDay - 1 -> "Yesterday"
            else -> fullDateFormatter.format(Date(rec.timestamp))
        }

        result.getOrPut(dayLabel) { mutableListOf() }.add(rec)
    }

    return result
}

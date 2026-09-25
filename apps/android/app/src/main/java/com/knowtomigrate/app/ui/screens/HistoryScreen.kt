package com.knowtomigrate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.data.TransferDirection
import com.knowtomigrate.app.data.TransferRecord
import com.knowtomigrate.app.data.TransferRecordStatus
import com.knowtomigrate.app.ui.components.KmBadge
import com.knowtomigrate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val allRecords by manager.historyRepository.transfers.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredRecords = remember(allRecords, selectedFilter) {
        when (selectedFilter) {
            "SENT" -> allRecords.filter { it.direction == TransferDirection.SENT }
            "RECEIVED" -> allRecords.filter { it.direction == TransferDirection.RECEIVED }
            else -> allRecords
        }
    }

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transfer History",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KmTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = KmTextPrimary)
                    }
                },
                actions = {
                    if (allRecords.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear History", tint = KmTextMuted)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KmBlack)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${allRecords.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KmOrange.copy(alpha = 0.25f),
                        selectedLabelColor = KmOrange,
                        labelColor = KmTextSecondary
                    )
                )
                FilterChip(
                    selected = selectedFilter == "SENT",
                    onClick = { selectedFilter = "SENT" },
                    label = { Text("Sent (${allRecords.count { it.direction == TransferDirection.SENT }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KmOrange.copy(alpha = 0.25f),
                        selectedLabelColor = KmOrange,
                        labelColor = KmTextSecondary
                    )
                )
                FilterChip(
                    selected = selectedFilter == "RECEIVED",
                    onClick = { selectedFilter = "RECEIVED" },
                    label = { Text("Received (${allRecords.count { it.direction == TransferDirection.RECEIVED }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KmOrange.copy(alpha = 0.25f),
                        selectedLabelColor = KmOrange,
                        labelColor = KmTextSecondary
                    )
                )
            }

            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = KmTextDisabled,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (allRecords.isEmpty()) "No Transfers Yet" else "No matching transfers",
                            style = MaterialTheme.typography.titleMedium,
                            color = KmTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (allRecords.isEmpty()) "All incoming and outgoing file transfers will appear here with cryptographic integrity verification records." else "Try changing your filter to view all transfer history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KmTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        TransferHistoryCard(
                            record = record,
                            onDelete = {
                                manager.historyRepository.delete(record.id)
                            }
                        )
                    }
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear All Transfer History?", color = KmTextPrimary) },
                text = { Text("This removes the transfer activity log. Transferred files in your Downloads folder will not be deleted.", color = KmTextMuted) },
                confirmButton = {
                    Button(
                        onClick = {
                            manager.historyRepository.clearAll()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KmError)
                    ) {
                        Text("Clear All", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel", color = KmTextMuted)
                    }
                },
                containerColor = KmBlackCard
            )
        }
    }
}

@Composable
private fun TransferHistoryCard(record: TransferRecord, onDelete: () -> Unit) {
    val statusColor = when (record.status) {
        TransferRecordStatus.COMPLETED -> KmSuccess
        TransferRecordStatus.TRANSFERRING -> KmOrange
        TransferRecordStatus.CONNECTING,
        TransferRecordStatus.PREPARING,
        TransferRecordStatus.WAITING_ACCEPTANCE,
        TransferRecordStatus.VERIFYING -> KmInfo
        TransferRecordStatus.FAILED -> KmError
        TransferRecordStatus.CANCELLED -> KmTextMuted
    }

    val dirIcon = if (record.direction == TransferDirection.SENT) Icons.Default.Upload else Icons.Default.Download

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = KmBlackCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Direction indicator with vector icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KmOrangeGlow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = dirIcon,
                    contentDescription = record.direction.name,
                    tint = KmOrange,
                    modifier = Modifier.size(22.dp)
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
                    text = "${record.peerDeviceName} • ${record.formattedSize} • ${record.transport}",
                    color = KmTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = record.formattedDate,
                    color = KmTextMuted,
                    fontSize = 11.sp,
                )
            }

            // Status badge
            KmBadge(
                text = record.status.displayName,
                color = statusColor
            )

            // Remove button
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove record",
                    tint = KmTextDisabled,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

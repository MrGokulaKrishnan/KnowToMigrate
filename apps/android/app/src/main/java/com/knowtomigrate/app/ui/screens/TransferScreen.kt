package com.knowtomigrate.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavController
import com.knowtomigrate.app.data.TransferRecord
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(navController: NavController, sessionId: String) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }

    val clientProg by manager.clientProgress.collectAsState()
    val serverProg by manager.serverProgress.collectAsState()

    // Determine active progress object
    val activeProg = if (clientProg.totalBytes > 0) clientProg else serverProg
    val isSender = clientProg.totalBytes > 0

    val progressFraction = if (activeProg.totalBytes > 0) {
        (activeProg.bytesTransferred.toFloat() / activeProg.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val speedMBps = activeProg.speedMBps
    val bytesTransferred = activeProg.bytesTransferred
    val totalBytes = activeProg.totalBytes
    val currentFile = activeProg.currentFileName.ifBlank { "Encrypting & preparing..." }
    val isComplete = activeProg.isCompleted
    val hasError = activeProg.errorMessage != null
    val peerName = activeProg.peerName.ifBlank { "Nearby Device" }

    val etaSecs = if (speedMBps > 0 && progressFraction < 1f && totalBytes > bytesTransferred) {
        val remainingBytes = totalBytes - bytesTransferred
        (remainingBytes / (speedMBps * 1024 * 1024)).toInt()
    } else 0

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isComplete -> "Transfer Complete"
                            hasError -> "Transfer Failed"
                            else -> if (isSender) "Sending Files" else "Receiving Files"
                        },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KmBlack)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Session info & active transport badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KmBadge(
                    text = "Session: ${if (sessionId.isNotBlank()) sessionId.takeLast(12) else "Direct"}",
                    color = KmInfo
                )
                KmTransportPill(
                    label = activeProg.transportType.ifBlank { "Wi-Fi Direct" },
                    isBest = true,
                    color = KmOrange
                )
            }

            // Device names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeviceLabel(
                    name = manager.localDeviceName,
                    role = if (isSender) "This Device (Sender)" else "This Device (Receiver)"
                )
                DeviceLabel(
                    name = peerName,
                    role = if (isSender) "Recipient" else "Sender"
                )
            }

            // Animated Transfer / Verification indicator
            KmTransferAnimation(modifier = Modifier.fillMaxWidth())

            // Current file & progress card
            KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current Item (${activeProg.currentFileIndex}/${activeProg.totalFiles.coerceAtLeast(1)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = KmTextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentFile,
                    style = MaterialTheme.typography.titleMedium,
                    color = KmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Progress percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KmOrange,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${TransferRecord.formatBytes(bytesTransferred)} / ${TransferRecord.formatBytes(totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = KmTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                KmProgressBar(progress = progressFraction, modifier = Modifier.fillMaxWidth())
            }

            // Speed + ETA + Files stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Speed",
                    value = if (speedMBps > 0) "%.1f MB/s".format(Locale.US, speedMBps) else "Active",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "ETA",
                    value = formatEta(etaSecs),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Files",
                    value = "${activeProg.currentFileIndex} of ${activeProg.totalFiles.coerceAtLeast(1)}",
                    modifier = Modifier.weight(1f)
                )
            }

            if (hasError) {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = KmError, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = activeProg.errorMessage ?: "Transfer encountered an error",
                            style = MaterialTheme.typography.bodySmall,
                            color = KmError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            if (isComplete) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = KmSuccess,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Transfer Verified & Complete!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = KmSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    KmPrimaryButton(
                        text = "Done",
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                KmSecondaryButton(
                    text = "Close Screen (Transfer runs in background)",
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DeviceLabel(name: String, role: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = role, style = MaterialTheme.typography.labelSmall, color = KmTextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = name, style = MaterialTheme.typography.bodyMedium, color = KmTextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KmBlackElevated)
            .border(1.dp, KmGlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = KmTextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = KmTextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatEta(seconds: Int): String {
    if (seconds <= 0) return "--:--"
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

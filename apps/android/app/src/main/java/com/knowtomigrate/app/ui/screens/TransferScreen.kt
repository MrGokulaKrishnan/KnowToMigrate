package com.knowtomigrate.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TransferScreen(navController: NavController, sessionId: String) {
    // Demo: animate progress from 0 to 1 over ~10 seconds
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var isCancelled by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }

    val speedMBps = remember { 86.4f }
    val totalBytes = remember { 2_400_000_000L }
    val bytesSent by remember(progress) { derivedStateOf { (totalBytes * progress).toLong() } }
    val etaSecs by remember(progress) { derivedStateOf { if (progress >= 1f) 0 else ((1f - progress) * totalBytes / (speedMBps * 1_048_576)).toInt() } }

    LaunchedEffect(isPaused, isCancelled) {
        if (!isPaused && !isCancelled && !isComplete) {
            while (progress < 1f && !isPaused && !isCancelled) {
                delay(100)
                progress = (progress + 0.01f).coerceAtMost(1f)
                if (progress >= 1f) isComplete = true
            }
        }
    }

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isComplete) "Transfer Complete" else "Transferring",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KmTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (isComplete || isCancelled) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = KmTextPrimary)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Session ID badge
            KmBadge(text = "Session: ${sessionId.takeLast(16)}", color = KmInfo)

            // Device names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DeviceLabel(name = "This Device", role = "Sender")
                DeviceLabel(name = "iPhone 15 Pro", role = "Receiver")
            }

            // Transfer animation
            KmTransferAnimation(modifier = Modifier.fillMaxWidth())

            // Current file
            KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current File",
                    style = MaterialTheme.typography.labelMedium,
                    color = KmTextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "vacation_photos_2024.zip",
                    style = MaterialTheme.typography.titleMedium,
                    color = KmTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Progress percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KmOrange,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatBytes(bytesSent) + " / " + formatBytes(totalBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = KmTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                KmProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
            }

            // Speed + ETA stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(label = "Speed", value = "$speedMBps MB/s", modifier = Modifier.weight(1f))
                StatCard(label = "ETA", value = formatEta(etaSecs), modifier = Modifier.weight(1f))
                StatCard(label = "Files", value = "1 of 1", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Controls
            if (!isComplete && !isCancelled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KmSecondaryButton(
                        text = if (isPaused) "Resume" else "Pause",
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier.weight(1f)
                    )
                    KmSecondaryButton(
                        text = "Cancel",
                        onClick = {
                            isCancelled = true
                            navController.popBackStack()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (isComplete) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KmSuccess, modifier = Modifier.size(48.dp))
                    Text(
                        text = "Transfer complete!",
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
            }
        }
    }
}

@Composable
private fun DeviceLabel(name: String, role: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = role, style = MaterialTheme.typography.labelSmall, color = KmTextMuted)
        Text(text = name, style = MaterialTheme.typography.bodyMedium, color = KmTextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KmBlackElevated)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = KmTextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = KmTextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024L         -> "%.1f KB".format(bytes / 1_024.0)
        else                    -> "$bytes B"
    }
}

private fun formatEta(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

package com.knowtomigrate.app.ui.screens

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.data.TransferDirection
import com.knowtomigrate.app.data.TransferRecord
import com.knowtomigrate.app.data.TransferRecordStatus
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    sharedUris: List<Uri> = emptyList()
) {
    var selectedNavItem by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val discoveredDevices by manager.discoveredDevices.collectAsState()
    val transfers by manager.historyRepository.transfers.collectAsState()
    val recentTransfers = transfers.take(5)

    val uiDevices = discoveredDevices.map { dev ->
        UiDevice(
            id = dev.deviceId,
            name = dev.deviceName,
            platform = dev.platform.lowercase(),
            ip = dev.ipAddress,
            status = if (dev.isOnline) "online" else "offline",
            supportedTransports = dev.supportedTransports,
            bestTransport = dev.bestTransport,
            isWifiLanReachable = dev.isWifiLanReachable
        )
    }

    Scaffold(
        containerColor = KmBlack,
        bottomBar = {
            KmBottomBar(
                selectedIndex = selectedNavItem,
                onSelect = { idx ->
                    selectedNavItem = idx
                    when (idx) {
                        0 -> { /* already home */ }
                        1 -> navController.navigate(Screen.Send.route)
                        2 -> navController.navigate(Screen.Receive.route)
                        3 -> navController.navigate(Screen.History.route)
                        4 -> navController.navigate(Screen.Settings.route)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                HomeHeader(deviceName = manager.localDeviceName)
            }

            // Share Sheet banner
            if (sharedUris.isNotEmpty()) {
                item {
                    SharedFilesBanner(sharedUris, navController)
                }
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KmPrimaryButton(
                        text = "Launch Transfer",
                        onClick = { navController.navigate(Screen.Send.route) },
                        modifier = Modifier.weight(1f)
                    )
                    KmSecondaryButton(
                        text = "Landing",
                        onClick = { navController.navigate(Screen.Receive.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Distinctive Discovery Radar with live discovered devices
            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Migration Radar",
                            style = MaterialTheme.typography.titleMedium,
                            color = KmTextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        KmInlineRadarStatus(
                            deviceCount = uiDevices.size,
                            isScanning = true
                        )
                    }
                    Text(
                        text = if (uiDevices.isEmpty()) "Wi-Fi • Local Network" else "${uiDevices.size} ready for migration",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiDevices.isNotEmpty()) KmSuccess else KmTextMuted,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    KmDiscoveryRadar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        devices = uiDevices,
                        onDeviceClick = {
                            navController.navigate(Screen.Send.route)
                        }
                    )
                }
            }

            // Nearby devices list
            item {
                KmSectionHeader(
                    title = "Migration Targets",
                    subtitle = if (uiDevices.isEmpty()) "Open KnowToMigrate on PC or other phone" else "${uiDevices.size} ready for migration"
                )
            }
            if (uiDevices.isEmpty()) {
                item {
                    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = KmTextDisabled,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Migration Targets Yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = KmTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Open KnowToMigrate on another device nearby.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiDevices) { device ->
                    KmDeviceCard(
                        device = device,
                        onClick = { navController.navigate(Screen.Send.route) }
                    )
                }
            }

            // Recent transfers section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KmSectionHeader(
                        title = "Latest Moves",
                        subtitle = if (recentTransfers.isNotEmpty()) "${transfers.size} total" else null
                    )
                    if (transfers.isNotEmpty()) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium,
                            color = KmOrange,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { navController.navigate(Screen.History.route) }
                                .padding(4.dp)
                        )
                    }
                }
            }

            if (recentTransfers.isEmpty()) {
                item {
                    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = KmTextDisabled,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transfers yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = KmTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Send files to a nearby device or tap Receive to accept transfers from your PC or phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recentTransfers) { record ->
                    HomeTransferRow(record)
                }
            }

            // Universal Clipboard Card
            item {
                UniversalClipboardCard(
                    discoveredDevices = discoveredDevices,
                    manager = manager
                )
            }

            // Full device migration CTA
            item {
                MigrationCtaCard(navController)
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun HomeHeader(deviceName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
                .border(1.dp, KmOrange, RoundedCornerShape(10.dp))
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.knowtomigrate.app.R.drawable.logo),
                contentDescription = "KnowToMigrate Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "KnowToMigrate",
                style = MaterialTheme.typography.titleLarge,
                color = KmTextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodySmall,
                color = KmOrange
            )
        }
    }
}

@Composable
private fun SharedFilesBanner(uris: List<Uri>, navController: NavController) {
    KmGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Screen.Send.route) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Share, contentDescription = null, tint = KmOrange, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${uris.size} file(s) ready to send",
                style = MaterialTheme.typography.bodyMedium,
                color = KmTextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = KmOrange, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HomeTransferRow(record: TransferRecord) {
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KmBlackCard)
            .border(1.dp, KmGlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KmOrangeGlow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = dirIcon,
                contentDescription = record.direction.name,
                tint = KmOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = KmTextPrimary,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${record.formattedSize} • ${record.peerDeviceName} • ${record.formattedDate}",
                style = MaterialTheme.typography.bodySmall,
                color = KmTextMuted,
                maxLines = 1
            )
        }
        KmBadge(text = record.status.displayName, color = statusColor)
    }
}

@Composable
private fun MigrationCtaCard(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(listOf(KmOrangeDark, KmOrange))
            )
            .clickable { navController.navigate(Screen.Migration.route) }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Full Device Migration",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Transfer photos, videos, contacts and files in one go",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Icon(
            imageVector = Icons.Default.MoveDown,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun UniversalClipboardCard(
    discoveredDevices: List<com.knowtomigrate.app.network.DiscoveredDevice>,
    manager: com.knowtomigrate.app.network.KtmAndroidManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = remember {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    }

    var clipboardInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(true) }
    var isWorking by remember { mutableStateOf(false) }

    val targetDevice = discoveredDevices.firstOrNull { it.isOnline && it.ipAddress.isNotBlank() }

    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = KmOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Universal Clipboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = KmTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                KmBadge(
                    text = if (targetDevice != null) targetDevice.deviceName.take(14) else "No Device",
                    color = if (targetDevice != null) KmSuccess else KmTextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sync clipboard text, URLs, or notes between Android and your PC instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = KmTextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = clipboardInput,
                onValueChange = { clipboardInput = it },
                placeholder = {
                    Text(
                        text = "Paste or type text to sync with PC…",
                        color = KmTextDisabled,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KmOrange,
                    unfocusedBorderColor = KmGlassBorder,
                    focusedTextColor = KmTextPrimary,
                    unfocusedTextColor = KmTextPrimary,
                    cursorColor = KmOrange
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Paste from Android button
                KmSecondaryButton(
                    text = "Paste Local",
                    onClick = {
                        val clip = clipboardManager?.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            clipboardInput = clip.getItemAt(0).coerceToText(context).toString()
                            statusMessage = "Pasted from Android clipboard"
                            isSuccess = true
                        } else {
                            statusMessage = "Android clipboard is empty"
                            isSuccess = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Send to PC button
                KmPrimaryButton(
                    text = if (isWorking) "Syncing…" else "Send to PC",
                    onClick = {
                        if (targetDevice == null) {
                            statusMessage = "No PC connected on network"
                            isSuccess = false
                            return@KmPrimaryButton
                        }
                        if (clipboardInput.isBlank()) {
                            statusMessage = "Enter or paste text first"
                            isSuccess = false
                            return@KmPrimaryButton
                        }
                        isWorking = true
                        scope.launch {
                            val ok = manager.sendClipboardText(targetDevice.ipAddress, clipboardInput)
                            isWorking = false
                            if (ok) {
                                statusMessage = "✓ Sent to ${targetDevice.deviceName} clipboard!"
                                isSuccess = true
                            } else {
                                statusMessage = "Failed to sync to PC (port 54125 unreachable)"
                                isSuccess = false
                            }
                        }
                    },
                    enabled = !isWorking && targetDevice != null,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fetch PC Clipboard button
            KmSecondaryButton(
                text = "Pull from PC Clipboard",
                onClick = {
                    if (targetDevice == null) {
                        statusMessage = "No PC connected on network"
                        isSuccess = false
                        return@KmSecondaryButton
                    }
                    isWorking = true
                    scope.launch {
                        val text = manager.fetchClipboardText(targetDevice.ipAddress)
                        isWorking = false
                        if (!text.isNullOrBlank()) {
                            clipboardInput = text
                            val clipData = android.content.ClipData.newPlainText("KnowToMigrate Clipboard", text)
                            clipboardManager?.setPrimaryClip(clipData)
                            statusMessage = "✓ Copied from ${targetDevice.deviceName} to Android!"
                            isSuccess = true
                        } else {
                            statusMessage = "PC clipboard is empty or unreachable"
                            isSuccess = false
                        }
                    }
                },
                enabled = !isWorking && targetDevice != null,
                modifier = Modifier.fillMaxWidth()
            )

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSuccess) KmSuccess else KmError,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun KmBottomBar(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val tabs = remember { KmNavTab.values() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(
                    width = 1.dp,
                    color = KmGlassBorder,
                    shape = RoundedCornerShape(26.dp)
                )
                .drawBehind {
                    // Top subtle specular gloss line
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0x30FFFFFF), Color.Transparent)
                        ),
                        start = Offset(26.dp.toPx(), 1.dp.toPx()),
                        end = Offset(this.size.width - 26.dp.toPx(), 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                },
            shape = RoundedCornerShape(26.dp),
            color = Color(0xF2080808),
            shadowElevation = 14.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { idx, tab ->
                    val isSelected = selectedIndex == idx

                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
                        label = "tab_scale_$idx"
                    )
                    val capsuleAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1.0f else 0.0f,
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        label = "tab_capsule_$idx"
                    )

                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(KmOrangeGlowStrong.copy(alpha = KmOrangeGlowStrong.alpha * capsuleAlpha))
                            .then(
                                if (isSelected) Modifier.border(
                                    width = 1.dp,
                                    color = KmOrange.copy(alpha = 0.45f * capsuleAlpha),
                                    shape = RoundedCornerShape(16.dp)
                                ) else Modifier
                            )
                            .clickable { onSelect(idx) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            KmNavIcon(
                                tab = tab,
                                isSelected = isSelected,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) KmOrange else KmTextMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

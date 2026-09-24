package com.knowtomigrate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(navController: NavController) {
    var isDiscoverable by remember { mutableStateOf(true) }
    var showIncomingRequest by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context)
    val serverProg by manager.serverProgress.collectAsState()

    var incomingSender by remember { mutableStateOf("") }
    var incomingPin by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        manager.transferServer.onHandshakeReceived = { sender, pin ->
            incomingSender = sender
            incomingPin = pin
            showIncomingRequest = true
            true // auto-accept or display dialog
        }
        onDispose {
            manager.transferServer.onHandshakeReceived = null
        }
    }

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Receive Files",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Discoverable toggle
            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Discoverable",
                                style = MaterialTheme.typography.titleMedium,
                                color = KmTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isDiscoverable) "Listening on TCP port 54124" else "Hidden from other devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDiscoverable) KmSuccess else KmTextMuted
                            )
                        }
                        Switch(
                            checked = isDiscoverable,
                            onCheckedChange = { isDiscoverable = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = KmOrange,
                                uncheckedThumbColor = KmTextMuted,
                                uncheckedTrackColor = KmBlackElevated
                            )
                        )
                    }
                }
            }

            // Live Incoming Progress Card
            if (serverProg.totalBytes > 0) {
                item {
                    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = if (serverProg.isCompleted) "Transfer Complete" else "Receiving from ${serverProg.peerName}...",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (serverProg.isCompleted) KmSuccess else KmTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "File: ${serverProg.currentFileName} (${serverProg.currentFileIndex}/${serverProg.totalFiles})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KmOrange
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Speed: ${"%.1f".format(serverProg.speedMBps)} MB/s · ${serverProg.bytesTransferred / 1048576} MB / ${serverProg.totalBytes / 1048576} MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (serverProg.percentage / 100f).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = KmOrange,
                                trackColor = KmBlackCard
                            )
                        }
                    }
                }
            }

            // Device info / QR display
            item {
                KmSectionHeader(
                    title = "Your Device",
                    subtitle = "This phone is ready to receive transfers"
                )
            }

            item {
                DeviceQrDisplay(deviceName = manager.localDeviceName, deviceId = manager.localDeviceId)
            }

            // Destination directory card
            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Save Destination",
                            style = MaterialTheme.typography.titleSmall,
                            color = KmTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = manager.downloadDirectory.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = KmOrange
                        )
                    }
                }
            }
        }

        // Incoming request bottom sheet
        if (showIncomingRequest) {
            ModalBottomSheet(
                onDismissRequest = { showIncomingRequest = false },
                sheetState = sheetState,
                containerColor = KmBlackCard,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(KmGlassBorder)
                    )
                }
            ) {
                IncomingRequestSheet(
                    device = UiDevice("remote", incomingSender.ifBlank { "Nearby Device" }, "remote", "", "online"),
                    files = listOf("PIN: ${incomingPin.ifBlank { "123456" }}", "Incoming P2P Transfer"),
                    onAccept = { showIncomingRequest = false },
                    onDecline = { showIncomingRequest = false }
                )
            }
        }
    }
}

@Composable
private fun DeviceQrDisplay(deviceName: String, deviceId: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // QR-like decorative box
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(KmBlackElevated)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(KmOrange, KmOrangeDark)),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Corner marks simulating QR code
                QrCornerMarks()
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = KmOrange,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = deviceId,
                    style = MaterialTheme.typography.labelMedium,
                    color = KmTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = deviceName,
            style = MaterialTheme.typography.titleMedium,
            color = KmTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Scan or share device ID",
            style = MaterialTheme.typography.bodySmall,
            color = KmTextMuted
        )
    }
}

@Composable
private fun QrCornerMarks() {
    // Simple decorative corner squares to evoke a QR code
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Box(modifier = Modifier.size(18.dp).border(2.dp, KmOrange, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.size(18.dp).border(2.dp, KmOrange, RoundedCornerShape(3.dp)))
    }
}

@Composable
private fun IncomingRequestSheet(
    device: UiDevice,
    files: List<String>,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Incoming Transfer Request",
            style = MaterialTheme.typography.headlineSmall,
            color = KmTextPrimary,
            fontWeight = FontWeight.Bold
        )
        KmGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Smartphone, contentDescription = null, tint = KmOrange, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = device.name, style = MaterialTheme.typography.titleSmall, color = KmTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(text = device.ip, style = MaterialTheme.typography.bodySmall, color = KmTextMuted)
                }
            }
        }
        Text(
            text = "Files to receive:",
            style = MaterialTheme.typography.labelMedium,
            color = KmTextSecondary
        )
        files.forEach { file ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = KmTextMuted, modifier = Modifier.size(16.dp))
                Text(text = file, style = MaterialTheme.typography.bodySmall, color = KmTextSecondary)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KmSecondaryButton(text = "Decline", onClick = onDecline, modifier = Modifier.weight(1f))
            KmPrimaryButton(text = "Accept", onClick = onAccept, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

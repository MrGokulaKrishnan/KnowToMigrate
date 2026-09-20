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

    // Simulate an incoming request after 3 seconds in preview/demo
    val sampleIncomingDevice = UiDevice("demo", "iPhone 15 Pro", "ios", "192.168.1.10", "online")
    val sampleFiles = listOf("photos_2024.zip (2.3 GB)", "contacts.vcf (48 KB)")

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
                                text = if (isDiscoverable) "Visible to nearby devices" else "Hidden from other devices",
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

            // Device info / QR display
            item {
                KmSectionHeader(
                    title = "Your Device",
                    subtitle = "Share this code with the sender"
                )
            }

            item {
                DeviceQrDisplay(deviceName = "My Android Device", deviceId = "KTM-A8F2-3C9D")
            }

            // Status
            item {
                if (isDiscoverable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KmStatusDot(status = "online")
                        Text(
                            text = "Listening for connections on port 7979",
                            style = MaterialTheme.typography.bodySmall,
                            color = KmTextMuted
                        )
                    }
                }
            }

            // Simulate incoming button (demo only)
            item {
                KmSecondaryButton(
                    text = "Simulate Incoming Request",
                    onClick = { showIncomingRequest = true },
                    modifier = Modifier.fillMaxWidth()
                )
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
                    device = sampleIncomingDevice,
                    files = sampleFiles,
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

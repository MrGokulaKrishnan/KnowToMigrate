package com.knowtomigrate.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(navController: NavController) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val preferences = manager.preferences

    val isDiscoverable by preferences.isDiscoverable.collectAsState()
    val localDeviceName by preferences.deviceName.collectAsState()
    val folderDisplay by preferences.receiveFolderDisplay.collectAsState()
    val serverProg by manager.serverProgress.collectAsState()

    var showIncomingRequest by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    var incomingSender by remember { mutableStateOf("") }
    var incomingPin by remember { mutableStateOf("") }
    var incomingTransport by remember { mutableStateOf("Wi-Fi LAN") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(localDeviceName) }

    // Folder picker for Scoped Storage
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}

            val path = uri.path ?: ""
            val cleanDisplay = if (path.contains(":")) {
                path.substringAfterLast(":")
            } else {
                uri.lastPathSegment ?: "Custom Folder"
            }
            preferences.setReceiveFolder("Downloads / $cleanDisplay", uri.toString())
        }
    }

    DisposableEffect(Unit) {
        manager.transferServer.onHandshakeReceived = { sender, pin, transport ->
            incomingSender = sender
            incomingPin = pin
            incomingTransport = transport
            showIncomingRequest = true
            true
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "Ready to Receive" status card
            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isDiscoverable) KmSuccess else KmTextMuted)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isDiscoverable) "Ready to Receive" else "Visibility Hidden",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KmTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDiscoverable) "Listening on Port 54124 • Local Network" else "Device not discoverable by nearby peers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDiscoverable) KmSuccess else KmTextMuted
                                )
                            }
                        }
                        Switch(
                            checked = isDiscoverable,
                            onCheckedChange = { preferences.setDiscoverable(it) },
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (serverProg.isCompleted) "Transfer Completed" else "Receiving from ${serverProg.peerName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (serverProg.isCompleted) KmSuccess else KmTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                KmBadge(
                                    text = if (serverProg.isCompleted) "Verified" else "Receiving",
                                    color = if (serverProg.isCompleted) KmSuccess else KmOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "File: ${serverProg.currentFileName} (${serverProg.currentFileIndex}/${serverProg.totalFiles})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KmOrangeLight,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Speed: ${"%.1f".format(serverProg.speedMBps)} MB/s · ${serverProg.bytesTransferred / 1048576} MB / ${serverProg.totalBytes / 1048576} MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (serverProg.percentage / 100f).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = KmOrange,
                                trackColor = KmBlackElevated
                            )
                        }
                    }
                }
            }

            // Device Identification Card
            item {
                KmSectionHeader(
                    title = "Your Device Identity",
                    subtitle = "Visible to nearby senders on Wi-Fi and Wi-Fi Direct"
                )
            }

            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(KmBlackElevated)
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(listOf(KmOrange, KmOrangeDark)),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = KmOrange,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "KTM v2 DIRECT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KmTextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = localDeviceName,
                                style = MaterialTheme.typography.titleMedium,
                                color = KmTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    editedName = localDeviceName
                                    showEditNameDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = KmOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "Unique ID: ${manager.localDeviceId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = KmTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "End-to-End Encrypted (AES-256-GCM)",
                            style = MaterialTheme.typography.labelSmall,
                            color = KmSuccess
                        )
                    }
                }
            }

            // Save Destination Directory Card
            item {
                KmSectionHeader(
                    title = "Save Destination",
                    subtitle = "Where incoming files are stored on this device"
                )
            }

            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KmOrangeGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = KmOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = folderDisplay,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KmTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Auto-indexed to Gallery & Files app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KmTextMuted
                                )
                            }
                        }
                        KmSecondaryButton(
                            text = "Change",
                            onClick = { folderPicker.launch(null) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Incoming transfer request bottom sheet
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
                    pin = incomingPin,
                    transport = incomingTransport,
                    files = listOf("Incoming Encrypted Transfer"),
                    onAccept = { showIncomingRequest = false },
                    onDecline = { showIncomingRequest = false }
                )
            }
        }

        // Edit Device Name Dialog
        if (showEditNameDialog) {
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = {
                    Text(
                        text = "Edit Device Name",
                        color = KmTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Nearby devices will identify this phone by this name.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KmTextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KmOrange,
                                unfocusedBorderColor = KmGlassBorder,
                                focusedTextColor = KmTextPrimary,
                                unfocusedTextColor = KmTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editedName.isNotBlank()) {
                                preferences.setDeviceName(editedName)
                            }
                            showEditNameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KmOrange)
                    ) {
                        Text("Save", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditNameDialog = false }) {
                        Text("Cancel", color = KmTextMuted)
                    }
                },
                containerColor = KmBlackCard
            )
        }
    }
}

@Composable
private fun IncomingRequestSheet(
    device: UiDevice,
    pin: String,
    transport: String,
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
                Icon(Icons.Default.Smartphone, contentDescription = null, tint = KmOrange, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = device.name, style = MaterialTheme.typography.titleMedium, color = KmTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(text = "Negotiated Transport: $transport", style = MaterialTheme.typography.bodySmall, color = KmOrange)
                }
                KmTransportPill(label = transport, isBest = true, color = KmOrange)
            }
        }
        KmGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Authentication PIN", style = MaterialTheme.typography.labelMedium, color = KmTextSecondary)
                    Text(text = "Verify matching code on sender", style = MaterialTheme.typography.bodySmall, color = KmTextMuted)
                }
                Text(
                    text = pin.ifBlank { "000000" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = KmOrange
                )
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

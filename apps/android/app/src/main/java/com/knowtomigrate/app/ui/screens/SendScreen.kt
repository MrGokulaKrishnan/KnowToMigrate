package com.knowtomigrate.app.ui.screens

import android.net.Uri
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*

@Composable
fun SendScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<UiDevice?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("") }
    var userTransportOverride by remember { mutableStateOf<String?>(null) }
    var isCheckingCapabilities by remember { mutableStateOf(false) }

    val discoveredDevices by com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context).discoveredDevices.collectAsState()
    val nearbyDevices = discoveredDevices.map { dev ->
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

    LaunchedEffect(selectedDevice?.id) {
        val dev = selectedDevice
        if (dev != null) {
            isCheckingCapabilities = true
            val targetRaw = discoveredDevices.find { it.deviceId == dev.id }
            if (targetRaw != null) {
                com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context).evaluateTargetTransport(targetRaw)
            }
            isCheckingCapabilities = false
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        selectedUris = uris
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Send Files",
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
            // File picker section
            item {
                KmSectionHeader(title = "Select Files", subtitle = "Tap to browse your device")
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(KmGlassBackground)
                        .border(
                            width = 1.dp,
                            color = if (selectedUris.isEmpty()) KmGlassBorder else KmOrange.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { filePicker.launch(arrayOf("*/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUris.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Pick files",
                                tint = KmOrange,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to select files",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KmTextMuted
                            )
                            Text(
                                text = "Any file type supported",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextDisabled
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = KmSuccess,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${selectedUris.size} file(s) selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KmTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap to change selection",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted
                            )
                        }
                    }
                }
            }

            // Selected files list
            if (selectedUris.isNotEmpty()) {
                item {
                    KmSectionHeader(title = "Selected Files")
                }
                items(selectedUris) { uri ->
                    SelectedFileRow(uri = uri, onRemove = {
                        selectedUris = selectedUris.filter { it != uri }
                    })
                }
            }

            // Device selector
            item {
                KmSectionHeader(
                    title = "Choose Destination",
                    subtitle = "${nearbyDevices.size} devices nearby"
                )
            }

            items(nearbyDevices) { device ->
                val isSelected = selectedDevice?.id == device.id
                KmDeviceCard(
                    device = device,
                    onClick = { selectedDevice = if (isSelected) null else device },
                    modifier = Modifier.border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) KmOrange else KmGlassBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                )
            }

            // Capability Check & Transport Selection Card
            if (selectedDevice != null) {
                item {
                    val activeTransportCode = userTransportOverride ?: selectedDevice!!.bestTransport
                    val isAuto = userTransportOverride == null
                    val bestType = com.knowtomigrate.app.network.KtmTransportType.fromCode(activeTransportCode)

                    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Transport Capability Check",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KmTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                KmTransportPill(
                                    label = if (isAuto) "${bestType.displayName}" else "${bestType.displayName} (Manual)",
                                    isBest = true,
                                    color = KmOrange
                                )
                            }

                            Text(
                                text = "Pipeline: Nearby Device ➔ Capability Check ➔ Select Best ➔ Authenticate ➔ Transfer",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted
                            )

                            // Transport Selection Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = isAuto,
                                    onClick = { userTransportOverride = null },
                                    label = { Text("Auto (Best)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = KmOrange.copy(alpha = 0.25f),
                                        selectedLabelColor = KmOrange,
                                        labelColor = KmTextSecondary
                                    )
                                )
                                FilterChip(
                                    selected = userTransportOverride == "WIFI_LAN",
                                    onClick = { userTransportOverride = "WIFI_LAN" },
                                    label = { Text("Wi-Fi") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = KmOrange.copy(alpha = 0.25f),
                                        selectedLabelColor = KmOrange,
                                        labelColor = KmTextSecondary
                                    )
                                )
                                FilterChip(
                                    selected = userTransportOverride == "WIFI_DIRECT",
                                    onClick = { userTransportOverride = "WIFI_DIRECT" },
                                    label = { Text("Direct") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = KmInfo.copy(alpha = 0.25f),
                                        selectedLabelColor = KmInfo,
                                        labelColor = KmTextSecondary
                                    )
                                )
                                FilterChip(
                                    selected = userTransportOverride == "BLUETOOTH",
                                    onClick = { userTransportOverride = "BLUETOOTH" },
                                    label = { Text("BT") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = androidx.compose.ui.graphics.Color(0xFF60A5FA).copy(alpha = 0.25f),
                                        selectedLabelColor = androidx.compose.ui.graphics.Color(0xFF60A5FA),
                                        labelColor = KmTextSecondary
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Speed: ${bestType.speedRating}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KmTextSecondary
                                )
                                Text(
                                    text = "Security: AES-256-GCM + PIN",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KmSuccess
                                )
                            }
                        }
                    }
                }
            }

            // Direct IP Connect Card
            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Direct Connect by IP",
                            style = MaterialTheme.typography.titleSmall,
                            color = KmTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "If device is not auto-discovered due to Wi-Fi isolation:",
                            style = MaterialTheme.typography.bodySmall,
                            color = KmTextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = manualIp,
                                onValueChange = { manualIp = it },
                                placeholder = { Text("e.g. 192.168.1.100", color = KmTextMuted) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = KmOrange,
                                    unfocusedBorderColor = KmGlassBorder,
                                    focusedTextColor = KmTextPrimary,
                                    unfocusedTextColor = KmTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            KmSecondaryButton(
                                text = "Use IP",
                                onClick = {
                                    if (manualIp.isNotBlank()) {
                                        com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context).discoveryService.addManualDevice(manualIp.trim())
                                        selectedDevice = UiDevice("manual_${manualIp.trim()}", "PC / Phone (${manualIp.trim()})", "remote", manualIp.trim(), "online")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Send button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                val canSend = selectedUris.isNotEmpty() && selectedDevice != null && !isSending
                KmPrimaryButton(
                    text = if (isSending) "Streaming over ${userTransportOverride ?: selectedDevice?.bestTransport ?: "Wi-Fi"}…" else "Send ${if (selectedUris.isEmpty()) "" else "${selectedUris.size} File(s)"}",
                    onClick = {
                        if (canSend && selectedDevice != null) {
                            isSending = true
                            val targetRaw = discoveredDevices.find { it.deviceId == selectedDevice!!.id } ?: com.knowtomigrate.app.network.DiscoveredDevice(
                                deviceId = selectedDevice!!.id,
                                deviceName = selectedDevice!!.name,
                                ipAddress = selectedDevice!!.ip
                            )
                            val chosen = userTransportOverride ?: selectedDevice!!.bestTransport
                            scope.launch {
                                val success = com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context).sendUris(
                                    target = targetRaw,
                                    uris = selectedUris,
                                    forcedTransport = chosen
                                )
                                isSending = false
                                if (success) {
                                    android.widget.Toast.makeText(context, "Transfer completed and verified", android.widget.Toast.LENGTH_LONG).show()
                                    navController.navigate(Screen.Home.route)
                                } else {
                                    android.widget.Toast.makeText(context, "Transfer failed or was rejected by recipient", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SelectedFileRow(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    var fileName by remember(uri) { mutableStateOf(uri.lastPathSegment ?: "file") }
    var fileSize by remember(uri) { mutableStateOf(-1L) }

    LaunchedEffect(uri) {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx != -1 && !cursor.isNull(nameIdx)) fileName = cursor.getString(nameIdx)
                    if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) fileSize = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {}
        if (fileSize <= 0) {
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    if (afd.length > 0) fileSize = afd.length
                }
            } catch (_: Exception) {}
        }
    }

    val sizeStr = if (fileSize > 0) {
        when {
            fileSize >= 1024 * 1024 * 1024 -> "%.1f GB".format(fileSize / (1024.0 * 1024 * 1024))
            fileSize >= 1024 * 1024 -> "%.1f MB".format(fileSize / (1024.0 * 1024))
            fileSize >= 1024 -> "%.1f KB".format(fileSize / 1024.0)
            else -> "$fileSize B"
        }
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KmBlackCard)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = KmOrange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = KmTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (sizeStr.isNotBlank()) {
                Text(
                    text = sizeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = KmTextMuted
                )
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = KmTextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

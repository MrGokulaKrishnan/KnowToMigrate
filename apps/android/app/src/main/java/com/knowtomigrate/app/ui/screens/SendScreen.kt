package com.knowtomigrate.app.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<UiDevice?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("") }
    var userTransportOverride by remember { mutableStateOf<String?>(null) }
    var showAdvancedIp by remember { mutableStateOf(false) }

    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val discoveredDevices by manager.discoveredDevices.collectAsState()
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

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val combined = (selectedUris + uris).distinct()
            selectedUris = combined
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(selectedDevice?.id) {
        val dev = selectedDevice
        if (dev != null) {
            val targetRaw = discoveredDevices.find { it.deviceId == dev.id }
            if (targetRaw != null) {
                manager.evaluateTargetTransport(targetRaw)
            }
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
                KmSectionHeader(title = "Select Files", subtitle = "Choose photos, videos, archives, or documents")
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(KmGlassBackground)
                        .border(
                            width = 1.dp,
                            color = if (selectedUris.isEmpty()) KmGlassBorder else KmOrange.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { filePicker.launch(arrayOf("*/*")) }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUris.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(KmOrangeGlow)
                                    .border(1.dp, KmOrange, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload Files",
                                    tint = KmOrange,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tap to Select Files",
                                style = MaterialTheme.typography.titleMedium,
                                color = KmTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Photos, Videos, ZIP, Audio, APK, Documents",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(KmSuccess.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = KmSuccess,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${selectedUris.size} file(s) selected",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = KmTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to add more files",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KmOrange
                                    )
                                }
                            }
                            TextButton(onClick = { selectedUris = emptyList() }) {
                                Text("Clear", color = KmTextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Dynamic selected files list
            if (selectedUris.isNotEmpty()) {
                item {
                    KmSectionHeader(
                        title = "Selected Files (${selectedUris.size})",
                        subtitle = "Review and remove individual files before sending"
                    )
                }
                items(selectedUris) { uri ->
                    SelectedFileCard(
                        uri = uri,
                        onRemove = {
                            selectedUris = selectedUris.filter { it != uri }
                        }
                    )
                }
            }

            // Destination device selector
            item {
                KmSectionHeader(
                    title = "Choose Destination Device",
                    subtitle = if (nearbyDevices.isEmpty()) "Searching for nearby devices..." else "${nearbyDevices.size} nearby device(s) online"
                )
            }

            if (nearbyDevices.isEmpty()) {
                item {
                    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = KmOrange,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Looking for devices on your network...",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted
                            )
                        }
                    }
                }
            } else {
                items(nearbyDevices) { device ->
                    val isSelected = selectedDevice?.id == device.id
                    KmDeviceCard(
                        device = device,
                        onClick = { selectedDevice = if (isSelected) null else device },
                        modifier = Modifier.border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) KmOrange else KmGlassBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                    )
                }
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
                                    text = "Transport Pipeline",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KmTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                KmTransportPill(
                                    label = if (isAuto) bestType.displayName else "${bestType.displayName} (Manual)",
                                    isBest = true,
                                    color = KmOrange
                                )
                            }

                            Text(
                                text = "Capability Check ➔ Negotiate Best Transport ➔ AES-256-GCM + PIN Handshake",
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
                                    label = { Text("Wi-Fi Direct") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = KmInfo.copy(alpha = 0.25f),
                                        selectedLabelColor = KmInfo,
                                        labelColor = KmTextSecondary
                                    )
                                )
                                FilterChip(
                                    selected = userTransportOverride == "BLUETOOTH",
                                    onClick = { userTransportOverride = "BLUETOOTH" },
                                    label = { Text("Bluetooth") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF60A5FA).copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFF60A5FA),
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
                                    text = "Estimated Speed: ${bestType.speedRating}",
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

            // Secondary: Expandable Advanced Direct IP Connect Card
            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedIp = !showAdvancedIp },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SettingsEthernet,
                                    contentDescription = null,
                                    tint = KmTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Advanced: Direct IP Connection",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = KmTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Connect directly if UDP discovery is blocked by router",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KmTextMuted
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showAdvancedIp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = KmTextMuted
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedIp) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
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
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    KmSecondaryButton(
                                        text = "Connect",
                                        onClick = {
                                            if (manualIp.isNotBlank()) {
                                                val clean = manualIp.trim()
                                                manager.discoveryService.addManualDevice(clean)
                                                selectedDevice = UiDevice("manual_$clean", "Device ($clean)", "remote", clean, "online")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // State-aware Send button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                when {
                    selectedUris.isEmpty() -> {
                        KmPrimaryButton(
                            text = "Choose Files to Send",
                            onClick = { filePicker.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    selectedDevice == null -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = KmBlackElevated,
                                disabledContentColor = KmTextMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Select Destination Device Above",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    isSending -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = KmBlackElevated,
                                disabledContentColor = KmOrange
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = KmOrange,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Streaming to ${selectedDevice?.name}...",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    else -> {
                        val transportCode = userTransportOverride ?: selectedDevice?.bestTransport ?: "WIFI_LAN"
                        KmPrimaryButton(
                            text = "Send Now • ${selectedUris.size} File(s)",
                            onClick = {
                                isSending = true
                                val targetRaw = discoveredDevices.find { it.deviceId == selectedDevice!!.id } ?: com.knowtomigrate.app.network.DiscoveredDevice(
                                    deviceId = selectedDevice!!.id,
                                    deviceName = selectedDevice!!.name,
                                    ipAddress = selectedDevice!!.ip
                                )
                                scope.launch {
                                    val success = manager.sendUris(
                                        target = targetRaw,
                                        uris = selectedUris,
                                        forcedTransport = transportCode
                                    )
                                    isSending = false
                                    if (success) {
                                        android.widget.Toast.makeText(context, "Transfer completed and verified", android.widget.Toast.LENGTH_LONG).show()
                                        navController.navigate(Screen.Home.route)
                                    } else {
                                        android.widget.Toast.makeText(context, "Transfer failed or was rejected by recipient", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SelectedFileCard(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    var fileName by remember(uri) { mutableStateOf(uri.lastPathSegment ?: "File") }
    var fileSize by remember(uri) { mutableStateOf(-1L) }

    LaunchedEffect(uri) {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
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

    val extension = fileName.substringAfterLast('.', "").uppercase(Locale.getDefault()).take(4)
    val extBadge = if (extension.isNotBlank()) extension else "FILE"

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
            .clip(RoundedCornerShape(12.dp))
            .background(KmBlackCard)
            .border(1.dp, KmGlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KmOrangeGlow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = extBadge,
                color = KmOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = KmTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
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

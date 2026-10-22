package com.knowtomigrate.app.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.network.DiscoveredDevice
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*
import kotlinx.coroutines.launch

data class StagedFile(
    val id: String,
    val name: String,
    val sizeText: String,
    val uri: Uri? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SendScreen(
    navController: NavController,
    initialUris: List<Uri> = emptyList()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val discoveredDevices by manager.discoveredDevices.collectAsState()

    var files by remember {
        mutableStateOf(
            if (initialUris.isNotEmpty()) {
                initialUris.map { uri ->
                    StagedFile(uri.toString(), "Shared file", "Ready", uri)
                }
            } else {
                listOf(
                    StagedFile("1", "Photos_March.zip", "1.2 GB"),
                    StagedFile("2", "Resume_2026.pdf", "0.4 MB")
                )
            }
        )
    }

    var selectedDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var showIpDialog by remember { mutableStateOf(false) }
    var customIpInput by remember { mutableStateOf("") }

    // Auto-select first discovered device if none is selected
    LaunchedEffect(discoveredDevices) {
        if (selectedDevice == null && discoveredDevices.isNotEmpty()) {
            selectedDevice = discoveredDevices.first()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newItems = uris.map { uri ->
                var name = "Document"
                var size = ""
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "Document"
                        if (sizeIdx >= 0) {
                            val b = cursor.getLong(sizeIdx)
                            size = if (b >= 1024 * 1024) "${(b / (1024f * 1024f)).toInt()} MB" else "${(b / 1024f).toInt()} KB"
                        }
                    }
                }
                StagedFile(uri.toString(), name, size.ifBlank { "Ready" }, uri)
            }
            files = (files + newItems).distinctBy { it.name }
        }
    }

    val hasFiles = files.isNotEmpty()
    val hasDevice = selectedDevice != null
    val isReady = hasFiles && hasDevice

    val stepNumber = if (isReady) 2 else if (!hasFiles) 1 else 2
    val stepTitle = if (!hasFiles) "Add files" else if (!hasDevice) "Waiting for a device" else "Ready to send"

    val buttonLabel = when {
        !hasFiles -> "Add files to send"
        !hasDevice -> "Waiting for a device"
        else -> "Send ${files.size} file${if (files.size > 1) "s" else ""} to ${selectedDevice?.deviceName ?: "device"}"
    }

    Scaffold(
        containerColor = KmBlack,
        bottomBar = {
            KmFloatingBottomBar(
                selectedIndex = 1,
                onSelect = { idx ->
                    when (idx) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> { /* already send */ }
                        2 -> navController.navigate(Screen.Receive.route)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "Send",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = KmTextPrimary
                        )
                        Text(
                            text = "Step $stepNumber of 2 · $stepTitle",
                            fontSize = 12.sp,
                            color = KmTextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Step 1: Files
                item {
                    Text(
                        text = "1. FILES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.08.sp,
                        color = KmTextMuted,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(18.dp))
                            .drawBehind {
                                drawLine(
                                    color = KmGlassTopHighlight,
                                    start = Offset(18.dp.toPx(), 1.dp.toPx()),
                                    end = Offset(size.width - 18.dp.toPx(), 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (files.isEmpty()) {
                                Text(
                                    text = "No files yet. Add at least one file to continue.",
                                    fontSize = 12.sp,
                                    color = KmTextMuted,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                // Chips
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    files.forEach { file ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(0x0FFFFFFF))
                                                .border(1.dp, KmGlassBorder, RoundedCornerShape(999.dp))
                                                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = file.name,
                                                fontSize = 12.sp,
                                                color = KmTextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        files = files.filter { it.id != file.id }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "×",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = KmTextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Add files button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x0FFFFFFF))
                                    .border(1.dp, KmGlassBorder, RoundedCornerShape(12.dp))
                                    .clickable { filePicker.launch(arrayOf("*/*")) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = KmTextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add files",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KmTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 2: Device
                item {
                    Text(
                        text = "2. DEVICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.08.sp,
                        color = KmTextMuted,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                if (discoveredDevices.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(KmGlassBackground)
                                .border(1.dp, KmGlassBorder, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = KmOrange,
                                trackColor = Color(0x1AFFFFFF)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Looking for devices…",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KmTextPrimary
                                )
                                Text(
                                    text = "Open KnowToMigrate on your PC.",
                                    fontSize = 12.sp,
                                    color = KmTextMuted,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(discoveredDevices) { dev ->
                        val isSelected = selectedDevice?.deviceId == dev.deviceId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(KmGlassBackground)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) KmOrange else KmGlassBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedDevice = dev }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x0FFFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (dev.platform.contains("win", ignoreCase = true)) Icons.Default.Computer else Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = if (isSelected) KmOrange else KmTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.deviceName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KmTextPrimary
                                )
                                Text(
                                    text = "Found on Wi-Fi · ${dev.ipAddress.ifBlank { "Local" }}",
                                    fontSize = 12.sp,
                                    color = KmTextMuted,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }

                            // Radio selection circle
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSelected) KmOrange else KmGlassBorder,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(KmOrangeDark, KmOrangeLight)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // Advanced row: Connect by IP address
                item {
                    Text(
                        text = "ADVANCED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.08.sp,
                        color = KmTextMuted,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(16.dp))
                            .clickable { showIpDialog = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connect by IP address",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = KmTextPrimary
                            )
                            Text(
                                text = "Use only if the device is not found",
                                fontSize = 12.sp,
                                color = KmTextMuted,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = KmTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom Primary Send Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isReady) {
                                Brush.linearGradient(
                                    listOf(KmOrangeDark, KmOrange, KmOrangeLight)
                                )
                            } else {
                                SolidColor(Color(0x1FFFFFFF))
                            }
                        )
                        .drawBehind {
                            if (isReady) {
                                drawLine(
                                    color = Color(0x73FFFFFF),
                                    start = Offset(14.dp.toPx(), 1.dp.toPx()),
                                    end = Offset(size.width - 14.dp.toPx(), 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                        .clickable(enabled = isReady) {
                            val realUris = files.mapNotNull { it.uri }
                            val target = selectedDevice
                            if (target != null) {
                                if (realUris.isNotEmpty()) {
                                    scope.launch {
                                        manager.sendUris(target, realUris)
                                    }
                                }
                                navController.navigate(Screen.Transfer.withSession("send_session"))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReady) KmTextOnGradient else KmTextDisabled
                    )
                }
            }
        }
    }

    if (showIpDialog) {
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            containerColor = KmBlackCard,
            title = {
                Text("Connect by IP", color = KmTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter the IP address of the target device as shown on its KnowToMigrate screen.",
                        fontSize = 12.sp,
                        color = KmTextMuted
                    )
                    OutlinedTextField(
                        value = customIpInput,
                        onValueChange = { customIpInput = it },
                        placeholder = { Text("e.g. 192.168.1.50", color = KmTextDisabled) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x0AFFFFFF),
                            unfocusedContainerColor = Color(0x0AFFFFFF),
                            focusedBorderColor = KmOrange,
                            unfocusedBorderColor = KmGlassBorder,
                            focusedTextColor = KmTextPrimary,
                            unfocusedTextColor = KmTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customIpInput.isNotBlank()) {
                            val custom = DiscoveredDevice(
                                deviceId = "ip_${customIpInput.trim()}",
                                deviceName = "Target (${customIpInput.trim()})",
                                platform = "windows",
                                ipAddress = customIpInput.trim(),
                                transferPort = 54124,
                                lastSeen = System.currentTimeMillis()
                            )
                            selectedDevice = custom
                            showIpDialog = false
                        }
                    }
                ) {
                    Text("Select", color = KmOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpDialog = false }) {
                    Text("Cancel", color = KmTextMuted)
                }
            }
        )
    }
}

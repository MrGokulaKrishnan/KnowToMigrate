package com.knowtomigrate.app.ui.screens

/*
 * KnowToMigrate - Send Screen
 * Updated: 25 October 2026
 * Pure AMOLED #000000 base with KM Electric Orange accents and Dark Liquid Glass
 * Information Architecture:
 * - Two-step header: "Step 1 of 2: Add files" / "Step 2 of 2: Choose a device"
 * - Step 1: Removable file chips next to "Add files" button
 * - Step 2: Selectable device rows with spinner when searching
 * - Advanced: Collapsible disclosure with "Connect by IP address"
 * - Sticky Bottom Button: Exactly one primary action with dynamic state-aware label
 * - Bottom Bar: 3 items (Home, Send, Receive)
 */

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.R
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
        mutableStateOf<List<StagedFile>>(
            if (initialUris.isNotEmpty()) {
                initialUris.map { uri ->
                    StagedFile(uri.toString(), "Shared file", "Ready", uri)
                }
            } else {
                emptyList<StagedFile>()
            }
        )
    }

    var selectedDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var showIpDialog by remember { mutableStateOf(false) }
    var customIpInput by remember { mutableStateOf("") }
    var isAdvancedExpanded by remember { mutableStateOf(false) }

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

    // Dynamic state-aware button label
    val buttonLabel = when {
        !hasFiles -> stringResource(R.string.send_btn_missing_files)
        !hasDevice -> stringResource(R.string.send_btn_missing_device)
        files.size == 1 -> stringResource(R.string.send_btn_ready_single, selectedDevice?.deviceName ?: "device")
        else -> stringResource(R.string.send_btn_ready, files.size, selectedDevice?.deviceName ?: "device")
    }

    val stepHeader = if (!hasFiles) {
        stringResource(R.string.send_step_1)
    } else {
        stringResource(R.string.send_step_2)
    }

    Scaffold(
        containerColor = KmBlack,
        bottomBar = {
            KmFloatingBottomBar(
                selectedIndex = 1,
                onSelect = { idx ->
                    when (idx) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> { /* already on Send */ }
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
                // ── 1. Header: Send & Two-step indicator ──────────────────────────────
                item {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = stringResource(R.string.nav_send),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = KmTextPrimary
                        )
                        Text(
                            text = stepHeader,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = KmOrangeLight,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // ── 2. Step 1: Files Section with Removable Chips ───────────────────
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
                            .clip(RoundedCornerShape(KmRadii.Card))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Card))
                            .drawBehind {
                                drawLine(
                                    color = KmGlassTopHighlight,
                                    start = Offset(18.dp.toPx(), 1.dp.toPx()),
                                    end = Offset(size.width - 18.dp.toPx(), 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (files.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.empty_send_files),
                                    fontSize = 13.sp,
                                    color = KmTextMuted,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                // Removable file chips
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    files.forEach { file ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(0x14FFFFFF))
                                                .border(1.dp, KmGlassBorder, RoundedCornerShape(999.dp))
                                                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = file.name,
                                                fontSize = 12.sp,
                                                color = KmTextPrimary,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        files = files.filter { it.id != file.id }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove file",
                                                    tint = KmTextMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Add files button (48dp touch target)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(KmRadii.Button))
                                    .background(Color(0x14FFFFFF))
                                    .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Button))
                                    .clickable { filePicker.launch(arrayOf("*/*")) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = KmOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_add_files),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KmTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 3. Step 2: Choose a Device Section ──────────────────────────────
                item {
                    Text(
                        text = "2. CHOOSE A DEVICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.08.sp,
                        color = KmTextMuted,
                        modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                    )
                }

                if (discoveredDevices.isEmpty()) {
                    // Spinner row while looking for devices
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(KmRadii.ListRow))
                                .background(KmGlassBackground)
                                .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.ListRow))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = KmOrange,
                                trackColor = Color(0x1AFFFFFF)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.status_looking_for_devices),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KmTextPrimary
                                )
                                Text(
                                    text = "Make sure the receiving device is on the same Wi-Fi.",
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
                                .clip(RoundedCornerShape(KmRadii.ListRow))
                                .background(KmGlassBackground)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) KmOrange else KmGlassBorder,
                                    shape = RoundedCornerShape(KmRadii.ListRow)
                                )
                                .clickable { selectedDevice = dev }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(KmRadii.Button))
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
                                    text = "On Wi-Fi · Ready",
                                    fontSize = 12.sp,
                                    color = KmTextMuted,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }

                            // Radio selection indicator
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) KmOrange else KmTextMuted,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(KmOrange)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 4. Advanced Disclosure: Connect by IP address ───────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(KmRadii.ListRow))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.ListRow))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advanced",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = KmTextPrimary
                            )
                            Icon(
                                imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isAdvancedExpanded) "Collapse" else "Expand",
                                tint = KmTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = isAdvancedExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            ) {
                                HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(KmRadii.Button))
                                        .background(Color(0x14FFFFFF))
                                        .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Button))
                                        .clickable { showIpDialog = true }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = KmOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.action_connect_ip),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = KmTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Security notice row
                item {
                    KmSecurityNoticeRow()
                }
            }

            // ── 5. Sticky Bottom Action Button (The ONLY Primary Action) ─────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(KmRadii.Button))
                        .background(
                            if (isReady) KmBrandGradient else SolidColor(KmBlackElevated)
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
                            val dev = selectedDevice
                            if (dev != null && files.isNotEmpty()) {
                                scope.launch {
                                    val urisToSend = files.mapNotNull { it.uri }
                                    manager.sendUris(target = dev, uris = urisToSend)
                                    navController.navigate(Screen.Transfer.withSession("outgoing"))
                                }
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

    // Connect by IP Address Dialog
    if (showIpDialog) {
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            containerColor = KmBlackCard,
            title = {
                Text(
                    text = stringResource(R.string.action_connect_ip),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = KmTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter the local Wi-Fi IP address of the target PC:",
                        fontSize = 13.sp,
                        color = KmTextMuted
                    )
                    OutlinedTextField(
                        value = customIpInput,
                        onValueChange = { customIpInput = it },
                        placeholder = { Text("192.168.1.100", color = KmTextDisabled) },
                        singleLine = true,
                        shape = RoundedCornerShape(KmRadii.Input),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = KmGlassBackground,
                            unfocusedContainerColor = KmGlassBackground,
                            focusedBorderColor = KmOrange,
                            unfocusedBorderColor = KmGlassBorder,
                            focusedTextColor = KmTextPrimary,
                            unfocusedTextColor = KmTextPrimary
                        )
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
                    Text(stringResource(R.string.action_done), color = KmOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = KmTextMuted)
                }
            }
        )
    }
}

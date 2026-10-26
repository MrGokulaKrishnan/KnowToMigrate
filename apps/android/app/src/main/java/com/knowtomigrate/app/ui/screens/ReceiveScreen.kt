package com.knowtomigrate.app.ui.screens

/*
 * KnowToMigrate - ReceiveScreen
 * Updated: 22 October 2026
 * AMOLED black, KM Orange accents, Liquid Glass surfaces, 3-tab floating bottom bar
 */

import android.content.Context
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.R
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*

@Composable
fun ReceiveScreen(navController: NavController) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val serverProg by manager.serverProgress.collectAsState()

    var isAcceptingFiles by rememberSaveable { mutableStateOf(true) }
    var isDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    var saveFolder by rememberSaveable { mutableStateOf("Downloads / KnowToMigrate") }
    var deviceName by rememberSaveable { mutableStateOf(Build.MODEL ?: "Android Phone") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var pendingDeviceName by remember { mutableStateOf(deviceName) }

    // SAF folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val segment = uri.lastPathSegment ?: "Selected Folder"
            val cleanName = segment.substringAfterLast(":").ifEmpty { segment }
            saveFolder = cleanName
        }
    }

    // Local IP address detection using modern NetworkInterface (no deprecated WifiInfo APIs)
    val localIpAddress = remember {
        try {
            var foundIp: String? = null
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements() && foundIp == null) {
                    val nif = interfaces.nextElement()
                    if (nif.isLoopback || !nif.isUp) continue
                    val addrs = nif.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank() && !host.startsWith("127.")) {
                                foundIp = host
                                break
                            }
                        }
                    }
                }
            }
            foundIp ?: "192.168.1.10"
        } catch (_: Exception) {
            "192.168.1.10"
        }
    }

    // Navigate to active transfer if incoming transfer starts
    LaunchedEffect(serverProg.status) {
        if (serverProg.status == com.knowtomigrate.app.network.TransferStatus.TRANSFERRING) {
            navController.navigate(Screen.Transfer.withSession("incoming"))
        }
    }

    Scaffold(
        containerColor = KmBlack,
        bottomBar = {
            KmFloatingBottomBar(
                selectedIndex = 2,
                onSelect = { idx ->
                    when (idx) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> navController.navigate(Screen.Send.route)
                        2 -> { /* already on Receive */ }
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top row: Header & State text with toggle switch
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.nav_receive),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = KmTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isAcceptingFiles) KmOnline else KmTextMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    if (isAcceptingFiles) R.string.receive_status_active else R.string.receive_status_inactive
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isAcceptingFiles) KmOnline else KmTextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isAcceptingFiles,
                        onCheckedChange = { isAcceptingFiles = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = KmTextOnBrand,
                            checkedTrackColor = KmOrange,
                            uncheckedThumbColor = KmTextMuted,
                            uncheckedTrackColor = KmBlackElevated,
                            uncheckedBorderColor = KmGlassBorder
                        ),
                        modifier = Modifier.size(width = 52.dp, height = 48.dp)
                    )
                }
            }

            // 2. Pairing PIN Card (dimmed when toggle is off)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isAcceptingFiles) 1f else 0.38f)
                        .clip(RoundedCornerShape(KmRadii.Card))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    KmGlassSurfaceSecondary,
                                    KmGlassBackground
                                )
                            )
                        )
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Card))
                        .drawBehind {
                            drawLine(
                                color = Color(0x38FFFFFF),
                                start = Offset(0f, 0.5f),
                                end = Offset(size.width, 0.5f),
                                strokeWidth = 1f
                            )
                        }
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isAcceptingFiles) {
                            KmRadarWidget(size = 56.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = stringResource(R.string.pairing_pin_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = KmTextMuted
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "482 915",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 4.sp,
                                color = KmTextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.pairing_pin_caption),
                                fontSize = 12.sp,
                                color = KmTextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = KmTextMuted,
                                modifier = Modifier.size(44.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.receive_turn_on_to_receive),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KmTextSecondary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Turn on receiving above to accept incoming files and display your pairing PIN.",
                                fontSize = 12.sp,
                                color = KmTextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            // 3. "This phone" row (editable device name)
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x14FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = KmOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = deviceName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KmTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(
                                if (isAcceptingFiles) R.string.label_visible_nearby else R.string.label_hidden_nearby
                            ),
                            fontSize = 12.sp,
                            color = KmTextMuted
                        )
                    }

                    // Edit button (touch target >= 48dp)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                pendingDeviceName = deviceName
                                showEditNameDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit device name",
                            tint = KmTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 4. "Save to" row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KmRadii.ListRow))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.ListRow))
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x14FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = KmTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = saveFolder,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KmTextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Saved files show in Downloads and Files",
                            fontSize = 12.sp,
                            color = KmTextMuted,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    // Change folder button (touch target >= 48dp)
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(KmRadii.Button))
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Button))
                            .clickable { folderPickerLauncher.launch(null) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Change",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = KmTextPrimary
                        )
                    }
                }
            }

            // 5. Honest security status row
            item {
                KmSecurityNoticeRow()
            }

            // 6. Details disclosure (collapsed by default)
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
                            .heightIn(min = 48.dp)
                            .clickable { isDetailsExpanded = !isDetailsExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Details",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = KmTextPrimary
                        )
                        Icon(
                            imageVector = if (isDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isDetailsExpanded) "Collapse" else "Expand",
                            tint = KmTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isDetailsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(2.dp))
                            ReceiveDetailItem(
                                label = "Device ID",
                                value = "android-${Build.MODEL.lowercase().replace(' ', '-')}"
                            )
                            ReceiveDetailItem(
                                label = "Save path",
                                value = saveFolder
                            )
                            ReceiveDetailItem(
                                label = "Local IP",
                                value = localIpAddress
                            )
                            ReceiveDetailItem(
                                label = "Transfer port",
                                value = "54124"
                            )
                            ReceiveDetailItem(
                                label = "Discovery port",
                                value = "54123"
                            )
                            ReceiveDetailItem(
                                label = "Network",
                                value = "Local Wi-Fi"
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Device Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "Edit device name",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = KmTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = pendingDeviceName,
                    onValueChange = { pendingDeviceName = it },
                    label = { Text("Device name", color = KmTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = KmTextPrimary,
                        unfocusedTextColor = KmTextPrimary,
                        focusedBorderColor = KmOrange,
                        unfocusedBorderColor = KmGlassBorder,
                        focusedLabelColor = KmOrange,
                        unfocusedLabelColor = KmTextMuted,
                        cursorColor = KmOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = pendingDeviceName.trim()
                        if (trimmed.isNotEmpty()) {
                            deviceName = trimmed
                        }
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save", color = KmOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = KmTextMuted)
                }
            },
            containerColor = KmBlackCard,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(KmRadii.Card)
        )
    }
}

@Composable
private fun ReceiveDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = KmTextMuted
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = KmTextPrimary
        )
    }
}

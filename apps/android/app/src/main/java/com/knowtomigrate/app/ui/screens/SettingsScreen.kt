package com.knowtomigrate.app.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.KmBadge
import com.knowtomigrate.app.ui.components.KmSecondaryButton
import com.knowtomigrate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val preferences = manager.preferences

    val deviceName by preferences.deviceName.collectAsState()
    val folderDisplay by preferences.receiveFolderDisplay.collectAsState()
    val isDiscoverable by preferences.isDiscoverable.collectAsState()
    val autoAcceptTrusted by preferences.autoAcceptTrusted.collectAsState()
    val requirePin by preferences.requirePin.collectAsState()
    val verifySha256 by preferences.verifySha256.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(deviceName) }

    var wifiDirectEnabled by remember { mutableStateOf(true) }
    var bluetoothEnabled by remember { mutableStateOf(true) }

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

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Device Section
            SettingsSectionHeader(title = "Device Identity", icon = Icons.Default.Smartphone)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    SettingsClickableRow(
                        label = "Device Name",
                        value = deviceName,
                        subtitle = "Tap to rename this device",
                        onClick = {
                            editedName = deviceName
                            showEditNameDialog = true
                        }
                    )
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Hardware Model", value = Build.MODEL ?: "Android Device")
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Device ID", value = manager.localDeviceId.take(18) + "…")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Transfer Section
            SettingsSectionHeader(title = "Transfer & Storage", icon = Icons.Default.SwapHoriz)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    SettingsClickableRow(
                        label = "Receive Folder",
                        value = folderDisplay,
                        subtitle = "Files are indexed to Gallery & Downloads",
                        onClick = { folderPicker.launch(null) }
                    )
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = "SHA-256 Verification",
                        description = "Verify cryptographic integrity hash for every transferred chunk",
                        checked = verifySha256,
                        onCheckedChange = { preferences.setVerifySha256(it) }
                    )
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = "Automatic Resume",
                        description = "Automatically resume interrupted transfers from last verified chunk",
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Discovery Section
            SettingsSectionHeader(title = "Discovery & Transports", icon = Icons.Default.Wifi)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    SettingsToggleRow(
                        label = "Local Wi-Fi Discovery",
                        description = "Broadcast and discover devices via UDP beacon on port 54123",
                        checked = isDiscoverable,
                        onCheckedChange = { preferences.setDiscoverable(it) }
                    )
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = "Wi-Fi Direct Support",
                        description = "High-speed direct offline connection without local router",
                        checked = wifiDirectEnabled,
                        onCheckedChange = { wifiDirectEnabled = it }
                    )
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = "Bluetooth Assistance",
                        description = "Discover nearby devices even when Wi-Fi multicast is restricted",
                        checked = bluetoothEnabled,
                        onCheckedChange = { bluetoothEnabled = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Security Section
            SettingsSectionHeader(title = "Security & Verification", icon = Icons.Default.Security)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    SettingsToggleRow(
                        label = "Require 6-Digit PIN Verification",
                        description = "Display and match confirmation code before starting file transfer",
                        checked = requirePin,
                        onCheckedChange = { preferences.setRequirePin(it) }
                    )
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = "Auto-Accept Trusted Devices",
                        description = "Allow transfers from previously paired devices without prompt",
                        checked = autoAcceptTrusted,
                        onCheckedChange = { preferences.setAutoAcceptTrusted(it) }
                    )
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Path Traversal Guard", color = KmTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Blocks malicious ../ directory path injections", color = KmTextMuted, fontSize = 12.sp)
                        }
                        KmBadge(text = "Enforced", color = KmSuccess)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. About Section
            SettingsSectionHeader(title = "About KnowToMigrate", icon = Icons.Default.Info)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(1.dp, KmOrange, RoundedCornerShape(12.dp))
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.knowtomigrate.app.R.drawable.logo),
                                contentDescription = "KnowToMigrate Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "KnowToMigrate",
                                color = KmTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Move Anything. Anywhere. Seamlessly.",
                                color = KmTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Application Version", value = "1.0.0")
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Build Date", value = "2026.09.25")
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Protocol Specification", value = "KTM v2 Multi-Transport")
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Encryption Standard", value = "AES-256-GCM + X25519")
                    Divider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Official Website", value = "knowtomigrate.web.app")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Edit Name Dialog
        if (showEditNameDialog) {
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = { Text("Edit Device Name", color = KmTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("This name will be broadcast to nearby devices for discovery.", color = KmTextMuted, fontSize = 13.sp)
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
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = KmOrange, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            color = KmOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun SettingsClickableRow(
    label: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = KmTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(value, color = KmOrangeLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = KmTextMuted, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = KmTextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, color = KmTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = KmTextMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KmOrange,
                uncheckedThumbColor = KmTextMuted,
                uncheckedTrackColor = KmBlackElevated,
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = KmTextSecondary, fontSize = 14.sp)
        Text(value, color = KmTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

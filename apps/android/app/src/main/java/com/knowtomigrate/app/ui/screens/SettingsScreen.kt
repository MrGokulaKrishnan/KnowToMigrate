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
import androidx.compose.material.icons.automirrored.filled.*
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
import com.knowtomigrate.app.BuildConfig
import com.knowtomigrate.app.updater.UpdateManager
import com.knowtomigrate.app.updater.UpdateState
import com.knowtomigrate.app.ui.components.KmProgressBar
import java.util.Locale
import com.knowtomigrate.app.ui.components.KmBadge
import com.knowtomigrate.app.ui.components.KmSecondaryButton
import com.knowtomigrate.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val preferences = manager.preferences

    val coroutineScope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager.getInstance(context) }
    val updateState by updateManager.updateState.collectAsState()

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KmTextPrimary)
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
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Hardware Model", value = Build.MODEL ?: "Android Device")
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Device ID", value = manager.localDeviceId.take(18) + "â€¦")
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
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = "SHA-256 Verification",
                        description = "Verify cryptographic integrity hash for every transferred chunk",
                        checked = verifySha256,
                        onCheckedChange = { preferences.setVerifySha256(it) }
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
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
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = "Wi-Fi Direct Support",
                        description = "High-speed direct offline connection without local router",
                        checked = wifiDirectEnabled,
                        onCheckedChange = { wifiDirectEnabled = it }
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
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
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = "Auto-Accept Trusted Devices",
                        description = "Allow transfers from previously paired devices without prompt",
                        checked = autoAcceptTrusted,
                        onCheckedChange = { preferences.setAutoAcceptTrusted(it) }
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
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

            // 5. Update Center (Liquid Glass APK Self-Updater)
            SettingsSectionHeader(title = "Update Center", icon = Icons.Default.SystemUpdate)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header Row: Title, Current version, Check button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("KnowToMigrate APK Self-Updater", color = KmTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                color = KmTextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { updateManager.checkForUpdates() },
                            enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                            colors = ButtonDefaults.buttonColors(containerColor = KmOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            if (updateState is UpdateState.Checking) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Checking...", color = Color.White, fontSize = 12.sp)
                            } else {
                                Text("Check for Updates", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (val state = updateState) {
                        is UpdateState.Idle -> {
                            Text("Independent APK distribution via direct HTTPS download and cryptographic verification.", color = KmTextMuted, fontSize = 12.sp)
                        }
                        is UpdateState.Checking -> {
                            Text("Querying official release channel for latest package...", color = KmOrangeLight, fontSize = 12.sp)
                        }
                        is UpdateState.UpToDate -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = KmSuccess.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KmSuccess.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KmSuccess, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("KnowToMigrate is up to date (v${BuildConfig.VERSION_NAME}).", color = KmTextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                        is UpdateState.UpdateAvailable -> {
                            val manifest = state.manifest
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF140D00),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, KmOrange.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Update Available: v${manifest.versionName}", color = KmOrangeLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            KmBadge(text = manifest.formattedSize, color = KmOrange)
                                        }

                                        if (manifest.releaseNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("What's New in v${manifest.versionName}:", color = KmTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            manifest.releaseNotes.forEach { note ->
                                                Text("• $note", color = KmTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val isTransferring = updateManager.isTransferActive()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (isTransferring) {
                                        OutlinedButton(
                                            onClick = {
                                                updateManager.startDownload(manifest)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("Download in Background", color = KmTextPrimary, fontSize = 12.sp)
                                        }
                                    }
                                    Button(
                                        onClick = { updateManager.startDownload(manifest) },
                                        colors = ButtonDefaults.buttonColors(containerColor = KmOrange),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Update Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        is UpdateState.Downloading -> {
                            val manifest = state.manifest
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Downloading v${manifest.versionName}...", color = KmTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${(state.progress * 100).toInt()}%", color = KmOrangeLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                KmProgressBar(progress = state.progress)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${com.knowtomigrate.app.network.KtmFormatting.formatBytes(state.bytesDownloaded)} / ${com.knowtomigrate.app.network.KtmFormatting.formatBytes(state.totalBytes)}",
                                        color = KmTextMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", state.speedMBps)} MB/s · ETA ${state.etaSecs}s",
                                        color = KmTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { updateManager.cancelDownload(manifest) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Cancel", color = KmTextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                        is UpdateState.Verifying -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = KmOrange, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Verifying SHA-256 cryptographic signature...", color = KmOrangeLight, fontSize = 13.sp)
                            }
                        }
                        is UpdateState.ReadyToInstall -> {
                            val apkFile = state.apkFile
                            val manifest = state.manifest
                            val isTransferring = updateManager.isTransferActive()

                            Column {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0D2614),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, KmSuccess.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = KmSuccess, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("v Verified & Ready", color = KmTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("SHA-256 integrity check passed. Safe to install.", color = KmSuccess, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (isTransferring) {
                                        OutlinedButton(
                                            onClick = {
                                                updateManager.queueInstallAfterTransfer(apkFile, manifest)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("Install After Transfer", color = KmOrangeLight, fontSize = 12.sp)
                                        }
                                    }
                                    Button(
                                        onClick = { updateManager.installApk(apkFile, manifest) },
                                        colors = ButtonDefaults.buttonColors(containerColor = KmSuccess),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Install Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        is UpdateState.QueuedAfterTransfer -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF140D00),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KmOrange),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = KmOrange, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Update Queued for Installation", color = KmTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("v${state.manifest.versionName} will automatically launch PackageInstaller once active transfers finish.", color = KmTextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        is UpdateState.Installing -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = KmSuccess, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Launching Android PackageInstaller...", color = KmTextPrimary, fontSize = 13.sp)
                            }
                        }
                        is UpdateState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = KmError.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KmError.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = KmError, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Update Check Failed", color = KmError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(state.message, color = KmTextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. About Section
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
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Application Version", value = "v${BuildConfig.VERSION_NAME}")
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Updated On", value = try { BuildConfig.BUILD_DATE } catch (_: Throwable) { "04 October 2026" })
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Transfer Engine", value = "Pluto Engine")
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Protocol Specification", value = "Pluto v2 Multi-Transport")
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(label = "Encryption Standard", value = "AES-256-GCM + X25519")
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
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


private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format(Locale.US, "%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}

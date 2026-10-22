package com.knowtomigrate.app.ui.screens

/*
 * KnowToMigrate - SettingsScreen
 * Updated: 22 October 2026
 * AMOLED black, KM Orange accents, Liquid Glass surfaces
 * Standardized sections: Device, Receiving, Network, Updates, About
 */

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.BuildConfig
import com.knowtomigrate.app.R
import com.knowtomigrate.app.network.KtmFormatting
import com.knowtomigrate.app.ui.components.KmBadge
import com.knowtomigrate.app.ui.components.KmProgressBar
import com.knowtomigrate.app.ui.theme.*
import com.knowtomigrate.app.updater.UpdateManager
import com.knowtomigrate.app.updater.UpdateState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val preferences = manager.preferences

    val updateManager = remember { UpdateManager.getInstance(context) }
    val updateState by updateManager.updateState.collectAsState()

    val deviceName by preferences.deviceName.collectAsState()
    val folderDisplay by preferences.receiveFolderDisplay.collectAsState()
    val isDiscoverable by preferences.isDiscoverable.collectAsState()
    val autoAcceptTrusted by preferences.autoAcceptTrusted.collectAsState()
    val requirePin by preferences.requirePin.collectAsState()
    val verifySha256 by preferences.verifySha256.collectAsState()

    var keepBothFiles by rememberSaveable { mutableStateOf(true) }
    var isAdvancedNetworkExpanded by rememberSaveable { mutableStateOf(false) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(deviceName) }

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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = KmTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = KmTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KmBlack,
                    titleContentColor = KmTextPrimary,
                    navigationIconContentColor = KmTextPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // ─────────────────────────────────────────────────────────────────
            // 1. Device Section
            // ─────────────────────────────────────────────────────────────────
            SettingsSectionHeader(
                title = stringResource(R.string.settings_section_device),
                icon = Icons.Default.PhoneAndroid
            )
            Surface(
                shape = RoundedCornerShape(KmRadii.Card),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    SettingsClickableRow(
                        label = "Device name",
                        value = deviceName,
                        subtitle = "Visible to nearby devices on local network",
                        onClick = {
                            editedName = deviceName
                            showEditNameDialog = true
                        }
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsClickableRow(
                        label = stringResource(R.string.label_save_to),
                        value = folderDisplay,
                        subtitle = "Saved files show in Downloads and Files",
                        onClick = { folderPicker.launch(null) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─────────────────────────────────────────────────────────────────
            // 2. Receiving Section
            // ─────────────────────────────────────────────────────────────────
            SettingsSectionHeader(
                title = stringResource(R.string.settings_section_receiving),
                icon = Icons.Default.Download
            )
            Surface(
                shape = RoundedCornerShape(KmRadii.Card),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    SettingsToggleRow(
                        label = stringResource(R.string.settings_auto_accept),
                        description = "Allow transfers from paired devices without prompting",
                        checked = autoAcceptTrusted,
                        onCheckedChange = { preferences.setAutoAcceptTrusted(it) }
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = stringResource(R.string.settings_keep_both),
                        description = "Save duplicate as a new file if a file with the same name exists",
                        checked = keepBothFiles,
                        onCheckedChange = { keepBothFiles = it }
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = "Require pairing PIN",
                        description = "Confirm the same 6-digit code on both devices before accepting",
                        checked = requirePin,
                        onCheckedChange = { preferences.setRequirePin(it) }
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsToggleRow(
                        label = "Verify file integrity",
                        description = "Verify SHA-256 hash to ensure transfer completed without corruption",
                        checked = verifySha256,
                        onCheckedChange = { preferences.setVerifySha256(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─────────────────────────────────────────────────────────────────
            // 3. Network Section
            // ─────────────────────────────────────────────────────────────────
            SettingsSectionHeader(
                title = stringResource(R.string.settings_section_network),
                icon = Icons.Default.Wifi
            )
            Surface(
                shape = RoundedCornerShape(KmRadii.Card),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    SettingsToggleRow(
                        label = "Local Wi-Fi discovery",
                        description = "Allow other devices on this Wi-Fi network to find this phone",
                        checked = isDiscoverable,
                        onCheckedChange = { preferences.setDiscoverable(it) }
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)

                    // Advanced disclosure
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { isAdvancedNetworkExpanded = !isAdvancedNetworkExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Advanced",
                                color = KmTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Port settings and network protocols",
                                color = KmTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = if (isAdvancedNetworkExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isAdvancedNetworkExpanded) "Collapse" else "Expand",
                            tint = KmTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isAdvancedNetworkExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                            SettingsInfoRow(label = stringResource(R.string.settings_port), value = "54124")
                            SettingsInfoRow(label = "Discovery port", value = "54123")
                            SettingsInfoRow(label = "Transport", value = "Local Wi-Fi / Direct LAN")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─────────────────────────────────────────────────────────────────
            // 4. Updates Section (APK Self-Updater)
            // ─────────────────────────────────────────────────────────────────
            SettingsSectionHeader(
                title = "Updates",
                icon = Icons.Default.SystemUpdate
            )
            Surface(
                shape = RoundedCornerShape(KmRadii.Card),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Updates",
                                color = KmTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                color = KmTextMuted,
                                fontSize = 12.sp
                            )
                        }

                        // Check for updates button
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(KmRadii.Button))
                                .background(Color(0x1AFFFFFF))
                                .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Button))
                                .clickable(
                                    enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading
                                ) {
                                    updateManager.checkForUpdates()
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (updateState is UpdateState.Checking) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = KmOrange,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Checking...", color = KmOrange, fontSize = 12.sp)
                                }
                            } else {
                                Text(
                                    text = "Check",
                                    color = KmTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (val state = updateState) {
                        is UpdateState.Idle -> {
                            Text(
                                text = "Direct updates verified via SHA-256 cryptographic checksums.",
                                color = KmTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        is UpdateState.Checking -> {
                            Text(
                                text = "Checking for available updates...",
                                color = KmOrangeLight,
                                fontSize = 12.sp
                            )
                        }
                        is UpdateState.UpToDate -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x1F22C55E),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4022C55E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = KmOnline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "KnowToMigrate is up to date (v${BuildConfig.VERSION_NAME}).",
                                        color = KmTextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        is UpdateState.UpdateAvailable -> {
                            val manifest = state.manifest
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
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
                                            Text(
                                                text = "New version: v${manifest.versionName}",
                                                color = KmOrangeLight,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            KmBadge(text = manifest.formattedSize, color = KmOrange)
                                        }

                                        if (manifest.releaseNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            manifest.releaseNotes.forEach { note ->
                                                Text(
                                                    text = "• $note",
                                                    color = KmTextSecondary,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { updateManager.startDownload(manifest) },
                                    colors = ButtonDefaults.buttonColors(containerColor = KmOrange),
                                    shape = RoundedCornerShape(KmRadii.Button),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "Download and Install Update",
                                        color = KmTextOnBrand,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
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
                                    Text(
                                        text = "Downloading v${manifest.versionName}...",
                                        color = KmTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${(state.progress * 100).toInt()}%",
                                        color = KmOrangeLight,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                KmProgressBar(progress = state.progress)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${KtmFormatting.formatBytes(state.bytesDownloaded)} / ${KtmFormatting.formatBytes(state.totalBytes)}",
                                        color = KmTextMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", state.speedMBps)} MB/s · ETA ${state.etaSecs}s",
                                        color = KmTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        is UpdateState.Verifying -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = KmOrange, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Verifying SHA-256 cryptographic integrity...", color = KmOrangeLight, fontSize = 12.sp)
                            }
                        }
                        is UpdateState.ReadyToInstall -> {
                            val apkFile = state.apkFile
                            val manifest = state.manifest
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x1F22C55E),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4022C55E)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = KmOnline, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Verified & Ready", color = KmTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("SHA-256 hash verified. Safe to install.", color = KmOnline, fontSize = 11.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { updateManager.installApk(apkFile, manifest) },
                                    colors = ButtonDefaults.buttonColors(containerColor = KmOnline),
                                    shape = RoundedCornerShape(KmRadii.Button),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Install Update Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                        is UpdateState.QueuedAfterTransfer -> {
                            Text(
                                text = "Update will install automatically after active transfers complete.",
                                color = KmOrangeLight,
                                fontSize = 12.sp
                            )
                        }
                        is UpdateState.Installing -> {
                            Text(
                                text = "Opening PackageInstaller...",
                                color = KmOnline,
                                fontSize = 12.sp
                            )
                        }
                        is UpdateState.Error -> {
                            Text(
                                text = state.message,
                                color = KmError,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─────────────────────────────────────────────────────────────────
            // 5. About Section
            // ─────────────────────────────────────────────────────────────────
            SettingsSectionHeader(
                title = stringResource(R.string.settings_section_about),
                icon = Icons.Default.Info
            )
            Surface(
                shape = RoundedCornerShape(KmRadii.Card),
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black)
                                .border(1.dp, KmOrange, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "KM",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = KmOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "KnowToMigrate",
                                color = KmTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Move Anything. Anywhere. Seamlessly.",
                                color = KmTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(
                        label = "Version",
                        value = "v${BuildConfig.VERSION_NAME}"
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(
                        label = "Security status",
                        value = KtmUiConstants.SECURITY_LABEL
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(
                        label = "Release date",
                        value = "22 October 2026"
                    )
                    HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                    SettingsInfoRow(
                        label = "Website",
                        value = "knowtomigrate.web.app"
                    )
                }
            }
        }

        // Edit Name Dialog
        if (showEditNameDialog) {
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = { Text("Edit device name", color = KmTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "This name will be broadcast to nearby devices for discovery.",
                            color = KmTextMuted,
                            fontSize = 13.sp
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
                                unfocusedTextColor = KmTextPrimary,
                                cursorColor = KmOrange
                            ),
                            shape = RoundedCornerShape(KmRadii.Input),
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
                        colors = ButtonDefaults.buttonColors(containerColor = KmOrange),
                        shape = RoundedCornerShape(KmRadii.Button)
                    ) {
                        Text("Save", color = KmTextOnBrand, fontWeight = FontWeight.Bold)
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
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KmOrange,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(Locale.getDefault()),
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
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        Text(
            text = value,
            color = KmTextMuted,
            fontSize = 13.sp,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium
        )
    }
}

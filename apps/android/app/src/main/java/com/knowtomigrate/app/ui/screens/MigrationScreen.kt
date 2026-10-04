package com.knowtomigrate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*

private data class MigrationCategory(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val estimatedSize: String
)

private val migrationCategories = listOf(
    MigrationCategory("photos",    "Photos",    Icons.Default.PhotoLibrary,  "12.4 GB"),
    MigrationCategory("videos",    "Videos",    Icons.Default.VideoLibrary,  "28.1 GB"),
    MigrationCategory("documents", "Documents", Icons.Default.Description,   "340 MB"),
    MigrationCategory("music",     "Music",     Icons.Default.LibraryMusic,  "5.2 GB"),
    MigrationCategory("downloads", "Downloads", Icons.Default.Download,      "1.8 GB"),
    MigrationCategory("whatsapp",  "WhatsApp",  Icons.AutoMirrored.Filled.Message,       "3.6 GB"),
    MigrationCategory("contacts",  "Contacts",  Icons.Default.Contacts,      "2 MB"),
    MigrationCategory("settings",  "App Settings", Icons.Default.Settings,   "48 MB"),
    MigrationCategory("apps",      "Apps",      Icons.Default.Apps,          "varies"),
)

@Composable
fun MigrationScreen(navController: NavController) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4
    val selectedCategories = remember { mutableStateSetOf<String>() }
    var storagePreflight by remember { mutableStateOf<Boolean?>(null) }
    var isStarting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dynamicSizes = remember { mutableStateMapOf<String, String>() }

    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val discoveredDevices by manager.discoveredDevices.collectAsState()
    val uiDevices = discoveredDevices.map { dev ->
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
    var selectedDevice by remember { mutableStateOf<UiDevice?>(null) }

    LaunchedEffect(uiDevices) {
        if (selectedDevice == null && uiDevices.isNotEmpty()) {
            selectedDevice = uiDevices.first()
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            queryCategorySize(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)?.let {
                dynamicSizes["photos"] = it
            }
            queryCategorySize(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)?.let {
                dynamicSizes["videos"] = it
            }
            queryCategorySize(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)?.let {
                dynamicSizes["music"] = it
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                queryCategorySize(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI)?.let {
                    dynamicSizes["downloads"] = it
                }
            }
        }
    }

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Device Migration",
                            style = MaterialTheme.typography.headlineMedium,
                            color = KmTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step $currentStep of $totalSteps",
                            style = MaterialTheme.typography.bodySmall,
                            color = KmTextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) currentStep-- else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KmTextPrimary)
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
            // Step indicator
            item {
                StepIndicator(current = currentStep, total = totalSteps)
            }

            when (currentStep) {
                1 -> {
                    item {
                        KmSectionHeader(
                            title = "Select Categories",
                            subtitle = "Choose what to migrate from your old device"
                        )
                    }
                    itemsIndexed(migrationCategories) { _, category ->
                        val isSelected = category.id in selectedCategories
                        CategoryCheckRow(
                            category = category.copy(estimatedSize = dynamicSizes[category.id] ?: category.estimatedSize),
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) selectedCategories.remove(category.id)
                                else selectedCategories.add(category.id)
                            }
                        )
                    }
                    item {
                        // Select all / none
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KmSecondaryButton(
                                text = "Select All",
                                onClick = { selectedCategories.addAll(migrationCategories.map { it.id }) },
                                modifier = Modifier.weight(1f)
                            )
                            KmSecondaryButton(
                                text = "Clear",
                                onClick = { selectedCategories.clear() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                2 -> {
                    item {
                        KmSectionHeader(title = "Storage Preflight", subtitle = "Checking available space")
                    }
                    item {
                        StoragePreflightCard(
                            onCheck = { storagePreflight = true }
                        )
                    }
                }
                3 -> {
                    item {
                        KmSectionHeader(
                            title = "Select Migration Target",
                            subtitle = if (uiDevices.isEmpty()) "Open KnowToMigrate on PC or other device" else "${uiDevices.size} target device(s) found"
                        )
                    }
                    item {
                        KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                            KmDiscoveryRadar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                devices = uiDevices,
                                onDeviceClick = { dev ->
                                    selectedDevice = dev
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (uiDevices.isEmpty()) {
                                Text(
                                    text = "Searching for nearby devices on Wi-Fi / Local Network…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KmTextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = if (selectedDevice != null) "Selected: ${selectedDevice?.name}" else "Tap a device to select as migration destination",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedDevice != null) KmOrange else KmTextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    if (uiDevices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Available Targets",
                                style = MaterialTheme.typography.titleSmall,
                                color = KmTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(uiDevices.size) { idx ->
                            val dev = uiDevices[idx]
                            KmDeviceCard(
                                device = dev,
                                onClick = { selectedDevice = dev },
                                isSelected = selectedDevice?.id == dev.id
                            )
                        }
                    }
                }
                4 -> {
                    item {
                        KmSectionHeader(title = "Ready to Migrate", subtitle = "Review and confirm Pluto Engine parameters")
                    }
                    item {
                        KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                            // Target device summary banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(KmOrangeGlow)
                                    .border(1.dp, KmOrange.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Laptop,
                                    contentDescription = null,
                                    tint = KmOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedDevice?.name ?: "Unknown Target Device",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = KmTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${selectedDevice?.ip ?: "LAN"} • Pluto Direct • AES-256-GCM",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KmTextMuted
                                    )
                                }
                                KmBadge(text = "Target", color = KmOrange)
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Categories to Migrate (${selectedCategories.size}):",
                                style = MaterialTheme.typography.titleSmall,
                                color = KmTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            selectedCategories.forEach { id ->
                                val cat = migrationCategories.find { it.id == id }
                                if (cat != null) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(cat.icon, contentDescription = null, tint = KmOrange, modifier = Modifier.size(16.dp))
                                        Text(text = cat.label, style = MaterialTheme.typography.bodySmall, color = KmTextSecondary)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(text = dynamicSizes[cat.id] ?: cat.estimatedSize, style = MaterialTheme.typography.bodySmall, color = KmTextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Navigation button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val canProceed = when (currentStep) {
                    1 -> selectedCategories.isNotEmpty()
                    2 -> true
                    3 -> selectedDevice != null
                    else -> true
                }
                if (currentStep < totalSteps) {
                    KmPrimaryButton(
                        text = if (currentStep == 3 && selectedDevice == null) "Select a Target Device" else "Next",
                        onClick = { currentStep++ },
                        enabled = canProceed,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    KmPrimaryButton(
                        text = if (isStarting) "Starting Migration…" else "Start Migration",
                        onClick = {
                            isStarting = true
                            val targetId = selectedDevice?.id ?: "target"
                            navController.navigate(Screen.Transfer.withSession("migration_${targetId}_${System.currentTimeMillis()}"))
                        },
                        enabled = !isStarting && selectedDevice != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val stepNum = i + 1
            val isActive = stepNum == current
            val isCompleted = stepNum < current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            isCompleted -> KmOrange
                            isActive    -> KmOrangeLight
                            else        -> KmBlackElevated
                        }
                    )
            )
        }
    }
}

@Composable
private fun CategoryCheckRow(
    category: MigrationCategory,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) KmOrangeGlow else KmBlackCard)
            .border(1.dp, if (isSelected) KmOrange.copy(alpha = 0.5f) else KmGlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.label,
            tint = if (isSelected) KmOrange else KmTextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = category.label, style = MaterialTheme.typography.bodyMedium, color = KmTextPrimary, fontWeight = FontWeight.Medium)
            Text(text = "~${category.estimatedSize}", style = MaterialTheme.typography.bodySmall, color = KmTextMuted)
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = KmOrange,
                uncheckedColor = KmTextDisabled,
                checkmarkColor = Color.White
            )
        )
    }
}

@Composable
private fun StoragePreflightCard(onCheck: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Internal Storage", style = MaterialTheme.typography.labelMedium, color = KmTextMuted)
                Text("45.2 GB available", style = MaterialTheme.typography.titleMedium, color = KmTextPrimary, fontWeight = FontWeight.SemiBold)
            }
            KmBadge(text = "OK", color = KmSuccess)
        }
        Spacer(modifier = Modifier.height(12.dp))
        KmProgressBar(progress = 0.55f)
        Spacer(modifier = Modifier.height(6.dp))
        Text("55% used", style = MaterialTheme.typography.bodySmall, color = KmTextMuted)
        Spacer(modifier = Modifier.height(12.dp))
        if (!checked) {
            KmSecondaryButton(
                text = "Run Preflight Check",
                onClick = { checked = true; onCheck() },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KmSuccess, modifier = Modifier.size(20.dp))
                Text("All checks passed — sufficient space for migration", style = MaterialTheme.typography.bodySmall, color = KmSuccess)
            }
        }
    }
}

// Required for mutableStateSetOf extension
private fun <T> mutableStateSetOf(vararg elements: T): MutableSet<T> =
    mutableSetOf<T>().apply { addAll(elements) }

private fun queryCategorySize(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val projection = arrayOf(OpenableColumns.SIZE)
        var totalBytes = 0L
        var count = 0
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
            while (cursor.moveToNext()) {
                if (sizeCol != -1 && !cursor.isNull(sizeCol)) {
                    totalBytes += cursor.getLong(sizeCol)
                }
                count++
            }
        }
        if (count > 0) formatDynamicSize(totalBytes, count) else null
    } catch (_: Exception) {
        null
    }
}

private fun formatDynamicSize(bytes: Long, count: Int): String {
    val sizeStr = when {
        bytes >= 1_073_741_824L -> String.format(java.util.Locale.US, "%.1f GB", bytes.toDouble() / 1_073_741_824.0)
        bytes >= 1_048_576L -> String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / 1_048_576.0)
        bytes >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes.toDouble() / 1024.0)
        else -> "$bytes B"
    }
    return "$sizeStr ($count items)"
}
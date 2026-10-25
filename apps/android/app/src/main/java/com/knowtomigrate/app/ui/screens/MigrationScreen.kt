package com.knowtomigrate.app.ui.screens

/*
 * KnowToMigrate - MigrationScreen ("Move device")
 * Updated: 25 October 2026
 * AMOLED black, KM Orange accents, Liquid Glass surfaces
 * Rebranded to "Move device" with KmStepIndicator and KmStorageGauge
 */

import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.R
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class MigrationCategory(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val estimatedSize: String
)

private val migrationCategories = listOf(
    MigrationCategory("photos",    "Photos",       Icons.Default.PhotoLibrary,        "Scanning..."),
    MigrationCategory("videos",    "Videos",       Icons.Default.VideoLibrary,        "Scanning..."),
    MigrationCategory("documents", "Documents",    Icons.Default.Description,         "Ready to scan"),
    MigrationCategory("music",     "Music",        Icons.Default.LibraryMusic,        "Scanning..."),
    MigrationCategory("downloads", "Downloads",    Icons.Default.Download,            "Scanning..."),
    MigrationCategory("whatsapp",  "WhatsApp",     Icons.AutoMirrored.Filled.Message, "Ready to scan"),
    MigrationCategory("contacts",  "Contacts",     Icons.Default.Contacts,            "Ready to scan")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationScreen(navController: NavController) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4
    val selectedCategories = remember { mutableStateSetOf("photos", "documents", "contacts") }
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
            dynamicSizes["photos"] = queryCategorySize(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI) ?: "0 items (0 B)"
            dynamicSizes["videos"] = queryCategorySize(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI) ?: "0 items (0 B)"
            dynamicSizes["music"] = queryCategorySize(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) ?: "0 items (0 B)"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dynamicSizes["downloads"] = queryCategorySize(context, MediaStore.Downloads.EXTERNAL_CONTENT_URI) ?: "0 items (0 B)"
            } else {
                dynamicSizes["downloads"] = "0 items (0 B)"
            }
        }
    }

    // Build the 4-step wizard indicator
    val wizardSteps = remember(currentStep) {
        listOf(
            KmWizardStep(
                index = 1,
                title = "Categories",
                state = when {
                    currentStep > 1 -> KmStepState.DONE
                    currentStep == 1 -> KmStepState.CURRENT
                    else -> KmStepState.TODO
                }
            ),
            KmWizardStep(
                index = 2,
                title = "Storage",
                state = when {
                    currentStep > 2 -> KmStepState.DONE
                    currentStep == 2 -> KmStepState.CURRENT
                    else -> KmStepState.TODO
                }
            ),
            KmWizardStep(
                index = 3,
                title = "Destination",
                state = when {
                    currentStep > 3 -> KmStepState.DONE
                    currentStep == 3 -> KmStepState.CURRENT
                    else -> KmStepState.TODO
                }
            ),
            KmWizardStep(
                index = 4,
                title = "Review & Move",
                state = when {
                    currentStep == 4 -> KmStepState.CURRENT
                    else -> KmStepState.TODO
                }
            )
        )
    }

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.move_device_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = KmTextPrimary
                        )
                        Text(
                            text = "Step $currentStep of $totalSteps",
                            fontSize = 11.sp,
                            color = KmTextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep > 1) currentStep-- else navController.popBackStack()
                        },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Step Indicator Card
            item {
                Surface(
                    shape = RoundedCornerShape(KmRadii.Card),
                    color = KmGlassBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        KmStepIndicator(steps = wizardSteps)
                    }
                }
            }

            // 2. Step Content
            when (currentStep) {
                1 -> {
                    item {
                        Column {
                            Text(
                                text = "Choose what to move",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = KmTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Select data categories to transfer to your new device",
                                fontSize = 12.sp,
                                color = KmTextMuted
                            )
                        }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(KmRadii.Button))
                                    .background(Color(0x1AFFFFFF))
                                    .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Button))
                                    .clickable { selectedCategories.addAll(migrationCategories.map { it.id }) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Select All", color = KmTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(KmRadii.Button))
                                    .background(Color(0x1AFFFFFF))
                                    .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Button))
                                    .clickable { selectedCategories.clear() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Clear", color = KmTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Column {
                            Text(
                                text = "Storage check",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = KmTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Verify destination storage capacity before transferring",
                                fontSize = 12.sp,
                                color = KmTextMuted
                            )
                        }
                    }

                    item {
                        // Storage headroom check using KmStorageGauge
                        KmStorageGauge(
                            neededText = "18.5 GB",
                            freeText = "45.2 GB",
                            percentUsed = 0.55f
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(KmRadii.Card),
                            color = Color(0x1F22C55E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4022C55E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = KmOnline,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Sufficient storage space",
                                        color = KmTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Destination device has more than enough space for all selected categories.",
                                        color = KmOnline,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        Column {
                            Text(
                                text = "Choose destination device",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = KmTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiDevices.isEmpty()) "Open KnowToMigrate on your PC or target phone" else "${uiDevices.size} nearby device(s) found",
                                fontSize = 12.sp,
                                color = KmTextMuted
                            )
                        }
                    }

                    if (uiDevices.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(KmRadii.Card),
                                color = KmGlassBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    KmRadarWidget(size = 56.dp)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Looking for devices on your Wi-Fi…",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KmTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Make sure both devices are on the same Wi-Fi and have KnowToMigrate open.",
                                        fontSize = 12.sp,
                                        color = KmTextMuted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
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
                        Column {
                            Text(
                                text = "Review & Move",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = KmTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Confirm transfer details before moving your data",
                                fontSize = 12.sp,
                                color = KmTextMuted
                            )
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(KmRadii.Card),
                            color = KmGlassBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Target summary
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(KmRadii.ListRow))
                                        .background(Color(0x1AFF5A00))
                                        .border(1.dp, Color(0x40FF5A00), RoundedCornerShape(KmRadii.ListRow))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x26FF5A00)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Laptop,
                                            contentDescription = null,
                                            tint = KmOrange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedDevice?.name ?: "Unknown device",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KmTextPrimary
                                        )
                                        Text(
                                            text = "${selectedDevice?.ip ?: "LAN"} · Local Wi-Fi",
                                            fontSize = 11.sp,
                                            color = KmTextMuted
                                        )
                                    }
                                    KmBadge(text = "Target", color = KmOrange)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Categories to move (${selectedCategories.size}):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KmTextPrimary
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
                                            Text(text = cat.label, fontSize = 13.sp, color = KmTextSecondary)
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = dynamicSizes[cat.id] ?: cat.estimatedSize,
                                                fontSize = 12.sp,
                                                color = KmTextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Honest security status
                    item {
                        KmSecurityNoticeRow()
                    }
                }
            }

            // 3. Navigation / Action Button (The single primary action)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                val canProceed = when (currentStep) {
                    1 -> selectedCategories.isNotEmpty()
                    2 -> true
                    3 -> selectedDevice != null
                    else -> selectedDevice != null
                }

                if (currentStep < totalSteps) {
                    KmPrimaryButton(
                        text = if (currentStep == 1 && selectedCategories.isEmpty()) {
                            "Select at least one category"
                        } else if (currentStep == 3 && selectedDevice == null) {
                            "Choose a destination device"
                        } else {
                            "Continue"
                        },
                        onClick = { currentStep++ },
                        enabled = canProceed,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    KmPrimaryButton(
                        text = if (isStarting) "Starting move…" else "Start moving",
                        onClick = {
                            isStarting = true
                            val targetId = selectedDevice?.id ?: "target"
                            navController.navigate(Screen.Transfer.withSession("move_${targetId}_${System.currentTimeMillis()}"))
                        },
                        enabled = !isStarting && selectedDevice != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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
            .clip(RoundedCornerShape(KmRadii.ListRow))
            .background(if (isSelected) Color(0x1AFF5A00) else KmGlassBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0x66FF5A00) else KmGlassBorder,
                shape = RoundedCornerShape(KmRadii.ListRow)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0x33FF5A00) else Color(0x14FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.label,
                tint = if (isSelected) KmOrange else KmTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = KmTextPrimary
            )
            Text(
                text = "~${category.estimatedSize}",
                fontSize = 11.sp,
                color = KmTextMuted
            )
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = KmOrange,
                uncheckedColor = KmTextDisabled,
                checkmarkColor = KmTextOnBrand
            ),
            modifier = Modifier.size(48.dp)
        )
    }
}

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

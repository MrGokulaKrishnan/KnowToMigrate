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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    MigrationCategory("whatsapp",  "WhatsApp",  Icons.Default.Message,       "3.6 GB"),
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
                            category = category,
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
                        KmSectionHeader(title = "Connect Source Device", subtitle = "Ensure both devices are on the same Wi-Fi")
                    }
                    item {
                        KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                            KmDiscoveryRadar(modifier = Modifier.fillMaxWidth().height(160.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Searching for source device…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KmTextMuted
                            )
                        }
                    }
                }
                4 -> {
                    item {
                        KmSectionHeader(title = "Ready to Migrate", subtitle = "Review and confirm")
                    }
                    item {
                        KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Selected: ${selectedCategories.size} categories",
                                style = MaterialTheme.typography.bodyMedium,
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
                                        Text(text = cat.label, style = MaterialTheme.typography.bodySmall, color = KmTextSecondary)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(text = cat.estimatedSize, style = MaterialTheme.typography.bodySmall, color = KmTextMuted)
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
                if (currentStep < totalSteps) {
                    KmPrimaryButton(
                        text = "Next",
                        onClick = { currentStep++ },
                        enabled = if (currentStep == 1) selectedCategories.isNotEmpty() else true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    KmPrimaryButton(
                        text = if (isStarting) "Starting Migration…" else "Start Migration",
                        onClick = {
                            isStarting = true
                            navController.navigate(Screen.Transfer.withSession("migration_${System.currentTimeMillis()}"))
                        },
                        enabled = !isStarting,
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

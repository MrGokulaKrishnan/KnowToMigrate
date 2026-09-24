package com.knowtomigrate.app.ui.screens

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Sample data
// ─────────────────────────────────────────────────────────────────────────────

private val sampleDevices = listOf(
    UiDevice("1", "iPhone 15 Pro", "ios", "192.168.1.10", "online"),
    UiDevice("2", "MacBook Air M3", "macos", "192.168.1.11", "online"),
    UiDevice("3", "Pixel 8 Pro", "android", "192.168.1.12", "transferring"),
)

private data class RecentTransfer(
    val name: String,
    val size: String,
    val device: String,
    val direction: String,
    val status: String
)

private val sampleHistory = listOf(
    RecentTransfer("vacation_photos.zip", "2.3 GB", "iPhone 15 Pro", "sent", "completed"),
    RecentTransfer("project_docs.pdf", "18 MB", "MacBook Air M3", "received", "completed"),
    RecentTransfer("music_library.zip", "4.1 GB", "Pixel 8 Pro", "sent", "in progress"),
)

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    navController: NavController,
    sharedUris: List<Uri> = emptyList()
) {
    var selectedNavItem by remember { mutableIntStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val discoveredDevices by com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context).discoveredDevices.collectAsState()
    val uiDevices = discoveredDevices.map { dev ->
        UiDevice(
            id = dev.deviceId,
            name = dev.deviceName,
            platform = dev.platform.lowercase(),
            ip = dev.ipAddress,
            status = if (dev.isOnline) "online" else "offline"
        )
    }

    Scaffold(
        containerColor = KmBlack,
        bottomBar = {
            KmBottomBar(
                selectedIndex = selectedNavItem,
                onSelect = { idx ->
                    selectedNavItem = idx
                    when (idx) {
                        0 -> { /* already home */ }
                        1 -> navController.navigate(Screen.Send.route)
                        2 -> navController.navigate(Screen.Receive.route)
                        3 -> navController.navigate(Screen.History.route)
                        4 -> navController.navigate(Screen.Settings.route)
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                HomeHeader()
            }

            // Share Sheet banner
            if (sharedUris.isNotEmpty()) {
                item {
                    SharedFilesBanner(sharedUris, navController)
                }
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KmPrimaryButton(
                        text = "Send Files",
                        onClick = { navController.navigate(Screen.Send.route) },
                        modifier = Modifier.weight(1f)
                    )
                    KmSecondaryButton(
                        text = "Receive",
                        onClick = { navController.navigate(Screen.Receive.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Discovery radar
            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    KmSectionHeader(title = "Discovering nearby devices", subtitle = "Wi-Fi Direct • Local Network")
                    Spacer(modifier = Modifier.height(16.dp))
                    KmDiscoveryRadar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            // Nearby devices
            item {
                KmSectionHeader(
                    title = "Nearby Devices",
                    subtitle = if (uiDevices.isEmpty()) "Scanning on Wi-Fi (Port 54123)..." else "${uiDevices.size} discovered"
                )
            }
            if (uiDevices.isEmpty()) {
                item {
                    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No other devices detected on this network yet.\nMake sure KnowToMigrate is open on your Windows PC or other phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KmTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    }
                }
            } else {
                items(uiDevices) { device ->
                    KmDeviceCard(
                        device = device,
                        onClick = { navController.navigate(Screen.Send.route) }
                    )
                }
            }

            // Recent transfers
            item {
                KmSectionHeader(title = "Recent Transfers")
            }
            items(sampleHistory) { transfer ->
                RecentTransferRow(transfer)
            }

            // Migration CTA
            item {
                MigrationCtaCard(navController)
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header composable with KTM lightning bolt logo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(1.dp, KmOrange, RoundedCornerShape(8.dp))
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.knowtomigrate.app.R.drawable.logo),
                contentDescription = "KnowToMigrate Logo",
                modifier = Modifier.fillMaxSize().padding(2.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "KnowToMigrate",
                style = MaterialTheme.typography.titleLarge,
                color = KmTextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Ready to migrate",
                style = MaterialTheme.typography.bodySmall,
                color = KmOrange
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = {}) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = KmTextSecondary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared files banner (appears when launched via Share Sheet)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SharedFilesBanner(uris: List<Uri>, navController: NavController) {
    KmGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Screen.Send.route) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Share, contentDescription = null, tint = KmOrange, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${uris.size} file(s) ready to send",
                style = MaterialTheme.typography.bodyMedium,
                color = KmTextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = KmOrange, modifier = Modifier.size(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recent transfer row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentTransferRow(transfer: RecentTransfer) {
    val statusColor = when (transfer.status) {
        "completed"   -> KmSuccess
        "in progress" -> KmOrange
        "failed"      -> KmError
        else          -> KmTextMuted
    }
    val dirIcon = if (transfer.direction == "sent") Icons.Default.Upload else Icons.Default.Download

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KmBlackCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(dirIcon, contentDescription = transfer.direction, tint = KmOrange, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = transfer.name, style = MaterialTheme.typography.bodyMedium, color = KmTextPrimary)
            Text(text = "${transfer.size} • ${transfer.device}", style = MaterialTheme.typography.bodySmall, color = KmTextMuted)
        }
        KmBadge(text = transfer.status, color = statusColor)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Migration CTA card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MigrationCtaCard(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(listOf(KmOrangeDark, KmOrange))
            )
            .clickable { navController.navigate(Screen.Migration.route) }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Full Device Migration", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = "Transfer everything at once", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
        Icon(Icons.Default.MoveDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom navigation bar
// ─────────────────────────────────────────────────────────────────────────────

private data class NavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun KmBottomBar(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Send", Icons.Default.Upload),
        NavItem("Receive", Icons.Default.Download),
        NavItem("History", Icons.Default.History),
        NavItem("Settings", Icons.Default.Settings),
    )
    NavigationBar(
        containerColor = KmBlackCard,
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = KmGlassBorder,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        )
    ) {
        items.forEachIndexed { idx, item ->
            NavigationBarItem(
                selected = selectedIndex == idx,
                onClick = { onSelect(idx) },
                icon = {
                    Icon(item.icon, contentDescription = item.label)
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = KmOrange,
                    selectedTextColor = KmOrange,
                    unselectedIconColor = KmTextMuted,
                    unselectedTextColor = KmTextMuted,
                    indicatorColor = KmOrangeGlow
                )
            )
        }
    }
}

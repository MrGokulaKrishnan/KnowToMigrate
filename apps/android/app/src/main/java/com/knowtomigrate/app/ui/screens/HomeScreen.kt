package com.knowtomigrate.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.knowtomigrate.app.data.TransferDirection
import com.knowtomigrate.app.data.TransferRecord
import com.knowtomigrate.app.data.TransferRecordStatus
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavController,
    sharedUris: List<Uri> = emptyList()
) {
    var selectedNavItem by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val discoveredDevices by manager.discoveredDevices.collectAsState()
    val transfers by manager.historyRepository.transfers.collectAsState()
    val recentTransfers = transfers.take(5)

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
                HomeHeader(deviceName = manager.localDeviceName)
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

            // Distinctive Discovery Radar with live discovered devices
            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Nearby Device Discovery",
                                style = MaterialTheme.typography.titleMedium,
                                color = KmTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiDevices.isEmpty()) "Scanning Wi-Fi & Local Network (54123)" else "${uiDevices.size} nearby device(s) online",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiDevices.isNotEmpty()) KmSuccess else KmTextMuted
                            )
                        }
                        KmBadge(
                            text = if (uiDevices.isNotEmpty()) "${uiDevices.size} Found" else "Scanning",
                            color = if (uiDevices.isNotEmpty()) KmSuccess else KmOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    KmDiscoveryRadar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        devices = uiDevices,
                        onDeviceClick = {
                            navController.navigate(Screen.Send.route)
                        }
                    )
                }
            }

            // Nearby devices list
            item {
                KmSectionHeader(
                    title = "Nearby Devices",
                    subtitle = if (uiDevices.isEmpty()) "Open KnowToMigrate on PC or other phone" else "${uiDevices.size} ready for migration"
                )
            }
            if (uiDevices.isEmpty()) {
                item {
                    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = KmTextDisabled,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No other devices detected on this network yet.\nMake sure KnowToMigrate is running on your Windows PC or other phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
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

            // Recent transfers section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KmSectionHeader(
                        title = "Recent Transfers",
                        subtitle = if (recentTransfers.isNotEmpty()) "${transfers.size} total" else null
                    )
                    if (transfers.isNotEmpty()) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium,
                            color = KmOrange,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { navController.navigate(Screen.History.route) }
                                .padding(4.dp)
                        )
                    }
                }
            }

            if (recentTransfers.isEmpty()) {
                item {
                    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = KmTextDisabled,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transfers yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = KmTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Send files to a nearby device or tap Receive to accept transfers from your PC or phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recentTransfers) { record ->
                    HomeTransferRow(record)
                }
            }

            // Full device migration CTA
            item {
                MigrationCtaCard(navController)
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun HomeHeader(deviceName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
                .border(1.dp, KmOrange, RoundedCornerShape(10.dp))
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.knowtomigrate.app.R.drawable.logo),
                contentDescription = "KnowToMigrate Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "KnowToMigrate",
                style = MaterialTheme.typography.titleLarge,
                color = KmTextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodySmall,
                color = KmOrange
            )
        }
    }
}

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

@Composable
private fun HomeTransferRow(record: TransferRecord) {
    val statusColor = when (record.status) {
        TransferRecordStatus.COMPLETED -> KmSuccess
        TransferRecordStatus.TRANSFERRING -> KmOrange
        TransferRecordStatus.CONNECTING,
        TransferRecordStatus.PREPARING,
        TransferRecordStatus.WAITING_ACCEPTANCE,
        TransferRecordStatus.VERIFYING -> KmInfo
        TransferRecordStatus.FAILED -> KmError
        TransferRecordStatus.CANCELLED -> KmTextMuted
    }

    val dirIcon = if (record.direction == TransferDirection.SENT) Icons.Default.Upload else Icons.Default.Download

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KmBlackCard)
            .border(1.dp, KmGlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KmOrangeGlow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = dirIcon,
                contentDescription = record.direction.name,
                tint = KmOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = KmTextPrimary,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${record.formattedSize} • ${record.peerDeviceName} • ${record.formattedDate}",
                style = MaterialTheme.typography.bodySmall,
                color = KmTextMuted,
                maxLines = 1
            )
        }
        KmBadge(text = record.status.displayName, color = statusColor)
    }
}

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
            Text(
                text = "Full Device Migration",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Transfer photos, videos, contacts and files in one go",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Icon(
            imageVector = Icons.Default.MoveDown,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

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

package com.knowtomigrate.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*

@Composable
fun SendScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<UiDevice?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val nearbyDevices = remember {
        listOf(
            UiDevice("1", "iPhone 15 Pro", "ios", "192.168.1.10", "online"),
            UiDevice("2", "MacBook Air M3", "macos", "192.168.1.11", "online"),
            UiDevice("3", "Pixel 8 Pro", "android", "192.168.1.12", "online"),
        )
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        selectedUris = uris
    }

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Send Files",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File picker section
            item {
                KmSectionHeader(title = "Select Files", subtitle = "Tap to browse your device")
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(KmGlassBackground)
                        .border(
                            width = 1.dp,
                            color = if (selectedUris.isEmpty()) KmGlassBorder else KmOrange.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { filePicker.launch(arrayOf("*/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUris.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Pick files",
                                tint = KmOrange,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to select files",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KmTextMuted
                            )
                            Text(
                                text = "Any file type supported",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextDisabled
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = KmSuccess,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${selectedUris.size} file(s) selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KmTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap to change selection",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted
                            )
                        }
                    }
                }
            }

            // Selected files list
            if (selectedUris.isNotEmpty()) {
                item {
                    KmSectionHeader(title = "Selected Files")
                }
                items(selectedUris) { uri ->
                    SelectedFileRow(uri = uri, onRemove = {
                        selectedUris = selectedUris.filter { it != uri }
                    })
                }
            }

            // Device selector
            item {
                KmSectionHeader(
                    title = "Choose Destination",
                    subtitle = "${nearbyDevices.size} devices nearby"
                )
            }

            items(nearbyDevices) { device ->
                val isSelected = selectedDevice?.id == device.id
                KmDeviceCard(
                    device = device,
                    onClick = { selectedDevice = if (isSelected) null else device },
                    modifier = Modifier.border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) KmOrange else KmGlassBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                )
            }

            // Send button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                val canSend = selectedUris.isNotEmpty() && selectedDevice != null && !isSending
                KmPrimaryButton(
                    text = if (isSending) "Sending…" else "Send ${if (selectedUris.isEmpty()) "" else "${selectedUris.size} File(s)"}",
                    onClick = {
                        if (canSend) {
                            isSending = true
                            // Navigate to transfer screen with a mock session ID
                            navController.navigate(Screen.Transfer.withSession("session_${System.currentTimeMillis()}"))
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SelectedFileRow(uri: Uri, onRemove: () -> Unit) {
    val displayName = uri.lastPathSegment ?: uri.toString().takeLast(40)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(KmBlackCard)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = KmOrange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = KmTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = KmTextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

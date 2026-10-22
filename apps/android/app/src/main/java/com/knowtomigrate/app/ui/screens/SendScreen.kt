package com.knowtomigrate.app.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*
import kotlinx.coroutines.launch

data class StagedItem(
    val id: String,
    val name: String,
    val sizeText: String,
    var isChecked: Boolean = true,
    val uri: Uri? = null
)

@Composable
fun SendScreen(navController: NavController, initialUris: List<Uri> = emptyList()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val discoveredDevices by manager.discoveredDevices.collectAsState()
    val targetDeviceName = discoveredDevices.firstOrNull()?.deviceName ?: "Windows PC"

    // Default prototype items + user-picked files
    var stagedItems by remember {
        mutableStateOf(
            listOf(
                StagedItem("1", "Photos_March.zip", "1.2 GB", true),
                StagedItem("2", "Lecture_notes/", "320 MB · folder", true),
                StagedItem("3", "Resume_2026.pdf", "0.4 MB", true)
            )
        )
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newItems = uris.map { uri ->
                var name = "Document"
                var size = ""
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "Document"
                        if (sizeIdx >= 0) {
                            val b = cursor.getLong(sizeIdx)
                            size = if (b >= 1024 * 1024) "${(b / (1024f * 1024f)).toInt()} MB" else "${(b / 1024f).toInt()} KB"
                        }
                    }
                }
                StagedItem(uri.toString(), name, size.ifBlank { "File" }, true, uri)
            }
            stagedItems = (newItems + stagedItems).distinctBy { it.name }
        }
    }

    // Include initialUris from Android ShareSheet if any
    LaunchedEffect(initialUris) {
        if (initialUris.isNotEmpty()) {
            val shared = initialUris.map { uri ->
                StagedItem(uri.toString(), "Shared file", "Ready", true, uri)
            }
            stagedItems = (shared + stagedItems).distinctBy { it.id }
        }
    }

    val selectedCount = stagedItems.count { it.isChecked }

    Scaffold(
        containerColor = KmBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Header: Back icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFF5F5F5)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F5F5)
                )
            }

            Text(
                text = "To $targetDeviceName",
                fontSize = 12.sp,
                color = Color(0xFF8C8C8C),
                modifier = Modifier.padding(start = 44.dp, bottom = 16.dp)
            )

            // Staged Items List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stagedItems, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x09FFFFFF))
                            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(14.dp))
                            .clickable {
                                stagedItems = stagedItems.map {
                                    if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KmCustomCheckbox(
                            isChecked = item.isChecked,
                            onCheckedChange = { checked ->
                                stagedItems = stagedItems.map {
                                    if (it.id == item.id) it.copy(isChecked = checked) else it
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF5F5F5)
                            )
                            Text(
                                text = item.sizeText,
                                fontSize = 12.sp,
                                color = Color(0xFF8C8C8C),
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }

                // Add more files action
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x05FFFFFF))
                            .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { filePicker.launch(arrayOf("*/*")) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Add files or folders",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF8A00)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Full-width gradient action button
            KmPrimaryButton(
                text = if (selectedCount > 0) "Send $selectedCount item${if (selectedCount > 1) "s" else ""}" else "Select items to send",
                enabled = selectedCount > 0,
                onClick = {
                    val realUris = stagedItems.filter { it.isChecked && it.uri != null }.mapNotNull { it.uri }
                    val target = discoveredDevices.firstOrNull()
                    if (target != null && realUris.isNotEmpty()) {
                        scope.launch {
                            manager.sendUris(target, realUris)
                        }
                    }
                    navController.navigate(Screen.Transfer.withSession("active"))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

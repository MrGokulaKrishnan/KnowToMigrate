package com.knowtomigrate.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.knowtomigrate.app.network.TransferProgressInfo
import com.knowtomigrate.app.network.TransferStatus
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.theme.*
import java.io.File

// ─── File-type helpers ────────────────────────────────────────────────────────

private fun mimeForFile(file: File): String {
    val ext = file.extension.lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
}

private fun iconForFile(file: File): ImageVector {
    val ext = file.extension.lowercase()
    return when {
        ext in listOf("jpg","jpeg","png","gif","webp","heic","bmp","svg") -> Icons.Default.Image
        ext in listOf("mp4","mkv","avi","mov","webm","m4v","3gp")        -> Icons.Default.VideoFile
        ext in listOf("mp3","aac","flac","ogg","m4a","wav")              -> Icons.Default.AudioFile
        ext in listOf("pdf")                                              -> Icons.Default.PictureAsPdf
        ext in listOf("doc","docx","odt")                                -> Icons.Default.Description
        ext in listOf("xls","xlsx","ods","csv")                          -> Icons.Default.GridOn
        ext in listOf("ppt","pptx","odp")                                -> Icons.Default.Slideshow
        ext in listOf("zip","rar","7z","tar","gz")                       -> Icons.Default.Archive
        ext in listOf("apk")                                             -> Icons.Default.Android
        else                                                              -> Icons.Default.InsertDriveFile
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024L          -> "%.0f KB".format(bytes / 1024.0)
        else                    -> "$bytes B"
    }
}

// ─── Premium Transfer-Complete Completion Card ────────────────────────────────

@Composable
private fun TransferCompleteCard(
    prog: TransferProgressInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(prog.finalizedFilePath) {
        if (prog.finalizedFilePath.isNotBlank()) File(prog.finalizedFilePath) else null
    }
    val fileName = prog.finalizedFileName.ifBlank { prog.currentFileName.ifBlank { "Received File" } }
    val fileSize = if (prog.totalBytes > 0) formatBytes(prog.totalBytes) else ""
    val senderName = prog.peerName.ifBlank { "Nearby Device" }
    var copiedFeedback by remember { mutableStateOf(false) }

    // Entry animation
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "card_scale"
    )
    // Checkmark ring pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .scale(scale)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF0D0D0D))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(28.dp))
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close button
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF666666), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Success ring + check
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                        // Outer pulse ring
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(KmSuccess.copy(alpha = pulseAlpha))
                        )
                        // Static ring
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF1A3A2A), Color(0xFF0D1F16))
                                    )
                                )
                                .border(2.dp, KmSuccess.copy(alpha = 0.6f), CircleShape)
                        )
                        // Checkmark icon
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = KmSuccess,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "File Received",
                        style = MaterialTheme.typography.headlineSmall,
                        color = KmTextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Verified · End-to-End Encrypted",
                        style = MaterialTheme.typography.bodySmall,
                        color = KmSuccess,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // File info card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF181818))
                            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // File type icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(KmOrangeGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (file != null) iconForFile(file) else Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = KmOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KmTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                                if (fileSize.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = fileSize,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KmTextMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metadata row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetadataChip(
                            icon = Icons.Default.Smartphone,
                            label = "From",
                            value = senderName,
                            modifier = Modifier.weight(1f)
                        )
                        MetadataChip(
                            icon = Icons.Default.Folder,
                            label = "Saved to",
                            value = "Downloads / KnowToMigrate",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Open file
                        if (file != null && file.exists()) {
                            Button(
                                onClick = {
                                    try {
                                        val mime = mimeForFile(file)
                                        val uri: Uri = if (Build.VERSION.SDK_INT >= 24) {
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                file
                                            )
                                        } else {
                                            Uri.fromFile(file)
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, mime)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        // Fallback: open file manager at location
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.fromFile(file.parentFile), "resource/folder")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try { context.startActivity(intent) } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(listOf(KmOrangeDark, KmOrangeLight)),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Text("Open", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        // Share
                        if (file != null && file.exists()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val mime = mimeForFile(file)
                                        val uri: Uri = if (Build.VERSION.SDK_INT >= 24) {
                                            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        } else {
                                            Uri.fromFile(file)
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = mime
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = KmTextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x20FFFFFF))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = KmTextSecondary, modifier = Modifier.size(16.dp))
                                    Text("Share", color = KmTextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161616))
            .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = KmOrange, modifier = Modifier.size(12.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = KmTextMuted, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, color = KmTextSecondary, fontWeight = FontWeight.Medium, maxLines = 2, fontSize = 11.sp)
        }
    }
}

// ─── Receive Screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(navController: NavController) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val preferences = manager.preferences

    val isDiscoverable by preferences.isDiscoverable.collectAsState()
    val localDeviceName by preferences.deviceName.collectAsState()
    val folderDisplay by preferences.receiveFolderDisplay.collectAsState()
    val serverProg by manager.serverProgress.collectAsState()

    var showIncomingRequest by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    var incomingSender by remember { mutableStateOf("") }
    var incomingPin by remember { mutableStateOf("") }
    var incomingTransport by remember { mutableStateOf("Wi-Fi LAN") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(localDeviceName) }

    // Track last shown completion session so we don't show the card again after dismiss
    var lastShownCompletionSession by remember { mutableStateOf("") }
    val showCompletionCard = serverProg.isCompleted &&
            serverProg.status == TransferStatus.COMPLETED &&
            serverProg.sessionId.isNotBlank() &&
            serverProg.sessionId != lastShownCompletionSession

    // Folder picker for Scoped Storage
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

    DisposableEffect(Unit) {
        manager.transferServer.onHandshakeReceived = { sender, pin, transport ->
            incomingSender = sender
            incomingPin = pin
            incomingTransport = transport
            showIncomingRequest = true
            true
        }
        onDispose {
            manager.transferServer.onHandshakeReceived = null
        }
    }

    // Premium completion card
    if (showCompletionCard) {
        TransferCompleteCard(
            prog = serverProg,
            onDismiss = { lastShownCompletionSession = serverProg.sessionId }
        )
    }

    Scaffold(
        containerColor = KmBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Incoming Transfers",
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
            // "Ready to Receive" status card
            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isDiscoverable) KmSuccess else KmTextMuted)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isDiscoverable) "Incoming Mode Ready" else "Visibility Hidden",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KmTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDiscoverable) "Pluto Engine Active · Local Network" else "Device not discoverable by nearby peers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDiscoverable) KmSuccess else KmTextMuted
                                )
                            }
                        }
                        Switch(
                            checked = isDiscoverable,
                            onCheckedChange = { preferences.setDiscoverable(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = KmOrange,
                                uncheckedThumbColor = KmTextMuted,
                                uncheckedTrackColor = KmBlackElevated
                            )
                        )
                    }
                }
            }

            // Live Incoming Progress Card (visible during transfer, hidden after completion card shown)
            if (serverProg.totalBytes > 0 && !showCompletionCard) {
                item {
                    KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (serverProg.isCompleted) "Transfer Completed" else "Receiving from ${serverProg.peerName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (serverProg.isCompleted) KmSuccess else KmTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                KmBadge(
                                    text = when {
                                        serverProg.isCompleted -> "Verified"
                                        serverProg.status == TransferStatus.VERIFYING -> "Verifying"
                                        else -> "Receiving"
                                    },
                                    color = if (serverProg.isCompleted) KmSuccess else KmOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "File: ${serverProg.currentFileName} (${serverProg.currentFileIndex}/${serverProg.totalFiles})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KmOrangeLight,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Speed: ${"%.1f".format(serverProg.speedMBps)} MB/s · ${serverProg.bytesTransferred / 1048576} MB / ${serverProg.totalBytes / 1048576} MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmTextMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (serverProg.percentage / 100f).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = KmOrange,
                                trackColor = KmBlackElevated
                            )
                        }
                    }
                }
            }

            // Device Identification Card
            item {
                KmSectionHeader(
                    title = "Your Device Identity",
                    subtitle = "Visible to nearby senders on Wi-Fi and Wi-Fi Direct"
                )
            }

            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(KmBlackElevated)
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(listOf(KmOrange, KmOrangeDark)),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = KmOrange,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "PLUTO DIRECT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KmTextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = localDeviceName,
                                style = MaterialTheme.typography.titleMedium,
                                color = KmTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    editedName = localDeviceName
                                    showEditNameDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = KmOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "Unique ID: ${manager.localDeviceId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = KmTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "End-to-End Encrypted (AES-256-GCM)",
                            style = MaterialTheme.typography.labelSmall,
                            color = KmSuccess
                        )
                    }
                }
            }

            // Save Destination Directory Card
            item {
                KmSectionHeader(
                    title = "Save Destination",
                    subtitle = "Where incoming files are stored on this device"
                )
            }

            item {
                KmGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KmOrangeGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = KmOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = folderDisplay,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KmTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Auto-indexed to Gallery & Files app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KmTextMuted
                                )
                            }
                        }
                        KmSecondaryButton(
                            text = "Change",
                            onClick = { folderPicker.launch(null) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Incoming transfer request bottom sheet
        if (showIncomingRequest) {
            ModalBottomSheet(
                onDismissRequest = { showIncomingRequest = false },
                sheetState = sheetState,
                containerColor = KmBlackCard,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(KmGlassBorder)
                    )
                }
            ) {
                IncomingRequestSheet(
                    device = UiDevice("remote", incomingSender.ifBlank { "Nearby Device" }, "remote", "", "online"),
                    pin = incomingPin,
                    transport = incomingTransport,
                    files = listOf("Incoming Encrypted Transfer"),
                    onAccept = { showIncomingRequest = false },
                    onDecline = { showIncomingRequest = false }
                )
            }
        }

        // Edit Device Name Dialog
        if (showEditNameDialog) {
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = {
                    Text(
                        text = "Edit Device Name",
                        color = KmTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Nearby devices will identify this phone by this name.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KmTextMuted
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
private fun IncomingRequestSheet(
    device: UiDevice,
    pin: String,
    transport: String,
    files: List<String>,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Incoming Transfer Request",
            style = MaterialTheme.typography.headlineSmall,
            color = KmTextPrimary,
            fontWeight = FontWeight.Bold
        )
        KmGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Smartphone, contentDescription = null, tint = KmOrange, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = device.name, style = MaterialTheme.typography.titleMedium, color = KmTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(text = "Negotiated Transport: $transport", style = MaterialTheme.typography.bodySmall, color = KmOrange)
                }
                KmTransportPill(label = transport, isBest = true, color = KmOrange)
            }
        }
        KmGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Authentication PIN", style = MaterialTheme.typography.labelMedium, color = KmTextSecondary)
                    Text(text = "Verify matching code on sender", style = MaterialTheme.typography.bodySmall, color = KmTextMuted)
                }
                Text(
                    text = pin.ifBlank { "000000" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = KmOrange
                )
            }
        }
        Text(
            text = "Files to receive:",
            style = MaterialTheme.typography.labelMedium,
            color = KmTextSecondary
        )
        files.forEach { file ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = KmTextMuted, modifier = Modifier.size(16.dp))
                Text(text = file, style = MaterialTheme.typography.bodySmall, color = KmTextSecondary)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KmSecondaryButton(text = "Decline", onClick = onDecline, modifier = Modifier.weight(1f))
            KmPrimaryButton(text = "Accept", onClick = onAccept, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

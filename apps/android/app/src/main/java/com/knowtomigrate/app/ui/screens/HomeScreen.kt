package com.knowtomigrate.app.ui.screens

/*
 * KnowToMigrate - Home Screen
 * Updated: 25 October 2026
 * Pure AMOLED #000000 base with KM Electric Orange accents and Dark Liquid Glass
 * Information Architecture:
 * - Top Bar: App Mark, App Name, Status Line ("On Wi-Fi · Local network"), History & Settings icons
 * - Compact Nearby Card: Radar + "Looking for devices" or "N devices nearby"
 * - Equal-width Dual Actions: Send files (Single Primary action) and Receive (Secondary glass)
 * - Nearby devices: Discovered devices or guidance empty state
 * - Recent: Last 3 transfers grouped with verified badge
 * - Collapsed Clipboard row: Opens bottom sheet with text field, Paste, and Send to PC
 * - Move device: Secondary glass action row
 * - Bottom Bar: 3 items (Home, Send, Receive)
 */

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    sharedUris: List<Uri> = emptyList()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val discoveredDevices by manager.discoveredDevices.collectAsState()
    val allHistory by manager.historyRepository.transfers.collectAsState()

    var showClipboardSheet by remember { mutableStateOf(false) }
    var clipboardText by remember { mutableStateOf("") }
    var clipboardStatusMsg by remember { mutableStateOf<String?>(null) }
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    Scaffold(
        containerColor = KmBlack,
        bottomBar = {
            KmFloatingBottomBar(
                selectedIndex = 0,
                onSelect = { idx ->
                    when (idx) {
                        0 -> { /* already on Home */ }
                        1 -> navController.navigate(Screen.Send.route)
                        2 -> navController.navigate(Screen.Receive.route)
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Header: Monogram, Name, Status line, History & Settings icons ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // KM Monogram (40x40 dp)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(KmRadii.Button))
                            .background(KmBrandGradient)
                            .drawBehind {
                                drawLine(
                                    color = Color(0x73FFFFFF),
                                    start = Offset(0f, 1.dp.toPx()),
                                    end = Offset(size.width, 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "KM",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = KmTextOnGradient
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = KmTextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(KmOnline)
                            )
                            Text(
                                text = stringResource(R.string.status_network),
                                fontSize = 12.sp,
                                color = KmTextMuted
                            )
                        }
                    }

                    // History icon button (48dp touch target)
                    IconButton(
                        onClick = { navController.navigate(Screen.History.route) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.nav_history),
                            tint = KmTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Settings icon button (48dp touch target)
                    IconButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                            tint = KmTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Optional ShareSheet quick banner
            if (sharedUris.isNotEmpty()) {
                item {
                    KmGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.Send.route) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = KmOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${sharedUris.size} file(s) ready to send",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = KmTextPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = KmOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // ── 2. Compact Nearby Card with Radar (64dp) ──────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KmRadii.Card))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Card))
                        .drawBehind {
                            drawLine(
                                color = KmGlassTopHighlight,
                                start = Offset(18.dp.toPx(), 1.dp.toPx()),
                                end = Offset(size.width - 18.dp.toPx(), 1.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        KmRadarWidget(size = 64.dp)

                        Column(modifier = Modifier.weight(1f)) {
                            val count = discoveredDevices.size
                            Text(
                                text = if (count == 0) stringResource(R.string.status_looking_for_devices) else "$count device${if (count > 1) "s" else ""} nearby",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KmTextPrimary
                            )
                            Text(
                                text = if (count == 0) "Make sure both devices are on the same Wi-Fi." else "Ready for secure peer transfer.",
                                fontSize = 12.sp,
                                color = KmTextMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // ── 3. Dual Equal-Width Action Buttons: Send (Primary) & Receive ─────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Send files button (ONLY primary action on screen: KM Gradient, #0A0A0A text)
                    val sendInteraction = remember { MutableInteractionSource() }
                    val sendPressed by sendInteraction.collectIsPressedAsState()
                    val sendScale by animateFloatAsState(
                        targetValue = if (sendPressed) 0.97f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                        label = "send_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(sendScale)
                            .defaultMinSize(minHeight = 84.dp)
                            .clip(RoundedCornerShape(KmRadii.ListRow))
                            .background(KmBrandGradient)
                            .drawBehind {
                                drawLine(
                                    color = Color(0x73FFFFFF),
                                    start = Offset(14.dp.toPx(), 1.dp.toPx()),
                                    end = Offset(size.width - 14.dp.toPx(), 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable(
                                interactionSource = sendInteraction,
                                indication = null
                            ) { navController.navigate(Screen.Send.route) }
                            .padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = KmTextOnGradient,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_send_files),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KmTextOnGradient
                            )
                            Text(
                                text = "Photos, videos, files",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = KmTextOnGradient.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Receive button (Secondary action: Liquid Glass surface)
                    val recvInteraction = remember { MutableInteractionSource() }
                    val recvPressed by recvInteraction.collectIsPressedAsState()
                    val recvScale by animateFloatAsState(
                        targetValue = if (recvPressed) 0.97f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                        label = "recv_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(recvScale)
                            .defaultMinSize(minHeight = 84.dp)
                            .clip(RoundedCornerShape(KmRadii.ListRow))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.ListRow))
                            .drawBehind {
                                drawLine(
                                    color = KmGlassTopHighlight,
                                    start = Offset(14.dp.toPx(), 1.dp.toPx()),
                                    end = Offset(size.width - 14.dp.toPx(), 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable(
                                interactionSource = recvInteraction,
                                indication = null
                            ) { navController.navigate(Screen.Receive.route) }
                            .padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = KmTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_receive_files),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KmTextPrimary
                            )
                            Text(
                                text = "Wait for a PC or phone",
                                fontSize = 11.sp,
                                color = KmTextMuted
                            )
                        }
                    }
                }
            }

            // ── 4. Section: Nearby devices ─────────────────────────────────────────
            item {
                Text(
                    text = stringResource(R.string.section_nearby_devices),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KmTextPrimary,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                )
            }

            if (discoveredDevices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(KmRadii.ListRow))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.ListRow))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.empty_nearby_devices),
                            fontSize = 13.sp,
                            color = KmTextMuted
                        )
                    }
                }
            } else {
                items(discoveredDevices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(KmRadii.ListRow))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.ListRow))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(KmRadii.Button))
                                .background(Color(0x0FFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (device.platform.contains("win", ignoreCase = true)) Icons.Default.Computer else Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = KmTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.deviceName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KmTextPrimary
                            )
                            Text(
                                text = "On Wi-Fi · Ready to receive",
                                fontSize = 12.sp,
                                color = KmTextMuted,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        // Send CTA
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, KmGlassBorder, RoundedCornerShape(10.dp))
                                .clickable { navController.navigate(Screen.Send.route) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_send),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KmTextPrimary
                            )
                        }
                    }
                }
            }

            // ── 5. Section: Recent Transfers (Genuine Dynamic History) ─────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp, top = 4.dp, end = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.section_recent),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KmTextPrimary
                    )
                    if (allHistory.isNotEmpty()) {
                        Text(
                            text = "See all",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = KmOrange,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { navController.navigate(Screen.History.route) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (allHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(KmRadii.ListRow))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.ListRow))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.empty_recent_transfers),
                            fontSize = 13.sp,
                            color = KmTextMuted
                        )
                    }
                }
            } else {
                val recentItems = allHistory.take(3)
                items(recentItems, key = { it.id }) { record ->
                    val isComp = record.status == com.knowtomigrate.app.data.TransferRecordStatus.COMPLETED
                    val isOut = record.direction == com.knowtomigrate.app.data.TransferDirection.SENT
                    val formattedTime = formatRecentTimestamp(record.timestamp)
                    val isFolder = record.fileCount > 1 || record.fileName.endsWith("/")
                    KmRecentTransferRow(
                        filename = record.fileName,
                        subtitle = "${record.formattedSize} · ${if (isOut) "Sent" else "Received"} · $formattedTime",
                        isVerified = isComp,
                        isFolder = isFolder,
                        onClick = { navController.navigate(Screen.History.route) }
                    )
                }
            }

            // ── 6. Collapsed Clipboard Row (Tapping opens Bottom Sheet) ───────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KmRadii.ListRow))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.ListRow))
                        .clickable { showClipboardSheet = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(KmRadii.Button))
                                .background(Color(0x0FFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = KmTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.nav_clipboard),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KmTextPrimary
                            )
                            Text(
                                text = "Share text with your PC",
                                fontSize = 12.sp,
                                color = KmTextMuted,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open",
                            tint = KmTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── 7. Secondary Action: Move device ──────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KmRadii.ListRow))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.ListRow))
                        .clickable { navController.navigate(Screen.Migration.route) }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(KmRadii.Button))
                                .background(Color(0x0FFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = KmOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.move_device_title),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KmTextPrimary
                            )
                            Text(
                                text = stringResource(R.string.move_device_caption),
                                fontSize = 12.sp,
                                color = KmTextMuted,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open",
                            tint = KmTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Clipboard Modal Bottom Sheet ──────────────────────────────────────────
    if (showClipboardSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClipboardSheet = false },
            containerColor = KmBlackCard,
            dragHandle = { BottomSheetDefaults.DragHandle(color = KmGlassBorder) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.nav_clipboard),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = KmTextPrimary
                )

                Text(
                    text = "Text is transferred directly over your local Wi-Fi.",
                    fontSize = 12.sp,
                    color = KmTextMuted
                )

                OutlinedTextField(
                    value = clipboardText,
                    onValueChange = {
                        clipboardText = it
                        clipboardStatusMsg = null
                    },
                    placeholder = {
                        Text(
                            text = "Paste or type text to share…",
                            fontSize = 13.sp,
                            color = KmTextDisabled
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(KmRadii.Input),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = KmGlassBackground,
                        unfocusedContainerColor = KmGlassBackground,
                        focusedBorderColor = KmOrange,
                        unfocusedBorderColor = KmGlassBorder,
                        focusedTextColor = KmTextPrimary,
                        unfocusedTextColor = KmTextPrimary
                    )
                )

                if (clipboardStatusMsg != null) {
                    Text(
                        text = clipboardStatusMsg ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (clipboardStatusMsg?.contains("Sent", ignoreCase = true) == true) KmOnline else KmWarning
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Paste action (Secondary glass button)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(KmRadii.Button))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Button))
                            .clickable {
                                val clip = clipboardManager?.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        clipboardText = text
                                        clipboardStatusMsg = "Pasted from phone clipboard"
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.action_paste),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KmTextPrimary
                        )
                    }

                    // Send to PC action (Gradient button with #0A0A0A text)
                    val canSend = clipboardText.isNotBlank()
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(KmRadii.Button))
                            .background(
                                if (canSend) KmBrandGradient else SolidColor(Color(0x1AFFFFFF))
                            )
                            .clickable(enabled = canSend) {
                                val target = discoveredDevices.firstOrNull()
                                if (target != null) {
                                    scope.launch {
                                        val ok = manager.sendClipboardText(target.ipAddress, clipboardText)
                                        clipboardStatusMsg = if (ok) "Sent to PC clipboard!" else "Could not connect to PC."
                                    }
                                } else {
                                    clipboardStatusMsg = "No PC found on Wi-Fi."
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.action_send_to_pc),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canSend) KmTextOnGradient else KmTextDisabled
                        )
                    }
                }
            }
        }
    }
}
private fun formatRecentTimestamp(timestamp: Long): String {
    val now = Calendar.getInstance()
    val todayYear = now.get(Calendar.YEAR)
    val todayDay = now.get(Calendar.DAY_OF_YEAR)

    val itemCal = Calendar.getInstance()
    itemCal.timeInMillis = timestamp

    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormatter.format(Date(timestamp))

    return when {
        itemCal.get(Calendar.YEAR) == todayYear && itemCal.get(Calendar.DAY_OF_YEAR) == todayDay -> timeStr
        itemCal.get(Calendar.YEAR) == todayYear && itemCal.get(Calendar.DAY_OF_YEAR) == todayDay - 1 -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

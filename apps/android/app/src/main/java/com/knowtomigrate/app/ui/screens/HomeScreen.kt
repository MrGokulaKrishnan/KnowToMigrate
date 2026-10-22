package com.knowtomigrate.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    sharedUris: List<Uri> = emptyList()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val discoveredDevices by manager.discoveredDevices.collectAsState()

    var isClipboardExpanded by remember { mutableStateOf(false) }
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
                        0 -> { /* already home */ }
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
            // 1. Top row: Monogram, App Name, Status line, and Ledger/Control action icons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "KM" Monogram box (40x40 dp)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(KmOrangeDark, KmOrange, KmOrangeLight)
                                )
                            )
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
                            text = "KnowToMigrate",
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
                                    .background(KmSuccess)
                            )
                            Text(
                                text = "On Wi-Fi · Local network",
                                fontSize = 12.sp,
                                color = KmTextMuted
                            )
                        }
                    }

                    // Ledger (History) icon button
                    IconButton(
                        onClick = { navController.navigate(Screen.History.route) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Migration Ledger",
                            tint = KmTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Control (Settings) icon button
                    IconButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Control Center",
                            tint = KmTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Optional Android ShareSheet banner
            if (sharedUris.isNotEmpty()) {
                item {
                    KmGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.Send.route) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = KmOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${sharedUris.size} file(s) ready to send",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = KmTextPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = KmOrange, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // 2. Compact Radar Nearby Card (64 dp height)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(18.dp))
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
                                text = if (count == 0) "Looking for devices…" else "$count device${if (count > 1) "s" else ""} nearby",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KmTextPrimary
                            )
                            Text(
                                text = "Open KnowToMigrate on your PC to see it here.",
                                fontSize = 12.sp,
                                color = KmTextMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // 3. Eyebrow: Start
            item {
                Text(
                    text = "START",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.08.sp,
                    color = KmTextMuted,
                    modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                )
            }

            // 4. Equal-width Action Tiles: Send files (primary gradient) & Receive (glass)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Send files button (Gradient, Primary, Near-black text)
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.97f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                        label = "send_tile_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .defaultMinSize(minHeight = 88.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(KmOrangeDark, KmOrange, KmOrangeLight)
                                )
                            )
                            .drawBehind {
                                drawLine(
                                    color = Color(0x73FFFFFF),
                                    start = Offset(14.dp.toPx(), 1.dp.toPx()),
                                    end = Offset(size.width - 14.dp.toPx(), 1.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { navController.navigate(Screen.Send.route) }
                            .padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = KmTextOnGradient,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Send files",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KmTextOnGradient
                            )
                            Text(
                                text = "Photos, videos, documents",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = KmTextOnGradient.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Receive button (Glass surface)
                    val recvInteraction = remember { MutableInteractionSource() }
                    val recvPressed by recvInteraction.collectIsPressedAsState()
                    val recvScale by animateFloatAsState(
                        targetValue = if (recvPressed) 0.97f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                        label = "recv_tile_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(recvScale)
                            .defaultMinSize(minHeight = 88.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(14.dp))
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
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = KmTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Receive",
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

            // 5. Eyebrow: Devices
            item {
                Text(
                    text = "DEVICES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.08.sp,
                    color = KmTextMuted,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                )
            }

            if (discoveredDevices.isEmpty()) {
                // Empty state: "No devices yet" + "Make sure both devices are on the same Wi-Fi."
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "No devices yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = KmTextPrimary
                            )
                            Text(
                                text = "Make sure both devices are on the same Wi-Fi.",
                                fontSize = 12.sp,
                                color = KmTextMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                items(discoveredDevices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(KmGlassBackground)
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                                text = "Trusted · Ready to receive",
                                fontSize = 12.sp,
                                color = KmTextMuted,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        // Send action button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x0FFFFFFF))
                                .border(1.dp, KmGlassBorder, RoundedCornerShape(10.dp))
                                .clickable { navController.navigate(Screen.Send.route) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Send",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KmTextPrimary
                            )
                        }
                    }
                }
            }

            // 6. Eyebrow: Clipboard sync
            item {
                Text(
                    text = "CLIPBOARD SYNC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.08.sp,
                    color = KmTextMuted,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                )
            }

            // Collapsed by default "Share text with your PC" disclosure card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isClipboardExpanded = !isClipboardExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
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
                                    text = "Share text with your PC",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KmTextPrimary
                                )
                                Text(
                                    text = "Text only, sent over your Wi-Fi",
                                    fontSize = 12.sp,
                                    color = KmTextMuted,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }

                            Text(
                                text = if (isClipboardExpanded) "Hide" else "Show",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = KmTextMuted,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isClipboardExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Text(
                                    text = "Use this only on a network you trust.",
                                    fontSize = 12.sp,
                                    color = KmTextMuted,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = clipboardText,
                                    onValueChange = {
                                        clipboardText = it
                                        clipboardStatusMsg = null
                                    },
                                    placeholder = {
                                        Text(
                                            text = "Paste or type text",
                                            fontSize = 13.sp,
                                            color = KmTextDisabled
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(84.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0x0AFFFFFF),
                                        unfocusedContainerColor = Color(0x0AFFFFFF),
                                        focusedBorderColor = KmOrange,
                                        unfocusedBorderColor = KmGlassBorder,
                                        focusedTextColor = KmTextPrimary,
                                        unfocusedTextColor = KmTextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Paste button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x0AFFFFFF))
                                            .border(1.dp, KmGlassBorder, RoundedCornerShape(12.dp))
                                            .clickable {
                                                val clip = clipboardManager?.primaryClip
                                                if (clip != null && clip.itemCount > 0) {
                                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                                    clipboardText = text
                                                    clipboardStatusMsg = "Pasted from this phone"
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Paste",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = KmTextPrimary
                                        )
                                    }

                                    // Send to PC button
                                    val canSend = clipboardText.isNotBlank()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (canSend) {
                                                    Brush.linearGradient(
                                                        listOf(KmOrangeDark, KmOrange, KmOrangeLight)
                                                    )
                                                } else {
                                                    SolidColor(Color(0x1AFFFFFF))
                                                }
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
                                            text = "Send to PC",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (canSend) KmTextOnGradient else KmTextDisabled
                                        )
                                    }
                                }

                                if (clipboardStatusMsg != null) {
                                    Text(
                                        text = clipboardStatusMsg!!,
                                        fontSize = 12.sp,
                                        color = if (clipboardStatusMsg!!.startsWith("Sent")) KmSuccess else KmWarning,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Full device migration row (Secondary action, NOT gradient!)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(16.dp))
                        .drawBehind {
                            drawLine(
                                color = KmGlassTopHighlight,
                                start = Offset(16.dp.toPx(), 1.dp.toPx()),
                                end = Offset(size.width - 16.dp.toPx(), 1.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .clickable { navController.navigate(Screen.Migration.route) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0FFFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = KmTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Full device migration",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KmTextPrimary
                        )
                        Text(
                            text = "Photos, contacts and files in one plan",
                            fontSize = 12.sp,
                            color = KmTextMuted,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

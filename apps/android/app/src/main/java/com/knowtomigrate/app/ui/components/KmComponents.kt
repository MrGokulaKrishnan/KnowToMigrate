package com.knowtomigrate.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.knowtomigrate.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Data model used across device-related components
// ─────────────────────────────────────────────────────────────────────────────

data class UiDevice(
    val id: String,
    val name: String,
    val platform: String,        // "android" | "ios" | "macos" | "windows" | "linux"
    val ip: String = "",
    val status: String = "online", // "online" | "offline" | "transferring"
    val supportedTransports: List<String> = listOf("WIFI_LAN", "WIFI_DIRECT", "BLUETOOTH"),
    val bestTransport: String = "WIFI_LAN",
    val isWifiLanReachable: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// 1. KmPrimaryButton — orange gradient glossy button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = KmTextDisabled
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .height(52.dp)
            .drawBehind {
                if (enabled) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(KmOrangeLight, KmOrange, KmOrangeDark),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    )
                    // Top gloss highlight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x33FFFFFF), Color.Transparent),
                            endY = size.height / 2f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    )
                } else {
                    drawRoundRect(
                        color = KmBlackElevated,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    )
                }
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. KmSecondaryButton — glass button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KmGlassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = KmGlassBackground,
            contentColor = KmTextPrimary
        ),
        modifier = modifier.height(52.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. KmGlassCard — frosted glass container card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(KmGlassBackground)
            .border(1.dp, KmGlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. KmDeviceCard — device entry with platform icon, name, transport badges and status
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmTransportPill(
    label: String,
    isBest: Boolean = false,
    color: Color = KmOrange,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isBest) color.copy(alpha = 0.22f) else KmGlassBackground)
            .border(
                width = 1.dp,
                color = if (isBest) color else KmGlassBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isBest) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = if (isBest) "$label [Best]" else label,
                fontSize = 10.sp,
                fontWeight = if (isBest) FontWeight.Bold else FontWeight.Medium,
                color = if (isBest) color else KmTextSecondary
            )
        }
    }
}

@Composable
fun KmTransportCard(
    type: String, // "WIFI_LAN", "WIFI_DIRECT", "BLUETOOTH"
    title: String,
    description: String,
    statusText: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isBt = type.equals("BLUETOOTH", ignoreCase = true)
    val accentColor = if (isBt) Color(0xFF60A5FA) else KmOrange
    val iconBgColor = if (isBt) Color(0xFF0F172A) else KmOrangeGlow

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KmBlackElevated)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else KmGlassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact 1:1 proportioned icon box (36x36 dp)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBgColor)
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isBt -> Icons.Default.Bluetooth
                    type.contains("DIRECT", ignoreCase = true) -> Icons.Default.WifiTethering
                    else -> Icons.Default.Wifi
                },
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = KmTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = KmTextMuted,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Status pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun KmCompletionNotificationBanner(
    title: String,
    fileName: String,
    sizeText: String,
    peerInfo: String,
    isSuccess: Boolean = true,
    onDismiss: () -> Unit,
    onViewDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bannerColor = if (isSuccess) KmSuccess else KmError
    val bannerBg = if (isSuccess) Color(0xFF071C0F) else Color(0xFF230909)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bannerBg)
            .border(1.dp, bannerColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bannerColor.copy(alpha = 0.2f))
                    .border(1.dp, bannerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = bannerColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = KmTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KmTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "$sizeText · $peerInfo",
                    style = MaterialTheme.typography.bodySmall,
                    color = KmTextSecondary,
                    fontSize = 12.sp
                )
            }

            if (onViewDetails != null) {
                TextButton(onClick = onViewDetails) {
                    Text("Details", color = bannerColor, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = KmTextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun KmDeviceCard(
    device: UiDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KmBlackElevated)
            .border(1.dp, KmGlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Platform icon background
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KmOrangeGlow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = device.platform,
                tint = KmOrange,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleSmall,
                color = KmTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = device.ip.ifBlank { device.platform.replaceFirstChar { it.uppercase() } },
                style = MaterialTheme.typography.bodySmall,
                color = KmTextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Capability check transport badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (device.supportedTransports.contains("WIFI_LAN")) {
                    KmTransportPill(
                        label = "Wi-Fi",
                        isBest = device.bestTransport == "WIFI_LAN",
                        color = KmOrange
                    )
                }
                if (device.supportedTransports.contains("WIFI_DIRECT")) {
                    KmTransportPill(
                        label = "Direct",
                        isBest = device.bestTransport == "WIFI_DIRECT",
                        color = KmInfo
                    )
                }
                if (device.supportedTransports.contains("BLUETOOTH")) {
                    KmTransportPill(
                        label = "BT",
                        isBest = device.bestTransport == "BLUETOOTH",
                        color = Color(0xFF60A5FA)
                    )
                }
            }
        }
        KmStatusDot(status = device.status)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. KmProgressBar — animated orange gradient glow progress bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(KmBlackElevated)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clampedProgress)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(KmOrangeDark, KmOrange, KmOrangeLight)
                    )
                )
                .drawBehind {
                    // Glow effect
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(KmOrangeGlow, Color.Transparent)
                        ),
                        topLeft = Offset(0f, -4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx())
                    )
                }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. KmBadge — colored status badge pill
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmBadge(
    text: String,
    color: Color = KmSuccess,
    modifier: Modifier = Modifier
) {
    val bgColor = color.copy(alpha = 0.15f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. KmDiscoveryRadar — premium animated discovery radar with dynamic device nodes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmDiscoveryRadar(
    modifier: Modifier = Modifier,
    devices: List<UiDevice> = emptyList(),
    onDeviceClick: ((UiDevice) -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_core")

    val ring1 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "ring1"
    )
    val ring2 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, 800, easing = LinearEasing)),
        label = "ring2"
    )
    val ring3 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, 1600, easing = LinearEasing)),
        label = "ring3"
    )
    val sweepAngle = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "sweep"
    )
    val corePulse = infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "corePulse"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxRadius = minOf(cx, cy) * 0.92f

            // Static concentric grid rings
            drawCircle(color = KmGlassBorder, radius = maxRadius * 0.35f, center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))
            drawCircle(color = KmGlassBorder.copy(alpha = 0.5f), radius = maxRadius * 0.65f, center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))
            drawCircle(color = KmGlassBorder.copy(alpha = 0.3f), radius = maxRadius, center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))

            // Cross-hair grid lines (subtle)
            drawLine(
                color = KmGlassBorder.copy(alpha = 0.25f),
                start = Offset(cx - maxRadius, cy),
                end = Offset(cx + maxRadius, cy),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = KmGlassBorder.copy(alpha = 0.25f),
                start = Offset(cx, cy - maxRadius),
                end = Offset(cx, cy + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // Animated expanding rings
            fun drawPulseRing(progress: Float) {
                val radius = maxRadius * progress
                if (radius <= 2f) return
                val alpha = (1f - progress).coerceIn(0f, 1f)
                drawCircle(
                    color = KmOrange.copy(alpha = alpha * 0.45f),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(KmOrange.copy(alpha = alpha * 0.12f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(cx, cy)
                )
            }

            drawPulseRing(ring1.value)
            drawPulseRing(ring2.value)
            drawPulseRing(ring3.value)

            // Radar sweep line & trail
            val rad = Math.toRadians(sweepAngle.value.toDouble())
            val sweepX = cx + (maxRadius * kotlin.math.cos(rad)).toFloat()
            val sweepY = cy + (maxRadius * kotlin.math.sin(rad)).toFloat()
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(KmOrange, KmOrange.copy(alpha = 0.1f)),
                    start = Offset(cx, cy),
                    end = Offset(sweepX, sweepY)
                ),
                start = Offset(cx, cy),
                end = Offset(sweepX, sweepY),
                strokeWidth = 2.dp.toPx()
            )

            // Central Energy Node Core
            val coreGlowRadius = 28.dp.toPx() * corePulse.value
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(KmOrange.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = coreGlowRadius
                ),
                radius = coreGlowRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(KmOrangeLight, KmOrange, KmOrangeDark),
                    start = Offset(cx - 14.dp.toPx(), cy - 14.dp.toPx()),
                    end = Offset(cx + 14.dp.toPx(), cy + 14.dp.toPx())
                ),
                radius = 12.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(cx, cy)
            )
        }

        // Overlay dynamic satellite device nodes
        if (devices.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cx = maxWidth / 2
                val cy = maxHeight / 2
                val orbitRadius = minOf(maxWidth, maxHeight) * 0.32f

                devices.take(6).forEachIndexed { idx, dev ->
                    val angleDeg = (360f / devices.size.coerceAtMost(6)) * idx - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val offsetX = cx + (orbitRadius.value * kotlin.math.cos(angleRad)).dp - 24.dp
                    val offsetY = cy + (orbitRadius.value * kotlin.math.sin(angleRad)).dp - 24.dp

                    val isWindows = dev.platform.contains("win", ignoreCase = true)
                    val icon = if (isWindows) Icons.Default.Devices else Icons.Default.Smartphone

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(KmBlackElevated)
                            .border(1.5.dp, KmOrange, CircleShape)
                            .clickable { onDeviceClick?.invoke(dev) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = dev.name,
                                tint = KmOrangeLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. KmTransferAnimation — animated lightning bolt between two device icons
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmTransferAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "transfer")
    val boltAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "boltAlpha"
    )
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "particle"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Source device icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KmBlackElevated)
                .border(1.dp, KmGlassBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = "Source device",
                tint = KmTextSecondary,
                modifier = Modifier.size(32.dp)
            )
        }

        // Lightning bolt path canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                // Draw data particle dot along the line
                val particleX = w * particleProgress
                drawCircle(
                    color = KmOrange,
                    radius = 4.dp.toPx(),
                    center = Offset(particleX, midY)
                )
                // Glow behind particle
                drawCircle(
                    color = KmOrange.copy(alpha = 0.3f),
                    radius = 10.dp.toPx(),
                    center = Offset(particleX, midY)
                )

                // Zigzag lightning bolt
                val path = Path().apply {
                    moveTo(0f, midY)
                    lineTo(w * 0.35f, midY - 12.dp.toPx())
                    lineTo(w * 0.5f, midY + 8.dp.toPx())
                    lineTo(w * 0.65f, midY - 12.dp.toPx())
                    lineTo(w, midY)
                }
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            KmOrangeDark.copy(alpha = boltAlpha * 0.6f),
                            KmOrange.copy(alpha = boltAlpha),
                            KmOrangeLight.copy(alpha = boltAlpha * 0.6f)
                        )
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // Target device icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KmBlackElevated)
                .border(1.dp, KmOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = "Target device",
                tint = KmOrange,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. KmSectionHeader — title + optional subtitle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = KmTextPrimary,
            fontWeight = FontWeight.Bold
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = KmTextMuted
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. KmStatusDot — animated colored indicator dot
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmStatusDot(
    status: String,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        "online"      -> KmSuccess
        "transferring" -> KmOrange
        else          -> KmTextDisabled
    }

    val infiniteTransition = rememberInfiniteTransition(label = "statusDot")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = if (status == "transferring")
            infiniteRepeatable(tween(700), RepeatMode.Reverse)
        else
            infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = modifier.size(10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring (for active states)
        if (status != "offline") {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.3f))
            )
        }
        // Solid dot
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

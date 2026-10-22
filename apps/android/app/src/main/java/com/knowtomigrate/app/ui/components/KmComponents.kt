package com.knowtomigrate.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.text.style.TextAlign
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
// 1. KmPrimaryButton — orange gradient glossy button with spring micro-interaction
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.965f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "btn_press_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = KmTextDisabled
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 52.dp)
            .drawBehind {
                if (enabled) {
                    // Soft orange outer glow
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(KmOrangeGlowStrong, Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width * 0.6f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    )
                    // Full spectrum brand gradient: #FF4D00 -> #FF6500 -> #FF8A00
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(KmOrangeDark, KmOrangeMid, KmOrangeLight),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    )
                    // Specular glossy top sheen
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x45FFFFFF), Color(0x10FFFFFF), Color.Transparent),
                            endY = size.height * 0.58f
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
// 2. KmSecondaryButton — glass button with spring micro-interaction
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "btn_sec_scale"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) KmGlassBorder else KmGlassBorder.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = KmGlassSurfaceSecondary,
            contentColor = if (enabled) KmTextPrimary else KmTextDisabled,
            disabledContainerColor = KmGlassSurfaceSecondary.copy(alpha = 0.5f),
            disabledContentColor = KmTextDisabled
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 52.dp)
            .drawBehind {
                // Subtle top gloss highlight
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0x22FFFFFF), Color.Transparent)
                    ),
                    start = Offset(14.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 14.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. KmGlassCard — Liquid Glass frosted surface with top specular highlight
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        KmGlassSurfaceSecondary,
                        KmGlassBackground
                    )
                )
            )
            .border(1.dp, KmGlassBorder, RoundedCornerShape(18.dp))
            .drawBehind {
                // Subtle specular line at top border
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0x25FFFFFF), Color.Transparent)
                    ),
                    start = Offset(18.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 18.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(18.dp),
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
fun KmDeviceCard(
    device: UiDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) KmOrangeGlow else KmBlackElevated)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) KmOrange else KmGlassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Platform icon background
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) KmOrange.copy(alpha = 0.25f) else KmOrangeGlow),
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
                        label = "Bluetooth",
                        isBest = device.bestTransport == "BLUETOOTH",
                        color = Color(0xFF60A5FA)
                    )
                }
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = KmOrange,
                modifier = Modifier.size(22.dp)
            )
        } else {
            KmStatusDot(status = device.status)
        }
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

                    Column(
                        modifier = Modifier
                            .offset(x = offsetX - 16.dp, y = offsetY)
                            .width(80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(KmBlackElevated)
                                .border(1.5.dp, KmOrange, CircleShape)
                                .clickable { onDeviceClick?.invoke(dev) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = dev.name,
                                tint = KmOrangeLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dev.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = KmTextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
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

// ─────────────────────────────────────────────────────────────────────────────
// 12. Bespoke KnowToMigrate Navigation Tabs & Vector Iconography
// ─────────────────────────────────────────────────────────────────────────────

enum class KmNavTab(val label: String, val contentDescription: String) {
    COMMAND("Command", "Command Center"),
    LAUNCH("Launch", "Launch Transfer"),
    LANDING("Landing", "Landing Transfers"),
    LEDGER("Ledger", "Migration Ledger"),
    CONTROL("Control", "Control Center")
}

@Composable
fun KmNavIcon(
    tab: KmNavTab,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) KmOrange else KmTextMuted

    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 1.75.dp.toPx()

        when (tab) {
            KmNavTab.COMMAND -> {
                // Upper protective chevron / energy roof
                val roofPath = Path().apply {
                    moveTo(w * 0.18f, h * 0.44f)
                    lineTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.82f, h * 0.44f)
                }
                drawPath(roofPath, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Side walls and base
                val bodyPath = Path().apply {
                    moveTo(w * 0.24f, h * 0.42f)
                    lineTo(w * 0.24f, h * 0.84f)
                    lineTo(w * 0.76f, h * 0.84f)
                    lineTo(w * 0.76f, h * 0.42f)
                }
                drawPath(bodyPath, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Central glowing energy core circle
                drawCircle(
                    color = color,
                    radius = w * 0.11f,
                    center = Offset(w * 0.50f, h * 0.60f),
                    style = if (isSelected) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = strokePx)
                )
            }
            KmNavTab.LAUNCH -> {
                // Diagonal supersonic arrow shaft from bottom-left to top-right
                drawLine(
                    color = color,
                    start = Offset(w * 0.22f, h * 0.78f),
                    end = Offset(w * 0.78f, h * 0.22f),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
                // Arrowhead at top-right
                val headPath = Path().apply {
                    moveTo(w * 0.46f, h * 0.22f)
                    lineTo(w * 0.78f, h * 0.22f)
                    lineTo(w * 0.78f, h * 0.54f)
                }
                drawPath(headPath, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Speed fin chevron
                val finPath = Path().apply {
                    moveTo(w * 0.26f, h * 0.46f)
                    lineTo(w * 0.54f, h * 0.46f)
                    lineTo(w * 0.54f, h * 0.74f)
                }
                drawPath(finPath, color = color.copy(alpha = if (isSelected) 0.85f else 0.5f), style = Stroke(width = strokePx * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            KmNavTab.LANDING -> {
                // Horizontal landing receptor cradle at bottom
                val cradlePath = Path().apply {
                    moveTo(w * 0.18f, h * 0.72f)
                    lineTo(w * 0.18f, h * 0.84f)
                    lineTo(w * 0.82f, h * 0.84f)
                    lineTo(w * 0.82f, h * 0.72f)
                }
                drawPath(cradlePath, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Downward central landing vector
                drawLine(
                    color = color,
                    start = Offset(w * 0.50f, h * 0.16f),
                    end = Offset(w * 0.50f, h * 0.66f),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
                // Downward arrowhead
                val arrowHead = Path().apply {
                    moveTo(w * 0.32f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.66f)
                    lineTo(w * 0.68f, h * 0.48f)
                }
                drawPath(arrowHead, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            KmNavTab.LEDGER -> {
                // Cryptographic ledger document outline
                val cardPath = Path().apply {
                    moveTo(w * 0.24f, h * 0.16f)
                    lineTo(w * 0.76f, h * 0.16f)
                    lineTo(w * 0.76f, h * 0.84f)
                    lineTo(w * 0.24f, h * 0.84f)
                    close()
                }
                drawPath(cardPath, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // 3 ledger entry timeline bars
                drawLine(color = color, start = Offset(w * 0.38f, h * 0.34f), end = Offset(w * 0.64f, h * 0.34f), strokeWidth = strokePx, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.38f, h * 0.50f), end = Offset(w * 0.64f, h * 0.50f), strokeWidth = strokePx, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.38f, h * 0.66f), end = Offset(w * 0.54f, h * 0.66f), strokeWidth = strokePx, cap = StrokeCap.Round)

                // Checkpoint verification pip
                drawCircle(color = if (isSelected) KmOrange else KmTextMuted, radius = w * 0.05f, center = Offset(w * 0.32f, h * 0.50f))
            }
            KmNavTab.CONTROL -> {
                // Precision tuning sliders
                // Row 1: Line with node at right
                drawLine(color = color, start = Offset(w * 0.18f, h * 0.30f), end = Offset(w * 0.82f, h * 0.30f), strokeWidth = strokePx, cap = StrokeCap.Round)
                drawCircle(color = color, radius = w * 0.08f, center = Offset(w * 0.64f, h * 0.30f), style = if (isSelected) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = strokePx))

                // Row 2: Line with node at left
                drawLine(color = color, start = Offset(w * 0.18f, h * 0.50f), end = Offset(w * 0.82f, h * 0.50f), strokeWidth = strokePx, cap = StrokeCap.Round)
                drawCircle(color = color, radius = w * 0.08f, center = Offset(w * 0.36f, h * 0.50f), style = if (isSelected) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = strokePx))

                // Row 3: Line with node at center
                drawLine(color = color, start = Offset(w * 0.18f, h * 0.70f), end = Offset(w * 0.82f, h * 0.70f), strokeWidth = strokePx, cap = StrokeCap.Round)
                drawCircle(color = color, radius = w * 0.08f, center = Offset(w * 0.58f, h * 0.70f), style = if (isSelected) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = strokePx))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Unified Precision Vector Check Icon (consistent across Android & Windows)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmCheckIcon(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tint: Color = KmSuccess,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.52f)
            lineTo(w * 0.40f, h * 0.74f)
            lineTo(w * 0.82f, h * 0.28f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Exact Prototype Components (Monogram, Radar, Action Tiles, Sliding Lens Nav)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KmMonogramLogo(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFF4D00), Color(0xFFFF5A00), Color(0xFFFF8A00))
                )
            )
            .drawBehind {
                drawLine(
                    color = Color(0x73FFFFFF),
                    start = Offset(8.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 8.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "KM",
            color = Color(0xFF0A0A0A),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun KmRadarWidget(
    modifier: Modifier = Modifier,
    size: Dp = 86.dp
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val anim1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val anim2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, delayMillis = 866, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val anim3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, delayMillis = 1733, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxRadius = this.size.minDimension / 2f

        listOf(anim1, anim2, anim3).forEach { progress ->
            val scale = 0.25f + progress * 0.75f
            val alpha = (1f - progress) * 0.9f
            if (alpha > 0f) {
                drawCircle(
                    color = Color(0xFFFF5A00).copy(alpha = alpha.coerceIn(0f, 1f)),
                    radius = maxRadius * scale,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        val dotRadius = 7.dp.toPx()
        drawCircle(
            color = Color(0x66FF5A00),
            radius = dotRadius * 1.8f,
            center = center
        )
        drawCircle(
            color = Color(0xFFFF5A00),
            radius = dotRadius,
            center = center
        )
    }
}

@Composable
fun KmActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x09FFFFFF))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFFF5F5F5),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF5F5F5)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF8C8C8C)
            )
        }
    }
}

@Composable
fun KmRecentTransferRow(
    filename: String,
    subtitle: String,
    isVerified: Boolean = true,
    isFolder: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x09FFFFFF))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x0DFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (isFolder) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFF8C8C8C),
                    modifier = Modifier.size(18.dp)
                )
            } else if (isVerified) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF8C8C8C),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = filename,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF5F5F5),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF8C8C8C),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        if (isVerified) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Verified",
                color = Color(0xFF22C55E),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun KmCustomCheckbox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .then(
                if (isChecked) {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFF4D00), Color(0xFFFF5A00))
                        )
                    )
                } else {
                    Modifier
                        .background(Color.Transparent)
                        .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(7.dp))
                }
            )
            .clickable { onCheckedChange(!isChecked) },
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF0A0A0A),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun KmCircularProgressRing(
    progress: Float,
    percentageText: String,
    speedText: String,
    filenameText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 12.dp.toPx()
                val radius = (size.minDimension - strokeWidthPx) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(
                    color = Color(0x14FFFFFF),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidthPx)
                )

                if (progress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFFFF8A00), Color(0xFFFF5A00), Color(0xFFFF4D00), Color(0xFFFF8A00))
                        ),
                        startAngle = -90f,
                        sweepAngle = (progress * 360f).coerceIn(0f, 360f),
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = percentageText,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F5F5)
                )
                Text(
                    text = speedText,
                    fontSize = 12.sp,
                    color = Color(0xFF8C8C8C),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = filenameText,
            fontSize = 13.sp,
            color = Color(0xFF8C8C8C),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun KmFloatingBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Pair("Home", Icons.Default.Home),
        Pair("Send", Icons.AutoMirrored.Filled.Send),
        Pair("Receive", Icons.Default.ArrowDownward),
        Pair("History", Icons.Default.History)
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        val totalWidth = maxWidth
        val tabWidth = (totalWidth - 12.dp) / 4

        val lensOffset by animateDpAsState(
            targetValue = 6.dp + (tabWidth * selectedIndex),
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
            label = "lens_x"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(Color(0xE6141414))
                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(34.dp))
        ) {
            Box(
                modifier = Modifier
                    .offset(x = lensOffset)
                    .padding(vertical = 6.dp)
                    .width(tabWidth)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFF8A00), Color(0xFFFF5A00), Color(0xFFFF4D00))
                        )
                    )
                    .drawBehind {
                        drawLine(
                            color = Color(0x80FFFFFF),
                            start = Offset(12.dp.toPx(), 1.dp.toPx()),
                            end = Offset(size.width - 12.dp.toPx(), 1.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val contentColor = if (isSelected) Color(0xFF0A0A0A) else Color(0xFF8C8C8C)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { onSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = item.second,
                                contentDescription = item.first,
                                tint = contentColor,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = item.first,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}


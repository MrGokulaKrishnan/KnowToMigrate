package com.knowtomigrate.app.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.knowtomigrate.app.R
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.KmBlack
import com.knowtomigrate.app.ui.theme.KmOrange
import com.knowtomigrate.app.ui.theme.KmOrangeLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium KnowToMigrate Startup Sequence (Android + Windows brand motion).
 *
 * Sequence (~1.25 seconds total):
 * 1. Black Initialization: AMOLED black with rising ambient orange glow.
 * 2. Energy Ignition: Central energy pulse + expanding outward energy wave.
 * 3. Logo Reveal: 15% rounded container, scale 94% -> 100%, dynamic glow (low -> high -> settle).
 * 4. Energy Trail: Subtle orange migration light travels 1 cycle around the container perimeter.
 * 5. Logo Light Sweep: Single subtle glossy sheen moves left -> right across the logo.
 * 6. Application Reveal: Smooth transition (scale 100% -> 96%, opacity 100% -> 0%) crossfading into Home.
 *
 * Honors system reduced-motion preferences without blocking engine/network initialization.
 */
@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    // Detect system reduced-motion / animation disabled settings
    val isReducedMotion = remember {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Throwable) {
            false
        }
    }

    // Step 1: Ambient Glow & Background
    val ambientGlowAlpha = remember { Animatable(0f) }

    // Step 2: Energy Ignition Point & Wave
    val energyPointAlpha = remember { Animatable(0f) }
    val energyPointScale = remember { Animatable(0.2f) }
    val energyWaveRadius = remember { Animatable(0f) }
    val energyWaveAlpha = remember { Animatable(0f) }

    // Step 3: Logo Container Reveal & Dynamic Glow
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(if (isReducedMotion) 1f else 0.94f) }
    val dynamicGlowAlpha = remember { Animatable(0.12f) }
    val dynamicGlowRadius = remember { Animatable(0.9f) }

    // Step 4: Energy Trail around Container (0f -> 1f)
    val trailProgress = remember { Animatable(0f) }
    val trailAlpha = remember { Animatable(0f) }

    // Step 5: Logo Light Sweep (-0.5f -> 1.5f)
    val sweepProgress = remember { Animatable(-0.5f) }

    // Step 6: Application Reveal (100% -> 96% scale, 100% -> 0% alpha)
    val exitScale = remember { Animatable(1.0f) }
    val exitAlpha = remember { Animatable(1.0f) }

    LaunchedEffect(Unit) {
        if (isReducedMotion) {
            // Simplified reduced motion: Gentle fade in, then navigate
            logoAlpha.animateTo(1f, animationSpec = tween(durationMillis = 200, easing = LinearEasing))
            delay(180)
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            // ─────────────────────────────────────────────────────────
            // STEP 1 — BLACK INITIALIZATION (0ms - 200ms)
            // ─────────────────────────────────────────────────────────
            launch {
                ambientGlowAlpha.animateTo(
                    targetValue = 0.35f,
                    animationSpec = tween(durationMillis = 350, easing = EaseOutCubic)
                )
            }

            // ─────────────────────────────────────────────────────────
            // STEP 2 — ENERGY IGNITION (100ms - 380ms)
            // ─────────────────────────────────────────────────────────
            launch {
                delay(80)
                // Energy spark fades in, pulses, expands slightly
                launch {
                    energyPointAlpha.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
                    energyPointAlpha.animateTo(0.2f, tween(180, easing = FastOutLinearInEasing))
                    energyPointAlpha.animateTo(0f, tween(120, easing = LinearEasing))
                }
                launch {
                    energyPointScale.animateTo(1.4f, tween(260, easing = FastOutSlowInEasing))
                }
                // Energy wave radiates outward
                launch {
                    delay(120)
                    energyWaveAlpha.animateTo(0.75f, tween(100, easing = FastOutSlowInEasing))
                    launch {
                        energyWaveRadius.animateTo(1.6f, tween(360, easing = EaseOutCubic))
                    }
                    energyWaveAlpha.animateTo(0f, tween(260, easing = EaseOutCubic))
                }
            }

            // ─────────────────────────────────────────────────────────
            // STEP 3 — LOGO REVEAL (280ms - 680ms)
            // Scale: 94% -> 100%, Opacity: 0% -> 100%, Glow: Low -> High -> Settle
            // ─────────────────────────────────────────────────────────
            launch {
                delay(260)
                launch {
                    logoAlpha.animateTo(1.0f, tween(340, easing = FastOutSlowInEasing))
                }
                launch {
                    logoScale.animateTo(1.0f, tween(380, easing = FastOutSlowInEasing))
                }
                // Dynamic glow pulse (Low -> Medium/High -> Settle)
                launch {
                    dynamicGlowAlpha.animateTo(0.65f, tween(220, easing = FastOutSlowInEasing))
                    dynamicGlowRadius.animateTo(1.28f, tween(300, easing = EaseOutCubic))
                    dynamicGlowAlpha.animateTo(0.28f, tween(280, easing = FastOutSlowInEasing))
                    dynamicGlowRadius.animateTo(1.08f, tween(280, easing = FastOutSlowInEasing))
                }
            }

            // ─────────────────────────────────────────────────────────
            // STEP 4 — ENERGY TRAIL AROUND CONTAINER (500ms - 920ms)
            // Travels 1 complete cycle around the 15% rounded rectangle
            // ─────────────────────────────────────────────────────────
            launch {
                delay(480)
                launch {
                    trailAlpha.animateTo(0.9f, tween(100, easing = FastOutSlowInEasing))
                    delay(300)
                    trailAlpha.animateTo(0f, tween(140, easing = FastOutLinearInEasing))
                }
                trailProgress.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing)
                )
            }

            // ─────────────────────────────────────────────────────────
            // STEP 5 — LOGO LIGHT SWEEP (760ms - 1060ms)
            // Left -> Right subtle glossy light sweep
            // ─────────────────────────────────────────────────────────
            launch {
                delay(720)
                sweepProgress.animateTo(
                    targetValue = 1.5f,
                    animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
                )
            }

            // ─────────────────────────────────────────────────────────
            // STEP 6 — APPLICATION REVEAL (1120ms - 1340ms)
            // Logo scale: 100% -> 96%, opacity 100% -> 0%
            // ─────────────────────────────────────────────────────────
            delay(1120)
            launch {
                exitScale.animateTo(0.96f, tween(220, easing = FastOutSlowInEasing))
            }
            launch {
                exitAlpha.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
            }

            delay(220)
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KmBlack),
        contentAlignment = Alignment.Center
    ) {
        val containerSizeDp = 176.dp
        val cornerRadiusDp = 26.dp // ~15% of 176dp

        // Ambient radial glow behind the logo
        if (!isReducedMotion && (ambientGlowAlpha.value > 0.01f || dynamicGlowAlpha.value > 0.01f)) {
            Canvas(
                modifier = Modifier
                    .size(340.dp)
                    .scale(dynamicGlowRadius.value)
            ) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f
                val effectiveAlpha = (ambientGlowAlpha.value * 0.4f + dynamicGlowAlpha.value * 0.6f).coerceIn(0f, 1f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            KmOrange.copy(alpha = effectiveAlpha * 0.55f),
                            KmOrange.copy(alpha = effectiveAlpha * 0.20f),
                            Color.Transparent
                        ),
                        center = centerOffset,
                        radius = radius
                    ),
                    radius = radius,
                    center = centerOffset
                )
            }
        }

        // Energy Ignition spark & expanding wave
        if (!isReducedMotion && (energyPointAlpha.value > 0.01f || energyWaveAlpha.value > 0.01f)) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Central orange ignition point
                if (energyPointAlpha.value > 0.01f) {
                    val pRadius = 14.dp.toPx() * energyPointScale.value
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = energyPointAlpha.value),
                                KmOrangeLight.copy(alpha = energyPointAlpha.value * 0.8f),
                                KmOrange.copy(alpha = energyPointAlpha.value * 0.4f),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = pRadius
                        ),
                        radius = pRadius,
                        center = centerOffset
                    )
                }

                // Expanding outward energy wave
                if (energyWaveAlpha.value > 0.01f && energyWaveRadius.value > 0.05f) {
                    val wRadius = 65.dp.toPx() * energyWaveRadius.value
                    drawCircle(
                        color = KmOrange.copy(alpha = energyWaveAlpha.value * 0.6f),
                        radius = wRadius,
                        center = centerOffset,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                KmOrange.copy(alpha = energyWaveAlpha.value * 0.25f),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = wRadius
                        ),
                        radius = wRadius,
                        center = centerOffset
                    )
                }
            }
        }

        // Master KnowToMigrate Logo Container (15% Rounded Corner Radius, AMOLED Black, Orange Border/Glow)
        Box(
            modifier = Modifier
                .size(containerSizeDp)
                .alpha(logoAlpha.value * exitAlpha.value)
                .scale(logoScale.value * exitScale.value)
                .background(KmBlack, shape = RoundedCornerShape(cornerRadiusDp))
                .border(
                    width = 1.5.dp,
                    color = KmOrange.copy(alpha = (0.75f * logoAlpha.value).coerceIn(0f, 1f)),
                    shape = RoundedCornerShape(cornerRadiusDp)
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            // Approved KnowToMigrate Artwork (preserves original artwork, colors, and proportions)
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "KnowToMigrate Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Glossy Light Sweep across the container (Left -> Right)
            if (!isReducedMotion && sweepProgress.value > -0.4f && sweepProgress.value < 1.4f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(cornerRadiusDp))
                ) {
                    val w = size.width
                    val h = size.height
                    val sweepX = w * sweepProgress.value
                    val sweepWidth = w * 0.45f

                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x18FFFFFF),
                                Color(0x35FFB066),
                                Color(0x18FFFFFF),
                                Color.Transparent
                            ),
                            start = Offset(sweepX - sweepWidth / 2f, 0f),
                            end = Offset(sweepX + sweepWidth / 2f, h)
                        )
                    )
                }
            }
        }

        // Energy Trail traveling around the 15% rounded logo container perimeter
        if (!isReducedMotion && trailAlpha.value > 0.01f && trailProgress.value > 0.01f) {
            Canvas(
                modifier = Modifier
                    .size(containerSizeDp)
                    .alpha(exitAlpha.value)
                    .scale(logoScale.value * exitScale.value)
            ) {
                val cornerRadiusPx = cornerRadiusDp.toPx()
                val rectPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(Offset.Zero, size),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    )
                }

                val pathMeasure = PathMeasure()
                pathMeasure.setPath(rectPath, forceClosed = true)
                val totalLength = pathMeasure.length

                if (totalLength > 10f) {
                    val currentPosDistance = totalLength * trailProgress.value
                    val headOffset = pathMeasure.getPosition(currentPosDistance)

                    // Draw glowing energy particle at the leading edge
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = headOffset
                    )
                    drawCircle(
                        color = KmOrange,
                        radius = 6.dp.toPx(),
                        center = headOffset
                    )
                    drawCircle(
                        color = KmOrangeLight.copy(alpha = trailAlpha.value * 0.5f),
                        radius = 12.dp.toPx(),
                        center = headOffset
                    )

                    // Draw subtle trailing arc behind the particle
                    val tailLength = totalLength * 0.12f
                    val tailStartDist = (currentPosDistance - tailLength).coerceAtLeast(0f)
                    val tailPath = Path()
                    pathMeasure.getSegment(tailStartDist, currentPosDistance, tailPath, startWithMoveTo = true)

                    drawPath(
                        path = tailPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, KmOrangeLight.copy(alpha = trailAlpha.value)),
                            start = pathMeasure.getPosition(tailStartDist),
                            end = headOffset
                        ),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

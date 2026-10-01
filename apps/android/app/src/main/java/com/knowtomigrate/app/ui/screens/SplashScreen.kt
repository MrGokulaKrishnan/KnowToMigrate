package com.knowtomigrate.app.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
 * Premium KnowToMigrate Cinematic Startup Motion Sequence.
 *
 * 10-Phase Hardware-Accelerated Motion Graphics:
 * PHASE 1: Pure AMOLED Black initialization (#000000).
 * PHASE 2: Vibrant central orange energy ignition point pulses.
 * PHASE 3: Energy wave radiates outward.
 * PHASE 4: Rounded container (15% corner radius) fades and scales in (92% -> 100%).
 * PHASE 5: Approved KnowToMigrate logo powers on with dynamic glow.
 * PHASE 6: Orange laser border draws progressively from 0% to 100% around the container.
 * PHASE 7: High-energy particle orbits the perimeter, powering the migration engine.
 * PHASE 8: Soft glowing orange pulse blooms outward.
 * PHASE 9: Diagonal glossy light sweep travels left -> right across the logo face.
 * PHASE 10: Smooth settle and seamless application reveal crossfade into Home.
 */
@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    // Detect system reduced-motion preference safely
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

    // Phase 1 & 2: Background ambient glow & ignition point
    val ambientGlowAlpha = remember { Animatable(0f) }
    val ignitionPointAlpha = remember { Animatable(0f) }
    val ignitionPointScale = remember { Animatable(0.2f) }

    // Phase 3: Energy ripple wave
    val waveRadius = remember { Animatable(0f) }
    val waveAlpha = remember { Animatable(0f) }

    // Phase 4 & 5: Logo container & logo reveal
    val logoContainerAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(if (isReducedMotion) 1f else 0.92f) }
    val dynamicGlowAlpha = remember { Animatable(0f) }
    val dynamicGlowRadius = remember { Animatable(0.85f) }

    // Phase 6: Perimeter border drawing (0f -> 1f)
    val borderDrawProgress = remember { Animatable(0f) }

    // Phase 7: Orbiting energy trail particle (0f -> 1f)
    val orbitParticleProgress = remember { Animatable(0f) }
    val orbitParticleAlpha = remember { Animatable(0f) }

    // Phase 9: Diagonal light sweep sheen (-0.5f -> 1.5f)
    val sweepProgress = remember { Animatable(-0.5f) }

    // Phase 10: Application reveal crossfade
    val exitScale = remember { Animatable(1.0f) }
    val exitAlpha = remember { Animatable(1.0f) }

    LaunchedEffect(Unit) {
        if (isReducedMotion) {
            logoContainerAlpha.animateTo(1f, tween(250, easing = LinearEasing))
            delay(200)
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            // PHASE 1 — BLACK INITIALIZATION & AMBIENT GLOW (0ms)
            launch {
                ambientGlowAlpha.animateTo(
                    targetValue = 0.45f,
                    animationSpec = tween(durationMillis = 400, easing = EaseOutCubic)
                )
            }

            // PHASE 2 — ENERGY IGNITION POINT (60ms - 320ms)
            launch {
                delay(60)
                launch {
                    ignitionPointAlpha.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
                    delay(80)
                    ignitionPointAlpha.animateTo(0f, tween(140, easing = LinearEasing))
                }
                launch {
                    ignitionPointScale.animateTo(1.6f, tween(300, easing = FastOutSlowInEasing))
                }
            }

            // PHASE 3 — ENERGY RIPPLE WAVE (180ms - 540ms)
            launch {
                delay(180)
                waveAlpha.animateTo(0.85f, tween(120, easing = FastOutSlowInEasing))
                launch {
                    waveRadius.animateTo(2.0f, tween(380, easing = EaseOutCubic))
                }
                waveAlpha.animateTo(0f, tween(260, easing = EaseOutCubic))
            }

            // PHASE 4 & 5 — LOGO CONTAINER & ARTWORK REVEAL (260ms - 640ms)
            launch {
                delay(240)
                launch {
                    logoContainerAlpha.animateTo(1.0f, tween(320, easing = FastOutSlowInEasing))
                }
                launch {
                    logoScale.animateTo(1.0f, tween(360, easing = FastOutSlowInEasing))
                }
                // Dynamic glow pulse: low -> medium -> settle
                launch {
                    dynamicGlowAlpha.animateTo(0.75f, tween(240, easing = FastOutSlowInEasing))
                    dynamicGlowRadius.animateTo(1.35f, tween(320, easing = EaseOutCubic))
                    dynamicGlowAlpha.animateTo(0.32f, tween(300, easing = FastOutSlowInEasing))
                    dynamicGlowRadius.animateTo(1.10f, tween(300, easing = FastOutSlowInEasing))
                }
            }

            // PHASE 6 — ORANGE BORDER LASER DRAW (420ms - 880ms)
            launch {
                delay(400)
                borderDrawProgress.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing)
                )
            }

            // PHASE 7 — ORBITING ENERGY PARTICLE (700ms - 1160ms)
            launch {
                delay(680)
                launch {
                    orbitParticleAlpha.animateTo(1.0f, tween(120, easing = FastOutSlowInEasing))
                    delay(300)
                    orbitParticleAlpha.animateTo(0f, tween(160, easing = FastOutLinearInEasing))
                }
                orbitParticleProgress.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                )
            }

            // PHASE 9 — DIAGONAL GLOSSY LIGHT SWEEP (960ms - 1320ms)
            launch {
                delay(940)
                sweepProgress.animateTo(
                    targetValue = 1.5f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                )
            }

            // PHASE 10 — APPLICATION REVEAL (1420ms - 1640ms)
            delay(1420)
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
        val cornerRadiusDp = 26.dp // Exactly 15% corner radius for 176dp

        // Radial ambient orange bloom behind logo
        if (!isReducedMotion && (ambientGlowAlpha.value > 0.01f || dynamicGlowAlpha.value > 0.01f)) {
            Canvas(
                modifier = Modifier
                    .size(360.dp)
                    .scale(dynamicGlowRadius.value)
            ) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f
                val effectiveAlpha = (ambientGlowAlpha.value * 0.45f + dynamicGlowAlpha.value * 0.55f).coerceIn(0f, 1f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            KmOrange.copy(alpha = effectiveAlpha * 0.65f),
                            KmOrangeLight.copy(alpha = effectiveAlpha * 0.25f),
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

        // Energy Ignition Point & Expanding Shockwave
        if (!isReducedMotion && (ignitionPointAlpha.value > 0.01f || waveAlpha.value > 0.01f)) {
            Canvas(modifier = Modifier.size(260.dp)) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Central ignition point
                if (ignitionPointAlpha.value > 0.01f) {
                    val pRadius = 18.dp.toPx() * ignitionPointScale.value
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = ignitionPointAlpha.value),
                                KmOrangeLight.copy(alpha = ignitionPointAlpha.value * 0.9f),
                                KmOrange.copy(alpha = ignitionPointAlpha.value * 0.5f),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = pRadius
                        ),
                        radius = pRadius,
                        center = centerOffset
                    )
                }

                // Expanding energy wave
                if (waveAlpha.value > 0.01f && waveRadius.value > 0.05f) {
                    val wRadius = 75.dp.toPx() * waveRadius.value
                    drawCircle(
                        color = KmOrange.copy(alpha = waveAlpha.value * 0.75f),
                        radius = wRadius,
                        center = centerOffset,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                KmOrange.copy(alpha = waveAlpha.value * 0.30f),
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

        // Master Logo Container (15% Rounded Corners, AMOLED Black Surface, Artwork)
        Box(
            modifier = Modifier
                .size(containerSizeDp)
                .alpha(logoContainerAlpha.value * exitAlpha.value)
                .scale(logoScale.value * exitScale.value)
                .background(KmBlack, shape = RoundedCornerShape(cornerRadiusDp))
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

            // Diagonal Glossy Light Sweep across the logo
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
                                Color(0x22FFFFFF),
                                Color(0x55FFB066),
                                Color(0x22FFFFFF),
                                Color.Transparent
                            ),
                            start = Offset(sweepX - sweepWidth / 2f, 0f),
                            end = Offset(sweepX + sweepWidth / 2f, h)
                        )
                    )
                }
            }
        }

        // Perimeter Laser Border Drawing & Orbiting Energy Particle
        if (!isReducedMotion && logoContainerAlpha.value > 0.05f) {
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
                    // PHASE 6: Draw the perimeter border progressively (0% -> 100%)
                    val currentDrawDist = totalLength * borderDrawProgress.value.coerceIn(0f, 1f)
                    if (currentDrawDist > 1f) {
                        val drawnSegment = Path()
                        pathMeasure.getSegment(0f, currentDrawDist, drawnSegment, startWithMoveTo = true)
                        drawPath(
                            path = drawnSegment,
                            brush = Brush.linearGradient(
                                colors = listOf(KmOrange, KmOrangeLight, KmOrange)
                            ),
                            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Static subtle base border after drawing finishes
                    if (borderDrawProgress.value >= 0.99f) {
                        drawPath(
                            path = rectPath,
                            color = KmOrange.copy(alpha = 0.85f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // PHASE 7: Orbiting energy particle traveling along the border
                    if (orbitParticleAlpha.value > 0.01f && orbitParticleProgress.value > 0.01f) {
                        val currentPosDistance = totalLength * orbitParticleProgress.value
                        val headOffset = pathMeasure.getPosition(currentPosDistance)

                        // Glowing head particle
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = headOffset
                        )
                        drawCircle(
                            color = KmOrange,
                            radius = 7.dp.toPx(),
                            center = headOffset
                        )
                        drawCircle(
                            color = KmOrangeLight.copy(alpha = orbitParticleAlpha.value * 0.6f),
                            radius = 14.dp.toPx(),
                            center = headOffset
                        )

                        // Trailing arc
                        val tailLength = totalLength * 0.16f
                        val tailStartDist = (currentPosDistance - tailLength).coerceAtLeast(0f)
                        val tailPath = Path()
                        pathMeasure.getSegment(tailStartDist, currentPosDistance, tailPath, startWithMoveTo = true)

                        drawPath(
                            path = tailPath,
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, KmOrangeLight.copy(alpha = orbitParticleAlpha.value)),
                                start = pathMeasure.getPosition(tailStartDist),
                                end = headOffset
                            ),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}

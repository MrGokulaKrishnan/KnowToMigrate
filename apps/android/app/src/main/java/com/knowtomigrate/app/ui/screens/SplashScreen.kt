package com.knowtomigrate.app.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.knowtomigrate.app.R
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.KmBlack
import com.knowtomigrate.app.ui.theme.KmOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(if (isReducedMotion) 1f else 0.88f) }
    val glowAlpha = remember { Animatable(0f) }
    val glowRadiusFraction = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        if (isReducedMotion) {
            // Shortened reduced-motion transition: Black -> Logo -> Home
            logoAlpha.animateTo(1f, animationSpec = tween(durationMillis = 200, easing = LinearEasing))
            delay(150)
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            // Premium Startup Sequence (~900ms):
            // 1. Logo appears with subtle fade + scale (0 -> 350ms)
            // 2. Migration energy animation: subtle orange glow pulse + logo settles (350 -> 750ms)
            // 3. Smooth transition directly to Home (750 -> 900ms)
            
            // Fade in
            launch {
                logoAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            }
            
            // Scale and settle
            launch {
                logoScale.animateTo(
                    targetValue = 1.04f,
                    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                )
                logoScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 280, easing = EaseOutCubic)
                )
            }

            // Energy / subtle orange glow pulse
            launch {
                delay(220)
                launch {
                    glowAlpha.animateTo(
                        targetValue = 0.40f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                    )
                    glowAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing)
                    )
                }
                launch {
                    glowRadiusFraction.animateTo(
                        targetValue = 1.25f,
                        animationSpec = tween(durationMillis = 480, easing = EaseOutCubic)
                    )
                }
            }

            delay(880)

            // Smooth transition into Home screen
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
        // Subtle GPU-accelerated radial orange glow behind the logo
        if (!isReducedMotion && glowAlpha.value > 0.01f) {
            Canvas(
                modifier = Modifier
                    .size(320.dp)
                    .scale(glowRadiusFraction.value)
            ) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            KmOrange.copy(alpha = glowAlpha.value * 0.55f),
                            KmOrange.copy(alpha = glowAlpha.value * 0.15f),
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

        // Master KnowToMigrate Logo (aspect ratio preserved 1:1, optically centered)
        Image(
            painter = painterResource(id = R.drawable.splash_logo),
            contentDescription = "KnowToMigrate Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(190.dp)
                .alpha(logoAlpha.value)
                .scale(logoScale.value)
        )
    }
}

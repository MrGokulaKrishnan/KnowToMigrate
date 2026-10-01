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
    val logoScale = remember { Animatable(if (isReducedMotion) 1f else 0.96f) }
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
            // Premium Startup Sequence (~850ms):
            // 1. Logo appears with subtle fade + scale 96% -> 100%
            // 2. Migration energy animation: subtle orange glow pulse
            // 3. Smooth transition directly to Home
            
            // Fade in
            launch {
                logoAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            }
            
            // Scale smoothly from 96% -> 100%
            launch {
                logoScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
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
                if (radius > 1f) {
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
        }

        // Master KnowToMigrate Logo Container (15% Rounded Corner Radius, Orange Border/Glow)
        Box(
            modifier = Modifier
                .size(176.dp)
                .alpha(logoAlpha.value)
                .scale(logoScale.value)
                .background(Color.Black, shape = RoundedCornerShape(26.dp))
                .border(
                    width = 1.5.dp,
                    color = KmOrange.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "KnowToMigrate Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

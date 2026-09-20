package com.knowtomigrate.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KmColorScheme = darkColorScheme(
    primary = KmOrange,
    onPrimary = Color.White,
    primaryContainer = KmOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = KmOrangeLight,
    onSecondary = Color.White,
    background = KmBlack,
    onBackground = KmTextPrimary,
    surface = KmBlackCard,
    onSurface = KmTextPrimary,
    surfaceVariant = KmBlackElevated,
    onSurfaceVariant = KmTextSecondary,
    outline = KmGlassBorder,
    error = KmError,
    onError = Color.White,
)

@Composable
fun KnowToMigrateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KmColorScheme,
        typography = KmTypography,
        content = content
    )
}

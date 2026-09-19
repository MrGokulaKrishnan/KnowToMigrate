package com.knowtomigrate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = KmOrange,
    secondary = KmAmber,
    tertiary = KmBright,
    background = PureBlack,
    surface = DarkSurface,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun KnowToMigrateTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

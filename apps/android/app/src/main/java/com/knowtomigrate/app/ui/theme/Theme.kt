package com.knowtomigrate.app.ui.theme

/*
 * KnowToMigrate - Dark Liquid Glass Theme
 * Updated: 22 October 2026
 * Pure AMOLED #000000 base with KM Electric Orange accents and honest security disclosures
 */

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Global UI Constants (Single source of truth) ──────────────────────────
object KtmUiConstants {
    /**
     * Whether transfers are end-to-end encrypted.
     * Currently false. When cryptographic transport encryption is implemented,
     * toggle this single constant to update all user-facing disclaimers.
     */
    const val IS_ENCRYPTED: Boolean = false

    const val SECURITY_LABEL: String = "Not encrypted yet"
    const val SECURITY_CAPTION: String = "Use on trusted networks only."
    const val PAIRING_PIN_CAPTION: String = "Confirm the same PIN on the other device before accepting."
}

private val KmColorScheme = darkColorScheme(
    primary = KmOrange,
    onPrimary = KmTextOnGradient,
    primaryContainer = KmOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = KmOrangeLight,
    onSecondary = KmTextOnGradient,
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

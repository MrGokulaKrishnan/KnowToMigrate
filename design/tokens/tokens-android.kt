/**
 * KnowToMigrate Design System — Android / Jetpack Compose Tokens
 * Move Anything. Anywhere. Seamlessly.
 *
 * Generated from design/tokens/tokens.json v1.0.0
 *
 * Usage:
 *   import com.knowtomigrate.design.tokens.KtmColors
 *   import com.knowtomigrate.design.tokens.KtmSpacing
 *
 *   Box(modifier = Modifier.background(KtmColors.BrandPrimary))
 *   Text(text = "Hello", color = KtmColors.TextPrimary, fontSize = KtmTypography.TextLg)
 */

package com.knowtomigrate.design.tokens

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// COLORS
// ============================================================

object KtmColors {

    // --------------------------------------------------------
    // Background
    // --------------------------------------------------------
    val BackgroundPrimary   = Color(0xFF000000)
    val BackgroundSecondary = Color(0xFF050505)
    val BackgroundTertiary  = Color(0xFF080808)
    val BackgroundCard      = Color(0xFF0C0C0C)
    val BackgroundElevated  = Color(0xFF111111)

    // --------------------------------------------------------
    // Brand
    // --------------------------------------------------------
    val BrandPrimary    = Color(0xFFFF5A00)
    val BrandGradient1  = Color(0xFFFF4D00)
    val BrandGradient2  = Color(0xFFFF5A00)
    val BrandGradient3  = Color(0xFFFF6A00)
    val BrandGradient4  = Color(0xFFFF8A00)
    val BrandGradient5  = Color(0xFFFFB066)

    /** Primary brand gradient brush — use for Button, Progress, Accents */
    val BrandGradientBrush = Brush.linearGradient(
        colors = listOf(BrandGradient1, BrandGradient2, BrandGradient3, BrandGradient4)
    )

    /** Subtle brand gradient for backgrounds / tinted surfaces */
    val BrandSubtleGradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0x22FF4D00),
            Color(0x22FF8A00)
        )
    )

    // --------------------------------------------------------
    // Text
    // --------------------------------------------------------
    val TextPrimary   = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFD4D4D4)
    val TextMuted     = Color(0xFF8A8A8A)
    val TextDisabled  = Color(0xFF555555)

    // --------------------------------------------------------
    // Status
    // --------------------------------------------------------
    val StatusSuccess = Color(0xFF22C55E)
    val StatusWarning = Color(0xFFF59E0B)
    val StatusError   = Color(0xFFEF4444)
    val StatusInfo    = Color(0xFF38BDF8)

    // --------------------------------------------------------
    // Glass
    // --------------------------------------------------------
    val GlassBackground      = Color(0x0AFFFFFF) // rgba(255,255,255,0.04)
    val GlassBorder          = Color(0x14FFFFFF) // rgba(255,255,255,0.08)
    val GlassHighlight       = Color(0x1FFFFFFF) // rgba(255,255,255,0.12)
    val GlassOrangeGlow      = Color(0x26FF5A00) // rgba(255,90,0,0.15)
    val GlassOrangeGlowStrong= Color(0x40FF5A00) // rgba(255,90,0,0.25)

    // --------------------------------------------------------
    // Glass Card / Surface convenience
    // --------------------------------------------------------
    val GlassSurfaceBackground = Color(0x0AFFFFFF)
    val GlassCardBackground    = Color(0x0DFFFFFF)

    // --------------------------------------------------------
    // Progress bar
    // --------------------------------------------------------
    val ProgressFillBrush = Brush.horizontalGradient(
        colors = listOf(BrandGradient1, BrandGradient4)
    )

    // --------------------------------------------------------
    // Success glow gradient
    // --------------------------------------------------------
    val SuccessGlowBrush = Brush.linearGradient(
        colors = listOf(Color(0x2222C55E), Color(0x1122C55E))
    )

    // --------------------------------------------------------
    // Surface gradient
    // --------------------------------------------------------
    val SurfaceGradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF111111), Color(0xFF080808))
    )

    // --------------------------------------------------------
    // Card gradient
    // --------------------------------------------------------
    val CardGradientBrush = Brush.linearGradient(
        colors = listOf(Color(0x0DFFFFFF), Color(0x05FFFFFF))
    )

    // --------------------------------------------------------
    // Gloss (top highlight stripe)
    // --------------------------------------------------------
    val GlossBrush = Brush.verticalGradient(
        colors = listOf(Color(0x1FFFFFFF), Color(0x00FFFFFF))
    )
}

// ============================================================
// TYPOGRAPHY
// ============================================================

object KtmTypography {

    // Scale (TextUnit / sp)
    val TextXs:   TextUnit = 11.sp
    val TextSm:   TextUnit = 13.sp
    val TextBase: TextUnit = 15.sp
    val TextMd:   TextUnit = 17.sp
    val TextLg:   TextUnit = 20.sp
    val TextXl:   TextUnit = 24.sp
    val Text2Xl:  TextUnit = 30.sp
    val Text3Xl:  TextUnit = 36.sp
    val Text4Xl:  TextUnit = 48.sp
    val TextHero: TextUnit = 64.sp

    // Weights
    val WeightRegular:   FontWeight = FontWeight(400)
    val WeightMedium:    FontWeight = FontWeight(500)
    val WeightSemibold:  FontWeight = FontWeight(600)
    val WeightBold:      FontWeight = FontWeight(700)
    val WeightExtrabold: FontWeight = FontWeight(800)

    // Line height multipliers
    const val LineHeightTight:   Float = 1.2f
    const val LineHeightSnug:    Float = 1.35f
    const val LineHeightNormal:  Float = 1.5f
    const val LineHeightRelaxed: Float = 1.65f
}

// ============================================================
// SPACING
// ============================================================

object KtmSpacing {
    val Space1:  Dp = 4.dp
    val Space2:  Dp = 8.dp
    val Space3:  Dp = 12.dp
    val Space4:  Dp = 16.dp
    val Space5:  Dp = 20.dp
    val Space6:  Dp = 24.dp
    val Space8:  Dp = 32.dp
    val Space10: Dp = 40.dp
    val Space12: Dp = 48.dp
    val Space16: Dp = 64.dp
    val Space20: Dp = 80.dp
    val Space24: Dp = 96.dp
}

// ============================================================
// BORDER RADIUS
// ============================================================

object KtmRadius {
    val Sm:   Dp = 8.dp
    val Md:   Dp = 12.dp
    val Lg:   Dp = 18.dp
    val Xl:   Dp = 24.dp
    val Xxl:  Dp = 32.dp
    val Full: Dp = 9999.dp
}

// ============================================================
// ELEVATION / SHADOWS (represented as dp for shadow elevation)
// ============================================================

object KtmElevation {
    val Sm: Dp = 2.dp
    val Md: Dp = 4.dp
    val Lg: Dp = 8.dp
    val Xl: Dp = 16.dp
}

// ============================================================
// ANIMATION DURATIONS (ms)
// ============================================================

object KtmDuration {
    const val Instant:   Int = 80
    const val Fast:      Int = 150
    const val Normal:    Int = 250
    const val Slow:      Int = 400
    const val Slower:    Int = 600
    const val Discovery: Int = 2000
    const val Pulse:     Int = 1500
}

// ============================================================
// ICONS
// ============================================================

object KtmIcons {
    const val StrokeWidth: Float = 1.75f

    val SizeXs:  Dp = 14.dp
    val SizeSm:  Dp = 16.dp
    val SizeMd:  Dp = 20.dp
    val SizeLg:  Dp = 24.dp
    val SizeXl:  Dp = 32.dp
    val Size2Xl: Dp = 48.dp
}

// ============================================================
// COMPONENTS
// ============================================================

object KtmButton {
    val Height:     Dp = 48.dp
    val PaddingX:   Dp = 24.dp
    val Radius:     Dp = KtmRadius.Md
    val FontSize:   TextUnit = KtmTypography.TextBase
    val FontWeight: FontWeight = KtmTypography.WeightSemibold
    val MinTapTarget: Dp = 44.dp
}

object KtmCard {
    val Radius:  Dp = KtmRadius.Lg
    val Padding: Dp = KtmSpacing.Space5
}

object KtmDeviceCard {
    val Radius:       Dp    = KtmRadius.Lg
    val ActiveBorderColor = Color(0x66FF5A00)   // rgba(255,90,0,0.4)
    val ActiveGlowColor   = Color(0x40FF5A00)   // rgba(255,90,0,0.25)
    val ActiveElevation: Dp = 24.dp
}

object KtmProgressBar {
    val Height: Dp = 6.dp
    val Radius: Dp = KtmRadius.Full
    val Background = KtmColors.GlassBorder
    val FillBrush  = KtmColors.ProgressFillBrush
}

object KtmInput {
    val Height:  Dp = 48.dp
    val Radius:  Dp = KtmRadius.Md
    val Background      = KtmColors.GlassBackground
    val BorderColor     = KtmColors.GlassBorder
    val FocusBorderColor= Color(0x80FF5A00) // rgba(255,90,0,0.5)
}

object KtmBadge {
    val Radius:     Dp         = KtmRadius.Full
    val PaddingX:   Dp         = 10.dp
    val PaddingY:   Dp         = 3.dp
    val FontSize:   TextUnit   = 12.sp
    val FontWeight: FontWeight = KtmTypography.WeightSemibold
}

object KtmAccessibility {
    val MinTapTarget:    Dp    = 44.dp
    val FocusRingWidth:  Dp    = 3.dp
    val FocusRingColor         = Color(0x99FF5A00) // rgba(255,90,0,0.6)
    const val MinContrastRatio: Float = 4.5f
}

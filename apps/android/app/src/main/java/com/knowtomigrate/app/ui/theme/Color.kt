package com.knowtomigrate.app.ui.theme

/*
 * KnowToMigrate - Design Tokens (Dark Liquid Glass)
 * Updated: 22 October 2026
 * AMOLED black, KM Orange gradient, Liquid Glass surfaces, WCAG AA contrast (#0A0A0A on orange)
 */

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── AMOLED Base ─────────────────────────────────────────────────────────────
val KmBlack = Color(0xFF000000)
val KmBlackSecondary = Color(0xFF050505)
val KmBlackTertiary = Color(0xFF080808)
val KmBlackCard = Color(0xFF0C0C0C)
val KmBlackElevated = Color(0xFF111111)

// ── Primary Orange Spectrum (Primary Action & Active State ONLY) ───────────
val KmOrangeDark = Color(0xFFFF4D00)
val KmOrange = Color(0xFFFF5A00)
val KmOrangeLight = Color(0xFFFF8A00)
val KmOrangePale = Color(0xFFFFB066)
val KmOrangeMid = KmOrange

val KmBrandGradient = Brush.linearGradient(
    colors = listOf(KmOrangeDark, KmOrange, KmOrangeLight)
)

// ── Typography & Contrast (Strict WCAG AA Compliance) ───────────────────────
val KmTextPrimary = Color(0xFFF5F5F5)
val KmTextSecondary = Color(0xFFD4D4D4)
val KmTextMuted = Color(0xFF9A9A9A)
val KmTextDisabled = Color(0xFF555555)

// Strictly #0A0A0A on orange gradient (reaches 6.7:1 contrast ratio; NEVER white on orange)
val KmTextOnGradient = Color(0xFF0A0A0A)
val KmTextOnBrand = KmTextOnGradient

// ── Semantic-Only Status Colors (Never used decoratively) ───────────────────
// Green used EXCLUSIVELY for online status
val KmSuccess = Color(0xFF34D399)
val KmOnline = KmSuccess

// Amber used EXCLUSIVELY for warnings
val KmWarning = Color(0xFFFFC24D)

// Critical / Error
val KmError = Color(0xFFEF4444)
val KmInfo = Color(0xFF38BDF8)

// ── Dark Liquid Glass Tokens ───────────────────────────────────────────────
val KmGlassBackground = Color(0x0AFFFFFF)          // 4% white fill
val KmGlassSurfaceSecondary = KmGlassBackground
val KmGlassBorder = Color(0x1FFFFFFF)              // 12% white border (1 dp)
val KmGlassTopHighlight = Color(0x47FFFFFF)        // 28% white top highlight (1 dp)
val KmSolidSurfaceFallback = Color(0xFF111111)     // Reduce-transparency solid fallback

val KmOrangeGlow = Color(0x26FF5A00)
val KmOrangeGlowStrong = Color(0x4DFF5A00)

// ── Standard Design Radii ──────────────────────────────────────────────────
object KmRadii {
    val Button = 12.dp
    val Input = 12.dp
    val ListRow = 16.dp
    val Card = 18.dp
    val Container = 24.dp
    val BottomBar = 24.dp
}

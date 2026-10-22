package com.knowtomigrate.app.ui.theme

import androidx.compose.ui.graphics.Color

// KnowToMigrate AMOLED Base
val KmBlack = Color(0xFF000000)
val KmBlackSecondary = Color(0xFF050505)
val KmBlackTertiary = Color(0xFF080808)
val KmBlackCard = Color(0xFF0C0C0C)
val KmBlackElevated = Color(0xFF111111)

// Primary Orange Spectrum (used only for primary action & active state)
val KmOrangeDark = Color(0xFFFF4D00)
val KmOrange = Color(0xFFFF5A00)
val KmOrangeLight = Color(0xFFFF8A00)
val KmOrangePale = Color(0xFFFFB066)

// Typography & Contrast (WCAG AA compliant)
val KmTextPrimary = Color(0xFFF5F5F5)
val KmTextSecondary = Color(0xFFD4D4D4)
val KmTextMuted = Color(0xFF9A9A9A)
val KmTextDisabled = Color(0xFF555555)
val KmTextOnGradient = Color(0xFF0A0A0A)
val KmTextOnBrand = KmTextOnGradient

// Status Colors (Green used ONLY for online status, never decoration)
val KmSuccess = Color(0xFF34D399) // online
val KmOnline = KmSuccess
val KmWarning = Color(0xFFFFC24D) // warning
val KmError = Color(0xFFEF4444)
val KmInfo = Color(0xFF38BDF8)
val KmOrangeMid = KmOrange

// Liquid Glass System
val KmGlassBackground = Color(0x0AFFFFFF)       // 4% white fill
val KmGlassSurfaceSecondary = KmGlassBackground
val KmGlassBorder = Color(0x1FFFFFFF)           // 12% white border (1 dp)
val KmGlassTopHighlight = Color(0x47FFFFFF)     // 28% white top highlight (1 dp)
val KmOrangeGlow = Color(0x26FF5A00)
val KmOrangeGlowStrong = Color(0x4DFF5A00)

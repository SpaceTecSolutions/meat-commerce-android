package com.spacetecsolutions.meatapp.core.designsystem.theme

import androidx.compose.ui.graphics.Color

data class BrandColors(
    val primary: Color,
    val onPrimary: Color = Color.White,
    val secondary: Color,
    val onSecondary: Color = Color.White,
)

val DefaultBrandColors = BrandColors(
    primary = Color(0xFF8F1D2C),
    secondary = Color(0xFF6D4C41),
)
internal val LightBackground = Color(0xFFFAFAFA)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceVariant = Color(0xFFF7F7F7)
internal val LightOnSurface = Color(0xFF211A1B)
internal val LightOnSurfaceVariant = Color(0xFF6B6B6B)
internal val LightOutline = Color(0xFFE2E2E2)
internal val LightOutlineVariant = Color(0xFFEEEEEE)
internal val DarkBackground = Color(0xFF191113)
internal val DarkSurface = Color(0xFF211A1B)
internal val DarkOnSurface = Color(0xFFF0DEE0)

val Success = Color(0xFF247A42)
val Warning = Color(0xFF8A5A00)
val Info = Color(0xFF315D9B)
val SaleRed = Color(0xFFC51F2A)
val SoftRed = Color(0xFFFFEDEE)
val SoftGreen = Color(0xFFEAF6EC)
val SoftAmber = Color(0xFFFFF3DB)
val PromoBackground = Color(0xFF281719)

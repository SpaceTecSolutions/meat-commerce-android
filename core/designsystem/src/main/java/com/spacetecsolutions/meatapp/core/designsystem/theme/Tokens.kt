package com.spacetecsolutions.meatapp.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AppSpacing {
    val hairline = 2.dp
    val extraSmall = 4.dp
    val small = 8.dp
    val compact = 12.dp
    val medium = 16.dp
    val comfortable = 20.dp
    val large = 24.dp
    val extraLarge = 32.dp
}

object AppDimensions {
    val minimumTouchTarget = 48.dp
    val iconSmall = 20.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp
    val feedbackIcon = 64.dp
    val contentMaxWidth = 720.dp
    val formMaxWidth = 460.dp
    val dashboardMaxWidth = 1040.dp
    val expandedContentBreakpoint = 600.dp
    val categoryCardMinWidth = 148.dp
    val categoryGridMaxWidth = 880.dp
    val stateIllustration = 180.dp
    val mapHeight = 240.dp
}

object AppShapes {
    val extraSmall = RoundedCornerShape(8.dp)
    val small = RoundedCornerShape(10.dp)
    val medium = RoundedCornerShape(14.dp)
    val large = RoundedCornerShape(20.dp)
    val extraLarge = RoundedCornerShape(28.dp)
}

object AppMotion {
    const val QUICK_MILLIS = 120
    const val MICRO_MILLIS = 160
    const val STANDARD_MILLIS = 220
    const val SCREEN_MILLIS = 260
}

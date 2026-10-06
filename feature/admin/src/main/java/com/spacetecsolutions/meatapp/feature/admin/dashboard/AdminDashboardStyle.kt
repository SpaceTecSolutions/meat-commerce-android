package com.spacetecsolutions.meatapp.feature.admin.dashboard

import androidx.compose.foundation.shape.RoundedCornerShape
import com.spacetecsolutions.meatapp.core.designsystem.theme.AdminDashboardTokens
import androidx.compose.ui.unit.dp

/** Scoped Stitch tokens. The global app theme and other Admin screens remain untouched. */
internal object DashboardStyle {
    val canvas = AdminDashboardTokens.canvas
    val card = AdminDashboardTokens.card
    val inset = AdminDashboardTokens.inset
    val border = AdminDashboardTokens.border
    val ink = AdminDashboardTokens.ink
    val muted = AdminDashboardTokens.muted
    val red = AdminDashboardTokens.red
    val paleRed = AdminDashboardTokens.paleRed
    val metricShape = RoundedCornerShape(16.dp)
    val analyticsShape = RoundedCornerShape(24.dp)
    val maxWidth = 520.dp
}

package com.spacetecsolutions.meatapp.feature.admin.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.theme.AdminDashboardTokens

/** Stitch treatment scoped to Admin Reports; shared Dashboard palette stays unchanged. */
internal object ReportsStyle {
    val canvas = AdminDashboardTokens.canvas
    val surface = AdminDashboardTokens.card
    val border = AdminDashboardTokens.border
    val ink = AdminDashboardTokens.ink
    val muted = Color(0xFF64748B)
    val red = AdminDashboardTokens.red
    val rose = Color(0xFFFFF1F2)
    val inset = AdminDashboardTokens.inset
    val cardShape = RoundedCornerShape(16.dp)
    val chartShape = RoundedCornerShape(20.dp)
    val pillShape = RoundedCornerShape(50)
    val cardBorder = BorderStroke(1.dp, border)
}

@Composable
internal fun ReportsSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = ReportsStyle.cardShape,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth().background(ReportsStyle.surface, shape)
        .border(ReportsStyle.cardBorder, shape), content = content)
}

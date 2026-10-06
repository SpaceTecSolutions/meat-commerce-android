package com.spacetecsolutions.meatapp.core.designsystem.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

enum class AppWindowWidth { COMPACT, MEDIUM, EXPANDED }

data class AdaptiveInfo(
    val widthClass: AppWindowWidth,
    val horizontalPadding: Dp,
    val gridMinimumCellWidth: Dp,
)

@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 960.dp,
    content: @Composable (AdaptiveInfo) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val info = adaptiveInfo(maxWidth)
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().widthIn(max = maxContentWidth)
                .padding(horizontal = info.horizontalPadding),
        ) {
            content(info)
        }
    }
}

fun adaptiveInfo(availableWidth: Dp): AdaptiveInfo = when {
    availableWidth < 600.dp -> AdaptiveInfo(AppWindowWidth.COMPACT, AppSpacing.medium, 160.dp)
    availableWidth < 840.dp -> AdaptiveInfo(AppWindowWidth.MEDIUM, AppSpacing.large, 200.dp)
    else -> AdaptiveInfo(AppWindowWidth.EXPANDED, AppSpacing.extraLarge, 220.dp)
}

package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes

@Composable
internal fun CustomerHomeSkeleton() {
    Column(Modifier.fillMaxSize().widthIn(max = AppDimensions.contentMaxWidth)
        .wrapContentWidth(Alignment.CenterHorizontally).padding(16.dp)
        .clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HomeSkeletonBlock(Modifier.fillMaxWidth().height(176.dp))
        HomeSkeletonBlock(Modifier.fillMaxWidth(.34f).height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { HomeSkeletonBlock(Modifier.size(56.dp)) }
        }
        HomeSkeletonBlock(Modifier.fillMaxWidth(.34f).height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { HomeSkeletonBlock(Modifier.width(146.dp).height(160.dp)) }
        }
    }
}

@Composable
private fun HomeSkeletonBlock(modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, AppShapes.small))
}

package com.spacetecsolutions.meatapp.feature.superadmin.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

private data class QuickAccessItem(
    val label: String,
    val icon: ImageVector,
    val action: DashboardQuickAction,
)

@Composable
fun QuickAccessSection(
    columns: Int,
    onAction: (DashboardQuickAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = listOf(
        QuickAccessItem("Admins", AppIcons.Admin, DashboardQuickAction.ADMINS),
        QuickAccessItem("Reports", AppIcons.Reports, DashboardQuickAction.REPORTS),
        QuickAccessItem("Product Limit", AppIcons.ProductLimit, DashboardQuickAction.PRODUCT_LIMIT),
        QuickAccessItem(
            "Feature Management",
            AppIcons.FeatureConfiguration,
            DashboardQuickAction.FEATURE_MANAGEMENT,
        ),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        Text("Quick access", style = MaterialTheme.typography.titleMedium)
        actions.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            ) {
                rowItems.forEach { item ->
                    QuickAccessCard(item, onAction, Modifier.weight(1f))
                }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    item: QuickAccessItem,
    onAction: (DashboardQuickAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.heightIn(min = AppSpacing.extraLarge * 2.5f)
            .clickable(onClickLabel = "Open ${item.label}") { onAction(item.action) },
        shape = AppShapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppSpacing.medium),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(
                item.label,
                modifier = Modifier.padding(top = AppSpacing.small),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

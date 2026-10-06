package com.spacetecsolutions.meatapp.feature.superadmin.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.spacetecsolutions.meatapp.core.designsystem.component.AppTopBar
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

@Composable
fun SuperAdminDashboardScreen(
    state: DashboardUiState,
    onQuickAction: (DashboardQuickAction) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        AppTopBar(title = "Dashboard")
        when (state) {
            DashboardUiState.Loading -> ContentStateView(ContentState.Loading)
            DashboardUiState.Offline -> ContentStateView(
                ContentState.Offline(),
                onAction = onRetry,
            )
            is DashboardUiState.Error -> ContentStateView(
                ContentState.Error(description = state.message),
                onAction = onRetry,
            )
            is DashboardUiState.Content -> DashboardContent(state.data, onQuickAction)
        }
    }
}

@Composable
private fun DashboardContent(
    data: SuperAdminDashboardData,
    onQuickAction: (DashboardQuickAction) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val expanded = maxWidth >= AppDimensions.expandedContentBreakpoint
        val metrics = dashboardMetrics(data)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = AppDimensions.dashboardMaxWidth),
            contentPadding = PaddingValues(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            item { MetricGrid(metrics, columns = if (expanded) 3 else 2) }
            item {
                if (expanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                        RevenueChart(data.revenuePoints, data.revenueLabels, Modifier.weight(1.6f))
                        PaymentDistribution(data.paymentMethods, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                        RevenueChart(data.revenuePoints, data.revenueLabels)
                        PaymentDistribution(data.paymentMethods)
                    }
                }
            }
            item {
                QuickAccessSection(
                    columns = if (expanded) 4 else 2,
                    onAction = onQuickAction,
                )
            }
        }
    }
}

private fun dashboardMetrics(data: SuperAdminDashboardData) = listOf(
    DashboardMetric("Total Revenue", data.totalRevenue, AppIcons.Revenue),
    DashboardMetric("Revenue this month", data.monthlyRevenue, AppIcons.Analytics),
    DashboardMetric("Total Orders", data.totalOrders, AppIcons.Orders),
    DashboardMetric("Pending Orders", data.pendingOrders, AppIcons.Pending),
    DashboardMetric("Total Customers", data.totalCustomers, AppIcons.Customers),
    DashboardMetric("Active Customers", data.activeCustomers, AppIcons.Users),
    DashboardMetric("New Customers", data.newCustomers, AppIcons.Add),
    DashboardMetric("Total Products", data.totalProducts, AppIcons.Products),
    DashboardMetric("Current product limit", data.currentProductLimit, AppIcons.ProductLimit),
)

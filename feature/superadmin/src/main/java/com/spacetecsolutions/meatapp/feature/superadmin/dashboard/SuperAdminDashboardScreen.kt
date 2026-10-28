package com.spacetecsolutions.meatapp.feature.superadmin.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.designsystem.component.AppTopBar
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

@Composable
fun SuperAdminDashboardRoute(
    onQuickAction: (DashboardQuickAction) -> Unit,
    viewModel: SuperAdminDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SuperAdminDashboardScreen(state, onQuickAction, viewModel::refresh)
}

@Composable
private fun SuperAdminDashboardScreen(
    state: DashboardUiState,
    onQuickAction: (DashboardQuickAction) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        AppTopBar(title = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_dashboard_d87f47b))
        when (state) {
            DashboardUiState.Loading -> ContentStateView(ContentState.Loading)
            is DashboardUiState.Error -> ContentStateView(
                if (state.error is AppError.Network || state.error is AppError.Offline || state.error is AppError.Timeout)
                    ContentState.Offline(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_dashboard_connection_error))
                else if (state.error is AppError.Forbidden || state.error is AppError.Unauthorized)
                    ContentState.Error(description = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_dashboard_access_error))
                else ContentState.Error(description = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_dashboard_load_error)),
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

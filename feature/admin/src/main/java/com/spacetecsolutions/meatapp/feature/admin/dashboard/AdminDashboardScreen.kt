package com.spacetecsolutions.meatapp.feature.admin.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.model.AdminDashboardPeriod

@Composable
fun AdminDashboardRoute(
    onMenu: () -> Unit = {}, onNotifications: () -> Unit = {}, onReports: () -> Unit = {},
    onOrders: () -> Unit = {}, onProducts: () -> Unit = {},
    showNotifications: Boolean = true,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AdminDashboardScreen(state, onNotifications, onReports, onOrders, onProducts, showNotifications,
        viewModel::selectPeriod, viewModel::refresh)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDashboardScreen(
    state: AdminDashboardUiState, onNotifications: () -> Unit,
    onReports: () -> Unit, onOrders: () -> Unit, onProducts: () -> Unit,
    showNotifications: Boolean,
    onPeriod: (AdminDashboardPeriod) -> Unit, onRefresh: () -> Unit,
) {
    val dashboard = state.dashboard
    Scaffold(
        containerColor = DashboardStyle.canvas,
    ) { padding ->
        when {
            state.loading && dashboard == null -> DashboardLoadingState(Modifier.padding(padding))
            dashboard == null -> ContentStateView(
                if (state.error?.contains("connection", true) == true) ContentState.Offline(state.error)
                else ContentState.Error(description = state.error), contentPadding = padding, onAction = onRefresh,
            )
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing, onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = DashboardStyle.maxWidth),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    item { AdminDashboardHeader(dashboard.unreadNotificationCount, onNotifications, showNotifications) }
                    item { DashboardMetricGrid(dashboard, onReports, onOrders) }
                    item { RevenueOverviewCard(dashboard, state.selectedPeriod, onPeriod) }
                    item { TopSellingProductSection(dashboard.topSellingProducts, dashboard.currencyCode, onProducts) }
                }
            }
        }
    }
}

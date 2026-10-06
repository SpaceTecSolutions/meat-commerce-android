package com.spacetecsolutions.meatapp.feature.superadmin.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.AppTopBar
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.model.ReportPeriodType
import com.spacetecsolutions.meatapp.core.model.SuperAdminReport

@Composable
fun SuperAdminReportsRoute(viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SuperAdminReportsScreen(
        state = state,
        onPeriodSelected = viewModel::selectPeriod,
        onStartChange = viewModel::updateStartDate,
        onEndChange = viewModel::updateEndDate,
        onApplyCustom = viewModel::applyCustomRange,
        onRefresh = viewModel::refresh,
    )
}

@Composable
private fun SuperAdminReportsScreen(
    state: ReportsUiState,
    onPeriodSelected: (ReportPeriodType) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onApplyCustom: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Revenue Reports",
            actionIcon = AppIcons.Calendar,
            actionDescription = "Custom report range",
            onAction = { onPeriodSelected(ReportPeriodType.CUSTOM) },
        )
        when {
            state.loading && state.report == null -> ContentStateView(ContentState.Loading)
            state.report == null -> ContentStateView(
                ContentState.Error(description = state.error),
                onAction = onRefresh,
            )
            else -> ReportContent(
                state,
                onPeriodSelected,
                onStartChange,
                onEndChange,
                onApplyCustom,
            )
        }
    }
}

@Composable
private fun ReportContent(
    state: ReportsUiState,
    onPeriodSelected: (ReportPeriodType) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onApplyCustom: () -> Unit,
) {
    val report = state.report ?: return
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val expanded = maxWidth >= AppDimensions.expandedContentBreakpoint
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = AppDimensions.dashboardMaxWidth),
            contentPadding = PaddingValues(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            item {
                ReportPeriodSelector(
                    state,
                    onPeriodSelected,
                    onStartChange,
                    onEndChange,
                    onApplyCustom,
                )
            }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        report.periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatMoney(report.revenueMinor, report.currencyCode),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        )
                        Text(
                            "Total Revenue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { ReportRevenueChart(report.revenueSeries) }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(AppSpacing.medium))
                ReportMetricGrid(report.primaryMetrics(), 3)
            }
            item { ReportMetricGrid(report.secondaryMetrics(), if (expanded) 4 else 2) }
            item { BestSellingProductsCard(report.bestSellingProducts, report.currencyCode) }
        }
    }
}

private fun SuperAdminReport.primaryMetrics() = listOf(
    ReportMetric("Orders", orders.toString()),
    ReportMetric("Avg. Order Value", formatMoney(averageOrderValueMinor, currencyCode)),
    ReportMetric("Customers", customers.toString()),
)

private fun SuperAdminReport.secondaryMetrics() = listOf(
    ReportMetric("Delivered", delivered.toString()),
    ReportMetric("Cancelled", cancelled.toString()),
    ReportMetric("New customers", newCustomers.toString()),
    ReportMetric("Active customers", activeCustomers.toString()),
)

package com.spacetecsolutions.meatapp.feature.admin.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.domain.repository.AdminReportsRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminReportsState(
    val loading: Boolean = true,
    val filter: ReportFilter = ReportFilter(ReportPeriodType.MONTHLY, LocalDate.now().toString()),
    val report: AdminReport? = null,
    val error: String? = null,
    val filterError: String? = null,
)

@HiltViewModel
class AdminReportsViewModel @Inject constructor(
    private val reports: AdminReportsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminReportsState())
    val state = mutableState.asStateFlow()
    private var reportJob: Job? = null

    init {
        loadReport()
    }

    fun applyFilter(
        period: ReportPeriodType,
        productId: String?,
        startDate: String?,
        endDate: String?,
    ): Boolean {
        val error = if (period == ReportPeriodType.CUSTOM) validateRange(startDate, endDate) else null
        if (error != null) { mutableState.update { it.copy(filterError = error) }; return false }
        mutableState.update {
            it.copy(filter = it.filter.copy(period = period, productId = productId,
                startDateIso = startDate, endDateIso = endDate), filterError = null)
        }
        loadReport()
        return true
    }

    fun movePeriod(amount: Long) {
        val current = state.value.filter
        if (current.period == ReportPeriodType.CUSTOM) return
        val anchor = LocalDate.parse(current.anchorDateIso)
        val next = when (current.period) {
            ReportPeriodType.WEEKLY -> anchor.plusWeeks(amount)
            ReportPeriodType.MONTHLY, ReportPeriodType.CURRENT_MONTH_BY_WEEK -> anchor.plusMonths(amount)
            ReportPeriodType.YEARLY -> anchor.plusYears(amount)
            ReportPeriodType.CUSTOM -> anchor
        }
        mutableState.update { it.copy(filter = current.copy(anchorDateIso = next.toString())) }
        loadReport()
    }

    fun refresh() = loadReport()

    private fun loadReport() {
        val filter = state.value.filter
        reportJob?.cancel()
        reportJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            val request = ReportRequest(filter.period, filter.startDateIso, filter.endDateIso,
                filter.anchorDateIso, filter.productId)
            val result = reports.getReport(request)
            if (filter != state.value.filter) return@launch
            when (result) {
                is AppResult.Success -> mutableState.update { it.copy(loading = false, report = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error.reportText()) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsRoute(
    back: () -> Unit,
    viewModel: AdminReportsViewModel = hiltViewModel(),
) {
    HideAppBottomBar()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filtersOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val report = state.report
        if (uri != null && report != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(report.excelCsv()) }
        }
    }
    Column(Modifier.fillMaxSize().background(ReportsStyle.canvas)) {
        ReportsTopBar(back, state.periodControlLabel(), { filtersOpen = true }, state.report?.let {
            { exportLauncher.launch("meatbush-${state.filter.period.name.lowercase()}-report.csv") }
        })
        when {
            state.loading && state.report == null -> ContentStateView(ContentState.Loading)
            state.report == null -> ContentStateView(
                if (state.error?.contains("connection", true) == true) ContentState.Offline(description = state.error)
                else ContentState.Error(description = state.error), onAction = viewModel::refresh,
            )
            else -> PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) { AdminReportContent(state, viewModel::movePeriod) }
        }
    }
    if (filtersOpen) ReportFilterBottomSheet(
        state = state,
        dismiss = { filtersOpen = false },
        apply = { period, product, start, end ->
            if (viewModel.applyFilter(period, product, start, end)) filtersOpen = false
        },
    )
}

private fun AdminReport.excelCsv(): String = buildString {
    fun row(vararg values: Any?) { append(values.joinToString(",") { value ->
        "\"${value.toString().replace("\"", "\"\"")}\"" }); appendLine() }
    row("MeatBush Sales Report", periodLabel)
    row("Metric", "Value")
    row("Revenue (${currencyCode})", revenueMinor / 100.0)
    row("Orders", orders); row("Completed orders", completedOrders); row("Cancelled orders", cancelledOrders)
    row("Customers", customers); row("Average order value", averageOrderValueMinor / 100.0)
    appendLine(); row("Sales period", "Revenue (${currencyCode})")
    revenueSeries.forEach { row(it.label, it.revenueMinor / 100.0) }
    appendLine(); row("Top product", "Quantity", "Unit", "Revenue (${currencyCode})", "Orders")
    topProducts.forEach { row(it.productName, it.quantitySold, it.unit, it.revenueMinor / 100.0, it.orderCount) }
}

@Composable
private fun AdminReportContent(
    state: AdminReportsState,
    movePeriod: (Long) -> Unit,
) {
    val report = state.report ?: return
    AnimatedContent(state.filter to report, label = "report content") { (_, currentReport) ->
        LazyColumn(
            Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = AppDimensions.dashboardMaxWidth),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PeriodNavigation(currentReport.periodLabel, state.filter.period, movePeriod) }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item { ReportMetricGrid(currentReport, false) }
            item { SalesBarChart(currentReport.revenueSeries, currentReport.currencyCode, state.filter.period) }
            item { TopProductsSection(currentReport.topProducts, currentReport.currencyCode) }
        }
    }
}

private fun AdminReportsState.periodControlLabel(): String {
    val anchor = LocalDate.parse(filter.anchorDateIso)
    val today = LocalDate.now()
    return when (filter.period) {
    ReportPeriodType.WEEKLY -> if (anchor.sameWeek(today)) "This Week" else report?.periodLabel ?: "Selected Week"
    ReportPeriodType.MONTHLY, ReportPeriodType.CURRENT_MONTH_BY_WEEK ->
        if (anchor.year == today.year && anchor.month == today.month) "This Month"
        else anchor.format(DateTimeFormatter.ofPattern("MMM yyyy"))
    ReportPeriodType.YEARLY -> if (anchor.year == today.year) "This Year" else anchor.year.toString()
    ReportPeriodType.CUSTOM -> "Custom Date"
}
}

private fun LocalDate.sameWeek(other: LocalDate): Boolean {
    val fields = WeekFields.of(Locale.getDefault())
    return year == other.year && get(fields.weekOfWeekBasedYear()) == other.get(fields.weekOfWeekBasedYear())
}

private fun validateRange(start: String?, end: String?) = runCatching {
    if (start == null || end == null) return@runCatching "Select both start and end dates"
    val from = LocalDate.parse(start); val to = LocalDate.parse(end)
    when {
        to.isBefore(from) -> "End date must be on or after start date"
        ChronoUnit.DAYS.between(from, to) > 365 -> "Date range cannot exceed 366 days"
        else -> null
    }
}.getOrElse { "Select a valid date range" }

private fun AppError.reportText() = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "Only an authorized Admin can view reports"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Unable to load business reports"
}

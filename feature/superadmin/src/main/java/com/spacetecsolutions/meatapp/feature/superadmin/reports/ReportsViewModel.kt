package com.spacetecsolutions.meatapp.feature.superadmin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.SuperAdminReportsRepository
import com.spacetecsolutions.meatapp.core.model.ReportPeriodType
import com.spacetecsolutions.meatapp.core.model.ReportRequest
import com.spacetecsolutions.meatapp.core.model.SuperAdminReport
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: SuperAdminReportsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ReportsUiState())
    val state = mutableState.asStateFlow()
    private val cache = mutableMapOf<ReportRequest, SuperAdminReport>()
    private var loadJob: Job? = null

    init { load(ReportRequest(ReportPeriodType.MONTHLY)) }

    fun selectPeriod(period: ReportPeriodType) {
        mutableState.update { it.copy(selectedPeriod = period, rangeError = null, error = null) }
        if (period != ReportPeriodType.CUSTOM) load(ReportRequest(period))
    }

    fun updateStartDate(value: String) = mutableState.update {
        it.copy(startDate = value.take(10), rangeError = null)
    }
    fun updateEndDate(value: String) = mutableState.update {
        it.copy(endDate = value.take(10), rangeError = null)
    }

    fun applyCustomRange() {
        val current = state.value
        val validation = validateRange(current.startDate, current.endDate)
        if (validation != null) {
            mutableState.update { it.copy(rangeError = validation) }
        } else {
            load(ReportRequest(ReportPeriodType.CUSTOM, current.startDate, current.endDate))
        }
    }

    fun refresh() {
        val current = state.value
        if (current.selectedPeriod == ReportPeriodType.CUSTOM) {
            val validation = validateRange(current.startDate, current.endDate)
            if (validation == null) load(
                ReportRequest(ReportPeriodType.CUSTOM, current.startDate, current.endDate),
                force = true,
            ) else mutableState.update { it.copy(rangeError = validation) }
        } else load(ReportRequest(current.selectedPeriod), force = true)
    }

    private fun load(request: ReportRequest, force: Boolean = false) {
        if (!force) cache[request]?.let { cached ->
            mutableState.update { it.copy(report = cached, loading = false, error = null) }
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            when (val result = repository.getReport(request)) {
                is AppResult.Success -> {
                    cache[request] = result.value
                    mutableState.update { it.copy(report = result.value, loading = false) }
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(loading = false, error = result.error.message())
                }
            }
        }
    }

    private fun validateRange(startValue: String, endValue: String): String? = try {
        val start = LocalDate.parse(startValue)
        val end = LocalDate.parse(endValue)
        when {
            end.isBefore(start) -> "End date must be on or after start date"
            ChronoUnit.DAYS.between(start, end) > MAX_CUSTOM_DAYS ->
                "Custom range cannot exceed ${MAX_CUSTOM_DAYS + 1} days"
            else -> null
        }
    } catch (_: DateTimeParseException) {
        "Enter dates as YYYY-MM-DD"
    }

    private companion object { const val MAX_CUSTOM_DAYS = 365L }
}

private fun AppError.message(): String = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "Only an authorized Super Admin can view reports"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    AppError.TooManyRequests -> "Report service is busy. Try again shortly"
    is AppError.Validation -> message
    else -> "Unable to load this report. Try again"
}

package com.spacetecsolutions.meatapp.feature.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.AdminDashboardRepository
import com.spacetecsolutions.meatapp.core.model.AdminDashboardPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val dashboardRepository: AdminDashboardRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminDashboardUiState())
    val state = mutableState.asStateFlow()
    private var requestVersion = 0

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        val version = ++requestVersion
        mutableState.update {
            it.copy(loading = it.dashboard == null, refreshing = it.dashboard != null, error = null)
        }
        val result = dashboardRepository.getDashboard(mutableState.value.selectedPeriod)
        if (version != requestVersion) return@launch
        when (result) {
            is AppResult.Success -> mutableState.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    dashboard = result.value,
                    error = null,
                )
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, refreshing = false, error = result.error.message())
            }
        }
    }

    fun selectPeriod(period: AdminDashboardPeriod) {
        if (period == mutableState.value.selectedPeriod) return
        mutableState.update { it.copy(selectedPeriod = period) }
        refresh()
    }
}

private fun AppError.message(): String = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "Only an authorized Admin can view this dashboard"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    else -> "Unable to load the dashboard. Try again"
}

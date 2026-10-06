package com.spacetecsolutions.meatapp.feature.admin.dashboard

import com.spacetecsolutions.meatapp.core.model.AdminDashboard
import com.spacetecsolutions.meatapp.core.model.AdminDashboardPeriod

data class AdminDashboardUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val dashboard: AdminDashboard? = null,
    val selectedPeriod: AdminDashboardPeriod = AdminDashboardPeriod.THIS_WEEK,
    val error: String? = null,
)

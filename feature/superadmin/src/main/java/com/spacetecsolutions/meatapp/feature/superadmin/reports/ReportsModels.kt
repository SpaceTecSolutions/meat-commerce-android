package com.spacetecsolutions.meatapp.feature.superadmin.reports

import com.spacetecsolutions.meatapp.core.model.ReportPeriodType
import com.spacetecsolutions.meatapp.core.model.SuperAdminReport

data class ReportsUiState(
    val selectedPeriod: ReportPeriodType = ReportPeriodType.MONTHLY,
    val startDate: String = "",
    val endDate: String = "",
    val rangeError: String? = null,
    val loading: Boolean = true,
    val report: SuperAdminReport? = null,
    val error: String? = null,
)

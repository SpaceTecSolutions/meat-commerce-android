package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.AdminReport
import com.spacetecsolutions.meatapp.core.model.ReportRequest

interface AdminReportsRepository {
    suspend fun getReport(request: ReportRequest): AppResult<AdminReport>
}

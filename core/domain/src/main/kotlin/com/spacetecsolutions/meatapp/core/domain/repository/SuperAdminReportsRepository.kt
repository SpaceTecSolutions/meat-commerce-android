package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.ReportRequest
import com.spacetecsolutions.meatapp.core.model.SuperAdminReport

interface SuperAdminReportsRepository {
    suspend fun getReport(request: ReportRequest): AppResult<SuperAdminReport>
}

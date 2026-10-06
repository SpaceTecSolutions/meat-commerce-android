package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.AdminDashboard
import com.spacetecsolutions.meatapp.core.model.AdminDashboardPeriod

interface AdminDashboardRepository {
    suspend fun getDashboard(period: AdminDashboardPeriod): AppResult<AdminDashboard>
}

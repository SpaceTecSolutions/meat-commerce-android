package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.SuperAdminDashboard

interface SuperAdminDashboardRepository {
    suspend fun getDashboard(): AppResult<SuperAdminDashboard>
}

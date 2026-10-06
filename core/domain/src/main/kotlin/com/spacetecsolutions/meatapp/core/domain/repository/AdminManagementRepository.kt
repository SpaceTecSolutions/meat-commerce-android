package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CreateAdminRequest
import com.spacetecsolutions.meatapp.core.model.UpdateAdminRequest
import com.spacetecsolutions.meatapp.core.model.User

interface AdminManagementRepository {
    suspend fun getAdmins(): AppResult<List<User>>
    suspend fun createAdmin(request: CreateAdminRequest): AppResult<User>
    suspend fun updateAdmin(request: UpdateAdminRequest): AppResult<User>
    suspend fun setAdminActive(userId: String, active: Boolean): AppResult<User>
}

package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CustomerHomeData

interface CustomerHomeRepository {
    suspend fun updateProfile(firstName: String, lastName: String): AppResult<Unit>
    suspend fun deleteAccount(): AppResult<Unit>
    suspend fun getHome(): AppResult<CustomerHomeData>
}

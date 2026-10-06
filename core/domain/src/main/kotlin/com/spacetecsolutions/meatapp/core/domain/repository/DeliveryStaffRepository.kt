package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CreateDeliveryStaffRequest
import com.spacetecsolutions.meatapp.core.model.UpdateDeliveryStaffRequest
import com.spacetecsolutions.meatapp.core.model.User

interface DeliveryStaffRepository {
    suspend fun getStaff(): AppResult<List<User>>
    suspend fun create(request: CreateDeliveryStaffRequest): AppResult<User>
    suspend fun update(request: UpdateDeliveryStaffRequest): AppResult<User>
    suspend fun setActive(userId: String, active: Boolean): AppResult<User>
}

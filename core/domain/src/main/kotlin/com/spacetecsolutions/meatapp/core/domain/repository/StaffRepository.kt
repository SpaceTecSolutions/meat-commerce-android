package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.StaffPermission
import com.spacetecsolutions.meatapp.core.model.User

data class StaffInput(val userId: String? = null, val displayName: String, val mobileNumber: String,
    val email: String?, val active: Boolean, val permissions: Set<StaffPermission>)

interface StaffRepository {
    suspend fun getStaff(): AppResult<List<User>>
    suspend fun saveStaff(input: StaffInput): AppResult<User>
}

package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeAuthenticatedUser(): Flow<AppResult<User?>>
    suspend fun getUser(userId: String): AppResult<User>
}

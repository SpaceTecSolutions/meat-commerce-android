package com.spacetecsolutions.meatapp.core.domain.service

import com.spacetecsolutions.meatapp.core.common.result.AppResult

interface AppCheckManager {
    fun initialize()
    suspend fun getToken(forceRefresh: Boolean = false): AppResult<String>
}

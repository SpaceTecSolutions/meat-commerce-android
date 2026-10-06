package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.ShopSupport

interface ShopSupportRepository {
    suspend fun getSupport(): AppResult<ShopSupport>
}

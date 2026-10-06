package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.ProductLimitStatus

interface ProductLimitRepository {
    suspend fun getStatus(): AppResult<ProductLimitStatus>
    suspend fun updateLimit(maxProducts: Int, expectedRevision: Long): AppResult<ProductLimitStatus>
}

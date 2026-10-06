package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.DeliveryConfig

interface DeliveryConfigurationRepository {
    suspend fun get(): AppResult<DeliveryConfig>
    suspend fun update(config: DeliveryConfig): AppResult<DeliveryConfig>
}

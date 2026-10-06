package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.FeatureConfig
import kotlinx.coroutines.flow.StateFlow

interface FeatureManagementRepository {
    val featureConfig: StateFlow<AppResult<FeatureConfig>?>
    suspend fun refresh(): AppResult<FeatureConfig>
    suspend fun update(config: FeatureConfig): AppResult<FeatureConfig>
}

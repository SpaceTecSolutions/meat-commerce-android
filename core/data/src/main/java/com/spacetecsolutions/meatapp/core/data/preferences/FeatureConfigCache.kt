package com.spacetecsolutions.meatapp.core.data.preferences

import com.spacetecsolutions.meatapp.core.model.FeatureConfig
import kotlinx.coroutines.flow.Flow

interface FeatureConfigCache {
    val config: Flow<FeatureConfig?>
    suspend fun save(config: FeatureConfig)
}

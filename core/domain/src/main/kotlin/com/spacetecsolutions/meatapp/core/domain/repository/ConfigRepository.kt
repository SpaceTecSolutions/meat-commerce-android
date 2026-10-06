package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.DeliveryConfig
import com.spacetecsolutions.meatapp.core.model.FeatureConfig
import com.spacetecsolutions.meatapp.core.model.PaymentConfig
import com.spacetecsolutions.meatapp.core.model.ShopConfig
import kotlinx.coroutines.flow.Flow

interface ConfigRepository {
    fun observeFeatureConfig(): Flow<AppResult<FeatureConfig>>
    fun observeShopConfig(): Flow<AppResult<ShopConfig>>
    fun observePaymentConfig(): Flow<AppResult<PaymentConfig>>
    fun observeDeliveryConfig(): Flow<AppResult<DeliveryConfig>>
}

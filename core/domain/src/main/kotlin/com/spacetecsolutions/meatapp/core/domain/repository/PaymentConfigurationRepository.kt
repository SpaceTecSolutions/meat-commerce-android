package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.PaymentConfig

interface PaymentConfigurationRepository {
    suspend fun get(): AppResult<PaymentConfig>
    suspend fun update(config: PaymentConfig): AppResult<PaymentConfig>
    suspend fun verifyRazorpay(): AppResult<Boolean>
}

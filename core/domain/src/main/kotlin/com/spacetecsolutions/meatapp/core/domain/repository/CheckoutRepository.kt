package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CheckoutQuote
import com.spacetecsolutions.meatapp.core.model.PlaceOrderRequest
import com.spacetecsolutions.meatapp.core.model.PlacedOrder

interface CheckoutRepository {
    suspend fun prepare(): AppResult<CheckoutQuote>
    suspend fun placeOrder(request: PlaceOrderRequest): AppResult<PlacedOrder>
    suspend fun reconcileOrder(idempotencyKey: String): AppResult<PlacedOrder?>
}

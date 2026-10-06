package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CodActor
import com.spacetecsolutions.meatapp.core.model.DeliveryLocation
import com.spacetecsolutions.meatapp.core.model.DeliveryRoute
import kotlinx.coroutines.flow.Flow

interface LiveTrackingRepository {
    suspend fun activate(actor: CodActor, orderId: String, sessionId: String): AppResult<Unit>
    suspend fun publish(sessionId: String, location: DeliveryLocation): AppResult<Unit>
    fun observeCustomerLocation(orderId: String, sessionId: String): Flow<AppResult<DeliveryLocation>>
    suspend fun getCustomerRoute(orderId: String): AppResult<DeliveryRoute>
    suspend fun stop(orderId: String, sessionId: String): AppResult<Unit>
}

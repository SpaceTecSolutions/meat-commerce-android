package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CodActor
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.FinalWeightAdjustment
import com.spacetecsolutions.meatapp.core.model.OrderSlotOptions
import kotlinx.coroutines.flow.Flow

interface CodOrderRepository {
    fun observeCustomerOrders(): Flow<AppResult<List<CodOrder>>>
    suspend fun getCustomerDeliveryOtp(orderId: String): AppResult<com.spacetecsolutions.meatapp.core.model.DeliveryOtp>
    suspend fun verifyDeliveryOtp(orderId: String, code: String): AppResult<Unit>
    suspend fun getCustomerOrders(): AppResult<List<CodOrder>>
    suspend fun reorderCustomerOrder(orderId: String): AppResult<Int>
    suspend fun cancelCustomerOrder(
        orderId: String, expectedRevision: Long, reason: String,
    ): AppResult<CodOrder>
    suspend fun getOrders(actor: CodActor): AppResult<List<CodOrder>>
    suspend fun confirm(
        orderId: String,
        expectedRevision: Long,
    ): AppResult<CodOrder>
    suspend fun startPreparing(orderId: String, expectedRevision: Long): AppResult<CodOrder>
    suspend fun markReadyForDelivery(orderId: String, expectedRevision: Long,
        adjustments: List<FinalWeightAdjustment>): AppResult<CodOrder>
    suspend fun getOrderSlotOptions(): AppResult<OrderSlotOptions>
    suspend fun updateOrderDeliverySlot(orderId: String, expectedRevision: Long,
        deliveryDateId: String, deliverySlotId: String, reason: String): AppResult<CodOrder>
    suspend fun cancel(orderId: String, expectedRevision: Long, reason: String): AppResult<CodOrder>
    suspend fun startPersonalDelivery(orderId: String, expectedRevision: Long): AppResult<CodOrder>
    suspend fun assignDeliveryUser(
        orderId: String, deliveryUserId: String, expectedRevision: Long,
    ): AppResult<CodOrder>
    suspend fun startDelivery(
        actor: CodActor, orderId: String, expectedRevision: Long,
    ): AppResult<CodOrder>
    suspend fun completeDelivery(
        actor: CodActor, orderId: String, paymentMethod: com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod,
        collectedMinor: Long?, expectedRevision: Long,
    ): AppResult<CodOrder>
    suspend fun reportAmountMismatch(
        actor: CodActor, orderId: String, actualMinor: Long, expectedRevision: Long,
    ): AppResult<CodOrder>
}

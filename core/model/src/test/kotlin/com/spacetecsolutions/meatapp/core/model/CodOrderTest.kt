package com.spacetecsolutions.meatapp.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodOrderTest {
    @Test
    fun `tracking requires every gate`() {
        val eligible = order(OrderStatus.OUT_FOR_DELIVERY, PaymentStatus.PENDING).copy(
            adminDeliveringPersonally = true,
            deliveryStartedByUserId = "admin-1",
            trackingLifecycle = DeliveryTrackingLifecycle.READY,
            trackingSessionId = "opaque-session",
        )
        assertTrue(eligible.canActivateTracking(true, true))
        assertFalse(eligible.canActivateTracking(false, true))
        assertFalse(eligible.canActivateTracking(true, false))
        assertFalse(eligible.copy(orderStatus = OrderStatus.PREPARING).canActivateTracking(true, true))
        assertFalse(eligible.copy(trackingSessionId = null).canActivateTracking(true, true))
        assertFalse(eligible.copy(deliveryStartedByUserId = null).canActivateTracking(true, true))
    }

    @Test fun `customer map requires active session and valid admin assignment`() {
        val active = order(OrderStatus.OUT_FOR_DELIVERY, PaymentStatus.PENDING).copy(
            adminDeliveringPersonally = true,
            assignedDeliveryRole = UserRole.ADMIN,
            deliveryStartedByUserId = "admin-1",
            trackingLifecycle = DeliveryTrackingLifecycle.ACTIVE,
            trackingSessionId = "session",
        )
        assertTrue(active.isRealtimeTrackingAvailable(true, true))
        assertFalse(active.copy(trackingLifecycle = DeliveryTrackingLifecycle.READY)
            .isRealtimeTrackingAvailable(true, true))
        assertFalse(active.isRealtimeTrackingAvailable(false, true))
    }
    @Test fun `new COD order can be confirmed`() {
        val order = order(OrderStatus.PENDING, PaymentStatus.PENDING)
        assertTrue(order.canConfirm)
        assertFalse(order.awaitingCollection)
    }

    @Test fun `only out for delivery pending COD awaits collection`() {
        assertTrue(order(OrderStatus.OUT_FOR_DELIVERY, PaymentStatus.PENDING).awaitingCollection)
        assertFalse(order(OrderStatus.CANCELLED, PaymentStatus.PENDING).awaitingCollection)
        assertFalse(order(OrderStatus.DELIVERED, PaymentStatus.COLLECTED).awaitingCollection)
    }

    @Test fun `cancelled and delivered orders cannot be cancelled again`() {
        assertFalse(order(OrderStatus.CANCELLED, PaymentStatus.PENDING).canCancel)
        assertFalse(order(OrderStatus.DELIVERED, PaymentStatus.COLLECTED).canCancel)
    }

    @Test fun `pending order exposes confirm and reject only`() {
        val pending = order(OrderStatus.PENDING, PaymentStatus.PAID)
        assertTrue(pending.canConfirm)
        assertTrue(pending.canReject)
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.PREPARING))
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED))
    }

    @Test fun `terminal states reject every transition`() {
        OrderStatus.entries.forEach { target ->
            assertFalse(OrderStatus.DELIVERED.canTransitionTo(target))
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(target))
        }
    }

    @Test fun `preparing order becomes assignable only after ready milestone`() {
        val preparing = order(OrderStatus.PREPARING, PaymentStatus.PAID)
        assertTrue(preparing.canMarkReadyForDelivery)
        assertFalse(preparing.readyForDeliveryAssignment)
        val ready = preparing.copy(readyForDeliveryAtEpochMillis = 1234, readyForDeliveryByAdminId = "admin")
        assertFalse(ready.canMarkReadyForDelivery)
        assertTrue(ready.readyForDeliveryAssignment)
    }

    @Test fun `ready order allows exactly one active delivery assignment`() {
        val ready = order(OrderStatus.PREPARING, PaymentStatus.PAID)
            .copy(readyForDeliveryAtEpochMillis = 1234)
        assertTrue(ready.canAssignDelivery)
        assertTrue(ready.canStartPersonalDelivery)
        assertFalse(ready.copy(assignedDeliveryUserId = "driver").canAssignDelivery)
        assertFalse(ready.copy(adminDeliveringPersonally = true).canAssignDelivery)
        assertTrue(ready.copy(assignedDeliveryUserId = "driver").canStartDelivery)
        assertTrue(ready.copy(adminDeliveringPersonally = true).canStartDelivery)
        assertFalse(ready.canStartDelivery)
    }

    private fun order(orderStatus: OrderStatus, paymentStatus: PaymentStatus) = CodOrder(
        id = "id", displayNumber = "100", customerName = "Customer", customerMobile = "9999999999",
        addressSummary = "Address", amountDueMinor = 10000, orderStatus = orderStatus,
        paymentStatus = paymentStatus,
    )
}

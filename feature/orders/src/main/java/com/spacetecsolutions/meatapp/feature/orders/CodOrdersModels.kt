package com.spacetecsolutions.meatapp.feature.orders

import com.spacetecsolutions.meatapp.core.model.CodActor
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.OrderStatus
import com.spacetecsolutions.meatapp.core.model.ProductCategory
import com.spacetecsolutions.meatapp.core.model.DeliveryTrackingLifecycle
import com.spacetecsolutions.meatapp.core.model.UserRole

data class CodOrdersUiState(
    val actor: CodActor? = null,
    val loading: Boolean = true,
    val orders: List<CodOrder> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val selectedStatus: OrderStatus = OrderStatus.PENDING,
    val selectedOrder: CodOrder? = null,
    val busyOrderId: String? = null,
    val collectionOrder: CodOrder? = null,
    val cancelOrder: CodOrder? = null,
    val collectedAmount: String = "",
    val otpOrder: CodOrder? = null,
    val otpInput: String = "",
    val otpError: String? = null,
    val otpBusy: Boolean = false,
    val otpVerifiedSessionId: String? = null,
    val cancelReason: String = "",
    val deliveryStaffAllowed: Boolean = false,
    val realtimeTrackingAllowed: Boolean = false,
    val realtimeTrackingAdminEnabled: Boolean = false,
    val trackingPermissionOrder: CodOrder? = null,
    val trackingCommand: TrackingCommand? = null,
    val assignmentOrder: CodOrder? = null,
    val eligibleStaff: List<com.spacetecsolutions.meatapp.core.model.User> = emptyList(),
    val loadingStaff: Boolean = false,
    val error: String? = null,
    val message: CodOrderMessage? = null,
)

data class CodOrderMessage(val text: String, val success: Boolean)

sealed interface TrackingCommand {
    data class Start(val orderId: String, val sessionId: String, val actor: CodActor) : TrackingCommand
    data object Stop : TrackingCommand
}

internal fun String.toMinorUnitsOrNull(): Long? = trim().takeIf(String::isNotEmpty)
    ?.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 && it.scale() <= 2 }
    ?.movePointRight(2)?.longValueExact()

internal fun CodOrder.canResumeLiveTracking(actor: CodActor?): Boolean {
    val assignedToActor = when (actor) {
        CodActor.ADMIN -> assignedDeliveryRole == UserRole.ADMIN && adminDeliveringPersonally
        CodActor.DELIVERY -> assignedDeliveryRole == UserRole.DELIVERY &&
            assignedDeliveryUserId == deliveryStartedByUserId
        null -> false
    }
    return assignedToActor && orderStatus == OrderStatus.OUT_FOR_DELIVERY &&
        trackingLifecycle in setOf(DeliveryTrackingLifecycle.READY, DeliveryTrackingLifecycle.ACTIVE) &&
        !trackingSessionId.isNullOrBlank()
}

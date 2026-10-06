package com.spacetecsolutions.meatapp.core.model

enum class CodActor { ADMIN, DELIVERY }
enum class DeliveryTrackingLifecycle { DISABLED, READY, ACTIVE, STOPPED }

data class DeliveryLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float? = null,
    val headingDegrees: Float? = null,
    val recordedAtEpochMillis: Long,
) {
    init {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
        require(accuracyMeters >= 0 && recordedAtEpochMillis >= 0)
        require(headingDegrees == null || headingDegrees in 0f..360f)
    }
}

data class CodOrder(
    val id: String,
    val displayNumber: String,
    val customerName: String,
    val customerMobile: String,
    val addressSummary: String,
    val addressLabel: String = "Home",
    val shopName: String = "Meat Station",
    val shopAddress: String = "",
    val amountDueMinor: Long,
    val items: List<OrderItemSnapshot> = emptyList(),
    val subtotalMinor: Long = 0,
    val discountMinor: Long = 0,
    val deliveryFeeMinor: Long = 0,
    val taxMinor: Long = 0,
    val totalMinor: Long = amountDueMinor,
    val paymentMethod: CheckoutPaymentMethod = CheckoutPaymentMethod.COD,
    val instructions: String = "",
    val deliverySlotDateLabel: String? = null,
    val deliverySlotTimeLabel: String? = null,
    val deliveryDateIso: String? = null,
    val deliverySlotStartMinutes: Int? = null,
    val deliverySlotEndMinutes: Int? = null,
    val currencyCode: String = "INR",
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentStatus,
    val createdAtEpochMillis: Long = 0,
    val customerCancellationAllowed: Boolean = false,
    val confirmedAtEpochMillis: Long? = null,
    val confirmedByAdminId: String? = null,
    val preparingAtEpochMillis: Long? = null,
    val preparingByAdminId: String? = null,
    val readyForDeliveryAtEpochMillis: Long? = null,
    val readyForDeliveryByAdminId: String? = null,
    val assignedDeliveryUserId: String? = null,
    val assignedDeliveryUserName: String? = null,
    val assignedAtEpochMillis: Long? = null,
    val assignedByAdminId: String? = null,
    val deliveryStartedAtEpochMillis: Long? = null,
    val deliveryStartedByUserId: String? = null,
    val assignedDeliveryRole: UserRole? = null,
    val deliveredAtEpochMillis: Long? = null,
    val deliveredByUserId: String? = null,
    val cancelledAtEpochMillis: Long? = null,
    val cancelledByUserId: String? = null,
    val cancelledByRole: UserRole? = null,
    val cancelReason: String? = null,
    val trackingLifecycle: DeliveryTrackingLifecycle = DeliveryTrackingLifecycle.DISABLED,
    val trackingSessionId: String? = null,
    val realtimeTrackingAvailable: Boolean = false,
    val deliveryContactName: String? = null,
    val deliveryContactMobile: String? = null,
    val deliveryDestinationLatitude: Double? = null,
    val deliveryDestinationLongitude: Double? = null,
    val adminDeliveringPersonally: Boolean = false,
    val codMismatchReported: Boolean = false,
    val revision: Long = 0,
) {
    init {
        require(amountDueMinor >= 0 && subtotalMinor >= 0 && discountMinor >= 0)
        require(deliveryFeeMinor >= 0 && taxMinor >= 0 && totalMinor >= 0)
        require(revision >= 0)
        require(listOfNotNull(
            confirmedAtEpochMillis, preparingAtEpochMillis, readyForDeliveryAtEpochMillis,
            assignedAtEpochMillis, deliveryStartedAtEpochMillis, createdAtEpochMillis,
            deliveredAtEpochMillis, cancelledAtEpochMillis,
        ).all { it >= 0 })
        require(!(adminDeliveringPersonally && assignedDeliveryUserId != null))
        require((deliveryDestinationLatitude == null) == (deliveryDestinationLongitude == null))
        require(deliveryDestinationLatitude == null || deliveryDestinationLatitude in -90.0..90.0)
        require(deliveryDestinationLongitude == null || deliveryDestinationLongitude in -180.0..180.0)
    }

    val canConfirm get() = orderStatus.canTransitionTo(OrderStatus.CONFIRMED)
    val canReject get() = orderStatus == OrderStatus.PENDING && orderStatus.canTransitionTo(OrderStatus.CANCELLED)
    val canCancel get() = orderStatus.canTransitionTo(OrderStatus.CANCELLED)
    val hasActiveAssignment get() = adminDeliveringPersonally || assignedDeliveryUserId != null
    val canAssignDelivery get() = readyForDeliveryAssignment && !hasActiveAssignment
    val canStartPersonalDelivery get() = canAssignDelivery
    val canStartDelivery get() = readyForDeliveryAssignment && hasActiveAssignment
    val canStartPreparing get() = orderStatus.canTransitionTo(OrderStatus.PREPARING)
    val canMarkReadyForDelivery get() = orderStatus == OrderStatus.PREPARING && readyForDeliveryAtEpochMillis == null
    val readyForDeliveryAssignment get() = orderStatus == OrderStatus.PREPARING && readyForDeliveryAtEpochMillis != null
    val awaitingCollection get() = orderStatus == OrderStatus.OUT_FOR_DELIVERY
        && paymentStatus == PaymentStatus.PENDING
    val canOpenDeliveryCompletion get() = orderStatus == OrderStatus.OUT_FOR_DELIVERY
    val paymentReadyForDeliveryCompletion get() = when (paymentMethod) {
        CheckoutPaymentMethod.COD -> paymentStatus == PaymentStatus.PENDING
        CheckoutPaymentMethod.RAZORPAY, CheckoutPaymentMethod.UPI -> paymentStatus == PaymentStatus.PAID
    }
    val canCustomerCancel get() = customerCancellationAllowed &&
        orderStatus in setOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)
    val canReorder get() = items.isNotEmpty() &&
        orderStatus in setOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED)

    fun canActivateTracking(realtimeAllowed: Boolean, adminEnabled: Boolean): Boolean =
        realtimeAllowed && adminEnabled && orderStatus == OrderStatus.OUT_FOR_DELIVERY &&
            deliveryStartedByUserId != null && trackingLifecycle == DeliveryTrackingLifecycle.READY &&
            !trackingSessionId.isNullOrBlank() && hasActiveAssignment
}

/** The single customer-side gate for creating an RTDB listener and map. */
fun CodOrder.isRealtimeTrackingAvailable(
    superAdminAllowed: Boolean,
    adminEnabled: Boolean,
): Boolean {
    val validAssignment = when (assignedDeliveryRole) {
        UserRole.ADMIN -> adminDeliveringPersonally && !deliveryStartedByUserId.isNullOrBlank()
        UserRole.DELIVERY -> !assignedDeliveryUserId.isNullOrBlank() &&
            assignedDeliveryUserId == deliveryStartedByUserId
        else -> false
    }
    return superAdminAllowed && adminEnabled && orderStatus == OrderStatus.OUT_FOR_DELIVERY &&
        validAssignment && trackingLifecycle == DeliveryTrackingLifecycle.ACTIVE &&
        !trackingSessionId.isNullOrBlank()
}

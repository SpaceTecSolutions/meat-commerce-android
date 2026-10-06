package com.spacetecsolutions.meatapp.core.model

data class DeliveryConfig(
    val normalDeliveryEnabled: Boolean = true,
    val deliveryChargeMinor: Long = 0,
    val freeDeliveryThresholdMinor: Long? = null,
    val minimumOrderMinor: Long = 0,
    val slots: List<DeliverySlot> = emptyList(),
    val scheduledDeliveryEnabled: Boolean = false,
    val realtimeTrackingEnabled: Boolean = false,
    val currencyCode: String = "INR",
    val revision: Long = 0,
) {
    init {
        require(deliveryChargeMinor >= 0)
        require(freeDeliveryThresholdMinor == null || freeDeliveryThresholdMinor >= 0)
        require(minimumOrderMinor >= 0)
        require(revision >= 0)
    }
}

data class DeliverySlot(
    val id: String,
    val label: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val active: Boolean = true,
) {
    init {
        require(id.isNotBlank() && label.isNotBlank())
        require(startMinutes in 0..1439 && endMinutes in 1..1440 && startMinutes < endMinutes)
    }
}

package com.spacetecsolutions.meatapp.core.model

data class FeatureConfig(
    val deliveryStaffManagementAllowed: Boolean = false,
    val staffManagementAllowed: Boolean = false,
    val subcategoriesAllowed: Boolean = false,
    val realtimeTrackingAllowed: Boolean = false,
    val codAllowed: Boolean = false,
    val razorpayAllowed: Boolean = false,
    val upiAllowed: Boolean = false,
    val offersAllowed: Boolean = false,
    val couponsAllowed: Boolean = false,
    val scheduledDeliveryAllowed: Boolean = false,
    val inAppNotificationsEnabled: Boolean = true,
    val maxProducts: Int = DEFAULT_MAX_PRODUCTS,
    val revision: Long = 0L,
) {
    init {
        require(maxProducts >= 0) { "maxProducts cannot be negative" }
        require(revision >= 0) { "revision cannot be negative" }
    }

    companion object {
        const val DEFAULT_MAX_PRODUCTS = 100
    }
}

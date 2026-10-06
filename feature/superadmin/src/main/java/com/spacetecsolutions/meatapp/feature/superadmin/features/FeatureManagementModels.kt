package com.spacetecsolutions.meatapp.feature.superadmin.features

import com.spacetecsolutions.meatapp.core.model.FeatureConfig

enum class ManagedFeature {
    DELIVERY_STAFF, REALTIME_TRACKING, COD, RAZORPAY, UPI, OFFERS, COUPONS, SCHEDULED_DELIVERY,
    IN_APP_NOTIFICATIONS, STAFF_MANAGEMENT, SUBCATEGORIES,
}

data class FeatureManagementUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val config: FeatureConfig? = null,
    val draft: FeatureConfig? = null,
    val maxProductsText: String = "",
    val maxProductsError: String? = null,
    val error: String? = null,
    val message: FeatureMessage? = null,
) {
    val dirty: Boolean get() = config != null && draft != config
}

data class FeatureMessage(val text: String, val success: Boolean)

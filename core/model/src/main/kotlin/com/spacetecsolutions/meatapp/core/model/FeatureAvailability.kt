package com.spacetecsolutions.meatapp.core.model

data class AdminFeatureConfig(
    val codEnabled: Boolean = false,
    val razorpayEnabled: Boolean = false,
    val razorpayConfigured: Boolean = false,
    val upiEnabled: Boolean = false,
    val realtimeTrackingEnabled: Boolean = false,
)

data class FeatureAvailability(
    val cod: Boolean,
    val razorpay: Boolean,
    val upi: Boolean,
    val realtimeTracking: Boolean,
)

fun FeatureConfig.resolve(admin: AdminFeatureConfig): FeatureAvailability = FeatureAvailability(
    cod = codAllowed && admin.codEnabled,
    razorpay = razorpayAllowed && admin.razorpayEnabled && admin.razorpayConfigured,
    upi = upiAllowed && admin.upiEnabled,
    realtimeTracking = realtimeTrackingAllowed && admin.realtimeTrackingEnabled,
)

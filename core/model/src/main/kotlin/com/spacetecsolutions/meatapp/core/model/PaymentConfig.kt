package com.spacetecsolutions.meatapp.core.model

data class PaymentConfig(
    val codEnabled: Boolean = false,
    val razorpayEnabled: Boolean = false,
    val razorpayConfigured: Boolean = false,
    val upiEnabled: Boolean = false,
    val upiVpa: String? = null,
    val revision: Long = 0,
) {
    init { require(revision >= 0) }

    val isRazorpayAvailable: Boolean
        get() = razorpayEnabled && razorpayConfigured
}

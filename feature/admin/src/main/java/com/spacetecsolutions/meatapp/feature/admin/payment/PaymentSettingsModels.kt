package com.spacetecsolutions.meatapp.feature.admin.payment

import com.spacetecsolutions.meatapp.core.model.PaymentConfig

data class PaymentPermissions(
    val cod: Boolean = false,
    val razorpay: Boolean = false,
    val upi: Boolean = false,
) {
    val any get() = cod || razorpay || upi
}

data class PaymentSettingsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val config: PaymentConfig? = null,
    val permissions: PaymentPermissions = PaymentPermissions(),
    val error: String? = null,
    val message: PaymentSettingsMessage? = null,
)

data class PaymentSettingsMessage(val text: String, val success: Boolean)


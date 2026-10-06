package com.spacetecsolutions.meatapp.feature.admin.delivery

import com.spacetecsolutions.meatapp.core.model.DeliveryConfig

data class DeliverySettingsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val config: DeliveryConfig? = null,
    val scheduledDeliveryAllowed: Boolean = false,
    val realtimeTrackingAllowed: Boolean = false,
    val deliveryCharge: String = "",
    val freeThreshold: String = "",
    val minimumOrder: String = "",
    val error: String? = null,
    val message: DeliverySettingsMessage? = null,
)

data class DeliverySettingsMessage(val text: String, val success: Boolean)

internal fun String.moneyMinorOrNull(): Long? = trim().takeIf(String::isNotEmpty)
    ?.toBigDecimalOrNull()?.movePointRight(2)?.longValueExact()

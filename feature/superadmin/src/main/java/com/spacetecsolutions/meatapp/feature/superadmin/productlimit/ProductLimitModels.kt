package com.spacetecsolutions.meatapp.feature.superadmin.productlimit

import com.spacetecsolutions.meatapp.core.model.ProductLimitStatus

data class ProductLimitUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val status: ProductLimitStatus? = null,
    val limitText: String = "",
    val limitError: String? = null,
    val error: String? = null,
    val message: ProductLimitMessage? = null,
) {
    val dirty: Boolean get() = status != null && limitText != status.maxProducts.toString()
}

data class ProductLimitMessage(val text: String, val success: Boolean)

object ProductLimitValidator {
    const val MAX_SUPPORTED_LIMIT = 100_000

    fun validate(value: Int?, currentCount: Int): String? = when {
        value == null -> "Maximum products is required"
        value < currentCount -> "Archive products before lowering below $currentCount"
        value > MAX_SUPPORTED_LIMIT -> "Maximum supported limit is $MAX_SUPPORTED_LIMIT"
        else -> null
    }
}

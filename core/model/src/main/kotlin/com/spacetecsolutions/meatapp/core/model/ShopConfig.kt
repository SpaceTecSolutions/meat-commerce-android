package com.spacetecsolutions.meatapp.core.model

data class ShopConfig(
    val shopId: String,
    val displayName: String,
    val currencyCode: String = "INR",
    val localeTag: String = "en-IN",
    val timeZoneId: String = "Asia/Kolkata",
    val supportPhone: String? = null,
    val minimumOrderAmountMinor: Long = 0L,
    val active: Boolean = true,
) {
    init {
        require(shopId.isNotBlank()) { "Shop id cannot be blank" }
        require(displayName.isNotBlank()) { "Shop name cannot be blank" }
        require(currencyCode.length == 3) { "Currency must be an ISO 4217 code" }
        require(minimumOrderAmountMinor >= 0) { "Minimum order amount cannot be negative" }
    }
}

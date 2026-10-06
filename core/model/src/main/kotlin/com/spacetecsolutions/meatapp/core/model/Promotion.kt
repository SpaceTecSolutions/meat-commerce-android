package com.spacetecsolutions.meatapp.core.model

enum class PromotionKind { OFFER, COUPON }
enum class DiscountType { PERCENTAGE, FIXED }

data class Promotion(
    val id: String,
    val name: String,
    val code: String? = null,
    val kind: PromotionKind,
    val discountType: DiscountType,
    val discountValue: Long,
    val minimumOrderMinor: Long = 0,
    val maximumDiscountMinor: Long? = null,
    val validFromEpochMillis: Long,
    val validUntilEpochMillis: Long,
    val active: Boolean = true,
    val revision: Long = 0,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(discountValue > 0 && minimumOrderMinor >= 0 && revision >= 0)
        require(discountType != DiscountType.PERCENTAGE || discountValue in 1..100)
        require(maximumDiscountMinor == null || maximumDiscountMinor > 0)
        require(validFromEpochMillis >= 0 && validUntilEpochMillis > validFromEpochMillis)
        require(kind != PromotionKind.COUPON || !code.isNullOrBlank())
    }

    fun isValidAt(nowEpochMillis: Long) = active && nowEpochMillis in validFromEpochMillis..validUntilEpochMillis

    fun discountFor(subtotalMinor: Long, nowEpochMillis: Long): Long {
        if (subtotalMinor < minimumOrderMinor || !isValidAt(nowEpochMillis)) return 0
        val raw = when (discountType) {
            DiscountType.PERCENTAGE -> subtotalMinor * discountValue / 100
            DiscountType.FIXED -> discountValue
        }
        return minOf(raw, maximumDiscountMinor ?: raw, subtotalMinor)
    }
}

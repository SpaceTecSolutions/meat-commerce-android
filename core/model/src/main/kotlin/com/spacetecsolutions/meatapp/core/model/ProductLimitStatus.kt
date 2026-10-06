package com.spacetecsolutions.meatapp.core.model

data class ProductLimitStatus(
    val countedProducts: Int,
    val maxProducts: Int,
    val revision: Long,
) {
    init {
        require(countedProducts >= 0) { "countedProducts cannot be negative" }
        require(maxProducts >= 0) { "maxProducts cannot be negative" }
        require(revision >= 0) { "revision cannot be negative" }
    }

    val limitReached: Boolean get() = countedProducts >= maxProducts
    val remaining: Int get() = (maxProducts - countedProducts).coerceAtLeast(0)
}

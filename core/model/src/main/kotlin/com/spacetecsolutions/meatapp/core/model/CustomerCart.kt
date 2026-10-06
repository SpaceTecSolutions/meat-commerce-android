package com.spacetecsolutions.meatapp.core.model

enum class CartAdjustmentType { PRICE_CHANGED, QUANTITY_REDUCED, OUT_OF_STOCK, UNAVAILABLE }

data class CartAdjustment(
    val productId: String,
    val type: CartAdjustmentType,
    val message: String,
)

data class CartLine(
    val productId: String,
    val name: String,
    val imageUrl: String? = null,
    val unit: ProductUnit,
    val unitPriceMinor: Long,
    val regularPriceMinor: Long,
    val quantity: Int,
    val maxQuantity: Int,
    val available: Boolean,
) {
    val lineTotalMinor: Long get() = unitPriceMinor * quantity
}

data class CustomerCart(
    val lines: List<CartLine> = emptyList(),
    val subtotalMinor: Long = 0,
    val discountMinor: Long = 0,
    val deliveryFeeMinor: Long = 0,
    val totalMinor: Long = 0,
    val deliveryEstimate: String? = null,
    val deliveryAddress: CustomerAddress? = null,
    val minimumOrderMinor: Long = 0,
    val adjustments: List<CartAdjustment> = emptyList(),
) {
    val quantity: Int get() = lines.sumOf(CartLine::quantity)
    val distinctItemCount: Int get() = lines.size
    val checkoutAllowed: Boolean get() = lines.isNotEmpty() && lines.all(CartLine::available) &&
        deliveryAddress != null && subtotalMinor >= minimumOrderMinor
}

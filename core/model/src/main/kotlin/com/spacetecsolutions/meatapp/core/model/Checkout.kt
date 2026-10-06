package com.spacetecsolutions.meatapp.core.model

enum class CheckoutDeliveryOption { NORMAL, SCHEDULED }
enum class CheckoutPaymentMethod { COD, RAZORPAY, UPI }

data class CheckoutDeliveryDate(
    val id: String,
    val label: String,
    val availableSlotIds: Set<String>,
)

data class CheckoutQuote(
    val quoteToken: String,
    val cart: CustomerCart,
    val addresses: List<CustomerAddress>,
    val normalDeliveryAvailable: Boolean,
    val scheduledDeliveryAvailable: Boolean,
    val deliveryDates: List<CheckoutDeliveryDate>,
    val deliverySlots: List<DeliverySlot>,
    val paymentMethods: Set<CheckoutPaymentMethod>,
    val currencyCode: String = "INR",
    val expiresAtEpochMillis: Long,
) {
    val defaultAddressId: String? get() = addresses.firstOrNull(CustomerAddress::isDefault)?.id
        ?: addresses.firstOrNull()?.id
}

data class PlaceOrderRequest(
    val quoteToken: String,
    val addressId: String,
    val deliveryOption: CheckoutDeliveryOption,
    val deliveryDateId: String,
    val deliverySlotId: String? = null,
    val instructions: String = "",
    val paymentMethod: CheckoutPaymentMethod,
    val idempotencyKey: String,
)

/** A point-in-time copy. Catalog edits must never mutate an order's history. */
data class OrderItemSnapshot(
    val productId: String,
    val name: String,
    val imageUrl: String?,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val unit: ProductUnit,
    val quantity: Int,
    val unitPriceMinor: Long,
    val regularPriceMinor: Long,
    val lineTotalMinor: Long,
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(productId.isNotBlank() && name.isNotBlank())
        require(quantity > 0)
        require(unitPriceMinor >= 0 && regularPriceMinor >= 0 && lineTotalMinor >= 0)
    }
}

data class FinalWeightAdjustment(
    val productId: String,
    val finalQuantity: Double,
)

data class PlacedOrder(
    val orderId: String,
    val orderNumber: String,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentStatus,
    val totalMinor: Long,
    val currencyCode: String,
    val estimatedDelivery: String,
    val items: List<OrderItemSnapshot>,
) {
    init {
        require(orderId.isNotBlank() && orderNumber.isNotBlank())
        require(orderStatus == OrderStatus.PENDING) { "A newly placed order must be PENDING" }
        require(totalMinor >= 0 && estimatedDelivery.isNotBlank() && items.isNotEmpty())
    }
}

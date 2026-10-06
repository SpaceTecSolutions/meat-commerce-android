package com.spacetecsolutions.meatapp.core.model

enum class OrderStatus {
    PENDING, CONFIRMED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED,
}

enum class PaymentStatus {
    PENDING, PROCESSING, PAID, FAILED, COLLECTED, REFUND_PENDING, REFUNDED, PARTIALLY_REFUNDED,
}

fun OrderStatus.canTransitionTo(target: OrderStatus): Boolean = target in when (this) {
    OrderStatus.PENDING -> setOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED)
    OrderStatus.CONFIRMED -> setOf(OrderStatus.PREPARING, OrderStatus.CANCELLED)
    OrderStatus.PREPARING -> setOf(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED)
    OrderStatus.OUT_FOR_DELIVERY -> setOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED)
    OrderStatus.DELIVERED, OrderStatus.CANCELLED -> emptySet()
}

fun isFinalizedRevenue(orderStatus: OrderStatus, paymentStatus: PaymentStatus): Boolean =
    orderStatus == OrderStatus.DELIVERED &&
        paymentStatus in setOf(PaymentStatus.PAID, PaymentStatus.COLLECTED)

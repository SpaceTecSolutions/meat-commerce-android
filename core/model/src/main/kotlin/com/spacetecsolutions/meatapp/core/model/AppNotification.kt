package com.spacetecsolutions.meatapp.core.model

enum class NotificationEvent {
    ORDER_PLACED, ORDER_SUBMITTED, ORDER_CONFIRMED, ORDER_PREPARING, ORDER_OUT_FOR_DELIVERY,
    DELIVERY_DELAYED, ORDER_DELIVERED, ORDER_CANCELLED, PAYMENT_STATUS, PAYMENT,
    PROMOTION, NEW_ORDER, CUSTOMER_CANCELLED_ORDER, PAYMENT_RECEIVED, LOW_STOCK,
    DELIVERY_ISSUE, DELIVERY_ASSIGNED, DELIVERY_REASSIGNED, DELIVERY_ASSIGNMENT_CANCELLED,
    DELIVERY_START_REMINDER, DELIVERY_CUSTOMER_ISSUE, FEATURE_CONFIG_CHANGED,
    WEEKLY_REPORT, MONTHLY_REPORT, OPERATIONAL, ASSIGNMENT_CREATED, ASSIGNMENT_REMOVED,
    IMPORTANT_STATUS, UNKNOWN,
}

enum class NotificationCategory { ORDER, PAYMENT, DELIVERY, STOCK, PROMOTION, SYSTEM, REPORT }
enum class NotificationPriority { NORMAL, HIGH }

data class AppNotification(
    val id: String,
    val event: NotificationEvent,
    val title: String,
    val body: String,
    val createdAtEpochMillis: Long,
    val readAtEpochMillis: Long? = null,
    val orderId: String? = null,
    val productId: String? = null,
    val reportPeriod: String? = null,
    val deepLinkRoute: String? = null,
    val category: NotificationCategory = NotificationCategory.SYSTEM,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
) {
    val isRead: Boolean get() = readAtEpochMillis != null
}

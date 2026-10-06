package com.spacetecsolutions.meatapp.feature.orders

import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.OrderStatus
import com.spacetecsolutions.meatapp.core.model.ProductUnit

internal enum class TimelineVisualState { COMPLETED, CURRENT, UPCOMING, CANCELLED }

internal enum class CustomerDeliveryProgress {
    AWAITING_CONFIRMATION,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERY_DELAYED,
    DELIVERED,
    CANCELLED,
}

internal data class CustomerTimelineItem(
    val label: String,
    val timestamp: Long?,
    val state: TimelineVisualState,
)

internal data class CustomerOrderUiModel(
    val order: CodOrder,
    val progress: CustomerDeliveryProgress,
    val displayStatus: String,
    val statusDetail: String?,
    val formattedCreatedDate: String,
    val formattedDeliveryTime: String,
    val isDelayed: Boolean,
    val canTrack: Boolean,
    val isLiveTrackingAvailable: Boolean,
)

private val ongoingStatuses = setOf(
    OrderStatus.PENDING,
    OrderStatus.CONFIRMED,
    OrderStatus.PREPARING,
    OrderStatus.OUT_FOR_DELIVERY,
)

internal fun CustomerOrdersUiState.filteredOrders(): List<CodOrder> = when (selectedTab) {
    CustomerOrderTab.ALL -> orders
    CustomerOrderTab.ONGOING -> orders.filter { it.orderStatus in ongoingStatuses }
    CustomerOrderTab.COMPLETED -> orders.filter { it.orderStatus == OrderStatus.DELIVERED }
    CustomerOrderTab.CANCELLED -> orders.filter { it.orderStatus == OrderStatus.CANCELLED }
}

internal fun CustomerOrdersUiState.filteredOrderModels(): List<CustomerOrderUiModel> =
    filteredOrders().map { it.toCustomerOrderUi(nowEpochMillis) }

internal val CodOrder.hasTrackingDestination: Boolean
    get() = deliveryDestinationLatitude != null && deliveryDestinationLongitude != null

internal fun CodOrder.toCustomerOrderUi(now: Long): CustomerOrderUiModel {
    val late = orderStatus in ongoingStatuses &&
        CustomerOrderDateFormatter.slotHasPassed(this, now)
    val progress = when {
        orderStatus == OrderStatus.DELIVERED -> CustomerDeliveryProgress.DELIVERED
        orderStatus == OrderStatus.CANCELLED -> CustomerDeliveryProgress.CANCELLED
        orderStatus == OrderStatus.OUT_FOR_DELIVERY -> CustomerDeliveryProgress.OUT_FOR_DELIVERY
        late -> CustomerDeliveryProgress.DELIVERY_DELAYED
        orderStatus == OrderStatus.PREPARING -> CustomerDeliveryProgress.PREPARING
        orderStatus == OrderStatus.CONFIRMED -> CustomerDeliveryProgress.CONFIRMED
        else -> CustomerDeliveryProgress.AWAITING_CONFIRMATION
    }
    val expected = CustomerOrderDateFormatter.expectedBy(this)
    val detail = when {
        late && orderStatus == OrderStatus.OUT_FOR_DELIVERY ->
            expected?.let { "Running late · Expected by ".plus(it) } ?: "Running late"
        late && orderStatus == OrderStatus.PENDING ->
            expected?.let { "Awaiting shop confirmation · Expected by ".plus(it) }
                ?: "Awaiting shop confirmation"
        late -> orderStatus.label().plus(" · Delivery time has passed")
        else -> null
    }
    return CustomerOrderUiModel(
        order = this,
        progress = progress,
        displayStatus = progress.label(),
        statusDetail = detail,
        formattedCreatedDate = CustomerOrderDateFormatter.created(createdAtEpochMillis, now),
        formattedDeliveryTime = CustomerOrderDateFormatter.delivery(this, now),
        isDelayed = late,
        canTrack = orderStatus in ongoingStatuses,
        isLiveTrackingAvailable = realtimeTrackingAvailable,
    )
}

private fun CustomerDeliveryProgress.label() = when (this) {
    CustomerDeliveryProgress.AWAITING_CONFIRMATION -> "Awaiting Confirmation"
    CustomerDeliveryProgress.CONFIRMED -> "Confirmed"
    CustomerDeliveryProgress.PREPARING -> "Preparing"
    CustomerDeliveryProgress.OUT_FOR_DELIVERY -> "Out for Delivery"
    CustomerDeliveryProgress.DELIVERY_DELAYED -> "Delivery Delayed"
    CustomerDeliveryProgress.DELIVERED -> "Delivered"
    CustomerDeliveryProgress.CANCELLED -> "Cancelled"
}

internal fun CustomerOrderTab.label() = name.lowercase().replaceFirstChar(Char::titlecase)

internal fun CustomerOrderTab.emptyCopy() = when (this) {
    CustomerOrderTab.ALL -> "No orders yet" to "Your placed orders will appear here."
    CustomerOrderTab.ONGOING -> "No ongoing orders" to "You don't have any active orders."
    CustomerOrderTab.COMPLETED -> "No completed orders" to "Completed orders will appear here."
    CustomerOrderTab.CANCELLED -> "No cancelled orders" to "No cancelled orders to show."
}

internal fun CodOrder.timelineItems(): List<CustomerTimelineItem> {
    val stages = listOf(
        Triple("Order Placed", OrderStatus.PENDING, createdAtEpochMillis.takeIf { it > 0 }),
        Triple("Order Confirmed", OrderStatus.CONFIRMED, confirmedAtEpochMillis),
        Triple("Preparing", OrderStatus.PREPARING, preparingAtEpochMillis),
        Triple("Out for Delivery", OrderStatus.OUT_FOR_DELIVERY, deliveryStartedAtEpochMillis),
        Triple("Delivered", OrderStatus.DELIVERED, deliveredAtEpochMillis),
    )
    if (orderStatus == OrderStatus.CANCELLED) {
        val completed = stages.takeWhile { (_, _, timestamp) -> timestamp != null }
            .map { (label, _, timestamp) ->
                CustomerTimelineItem(label, timestamp, TimelineVisualState.COMPLETED)
            }
        return completed + CustomerTimelineItem(
            "Cancelled",
            cancelledAtEpochMillis,
            TimelineVisualState.CANCELLED,
        )
    }
    val currentIndex = stages.indexOfFirst { it.second == orderStatus }
    return stages.mapIndexed { index, (label, _, timestamp) ->
        val visual = when {
            orderStatus == OrderStatus.DELIVERED -> TimelineVisualState.COMPLETED
            index < currentIndex -> TimelineVisualState.COMPLETED
            index == currentIndex && index == 0 -> TimelineVisualState.COMPLETED
            index == currentIndex -> TimelineVisualState.CURRENT
            else -> TimelineVisualState.UPCOMING
        }
        CustomerTimelineItem(label, timestamp, visual)
    }
}

internal fun CodOrder.numberLabel(): String = when {
    displayNumber.startsWith("#") -> displayNumber
    else -> "#".plus(displayNumber)
}

internal fun Long.timelineDateTime(): String =
    if (this <= 0) "" else CustomerOrderDateFormatter.created(this)

internal fun ProductUnit.shortLabel() = when (this) {
    ProductUnit.KILOGRAM -> "kg"
    ProductUnit.GRAM -> "g"
    ProductUnit.PIECE -> "pc"
    ProductUnit.PACK -> "pack"
}

internal fun CodOrder.deliveryLabel(): String = CustomerOrderDateFormatter.delivery(this)

internal fun CodOrder.trackingMessage() = when (orderStatus) {
    OrderStatus.PENDING -> "Waiting for shop confirmation"
    OrderStatus.CONFIRMED -> "Your order has been confirmed and will be prepared soon."
    OrderStatus.PREPARING -> "Your order is being prepared."
    OrderStatus.OUT_FOR_DELIVERY -> "Your order is on the way."
    OrderStatus.DELIVERED -> "Your order has been delivered."
    OrderStatus.CANCELLED -> cancelReason?.takeIf(String::isNotBlank)?.let { "Cancelled: ".plus(it) }
        ?: "This order was cancelled."
}

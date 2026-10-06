package com.spacetecsolutions.meatapp.feature.orders

import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.OrderStatus
import com.spacetecsolutions.meatapp.core.model.PaymentStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CustomerOrderPresentationTest {
    @Test
    fun `orders open on ongoing and map statuses correctly`() {
        val state = CustomerOrdersUiState(
            orders = OrderStatus.entries.mapIndexed { index, status -> order(status, index.toLong()) },
        )

        assertEquals(CustomerOrderTab.ONGOING, state.selectedTab)
        assertEquals(
            setOf(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY),
            state.filteredOrders().map(CodOrder::orderStatus).toSet(),
        )
        assertFalse(state.filteredOrders().any { it.orderStatus == OrderStatus.DELIVERED })
    }

    @Test
    fun `out for delivery timeline uses real timestamps and no fabricated delivered time`() {
        val order = order(OrderStatus.OUT_FOR_DELIVERY, 4).copy(
            confirmedAtEpochMillis = 2,
            preparingAtEpochMillis = 3,
            deliveryStartedAtEpochMillis = 4,
        )

        val timeline = order.timelineItems()

        assertEquals(TimelineVisualState.CURRENT, timeline[3].state)
        assertEquals(4L, timeline[3].timestamp)
        assertEquals(TimelineVisualState.UPCOMING, timeline[4].state)
        assertEquals(null, timeline[4].timestamp)
    }

    @Test
    fun `cancelled timeline ends at cancellation and omits impossible future stages`() {
        val timeline = order(OrderStatus.CANCELLED, 1).copy(
            confirmedAtEpochMillis = 2,
            cancelledAtEpochMillis = 3,
            cancelReason = "Customer requested",
        ).timelineItems()

        assertEquals(listOf("Order Placed", "Order Confirmed", "Cancelled"), timeline.map { it.label })
        assertEquals(TimelineVisualState.CANCELLED, timeline.last().state)
    }

    @Test
    fun `pending order past its delivery slot derives delayed without changing canonical status`() {
        val now = epoch("2026-09-09", 14, 0)
        val model = order(OrderStatus.PENDING, now).copy(
            deliveryDateIso = "2026-09-09",
            deliverySlotTimeLabel = "11:00 AM - 1:00 PM",
            deliverySlotEndMinutes = 13 * 60,
        ).toCustomerOrderUi(now)

        assertEquals(OrderStatus.PENDING, model.order.orderStatus)
        assertEquals(CustomerDeliveryProgress.DELIVERY_DELAYED, model.progress)
        assertEquals("Delivery Delayed", model.displayStatus)
        assertEquals("Today, 11:00 AM - 1:00 PM", model.formattedDeliveryTime)
    }

    @Test
    fun `out for delivery remains trackable and shows running late after slot`() {
        val now = epoch("2026-09-09", 14, 0)
        val model = order(OrderStatus.OUT_FOR_DELIVERY, now).copy(
            deliveryDateIso = "2026-09-09",
            deliverySlotEndMinutes = 13 * 60,
            realtimeTrackingAvailable = true,
        ).toCustomerOrderUi(now)

        assertEquals(CustomerDeliveryProgress.OUT_FOR_DELIVERY, model.progress)
        assertEquals("Out for Delivery", model.displayStatus)
        assertEquals("Running late · Expected by 1:00 PM", model.statusDetail)
        assertEquals(true, model.isLiveTrackingAvailable)
    }

    @Test
    fun `relative dates are recomputed from iso date`() {
        val now = epoch("2026-09-09", 9, 0)
        val tomorrow = order(OrderStatus.CONFIRMED, now).copy(
            deliveryDateIso = "2026-09-10",
            deliverySlotTimeLabel = "11:00 AM - 1:00 PM",
        ).toCustomerOrderUi(now)

        assertEquals("Tomorrow, 11:00 AM - 1:00 PM", tomorrow.formattedDeliveryTime)
    }

    private fun epoch(date: String, hour: Int, minute: Int): Long =
        LocalDate.parse(date).atTime(LocalTime.of(hour, minute))
            .atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()

    private fun order(status: OrderStatus, created: Long) = CodOrder(
        id = status.name,
        displayNumber = "#ORD-${status.ordinal}",
        customerName = "Customer",
        customerMobile = "9999999999",
        addressSummary = "Bengaluru",
        amountDueMinor = 100,
        totalMinor = 100,
        orderStatus = status,
        paymentStatus = PaymentStatus.PENDING,
        createdAtEpochMillis = created,
    )
}

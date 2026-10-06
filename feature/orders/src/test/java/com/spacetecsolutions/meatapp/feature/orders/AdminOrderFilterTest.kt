package com.spacetecsolutions.meatapp.feature.orders

import com.spacetecsolutions.meatapp.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class AdminOrderFilterTest {
    private val pending = order("ORD-100", "Rahul Sharma", "9876543210", OrderStatus.PENDING,
        CheckoutPaymentMethod.COD, PaymentStatus.PENDING)
    private val delivered = order("ORD-200", "Priya Mehta", "9123456780", OrderStatus.DELIVERED,
        CheckoutPaymentMethod.UPI, PaymentStatus.PAID)

    @Test fun `all status includes every order and status filters exactly`() {
        assertEquals(2, filterAdminOrders(listOf(pending, delivered), AdminOrderFilter.ALL, "", null, null).size)
        assertEquals(listOf(pending), filterAdminOrders(
            listOf(pending, delivered), AdminOrderFilter.PENDING, "", null, null,
        ))
    }

    @Test fun `search matches order number customer and mobile without new reads`() {
        val orders = listOf(pending, delivered)
        assertEquals(listOf(pending), filterAdminOrders(orders, AdminOrderFilter.ALL, "ord-100", null, null))
        assertEquals(listOf(delivered), filterAdminOrders(orders, AdminOrderFilter.ALL, "priya", null, null))
        assertEquals(listOf(pending), filterAdminOrders(orders, AdminOrderFilter.ALL, "4321", null, null))
    }

    @Test fun `payment filters combine with status`() {
        assertEquals(listOf(delivered), filterAdminOrders(
            listOf(pending, delivered), AdminOrderFilter.DELIVERED, "", CheckoutPaymentMethod.UPI, PaymentStatus.PAID,
        ))
    }

    @Test fun `category matches an order containing one product`() {
        val chicken = pending.copy(items = listOf(item("chicken", "Chicken")))
        assertEquals(listOf(chicken), filterAdminOrders(
            listOf(chicken, delivered), AdminOrderFilter.ALL, "", null, null, "chicken", "Chicken",
        ))
    }

    @Test fun `category matches any item in a mixed product order`() {
        val mixed = pending.copy(items = listOf(item("mutton", "Mutton"), item("chicken", "Chicken")))
        assertEquals(listOf(mixed), filterAdminOrders(
            listOf(mixed, delivered), AdminOrderFilter.ALL, "", null, null, "chicken", "Chicken",
        ))
        assertEquals(listOf(mixed), filterAdminOrders(
            listOf(mixed, delivered), AdminOrderFilter.ALL, "", null, null, "mutton", "Mutton",
        ))
    }

    @Test fun `legacy category name matching ignores case and extra spaces`() {
        val legacy = pending.copy(items = listOf(item(null, "  Fresh   Chicken ")))
        assertEquals(listOf(legacy), filterAdminOrders(
            listOf(legacy), AdminOrderFilter.ALL, "", null, null, "new-id", "fresh chicken",
        ))
    }

    private fun order(
        number: String, customer: String, mobile: String, status: OrderStatus,
        method: CheckoutPaymentMethod, payment: PaymentStatus,
    ) = CodOrder(
        id = number, displayNumber = number, customerName = customer, customerMobile = mobile,
        addressSummary = "Address", amountDueMinor = 10000, orderStatus = status,
        paymentStatus = payment, paymentMethod = method,
    )

    private fun item(categoryId: String?, categoryName: String) = OrderItemSnapshot(
        productId = "product-${categoryName.lowercase().replace(' ', '-')}", name = categoryName,
        imageUrl = null, categoryId = categoryId, categoryName = categoryName,
        unit = ProductUnit.KILOGRAM, quantity = 1, unitPriceMinor = 10000,
        regularPriceMinor = 10000, lineTotalMinor = 10000,
    )
}

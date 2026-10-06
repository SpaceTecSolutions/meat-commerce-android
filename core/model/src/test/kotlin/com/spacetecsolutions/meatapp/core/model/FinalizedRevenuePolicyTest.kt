package com.spacetecsolutions.meatapp.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalizedRevenuePolicyTest {
    @Test
    fun `delivered paid online and collected COD qualify`() {
        assertTrue(isFinalizedRevenue(OrderStatus.DELIVERED, PaymentStatus.PAID))
        assertTrue(isFinalizedRevenue(OrderStatus.DELIVERED, PaymentStatus.COLLECTED))
    }

    @Test
    fun `pending and cancelled orders never qualify`() {
        assertFalse(isFinalizedRevenue(OrderStatus.PENDING, PaymentStatus.PAID))
        assertFalse(isFinalizedRevenue(OrderStatus.CANCELLED, PaymentStatus.PAID))
        assertFalse(isFinalizedRevenue(OrderStatus.CANCELLED, PaymentStatus.COLLECTED))
    }

    @Test
    fun `non-final payment states never qualify`() {
        PaymentStatus.entries
            .filterNot { it == PaymentStatus.PAID || it == PaymentStatus.COLLECTED }
            .forEach { assertFalse(isFinalizedRevenue(OrderStatus.DELIVERED, it)) }
    }
}

package com.spacetecsolutions.meatapp.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerCartTest {
    private val line = CartLine("product", "Mutton", null, ProductUnit.KILOGRAM,
        75_000, 75_000, 2, 5, true)
    private val address = CustomerAddress("address", AddressType.HOME, "Akash", "+918000000000",
        "Koramangala", "", "Bengaluru", "Karnataka", "560034", true)

    @Test fun `cart exposes total quantity and distinct product count separately`() {
        val cart = CustomerCart(lines = listOf(line))
        assertEquals(2, cart.quantity)
        assertEquals(1, cart.distinctItemCount)
    }

    @Test fun `checkout requires address valid lines and minimum order`() {
        assertFalse(CustomerCart(lines = listOf(line), subtotalMinor = 150_000).checkoutAllowed)
        assertFalse(CustomerCart(lines = listOf(line), subtotalMinor = 150_000,
            deliveryAddress = address, minimumOrderMinor = 200_000).checkoutAllowed)
        assertTrue(CustomerCart(lines = listOf(line), subtotalMinor = 150_000,
            deliveryAddress = address, minimumOrderMinor = 100_000).checkoutAllowed)
    }

    @Test fun `line total uses integer minor units`() {
        assertEquals(150_000, line.lineTotalMinor)
    }
}

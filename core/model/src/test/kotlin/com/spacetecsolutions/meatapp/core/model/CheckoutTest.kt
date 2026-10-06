package com.spacetecsolutions.meatapp.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckoutTest {
    @Test fun `default address falls back to first address`() {
        val first = address("first", false)
        val preferred = address("preferred", true)
        assertEquals("preferred", quote(listOf(first, preferred)).defaultAddressId)
        assertEquals("first", quote(listOf(first)).defaultAddressId)
    }

    private fun quote(addresses: List<CustomerAddress>) = CheckoutQuote(
        quoteToken = "opaque", cart = CustomerCart(), addresses = addresses,
        normalDeliveryAvailable = true, scheduledDeliveryAvailable = false,
        deliveryDates = emptyList(), deliverySlots = emptyList(),
        paymentMethods = setOf(CheckoutPaymentMethod.COD),
        expiresAtEpochMillis = 1,
    )
    private fun address(id: String, default: Boolean) = CustomerAddress(
        id, AddressType.HOME, "Name", "+919999999999", "Street", city = "City",
        state = "State", postalCode = "123456", isDefault = default,
    )
}

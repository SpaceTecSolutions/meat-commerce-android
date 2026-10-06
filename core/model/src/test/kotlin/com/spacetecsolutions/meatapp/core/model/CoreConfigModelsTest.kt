package com.spacetecsolutions.meatapp.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreConfigModelsTest {
    @Test
    fun `razorpay availability requires enablement and completed configuration`() {
        assertFalse(PaymentConfig(razorpayEnabled = true).isRazorpayAvailable)
        assertTrue(
            PaymentConfig(
                razorpayEnabled = true,
                razorpayConfigured = true,
            ).isRazorpayAvailable,
        )
    }

    @Test
    fun `invalid shop and delivery values are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ShopConfig(shopId = "shop", displayName = "Shop", minimumOrderAmountMinor = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DeliveryConfig(deliveryChargeMinor = -1)
        }
    }

    @Test
    fun `payment revision cannot be negative`() {
        assertThrows(IllegalArgumentException::class.java) { PaymentConfig(revision = -1) }
    }
}

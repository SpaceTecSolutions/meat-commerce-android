package com.spacetecsolutions.meatapp.feature.checkout

import com.spacetecsolutions.meatapp.core.model.CheckoutDeliveryOption
import com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutModelsTest {
    @Test fun `checkout requires a configured delivery time`() {
        val state = CheckoutUiState(addressId = "address",
            deliveryOption = CheckoutDeliveryOption.NORMAL,
            paymentMethod = CheckoutPaymentMethod.COD)
        assertFalse(state.selectionComplete)
    }

    @Test fun `scheduled delivery requires slot`() {
        val state = CheckoutUiState(addressId = "address",
            deliveryOption = CheckoutDeliveryOption.SCHEDULED,
            paymentMethod = CheckoutPaymentMethod.UPI)
        assertFalse(state.selectionComplete)
        assertTrue(state.copy(deliveryDateId = "2026-09-06", deliverySlotId = "slot",
            paymentMethod = CheckoutPaymentMethod.COD).selectionComplete)
    }

    @Test fun `razorpay can complete delivery selection`() {
        assertTrue(CheckoutUiState(
            addressId = "address", deliveryOption = CheckoutDeliveryOption.SCHEDULED,
            deliveryDateId = "2026-09-10", deliverySlotId = "slot",
            paymentMethod = CheckoutPaymentMethod.RAZORPAY,
        ).selectionComplete)
    }
}

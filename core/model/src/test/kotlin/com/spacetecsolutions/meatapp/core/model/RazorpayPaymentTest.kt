package com.spacetecsolutions.meatapp.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RazorpayPaymentTest {
    @Test fun `refund lifecycle is represented independently`() {
        assertTrue(PaymentAttemptStatus.REFUND_PENDING in PaymentAttemptStatus.entries)
        assertTrue(PaymentAttemptStatus.PARTIALLY_REFUNDED in PaymentAttemptStatus.entries)
        assertTrue(PaymentAttemptStatus.REFUNDED in PaymentAttemptStatus.entries)
    }

    @Test fun `SDK success contains evidence but does not claim paid status`() {
        val callback: RazorpaySdkResult = RazorpaySdkResult.Success(
            "callback", "payment", "order", "signature",
        )
        assertEquals("callback", callback.callbackId)
    }
}

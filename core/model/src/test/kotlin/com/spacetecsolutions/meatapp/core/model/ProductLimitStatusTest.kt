package com.spacetecsolutions.meatapp.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductLimitStatusTest {
    @Test
    fun `status calculates remaining capacity and reached state`() {
        val available = ProductLimitStatus(countedProducts = 67, maxProducts = 100, revision = 2)
        val reached = ProductLimitStatus(countedProducts = 100, maxProducts = 100, revision = 2)

        assertEquals(33, available.remaining)
        assertFalse(available.limitReached)
        assertTrue(reached.limitReached)
    }
}

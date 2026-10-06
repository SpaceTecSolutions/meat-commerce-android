package com.spacetecsolutions.meatapp.feature.admin.delivery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeliverySettingsModelsTest {
    @Test fun `money converts to minor units`() {
        assertEquals(12550L, "125.50".moneyMinorOrNull())
        assertEquals(0L, "0".moneyMinorOrNull())
    }

    @Test fun `blank and invalid money are rejected`() {
        assertNull("".moneyMinorOrNull())
        assertNull("abc".moneyMinorOrNull())
    }
}

package com.spacetecsolutions.meatapp.feature.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodAmountTest {
    @Test fun `COD amount uses exact minor units`() {
        assertEquals(12550L, "125.50".toMinorUnitsOrNull())
        assertEquals(10000L, "100".toMinorUnitsOrNull())
    }

    @Test fun `invalid COD amount is rejected`() {
        assertNull("".toMinorUnitsOrNull())
        assertNull("-1".toMinorUnitsOrNull())
        assertNull("10.001".toMinorUnitsOrNull())
    }
}

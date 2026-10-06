package com.spacetecsolutions.meatapp.feature.superadmin.productlimit

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductLimitValidatorTest {
    @Test
    fun `limit equal to current count is valid`() {
        assertNull(ProductLimitValidator.validate(value = 67, currentCount = 67))
    }

    @Test
    fun `limit below current count is rejected`() {
        assertTrue(ProductLimitValidator.validate(value = 66, currentCount = 67)!!.contains("Archive"))
    }

    @Test
    fun `unsupported and empty limits are rejected`() {
        assertTrue(ProductLimitValidator.validate(null, 0) != null)
        assertTrue(
            ProductLimitValidator.validate(ProductLimitValidator.MAX_SUPPORTED_LIMIT + 1, 0) != null,
        )
    }
}

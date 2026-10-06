package com.spacetecsolutions.meatapp.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAvailabilityTest {
    @Test
    fun `admin cannot enable a feature forbidden by super admin`() {
        val superAdmin = FeatureConfig(razorpayAllowed = false)
        val admin = AdminFeatureConfig(razorpayEnabled = true, razorpayConfigured = true)

        assertFalse(superAdmin.resolve(admin).razorpay)
    }

    @Test
    fun `razorpay requires allowance enablement and configuration`() {
        val superAdmin = FeatureConfig(razorpayAllowed = true)
        val configured = AdminFeatureConfig(razorpayEnabled = true, razorpayConfigured = true)

        assertTrue(superAdmin.resolve(configured).razorpay)
        assertFalse(superAdmin.resolve(configured.copy(razorpayConfigured = false)).razorpay)
    }
}

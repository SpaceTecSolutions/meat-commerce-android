package com.spacetecsolutions.meatapp.feature.address

import com.spacetecsolutions.meatapp.core.model.PhoneNumberConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressValidatorTest {
    private val validator = AddressValidator(PhoneNumberConfig("+91", 10))
    private fun valid() = AddressFormState(
        name = "Akash Kumar", mobile = "9876543210", address = "12 Market Road",
        city = "Bengaluru", state = "Karnataka", postalCode = "560001",
        latitude = 12.97, longitude = 77.59,
    )

    @Test
    fun `delivery pin is required and invalid coordinates are rejected`() {
        assertTrue(validator.validate(valid().copy(latitude = null, longitude = null)).containsKey("location"))
        assertTrue(validator.validate(valid().copy(latitude = Double.NaN)).containsKey("location"))
        assertTrue(validator.validate(valid().copy(longitude = 200.0)).containsKey("location"))
    }

    @Test
    fun `valid Indian address is accepted and mobile normalized`() {
        assertTrue(validator.validate(valid()).isEmpty())
        assertEquals("+919876543210", validator.normalizedMobile("09876543210"))
    }

    @Test
    fun `invalid required fields mobile and postal code are rejected`() {
        val errors = validator.validate(valid().copy(name = "", mobile = "123", postalCode = "000000"))
        assertTrue(errors.keys.containsAll(listOf("name", "mobile", "postalCode")))
    }
}

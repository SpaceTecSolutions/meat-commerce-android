package com.spacetecsolutions.meatapp.feature.auth.validation

import com.spacetecsolutions.meatapp.core.model.PasswordPolicy
import com.spacetecsolutions.meatapp.core.model.PhoneNumberConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {
    private val validator = AuthValidator(PhoneNumberConfig(), PasswordPolicy())

    @Test
    fun `india phone input is normalized to e164`() {
        assertEquals(ValidationResult.Valid("+919876543210"), validator.mobile("09876 543210"))
        assertEquals(ValidationResult.Valid("+919876543210"), validator.mobile("+91-9876543210"))
    }

    @Test
    fun `invalid mobile and weak password are rejected`() {
        assertTrue(validator.mobile("123") is ValidationResult.Invalid)
        assertTrue(validator.password("weak") is ValidationResult.Invalid)
    }
}

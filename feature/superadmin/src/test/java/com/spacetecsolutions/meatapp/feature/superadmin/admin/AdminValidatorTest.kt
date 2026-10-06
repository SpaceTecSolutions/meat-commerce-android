package com.spacetecsolutions.meatapp.feature.superadmin.admin

import com.spacetecsolutions.meatapp.core.model.PasswordPolicy
import com.spacetecsolutions.meatapp.core.model.PhoneNumberConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminValidatorTest {
    private val validator = AdminValidator(PhoneNumberConfig("+91", 10), PasswordPolicy())

    @Test
    fun `valid national mobile is canonicalized`() {
        assertEquals(
            AdminValidation.Valid("+919876543210"),
            validator.mobile("98765 43210"),
        )
    }

    @Test
    fun `invalid admin fields are rejected`() {
        assertTrue(validator.name(" ") is AdminValidation.Invalid)
        assertTrue(validator.mobile("123") is AdminValidation.Invalid)
        assertTrue(validator.password("password") is AdminValidation.Invalid)
    }

    @Test
    fun `configured password policy is enforced`() {
        assertEquals(AdminValidation.Valid("StrongPass1"), validator.password("StrongPass1"))
    }
}

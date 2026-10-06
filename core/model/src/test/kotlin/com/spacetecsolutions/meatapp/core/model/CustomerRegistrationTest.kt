package com.spacetecsolutions.meatapp.core.model

import org.junit.Assert.assertThrows
import org.junit.Test

class CustomerRegistrationTest {
    @Test
    fun `self registration cannot create a privileged role`() {
        assertThrows(IllegalArgumentException::class.java) {
            CustomerRegistration(
                firstName = "Admin",
                lastName = "User",
                mobileNumber = "+919876543210",
                password = "Password1",
                verificationToken = "verified-token",
                role = UserRole.ADMIN,
            )
        }
    }

    @Test
    fun `self registration defaults to customer`() {
        val registration = CustomerRegistration(
            firstName = "New",
            lastName = "Customer",
            mobileNumber = "+919876543210",
            password = "Password1",
            verificationToken = "verified-token",
        )
        org.junit.Assert.assertEquals(UserRole.CUSTOMER, registration.role)
    }
}

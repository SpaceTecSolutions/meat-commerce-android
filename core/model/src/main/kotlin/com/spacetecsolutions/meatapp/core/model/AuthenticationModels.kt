package com.spacetecsolutions.meatapp.core.model

data class CustomerRegistration(
    val firstName: String,
    val lastName: String,
    val mobileNumber: String,
    val password: String,
    val verificationToken: String,
    val role: UserRole = UserRole.CUSTOMER,
) {
    init {
        require(role == UserRole.CUSTOMER) { "Self-registration can create CUSTOMER only" }
    }
}

data class RegistrationChallenge(
    val challengeId: String,
    val maskedDestination: String,
    val automaticallyVerified: Boolean = false,
)

data class VerifiedRegistrationChallenge(
    val verificationToken: String,
    val accountExists: Boolean,
)

data class PasswordResetChallenge(
    val challengeId: String,
    val maskedDestination: String,
    val automaticallyVerified: Boolean = false,
)

data class VerifiedResetChallenge(val resetToken: String)

data class PhoneNumberConfig(
    val countryCode: String = "+91",
    val nationalNumberLength: Int = 10,
)

data class PasswordPolicy(
    val minimumLength: Int = 8,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigit: Boolean = true,
)

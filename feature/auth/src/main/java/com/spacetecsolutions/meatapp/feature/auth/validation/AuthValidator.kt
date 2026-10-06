package com.spacetecsolutions.meatapp.feature.auth.validation

import com.spacetecsolutions.meatapp.core.model.PasswordPolicy
import com.spacetecsolutions.meatapp.core.model.PhoneNumberConfig
import javax.inject.Inject

sealed interface ValidationResult {
    data class Valid(val value: String) : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}

class AuthValidator @Inject constructor(
    private val phoneConfig: PhoneNumberConfig,
    private val passwordPolicy: PasswordPolicy,
) {
    fun mobile(value: String): ValidationResult {
        val compact = value.filterNot { it.isWhitespace() || it == '-' || it == '(' || it == ')' }
        val national = when {
            compact.startsWith(phoneConfig.countryCode) -> compact.removePrefix(phoneConfig.countryCode)
            compact.startsWith("0") -> compact.drop(1)
            else -> compact
        }
        if (national.length != phoneConfig.nationalNumberLength || national.any { !it.isDigit() }) {
            return ValidationResult.Invalid("Enter a valid mobile number")
        }
        return ValidationResult.Valid(phoneConfig.countryCode + national)
    }

    fun password(value: String): ValidationResult {
        val issue = when {
            value.isBlank() -> "Password is required"
            value.length < passwordPolicy.minimumLength ->
                "Use at least ${passwordPolicy.minimumLength} characters"
            passwordPolicy.requireUppercase && value.none(Char::isUpperCase) ->
                "Include an uppercase letter"
            passwordPolicy.requireLowercase && value.none(Char::isLowerCase) ->
                "Include a lowercase letter"
            passwordPolicy.requireDigit && value.none(Char::isDigit) -> "Include a number"
            else -> null
        }
        return issue?.let(ValidationResult::Invalid) ?: ValidationResult.Valid(value)
    }

    fun displayName(value: String): ValidationResult = when {
        value.isBlank() -> ValidationResult.Invalid("Name is required")
        value.trim().length < 2 -> ValidationResult.Invalid("Enter your full name")
        else -> ValidationResult.Valid(value.trim())
    }

    fun verificationCode(value: String): ValidationResult =
        if (value.length == 6 && value.all(Char::isDigit)) ValidationResult.Valid(value)
        else ValidationResult.Invalid("Enter the 6-digit verification code")
}

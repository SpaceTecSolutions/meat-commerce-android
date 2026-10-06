package com.spacetecsolutions.meatapp.feature.superadmin.admin

import com.spacetecsolutions.meatapp.core.model.PasswordPolicy
import com.spacetecsolutions.meatapp.core.model.PhoneNumberConfig
import javax.inject.Inject

sealed interface AdminValidation {
    data class Valid(val value: String) : AdminValidation
    data class Invalid(val message: String) : AdminValidation
}

class AdminValidator @Inject constructor(
    private val phoneConfig: PhoneNumberConfig,
    private val passwordPolicy: PasswordPolicy,
) {
    fun name(value: String): AdminValidation = when {
        value.isBlank() -> AdminValidation.Invalid("Name is required")
        value.trim().length < 2 -> AdminValidation.Invalid("Enter the Admin's full name")
        value.trim().length > 80 -> AdminValidation.Invalid("Name is too long")
        else -> AdminValidation.Valid(value.trim())
    }

    fun mobile(value: String): AdminValidation {
        val compact = value.filterNot { it.isWhitespace() || it in "-()" }
        val national = when {
            compact.startsWith(phoneConfig.countryCode) -> compact.removePrefix(phoneConfig.countryCode)
            compact.startsWith("0") -> compact.drop(1)
            else -> compact
        }
        return if (national.length == phoneConfig.nationalNumberLength && national.all(Char::isDigit)) {
            AdminValidation.Valid(phoneConfig.countryCode + national)
        } else AdminValidation.Invalid("Enter a valid mobile number")
    }

    fun password(value: String): AdminValidation {
        val message = when {
            value.isBlank() -> "Temporary password is required"
            value.length < passwordPolicy.minimumLength ->
                "Use at least ${passwordPolicy.minimumLength} characters"
            passwordPolicy.requireUppercase && value.none(Char::isUpperCase) -> "Include an uppercase letter"
            passwordPolicy.requireLowercase && value.none(Char::isLowerCase) -> "Include a lowercase letter"
            passwordPolicy.requireDigit && value.none(Char::isDigit) -> "Include a number"
            else -> null
        }
        return message?.let(AdminValidation::Invalid) ?: AdminValidation.Valid(value)
    }
}

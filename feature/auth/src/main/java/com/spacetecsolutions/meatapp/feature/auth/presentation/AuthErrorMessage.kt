package com.spacetecsolutions.meatapp.feature.auth.presentation

import com.spacetecsolutions.meatapp.core.common.result.AppError

data class AuthFeedback(val text: String, val success: Boolean = false)

internal fun AppError.toUserMessage(): String = when (this) {
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    AppError.InvalidCredentials, AppError.Unauthorized -> "Invalid mobile number or password"
    AppError.DisabledUser -> "This account has been disabled. Contact support"
    AppError.NotFound -> "No account is registered with this mobile number"
    AppError.DuplicateAccount -> "An account already exists for this mobile number"
    AppError.InvalidVerificationCode -> "The verification code is invalid or expired"
    AppError.TooManyRequests -> "Too many attempts. Please try again later"
    is AppError.Validation -> message
    else -> "Something went wrong. Please try again"
}

internal fun AppError.isPasswordError(): Boolean = this is AppError.Validation && field == "password"

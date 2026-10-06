package com.spacetecsolutions.meatapp.core.common.result

sealed interface AppError {
    data object Network : AppError
    data object Offline : AppError
    data object Unauthorized : AppError
    data object Forbidden : AppError
    data object NotFound : AppError
    data object Timeout : AppError
    data object InvalidCredentials : AppError
    data object DisabledUser : AppError
    data object DuplicateAccount : AppError
    data object InvalidVerificationCode : AppError
    data object TooManyRequests : AppError
    data class Validation(val field: String? = null, val message: String) : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}

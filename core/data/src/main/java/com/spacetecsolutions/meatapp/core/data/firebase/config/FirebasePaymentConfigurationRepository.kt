package com.spacetecsolutions.meatapp.core.data.firebase.config

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.PaymentConfigurationRepository
import com.spacetecsolutions.meatapp.core.model.PaymentConfig
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebasePaymentConfigurationRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : PaymentConfigurationRepository {
    override suspend fun get() = call("adminGetPaymentConfig", emptyMap())

    override suspend fun update(config: PaymentConfig) = call(
        "adminUpdatePaymentConfig",
        mapOf(
            "codEnabled" to config.codEnabled,
            "razorpayEnabled" to config.razorpayEnabled,
            "upiEnabled" to config.upiEnabled,
            "expectedRevision" to config.revision,
        ),
    )

    override suspend fun verifyRazorpay(): AppResult<Boolean> = try {
        val response = functions.getHttpsCallable("adminVerifyRazorpayConfiguration").call().await().data as? Map<*, *>
        AppResult.Success(response?.get("configured") == true)
    } catch (error: Exception) {
        val exception = error as? FirebaseFunctionsException
        AppResult.Failure(when (exception?.code) {
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
            FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
            else -> AppError.Validation(message = exception?.message ?: "Razorpay credentials could not be verified")
        })
    }

    private suspend fun call(name: String, payload: Map<String, Any>): AppResult<PaymentConfig> = try {
        val response = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: return AppResult.Failure(AppError.Unknown())
        val map = response["config"] as? Map<*, *> ?: response
        AppResult.Success(map.toPaymentConfig())
    } catch (error: Exception) {
        val exception = error as? FirebaseFunctionsException
        val appError = when (exception?.code) {
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
            FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                AppError.Validation(message = "Check payment settings")
            FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                AppError.Validation(message = exception.message ?: "Payment method is unavailable")
            FirebaseFunctionsException.Code.ABORTED ->
                AppError.Validation(message = "Settings changed. Refresh and retry")
            FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
            else -> AppError.Unknown(error)
        }
        AppResult.Failure(appError)
    }
}

private fun Map<*, *>.toPaymentConfig() = PaymentConfig(
    codEnabled = this["codEnabled"] as? Boolean ?: false,
    razorpayEnabled = this["razorpayEnabled"] as? Boolean ?: false,
    razorpayConfigured = this["razorpayConfigured"] as? Boolean ?: false,
    upiEnabled = this["upiEnabled"] as? Boolean ?: false,
    upiVpa = this["upiVpa"] as? String,
    revision = (this["revision"] as? Number)?.toLong() ?: 0,
)

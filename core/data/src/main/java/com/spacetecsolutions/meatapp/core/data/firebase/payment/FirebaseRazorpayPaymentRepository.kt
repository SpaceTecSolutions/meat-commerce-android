package com.spacetecsolutions.meatapp.core.data.firebase.payment

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.RazorpayPaymentRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseRazorpayPaymentRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : RazorpayPaymentRepository {
    override suspend fun createSession(appOrderId: String, idempotencyKey: String) = sessionCall(
        "customerCreateRazorpayOrder", mapOf("appOrderId" to appOrderId, "idempotencyKey" to idempotencyKey),
    )
    override suspend fun retry(attemptId: String, idempotencyKey: String) = sessionCall(
        "customerRetryRazorpayPayment", mapOf("attemptId" to attemptId, "idempotencyKey" to idempotencyKey),
    )
    override suspend fun markSdkOpened(attemptId: String) = attemptCall(
        "customerMarkRazorpaySdkOpened", mapOf("attemptId" to attemptId),
    )
    override suspend fun verify(attemptId: String, callback: RazorpaySdkResult.Success) = attemptCall(
        "customerVerifyRazorpayPayment", mapOf(
            "attemptId" to attemptId, "callbackId" to callback.callbackId,
            "razorpayPaymentId" to callback.paymentId,
            "razorpayOrderId" to callback.razorpayOrderId, "razorpaySignature" to callback.signature,
        ),
    )
    override suspend fun recordFailure(attemptId: String, callback: RazorpaySdkResult.Failure) = attemptCall(
        "customerRecordRazorpayFailure", mapOf(
            "attemptId" to attemptId, "callbackId" to callback.callbackId,
            "code" to callback.code, "description" to callback.description,
            "cancelled" to callback.cancelled,
        ),
    )
    override suspend fun reconcilePending(): AppResult<PaymentAttempt?> = try {
        val response = functions.getHttpsCallable("customerReconcilePendingRazorpayPayment").call().await().data
            as? Map<*, *> ?: return AppResult.Success(null)
        val raw = response["attempt"] as? Map<*, *> ?: return AppResult.Success(null)
        AppResult.Success(raw.toAttempt())
    } catch (error: Exception) { AppResult.Failure(error.toPaymentError()) }

    private suspend fun sessionCall(name: String, payload: Map<String, Any>): AppResult<RazorpayPaymentSession> = try {
        val data = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid Razorpay session")
        val raw = data["session"] as? Map<*, *> ?: data
        AppResult.Success(raw.toSession())
    } catch (error: Exception) { AppResult.Failure(error.toPaymentError()) }
    private suspend fun attemptCall(name: String, payload: Map<String, Any>): AppResult<PaymentAttempt> = try {
        val data = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid payment attempt")
        AppResult.Success((data["attempt"] as? Map<*, *> ?: data).toAttempt())
    } catch (error: Exception) { AppResult.Failure(error.toPaymentError()) }
}

private fun Map<*, *>.toSession() = RazorpayPaymentSession(
    attemptId = string("attemptId"), appOrderId = string("appOrderId"),
    razorpayOrderId = string("razorpayOrderId"), publicKeyId = string("publicKeyId"),
    amountMinor = long("amountMinor"), currencyCode = string("currencyCode"),
    merchantName = string("merchantName"), description = string("description"),
    customerName = this["customerName"] as? String, customerMobile = this["customerMobile"] as? String,
    customerEmail = this["customerEmail"] as? String,
)
private fun Map<*, *>.toAttempt() = PaymentAttempt(
    attemptId = string("attemptId"), appOrderId = string("appOrderId"),
    status = enum("status", PaymentAttemptStatus.VERIFICATION_PENDING),
    paymentStatus = enum("paymentStatus", PaymentStatus.PROCESSING),
    failureMessage = this["failureMessage"] as? String,
    retryAllowed = this["retryAllowed"] as? Boolean ?: false, revision = long("revision"),
)
private fun Map<*, *>.string(key: String) = this[key] as? String ?: error("Missing $key")
private fun Map<*, *>.long(key: String) = (this[key] as? Number)?.toLong() ?: 0
private inline fun <reified T : Enum<T>> Map<*, *>.enum(key: String, default: T) =
    runCatching { enumValueOf<T>(string(key)) }.getOrDefault(default)
private fun Throwable.toPaymentError(): AppError {
    val exception = this as? FirebaseFunctionsException
    val reason = (exception?.details as? Map<*, *>)?.get("reason") as? String
    return when {
        reason == "RAZORPAY_DISABLED" -> AppError.Forbidden
        reason == "VERIFICATION_PENDING" -> AppError.Validation(message = "Payment verification is pending")
        exception?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        exception?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        exception?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        exception?.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> AppError.Timeout
        else -> AppError.Unknown(this)
    }
}

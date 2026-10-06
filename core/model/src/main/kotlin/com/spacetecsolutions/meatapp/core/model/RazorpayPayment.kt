package com.spacetecsolutions.meatapp.core.model

enum class PaymentAttemptStatus {
    CREATED, SDK_OPEN, VERIFICATION_PENDING, PAID, FAILED, CANCELLED,
    REFUND_PENDING, REFUNDED, PARTIALLY_REFUNDED,
}

data class RazorpayPaymentSession(
    val attemptId: String,
    val appOrderId: String,
    val razorpayOrderId: String,
    val publicKeyId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val merchantName: String,
    val description: String,
    val customerName: String? = null,
    val customerMobile: String? = null,
    val customerEmail: String? = null,
) {
    init { require(amountMinor > 0) }
}

data class PaymentAttempt(
    val attemptId: String,
    val appOrderId: String,
    val status: PaymentAttemptStatus,
    val paymentStatus: PaymentStatus,
    val failureMessage: String? = null,
    val retryAllowed: Boolean = false,
    val revision: Long = 0,
)

sealed interface RazorpaySdkResult {
    val callbackId: String
    data class Success(
        override val callbackId: String,
        val paymentId: String,
        val razorpayOrderId: String,
        val signature: String,
    ) : RazorpaySdkResult
    data class Failure(
        override val callbackId: String,
        val code: Int,
        val description: String,
        val cancelled: Boolean,
    ) : RazorpaySdkResult
}

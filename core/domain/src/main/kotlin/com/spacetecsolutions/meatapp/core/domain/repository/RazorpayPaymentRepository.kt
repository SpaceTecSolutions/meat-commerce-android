package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.*

interface RazorpayPaymentRepository {
    suspend fun createSession(appOrderId: String, idempotencyKey: String): AppResult<RazorpayPaymentSession>
    suspend fun markSdkOpened(attemptId: String): AppResult<PaymentAttempt>
    suspend fun verify(attemptId: String, callback: RazorpaySdkResult.Success): AppResult<PaymentAttempt>
    suspend fun recordFailure(attemptId: String, callback: RazorpaySdkResult.Failure): AppResult<PaymentAttempt>
    suspend fun reconcilePending(): AppResult<PaymentAttempt?>
    suspend fun retry(attemptId: String, idempotencyKey: String): AppResult<RazorpayPaymentSession>
}

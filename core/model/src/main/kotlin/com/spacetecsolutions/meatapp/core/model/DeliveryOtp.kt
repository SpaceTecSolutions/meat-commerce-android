package com.spacetecsolutions.meatapp.core.model

/** Customer-visible code for one active delivery session; never persisted in the order model. */
data class DeliveryOtp(
    val code: String,
    val sessionId: String,
    val expiresAtEpochMillis: Long,
)

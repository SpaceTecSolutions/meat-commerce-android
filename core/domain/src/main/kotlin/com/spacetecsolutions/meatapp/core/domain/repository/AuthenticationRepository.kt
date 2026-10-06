package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CustomerRegistration
import com.spacetecsolutions.meatapp.core.model.PasswordResetChallenge
import com.spacetecsolutions.meatapp.core.model.RegistrationChallenge
import com.spacetecsolutions.meatapp.core.model.VerifiedRegistrationChallenge
import com.spacetecsolutions.meatapp.core.model.VerifiedResetChallenge
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {
    val authenticatedUserId: Flow<String?>

    suspend fun signIn(mobileNumber: String, password: String): AppResult<String>
    suspend fun requestCustomerOtp(mobileNumber: String): AppResult<RegistrationChallenge>
    suspend fun signInCustomerWithOtp(challengeId: String, code: String): AppResult<String>
    suspend fun requestCustomerRegistration(mobileNumber: String): AppResult<RegistrationChallenge>
    suspend fun verifyCustomerRegistration(
        challengeId: String,
        verificationCode: String,
    ): AppResult<VerifiedRegistrationChallenge>
    suspend fun registerCustomer(registration: CustomerRegistration): AppResult<String>
    suspend fun requestPasswordReset(mobileNumber: String): AppResult<PasswordResetChallenge>
    suspend fun verifyPasswordResetCode(
        challengeId: String,
        verificationCode: String,
    ): AppResult<VerifiedResetChallenge>
    suspend fun resetPassword(resetToken: String, newPassword: String): AppResult<Unit>
    suspend fun signOut(): AppResult<Unit>
}

package com.spacetecsolutions.meatapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.domain.repository.AuthenticationRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerCartRepository
import com.spacetecsolutions.meatapp.core.domain.repository.UserRepository
import com.spacetecsolutions.meatapp.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class CustomerAuthPhase { PHONE, REQUESTING, OTP, CONVERGING, VERIFYING, INVALID, SUCCESS }

internal data class CustomerAuthState(
    val open: Boolean = false,
    val phase: CustomerAuthPhase = CustomerAuthPhase.PHONE,
    val phone: String = "",
    val otp: String = "",
    val challengeId: String? = null,
    val secondsUntilResend: Int = 0,
    val error: String? = null,
    val completedId: Int = 0,
    val verificationStartedAt: Long = 0L,
)

@HiltViewModel
internal class CustomerAuthViewModel @Inject constructor(
    private val auth: AuthenticationRepository,
    private val cart: CustomerCartRepository,
    private val users: UserRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(CustomerAuthState())
    val state = mutable.asStateFlow()
    private var timer: Job? = null
    private var request: Job? = null

    fun open() { if (!state.value.open) mutable.update { CustomerAuthState(open = true) } }
    fun close() {
        if (state.value.phase in setOf(CustomerAuthPhase.CONVERGING, CustomerAuthPhase.VERIFYING)) return
        timer?.cancel(); request?.cancel(); mutable.value = CustomerAuthState()
    }
    fun phone(value: String) = mutable.update { it.copy(phone = value.filter(Char::isDigit).take(10), error = null) }
    fun changeNumber() {
        timer?.cancel(); mutable.update { it.copy(phase = CustomerAuthPhase.PHONE, otp = "",
            challengeId = null, error = null, secondsUntilResend = 0) }
    }
    fun requestOtp() {
        if (state.value.phase !in setOf(CustomerAuthPhase.PHONE, CustomerAuthPhase.OTP, CustomerAuthPhase.INVALID)) return
        if (state.value.phone.length != 10) {
            mutable.update { it.copy(error = "Enter a valid 10-digit mobile number") }; return
        }
        if (state.value.challengeId != null && state.value.secondsUntilResend > 0) return
        mutable.update { it.copy(phase = CustomerAuthPhase.REQUESTING, error = null) }
        request = viewModelScope.launch {
            when (val result = auth.requestCustomerOtp("+91${state.value.phone}")) {
                is AppResult.Success -> {
                    mutable.update { it.copy(phase = CustomerAuthPhase.OTP, challengeId = result.value.challengeId,
                        otp = "", secondsUntilResend = 30) }
                    timer?.cancel()
                    timer = viewModelScope.launch {
                        while (state.value.open && state.value.secondsUntilResend > 0) {
                            delay(1000); mutable.update { it.copy(secondsUntilResend = (it.secondsUntilResend - 1).coerceAtLeast(0)) }
                        }
                    }
                    if (result.value.automaticallyVerified) verify("")
                }
                is AppResult.Failure -> mutable.update { it.copy(phase = CustomerAuthPhase.PHONE,
                    error = result.error.otpRequestMessage()) }
            }
        }
    }
    fun otp(value: String) {
        if (state.value.phase !in setOf(CustomerAuthPhase.OTP, CustomerAuthPhase.INVALID)) return
        val digits = value.filter(Char::isDigit).take(6)
        mutable.update { it.copy(otp = digits, phase = CustomerAuthPhase.OTP, error = null) }
        if (digits.length == 6) verify(digits)
    }
    fun retryOtp() = mutable.update {
        if (it.phase == CustomerAuthPhase.INVALID) it.copy(phase = CustomerAuthPhase.OTP, otp = "") else it
    }
    private fun verify(code: String) {
        val challenge = state.value.challengeId ?: return
        if (state.value.phase in setOf(CustomerAuthPhase.CONVERGING, CustomerAuthPhase.VERIFYING)) return
        mutable.update { it.copy(phase = CustomerAuthPhase.CONVERGING,
            verificationStartedAt = android.os.SystemClock.elapsedRealtime()) }
        request = viewModelScope.launch {
            // The fan animation is a required part of the sign-in transition. Start verification
            // only after it has completed so a fast response can never skip the animation.
            delay(OTP_CONVERGENCE_DURATION_MS)
            mutable.update { current ->
                if (current.phase == CustomerAuthPhase.CONVERGING) {
                    current.copy(phase = CustomerAuthPhase.VERIFYING)
                } else current
            }
            when (val result = auth.signInCustomerWithOtp(challenge, code)) {
                is AppResult.Failure -> {
                    awaitMinimumValidationAnimation()
                    mutable.update { it.copy(phase = CustomerAuthPhase.INVALID,
                        error = result.error.otpMessage()) }
                }
                is AppResult.Success -> completeRoleAwareSignIn(result.value)
            }
        }
    }

    private suspend fun completeRoleAwareSignIn(userId: String) {
        when (val user = users.getUser(userId)) {
            is AppResult.Failure -> failAuthenticatedSession(
                "Unable to load your account. Please request a new OTP and try again.",
            )
            is AppResult.Success -> when {
                !user.value.active -> failAuthenticatedSession("This account is unavailable. Contact support.")
                user.value.role != UserRole.CUSTOMER -> completeSuccess()
                else -> when (cart.mergeGuestCart()) {
                    is AppResult.Failure -> failAuthenticatedSession(
                        "Cart sync failed. Your guest items are saved; request a new OTP to retry.",
                    )
                    is AppResult.Success -> completeSuccess()
                }
            }
        }
    }

    private suspend fun failAuthenticatedSession(message: String) {
        auth.signOut()
        mutable.update { it.copy(phase = CustomerAuthPhase.PHONE, challengeId = null, otp = "", error = message) }
    }

    private suspend fun completeSuccess() {
        awaitMinimumValidationAnimation()
        mutable.update { it.copy(phase = CustomerAuthPhase.SUCCESS, completedId = it.completedId + 1) }
    }
    private suspend fun awaitMinimumValidationAnimation() {
        val elapsed = android.os.SystemClock.elapsedRealtime() - state.value.verificationStartedAt
        if (elapsed in 0 until OTP_CONVERGENCE_DURATION_MS) {
            delay(OTP_CONVERGENCE_DURATION_MS - elapsed)
        }
    }
    fun consumeSuccess() { timer?.cancel(); mutable.value = CustomerAuthState() }
}

private const val OTP_CONVERGENCE_DURATION_MS = 820L

private fun AppError.otpMessage(): String = when (this) {
    AppError.InvalidVerificationCode -> "Incorrect or expired OTP. Request a new code and try again."
    AppError.Network, AppError.Offline, AppError.Timeout ->
        "Couldn't connect to verify your code. Check your connection and try again."
    AppError.TooManyRequests -> "Too many attempts. Please wait before requesting another code."
    AppError.DisabledUser, AppError.Forbidden -> "This account is unavailable. Contact support."
    is AppError.Validation -> message
    else -> "Phone sign-in is temporarily unavailable. Please try again later."
}

private fun AppError.otpRequestMessage(): String = when (this) {
    AppError.Network, AppError.Offline, AppError.Timeout ->
        "Couldn't connect to send the OTP. Check your connection and try again."
    AppError.TooManyRequests -> "SMS limit reached. Please wait before requesting another OTP."
    is AppError.Validation -> message
    else -> "Couldn't send an OTP right now. Please try again later."
}

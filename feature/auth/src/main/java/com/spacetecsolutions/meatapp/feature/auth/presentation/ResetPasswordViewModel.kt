package com.spacetecsolutions.meatapp.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.AuthenticationRepository
import com.spacetecsolutions.meatapp.feature.auth.validation.AuthValidator
import com.spacetecsolutions.meatapp.feature.auth.validation.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ResetStep { REQUEST, VERIFY, NEW_PASSWORD, COMPLETE }

data class ResetPasswordUiState(
    val step: ResetStep = ResetStep.REQUEST,
    val mobile: String = "",
    val verificationCode: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val mobileError: String? = null,
    val codeError: String? = null,
    val passwordError: String? = null,
    val confirmationError: String? = null,
    val feedback: AuthFeedback? = null,
    val maskedDestination: String = "",
    val loading: Boolean = false,
    internal val challengeId: String = "",
    internal val resetToken: String = "",
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val repository: AuthenticationRepository,
    private val validator: AuthValidator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ResetPasswordUiState())
    val state = mutableState.asStateFlow()

    fun updateMobile(value: String) = update { copy(mobile = value.filter(Char::isDigit), mobileError = null) }
    fun updateCode(value: String) = update {
        copy(verificationCode = value.filter(Char::isDigit).take(6), codeError = null)
    }
    fun updatePassword(value: String) = update { copy(newPassword = value, passwordError = null) }
    fun updateConfirmation(value: String) = update { copy(confirmPassword = value, confirmationError = null) }
    fun consumeFeedback() = update { copy(feedback = null) }

    fun requestCode() {
        val mobile = validator.mobile(state.value.mobile)
        if (mobile is ValidationResult.Invalid) return update {
            copy(mobileError = mobile.message, feedback = AuthFeedback(mobile.message))
        }
        launchRequest {
            when (val result = repository.requestPasswordReset((mobile as ValidationResult.Valid).value)) {
                is AppResult.Success -> {
                    update { copy(
                        step = ResetStep.VERIFY, loading = result.value.automaticallyVerified,
                        challengeId = result.value.challengeId, maskedDestination = result.value.maskedDestination,
                        feedback = AuthFeedback("Verification code sent", true),
                    ) }
                    if (result.value.automaticallyVerified) verifyChallenge(result.value.challengeId, "")
                }
                is AppResult.Failure -> fail(result)
            }
        }
    }

    fun verifyCode() {
        val code = validator.verificationCode(state.value.verificationCode)
        if (code is ValidationResult.Invalid) return update {
            copy(codeError = code.message, feedback = AuthFeedback(code.message))
        }
        launchRequest { verifyChallenge(state.value.challengeId, (code as ValidationResult.Valid).value) }
    }

    fun resetPassword() {
        val current = state.value
        val password = validator.password(current.newPassword)
        val passwordError = (password as? ValidationResult.Invalid)?.message
        val confirmationError = "Passwords do not match".takeIf { current.newPassword != current.confirmPassword }
        val error = passwordError ?: confirmationError
        if (error != null) return update { copy(
            passwordError = passwordError, confirmationError = confirmationError, feedback = AuthFeedback(error),
        ) }
        launchRequest {
            when (val result = repository.resetPassword(current.resetToken, current.newPassword)) {
                is AppResult.Success -> update { copy(
                    step = ResetStep.COMPLETE, loading = false,
                    feedback = AuthFeedback("Password updated successfully", true),
                ) }
                is AppResult.Failure -> fail(result)
            }
        }
    }

    private suspend fun verifyChallenge(challengeId: String, code: String) {
        when (val result = repository.verifyPasswordResetCode(challengeId, code)) {
            is AppResult.Success -> update { copy(
                step = ResetStep.NEW_PASSWORD, loading = false, resetToken = result.value.resetToken,
                feedback = AuthFeedback("Mobile number verified", true),
            ) }
            is AppResult.Failure -> fail(result)
        }
    }

    private fun launchRequest(block: suspend () -> Unit) = viewModelScope.launch {
        update { copy(loading = true, feedback = null) }
        block()
    }

    private fun fail(result: AppResult.Failure) = update {
        val message = result.error.toUserMessage()
        copy(
            loading = false,
            passwordError = if (result.error.isPasswordError()) message else passwordError,
            feedback = AuthFeedback(message),
        )
    }

    private inline fun update(transform: ResetPasswordUiState.() -> ResetPasswordUiState) =
        mutableState.update(transform)
}

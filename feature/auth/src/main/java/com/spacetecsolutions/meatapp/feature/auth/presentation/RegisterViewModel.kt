package com.spacetecsolutions.meatapp.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.AuthenticationRepository
import com.spacetecsolutions.meatapp.core.model.CustomerRegistration
import com.spacetecsolutions.meatapp.feature.auth.validation.AuthValidator
import com.spacetecsolutions.meatapp.feature.auth.validation.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RegisterStep { PHONE, VERIFY, DETAILS, ACCOUNT_EXISTS }

data class RegisterUiState(
    val step: RegisterStep = RegisterStep.PHONE,
    val firstName: String = "",
    val lastName: String = "",
    val mobile: String = "",
    val verificationCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val maskedDestination: String = "",
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val mobileError: String? = null,
    val codeError: String? = null,
    val passwordError: String? = null,
    val confirmationError: String? = null,
    val feedback: AuthFeedback? = null,
    val loading: Boolean = false,
    internal val challengeId: String = "",
    internal val verificationToken: String = "",
    internal val canonicalMobile: String = "",
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: AuthenticationRepository,
    private val validator: AuthValidator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RegisterUiState())
    val state = mutableState.asStateFlow()

    fun updateFirstName(value: String) = update { copy(firstName = value, firstNameError = null) }
    fun updateLastName(value: String) = update { copy(lastName = value, lastNameError = null) }
    fun updateMobile(value: String) = update { copy(mobile = value.filter(Char::isDigit), mobileError = null) }
    fun updateCode(value: String) = update {
        copy(verificationCode = value.filter(Char::isDigit).take(6), codeError = null)
    }
    fun updatePassword(value: String) = update { copy(password = value, passwordError = null) }
    fun updateConfirmation(value: String) = update { copy(confirmPassword = value, confirmationError = null) }
    fun changeNumber() = update { RegisterUiState(mobile = mobile) }
    fun consumeFeedback() = update { copy(feedback = null) }

    fun requestCode() {
        val mobile = validator.mobile(state.value.mobile)
        if (mobile is ValidationResult.Invalid) return update {
            copy(mobileError = mobile.message, feedback = AuthFeedback(mobile.message))
        }
        val canonicalMobile = (mobile as ValidationResult.Valid).value
        launchRequest {
            when (val result = repository.requestCustomerRegistration(canonicalMobile)) {
                is AppResult.Success -> {
                    update { copy(
                        step = RegisterStep.VERIFY, loading = result.value.automaticallyVerified,
                        challengeId = result.value.challengeId, maskedDestination = result.value.maskedDestination,
                        canonicalMobile = canonicalMobile,
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

    fun submit() {
        val current = state.value
        val first = validator.displayName(current.firstName)
        val last = validator.displayName(current.lastName)
        val password = validator.password(current.password)
        val firstError = (first as? ValidationResult.Invalid)?.message
        val lastError = (last as? ValidationResult.Invalid)?.message
        val passwordError = (password as? ValidationResult.Invalid)?.message
        val confirmationError = "Passwords do not match".takeIf { current.password != current.confirmPassword }
        val error = firstError ?: lastError ?: passwordError ?: confirmationError
        if (error != null) return update { copy(
            firstNameError = firstError, lastNameError = lastError, passwordError = passwordError,
            confirmationError = confirmationError, feedback = AuthFeedback(error),
        ) }
        launchRequest {
            when (val result = repository.registerCustomer(CustomerRegistration(
                firstName = (first as ValidationResult.Valid).value,
                lastName = (last as ValidationResult.Valid).value,
                mobileNumber = current.canonicalMobile,
                password = (password as ValidationResult.Valid).value,
                verificationToken = current.verificationToken,
            ))) {
                is AppResult.Success -> update {
                    copy(loading = false, feedback = AuthFeedback("Account created successfully", true))
                }
                is AppResult.Failure -> fail(result)
            }
        }
    }

    private suspend fun verifyChallenge(challengeId: String, code: String) {
        when (val result = repository.verifyCustomerRegistration(challengeId, code)) {
            is AppResult.Success -> update { copy(
                step = if (result.value.accountExists) RegisterStep.ACCOUNT_EXISTS else RegisterStep.DETAILS,
                loading = false, verificationToken = result.value.verificationToken,
                feedback = AuthFeedback(
                    if (result.value.accountExists) "This mobile number is already registered" else "Mobile number verified",
                    !result.value.accountExists,
                ),
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

    private inline fun update(transform: RegisterUiState.() -> RegisterUiState) = mutableState.update(transform)
}

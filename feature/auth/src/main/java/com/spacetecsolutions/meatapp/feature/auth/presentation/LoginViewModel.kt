package com.spacetecsolutions.meatapp.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.AuthenticationRepository
import com.spacetecsolutions.meatapp.feature.auth.validation.AuthValidator
import com.spacetecsolutions.meatapp.feature.auth.validation.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val mobileNumber: String = "",
    val password: String = "",
    val mobileError: String? = null,
    val passwordError: String? = null,
    val requestError: String? = null,
    val feedback: AuthFeedback? = null,
    val loading: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthenticationRepository,
    private val validator: AuthValidator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = mutableState.asStateFlow()

    fun updateMobile(value: String) = mutableState.update { it.copy(mobileNumber = value, mobileError = null) }
    fun updatePassword(value: String) = mutableState.update { it.copy(password = value, passwordError = null) }
    fun consumeFeedback() = mutableState.update { it.copy(feedback = null) }

    fun submit() {
        val current = state.value
        val mobile = validator.mobile(current.mobileNumber)
        val password = validator.password(current.password)
        if (mobile is ValidationResult.Invalid || password is ValidationResult.Invalid) {
            mutableState.update {
                it.copy(
                    mobileError = (mobile as? ValidationResult.Invalid)?.message,
                    passwordError = (password as? ValidationResult.Invalid)?.message,
                    feedback = AuthFeedback(
                        (mobile as? ValidationResult.Invalid)?.message
                            ?: (password as ValidationResult.Invalid).message,
                    ),
                )
            }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, requestError = null, feedback = null) }
            when (val result = repository.signIn(
                (mobile as ValidationResult.Valid).value,
                (password as ValidationResult.Valid).value,
            )) {
                is AppResult.Success -> mutableState.update {
                    it.copy(loading = false, feedback = AuthFeedback("Login successful", true))
                }
                is AppResult.Failure -> mutableState.update {
                    val message = result.error.toUserMessage()
                    it.copy(
                        loading = false, requestError = message,
                        passwordError = message.takeIf {
                            result.error == com.spacetecsolutions.meatapp.core.common.result.AppError.InvalidCredentials ||
                                result.error.isPasswordError()
                        },
                        feedback = AuthFeedback(message),
                    )
                }
            }
        }
    }
}

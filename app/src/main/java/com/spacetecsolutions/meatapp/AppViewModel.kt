package com.spacetecsolutions.meatapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.data.preferences.AppPreferences
import com.spacetecsolutions.meatapp.core.domain.repository.AuthenticationRepository
import com.spacetecsolutions.meatapp.core.domain.repository.UserRepository
import com.spacetecsolutions.meatapp.core.domain.repository.FeatureManagementRepository
import com.spacetecsolutions.meatapp.core.domain.repository.MessagingRepository
import com.spacetecsolutions.meatapp.core.model.FeatureConfig
import com.spacetecsolutions.meatapp.core.model.User
import com.spacetecsolutions.meatapp.core.model.UserRole
import com.spacetecsolutions.meatapp.core.domain.service.CartBadgeStore
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerCartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data class Unauthenticated(val message: String? = null) : SessionState
    data class Authenticated(val role: UserRole) : SessionState
    data class Error(val message: String) : SessionState
}

data class AppUiState(
    val darkThemeEnabled: Boolean? = null,
    val session: SessionState = SessionState.Loading,
    val cartQuantity: Int = 0,
    val featureConfig: FeatureConfig? = null,
    val authenticatedUser: User? = null,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    preferences: AppPreferences,
    private val authenticationRepository: AuthenticationRepository,
    private val userRepository: UserRepository,
    cartBadgeStore: CartBadgeStore,
    private val featureRepository: FeatureManagementRepository,
    private val messagingRepository: MessagingRepository,
    private val customerCart: CustomerCartRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            cartBadgeStore.quantity.collect { quantity ->
                mutableState.update { it.copy(cartQuantity = quantity) }
            }
        }
        viewModelScope.launch {
            preferences.darkThemeEnabled.collect { dark ->
                mutableState.update { it.copy(darkThemeEnabled = dark) }
            }
        }
        viewModelScope.launch {
            userRepository.observeAuthenticatedUser().collect(::handleUserResult)
        }
        viewModelScope.launch {
            featureRepository.featureConfig.collect { result ->
                mutableState.update { state -> state.copy(
                    featureConfig = (result as? AppResult.Success)?.value,
                ) }
            }
        }
    }

    private suspend fun handleUserResult(result: AppResult<User?>) {
        when (result) {
            is AppResult.Success -> handleUser(result.value)
            is AppResult.Failure -> if (result.error == AppError.NotFound) {
                // The deleted profile can disappear before the Auth listener reports sign-out.
                authenticationRepository.signOut()
                customerCart.clearGuestCart()
                handleUser(null)
            } else mutableState.update {
                it.copy(session = SessionState.Error(result.error.sessionMessage()))
            }
        }
    }

    private suspend fun handleUser(user: User?) {
        when {
            user == null -> {
                val message = (mutableState.value.session as? SessionState.Unauthenticated)?.message
                mutableState.update { it.copy(session = SessionState.Unauthenticated(message), authenticatedUser = null) }
                featureRepository.refresh()
            }
            !user.active -> {
                mutableState.update {
                    it.copy(session = SessionState.Unauthenticated("This account has been disabled. Contact support"))
                }
                authenticationRepository.signOut()
            }
            else -> {
                mutableState.update { it.copy(session = SessionState.Authenticated(user.role), authenticatedUser = user) }
                when (val token = messagingRepository.getRegistrationToken()) {
                    is AppResult.Success -> messagingRepository.registerDeviceToken(token.value)
                    is AppResult.Failure -> Unit
                }
                // The repository may have attempted its initial fetch before Firebase Auth was restored.
                // Refresh for every authenticated role so protected configuration never remains in that
                // pre-authentication error state.
                featureRepository.refresh()
            }
        }
    }

    fun retrySession() {
        mutableState.update { it.copy(session = SessionState.Loading) }
        viewModelScope.launch {
            val userId = authenticationRepository.authenticatedUserId.first()
            if (userId == null) handleUser(null) else handleUserResult(userRepository.getUser(userId))
        }
    }

    fun signOut() {
        if (mutableState.value.session == SessionState.Loading) return
        mutableState.update { it.copy(session = SessionState.Loading) }
        viewModelScope.launch {
            messagingRepository.deleteRegistrationToken()
            customerCart.clearGuestCart()
            mutableState.update { it.copy(cartQuantity = 0) }
            when (val result = authenticationRepository.signOut()) {
                is AppResult.Success -> mutableState.update {
                    it.copy(session = SessionState.Unauthenticated(), authenticatedUser = null)
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(session = SessionState.Error("Unable to log out. Try again"))
                }
            }
        }
    }
}

private fun AppError.sessionMessage(): String = when (this) {
    AppError.Network, AppError.Offline, AppError.Timeout -> "Unable to restore your session. Check your connection"
    AppError.NotFound -> "Your user profile could not be found. Contact support"
    else -> "Unable to restore your session"
}

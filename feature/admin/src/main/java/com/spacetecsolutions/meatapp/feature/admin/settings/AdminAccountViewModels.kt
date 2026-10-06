package com.spacetecsolutions.meatapp.feature.admin.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.AdminShopSettingsRepository
import com.spacetecsolutions.meatapp.core.model.AdminShopSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminProfileState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val mobile: String = "",
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class AdminProfileViewModel @Inject constructor(
    private val repository: AdminShopSettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminProfileState())
    val state = mutableState.asStateFlow()
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.getSettings()) {
            is AppResult.Success -> {
                val name = result.value.profileName.trim()
                mutableState.update { it.copy(loading = false, firstName = name.substringBefore(' '),
                    lastName = name.substringAfter(' ', ""), mobile = result.value.profileMobile) }
            }
            is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error.accountText()) }
        }
    }
    fun firstName(value: String) = mutableState.update { it.copy(firstName = value.take(50), error = null) }
    fun lastName(value: String) = mutableState.update { it.copy(lastName = value.take(50), error = null) }
    fun save() {
        val value = state.value
        if (value.firstName.trim().length < 2) {
            mutableState.update { it.copy(error = "Enter a valid first name") }; return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, error = null) }
            when (val result = repository.updateProfile(value.firstName.trim(), value.lastName.trim())) {
                is AppResult.Success -> mutableState.update { it.copy(saving = false, saved = true) }
                is AppResult.Failure -> mutableState.update { it.copy(saving = false, error = result.error.accountText()) }
            }
        }
    }
    fun consumeSaved() = mutableState.update { it.copy(saved = false) }
}

data class AdminPasswordState(
    val current: String = "", val password: String = "", val confirmation: String = "",
    val saving: Boolean = false, val error: String? = null, val changed: Boolean = false,
)

@HiltViewModel
class AdminPasswordViewModel @Inject constructor(
    private val repository: AdminShopSettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminPasswordState())
    val state = mutableState.asStateFlow()
    fun current(value: String) = mutableState.update { it.copy(current = value.take(128), error = null) }
    fun password(value: String) = mutableState.update { it.copy(password = value.take(128), error = null) }
    fun confirmation(value: String) = mutableState.update { it.copy(confirmation = value.take(128), error = null) }
    fun save() {
        val value = state.value
        val error = when {
            value.current.isBlank() -> "Current password is required"
            value.password.length < 8 || value.password.none(Char::isUpperCase) ||
                value.password.none(Char::isLowerCase) || value.password.none(Char::isDigit) ->
                "Use 8+ characters with uppercase, lowercase and a number"
            value.password != value.confirmation -> "Passwords do not match"
            value.current == value.password -> "New password must be different"
            else -> null
        }
        if (error != null) { mutableState.update { it.copy(error = error) }; return }
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, error = null) }
            when (val result = repository.changePassword(value.current, value.password)) {
                is AppResult.Success -> mutableState.update { AdminPasswordState(changed = true) }
                is AppResult.Failure -> mutableState.update { it.copy(saving = false, error = result.error.accountText()) }
            }
        }
    }
    fun consumeChanged() = mutableState.update { it.copy(changed = false) }
}

data class AdminHelpState(
    val loading: Boolean = true, val saving: Boolean = false,
    val settings: AdminShopSettings? = null, val error: String? = null, val saved: Boolean = false,
)

@HiltViewModel
class AdminHelpViewModel @Inject constructor(
    private val repository: AdminShopSettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminHelpState())
    val state = mutableState.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.getSettings()) {
            is AppResult.Success -> mutableState.update { it.copy(loading = false, settings = result.value) }
            is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error.accountText()) }
        }
    }
    fun update(value: AdminShopSettings) = mutableState.update { it.copy(settings = value, error = null) }
    fun save() {
        val value = state.value.settings ?: return
        val error = when {
            value.supportEmail.orEmpty().isNotBlank() && !value.supportEmail.orEmpty().matches(Regex("^\\S+@\\S+\\.\\S+$")) ->
                "Enter a valid support email"
            value.supportPhone.orEmpty().isNotBlank() && value.supportPhone.orEmpty().count(Char::isDigit) !in 10..15 ->
                "Enter a valid call number"
            value.supportWhatsApp.orEmpty().isNotBlank() && value.supportWhatsApp.orEmpty().count(Char::isDigit) !in 10..15 ->
                "Enter a valid WhatsApp number"
            value.supportEmail == null && value.supportPhone == null && value.supportWhatsApp == null ->
                "Provide at least one customer support method"
            else -> null
        }
        if (error != null) { mutableState.update { it.copy(error = error) }; return }
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, error = null) }
            when (val result = repository.saveSupportSettings(value)) {
                is AppResult.Success -> mutableState.update {
                    it.copy(saving = false, settings = result.value, saved = true)
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(saving = false, error = result.error.accountText())
                }
            }
        }
    }
    fun consumeSaved() = mutableState.update { it.copy(saved = false) }
}

private fun AppError.accountText() = when (this) {
    AppError.InvalidCredentials -> "Current password is incorrect"
    AppError.Forbidden, AppError.Unauthorized -> "This action is not authorized"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Something went wrong. Please try again"
}

package com.spacetecsolutions.meatapp.feature.admin.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.domain.repository.AdminShopSettingsRepository
import com.spacetecsolutions.meatapp.core.model.AdminShopSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminShopSettingsState(
    val loading: Boolean = true, val saving: Boolean = false,
    val settings: AdminShopSettings? = null, val error: String? = null, val message: String? = null,
)

@HiltViewModel
class AdminShopSettingsViewModel @Inject constructor(
    private val repository: AdminShopSettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminShopSettingsState())
    val state = mutableState.asStateFlow()
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.getSettings()) {
            is AppResult.Success -> mutableState.update { it.copy(loading = false, settings = result.value) }
            is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error.settingsText()) }
        }
    }
    fun update(value: AdminShopSettings) = mutableState.update { it.copy(settings = value, error = null) }
    fun save() {
        val value = state.value.settings ?: return
        val error = when {
            value.shopName.isBlank() -> "Shop name is required"
            value.address.isBlank() -> "Shop address is required"
            value.contactPhone.count(Char::isDigit) !in 10..15 -> "Enter a valid contact number"
            value.contactEmail.orEmpty().isNotBlank() && !value.contactEmail.orEmpty().contains('@') -> "Enter a valid contact email"
            else -> null
        }
        if (error != null) { mutableState.update { it.copy(error = error) }; return }
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, error = null) }
            when (val result = repository.saveSettings(value)) {
                is AppResult.Success -> mutableState.update {
                    it.copy(saving = false, settings = result.value, message = "Shop settings saved")
                }
                is AppResult.Failure -> mutableState.update { it.copy(saving = false, error = result.error.settingsText()) }
            }
        }
    }
    fun consumeMessage() = mutableState.update { it.copy(message = null) }
}

@Composable
fun AdminShopSettingsRoute(
    back: () -> Unit, onMessage: (String) -> Unit,
    viewModel: AdminShopSettingsViewModel = hiltViewModel(),
) {
    HideAppBottomBar()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) { state.message?.let { onMessage(it); viewModel.consumeMessage() } }
    Column(Modifier.fillMaxSize()) {
        AppBackTopBar("Shop Settings", back)
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.settings == null -> ContentStateView(ContentState.Error(description = state.error), onAction = viewModel::refresh)
            else -> ShopSettingsForm(state, viewModel)
        }
    }
}

@Composable
private fun ShopSettingsForm(state: AdminShopSettingsState, actions: AdminShopSettingsViewModel) {
    val value = state.settings ?: return
    LazyColumn(
        Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = AppDimensions.formMaxWidth),
        contentPadding = PaddingValues(AppSpacing.medium), verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
    ) {
        item { SettingsSectionTitle("Shop details") }
        item { SettingsTextField("Shop name", value.shopName) { actions.update(value.copy(shopName = it.take(100))) } }
        item { SettingsTextField("Shop address", value.address, minLines = 3) { actions.update(value.copy(address = it.take(300))) } }
        item { SettingsTextField("Contact number", value.contactPhone) { actions.update(value.copy(contactPhone = it.take(20))) } }
        item { SettingsTextField("Contact email", value.contactEmail.orEmpty()) { actions.update(value.copy(contactEmail = it.blankNull())) } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item {
            Button(actions::save, Modifier.fillMaxWidth(), enabled = !state.saving) {
                if (state.saving) CircularProgressIndicator(Modifier.size(AppDimensions.iconSmall)) else Text("Save Shop Settings")
            }
        }
    }
}

@Composable
internal fun SettingsTextField(label: String, value: String, minLines: Int = 1, change: (String) -> Unit) =
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) },
        minLines = minLines, singleLine = minLines == 1)

@Composable
internal fun SettingsSectionTitle(text: String) = Text(
    text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
)

internal fun String.blankNull() = trim().takeIf(String::isNotBlank)

internal fun AppError.settingsText() = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "This setting is not authorized"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Unable to update settings"
}

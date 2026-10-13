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

data class AdminAppSettingsState(
    val loading: Boolean = true, val saving: Boolean = false,
    val settings: AdminShopSettings? = null, val error: String? = null, val saved: Boolean = false,
)

@HiltViewModel
class AdminAppSettingsViewModel @Inject constructor(
    private val repository: AdminShopSettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminAppSettingsState())
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
        val settings = state.value.settings ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, error = null) }
            when (val result = repository.saveAppSettings(settings)) {
                is AppResult.Success -> mutableState.update { it.copy(saving = false, settings = result.value, saved = true) }
                is AppResult.Failure -> mutableState.update { it.copy(saving = false, error = result.error.settingsText()) }
            }
        }
    }
    fun consumeSaved() = mutableState.update { it.copy(saved = false) }
}

@Composable
fun AdminAppSettingsRoute(
    back: () -> Unit, onSaved: (String) -> Unit,
    viewModel: AdminAppSettingsViewModel = hiltViewModel(),
) {
    HideAppBottomBar()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) { onSaved("App settings saved"); viewModel.consumeSaved() } }
    Column(Modifier.fillMaxSize()) {
        AppBackTopBar(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_app_settings_09439e4), back)
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.settings == null -> ContentStateView(ContentState.Error(description = state.error), onAction = viewModel::refresh)
            else -> AppSettingsForm(state, viewModel)
        }
    }
}

@Composable
private fun AppSettingsForm(state: AdminAppSettingsState, actions: AdminAppSettingsViewModel) {
    state.settings ?: return
    LazyColumn(
        Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = AppDimensions.formMaxWidth),
        contentPadding = PaddingValues(AppSpacing.medium), verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        item { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_app_name_logo_launcher_icon_11019d7), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { ContentStateView(ContentState.Empty(
            title = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_brand_settings_are_flavor_controlled_a609f02),
            description = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_use_the_separate_home_banners_df2afd2),
        )) }
    }
}

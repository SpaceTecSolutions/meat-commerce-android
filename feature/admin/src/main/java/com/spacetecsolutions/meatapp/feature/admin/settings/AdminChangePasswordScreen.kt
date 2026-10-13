package com.spacetecsolutions.meatapp.feature.admin.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*

@Composable
fun AdminChangePasswordRoute(
    back: () -> Unit,
    onChanged: (String) -> Unit,
    viewModel: AdminPasswordViewModel = hiltViewModel(),
) {
    HideAppBottomBar()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.changed) {
        if (state.changed) { onChanged("Password updated successfully"); viewModel.consumeChanged(); back() }
    }
    Column(Modifier.fillMaxSize()) {
        AppBackTopBar(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_change_password_49289db), back)
        Column(
            Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = AppDimensions.formMaxWidth).verticalScroll(rememberScrollState())
                .padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_create_a_strong_password_493ef0c), style = MaterialTheme.typography.titleLarge)
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_use_at_least_8_characters_552b16c),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SecureField("Current password", state.current, viewModel::current, state.saving)
            SecureField("New password", state.password, viewModel::password, state.saving)
            SecureField("Confirm new password", state.confirmation, viewModel::confirmation, state.saving)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(viewModel::save, Modifier.fillMaxWidth(), enabled = !state.saving) {
                if (state.saving) CircularProgressIndicator(Modifier.size(AppDimensions.iconSmall))
                else { Icon(AppIcons.Security, null); Spacer(Modifier.width(AppSpacing.small)); Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_update_password_61dcf34)) }
            }
        }
    }
}

@Composable
private fun SecureField(label: String, value: String, change: (String) -> Unit, saving: Boolean) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, change, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        enabled = !saving,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton({ visible = !visible }) {
            Icon(if (visible) AppIcons.PasswordHidden else AppIcons.PasswordVisible,
                if (visible) "Hide password" else "Show password")
        } },
    )
}

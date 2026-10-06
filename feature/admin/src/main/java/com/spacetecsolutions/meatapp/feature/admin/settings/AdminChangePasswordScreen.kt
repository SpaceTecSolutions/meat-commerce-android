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
        AppBackTopBar("Change Password", back)
        Column(
            Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = AppDimensions.formMaxWidth).verticalScroll(rememberScrollState())
                .padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            Text("Create a strong password", style = MaterialTheme.typography.titleLarge)
            Text("Use at least 8 characters with uppercase, lowercase and a number.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SecureField("Current password", state.current, viewModel::current, state.saving)
            SecureField("New password", state.password, viewModel::password, state.saving)
            SecureField("Confirm new password", state.confirmation, viewModel::confirmation, state.saving)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(viewModel::save, Modifier.fillMaxWidth(), enabled = !state.saving) {
                if (state.saving) CircularProgressIndicator(Modifier.size(AppDimensions.iconSmall))
                else { Icon(AppIcons.Security, null); Spacer(Modifier.width(AppSpacing.small)); Text("Update Password") }
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

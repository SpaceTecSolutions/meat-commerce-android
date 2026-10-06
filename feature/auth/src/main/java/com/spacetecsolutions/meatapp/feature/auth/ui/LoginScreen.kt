package com.spacetecsolutions.meatapp.feature.auth.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.feature.auth.R
import com.spacetecsolutions.meatapp.feature.auth.presentation.LoginViewModel
import com.spacetecsolutions.meatapp.feature.auth.presentation.AuthFeedback

@Composable
internal fun LoginScreen(
    appName: String,
    @DrawableRes bgRes: Int,
    @DrawableRes logoRes: Int,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onFeedback: (AuthFeedback) -> Unit,
    sessionMessage: String? = null,
    allowRegistration: Boolean = true,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.feedback) {
        state.feedback?.let { onFeedback(it); viewModel.consumeFeedback() }
    }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    AuthFormScaffold(
        appName = appName,
        bgRes = bgRes,
        logoRes = logoRes,
        title = stringResource(R.string.auth_welcome),
        description = stringResource(R.string.auth_login_description),
    ) {
        MobileField(
            value = state.mobileNumber,
            onValueChange = viewModel::updateMobile,
            label = stringResource(R.string.auth_mobile),
            error = state.mobileError,
            enabled = !state.loading,
        )
        PasswordField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = stringResource(R.string.auth_password),
            error = state.passwordError,
            enabled = !state.loading,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(rememberMe, { rememberMe = it }, enabled = !state.loading)
            Text(stringResource(R.string.auth_remember_me), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onForgotPassword, enabled = !state.loading) {
                Text(stringResource(R.string.auth_forgot_password))
            }
        }
        RequestError(state.requestError ?: sessionMessage)
        LoadingButton(stringResource(R.string.auth_login).uppercase(), state.loading, viewModel::submit)
        if (allowRegistration) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.auth_new_here), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRegister, enabled = !state.loading) {
                Text(stringResource(R.string.auth_create_account))
            }
        }
    }
}

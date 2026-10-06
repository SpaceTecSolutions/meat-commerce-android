package com.spacetecsolutions.meatapp.feature.auth.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.feature.auth.R
import com.spacetecsolutions.meatapp.feature.auth.presentation.RegisterStep
import com.spacetecsolutions.meatapp.feature.auth.presentation.AuthFeedback
import com.spacetecsolutions.meatapp.feature.auth.presentation.RegisterViewModel

@Composable
internal fun RegisterScreen(
    appName: String,
    @DrawableRes logoRes: Int,
    @DrawableRes bgRes: Int,
    onLogin: () -> Unit,
    onFeedback: (AuthFeedback) -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.feedback) {
        state.feedback?.let { onFeedback(it); viewModel.consumeFeedback() }
    }
    AnimatedContent(
        targetState = state.step,
        transitionSpec = { fadeIn(tween(AppMotion.STANDARD_MILLIS)) togetherWith fadeOut() },
        label = "registration-step",
    ) { step ->
        AuthFormScaffold(appName, bgRes, logoRes, step.title(), step.description(state.maskedDestination)) {
            when (step) {
                RegisterStep.PHONE -> {
                    MobileField(state.mobile, viewModel::updateMobile, stringResource(R.string.auth_mobile),
                        state.mobileError, !state.loading)
                    LoadingButton(stringResource(R.string.auth_get_otp), state.loading, viewModel::requestCode)
                }
                RegisterStep.VERIFY -> {
                    OtpField(state.verificationCode, viewModel::updateCode, !state.loading, state.codeError)
                    LoadingButton(stringResource(R.string.auth_verify), state.loading, viewModel::verifyCode)
                    TextButton(onClick = viewModel::requestCode, enabled = !state.loading) {
                        Text(stringResource(R.string.auth_resend_otp))
                    }
                    TextButton(onClick = viewModel::changeNumber, enabled = !state.loading) {
                        Text(stringResource(R.string.auth_change_number))
                    }
                }
                RegisterStep.DETAILS -> RegistrationDetails(state, viewModel)
                RegisterStep.ACCOUNT_EXISTS -> LoadingButton(
                    stringResource(R.string.auth_go_to_login), false, onLogin,
                )
            }
            if (step != RegisterStep.ACCOUNT_EXISTS) {
                TextButton(onClick = onLogin, enabled = !state.loading) {
                    Text(stringResource(R.string.auth_already_registered))
                }
            }
        }
    }
}

@Composable
private fun RegistrationDetails(
    state: com.spacetecsolutions.meatapp.feature.auth.presentation.RegisterUiState,
    viewModel: RegisterViewModel,
) {
    OutlinedTextField(state.firstName, viewModel::updateFirstName, Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.auth_first_name)) }, singleLine = true,
        isError = state.firstNameError != null,
        supportingText = state.firstNameError?.let { { Text(it) } })
    OutlinedTextField(state.lastName, viewModel::updateLastName, Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.auth_last_name)) }, singleLine = true,
        isError = state.lastNameError != null,
        supportingText = state.lastNameError?.let { { Text(it) } })
    OutlinedTextField(state.mobile, {}, Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.auth_mobile)) }, enabled = false, singleLine = true)
    PasswordField(state.password, viewModel::updatePassword, stringResource(R.string.auth_password),
        state.passwordError, !state.loading)
    PasswordField(state.confirmPassword, viewModel::updateConfirmation,
        stringResource(R.string.auth_confirm_password), state.confirmationError, !state.loading)
    LoadingButton(stringResource(R.string.auth_create_account).uppercase(), state.loading, viewModel::submit)
}

@Composable
private fun RegisterStep.title() = when (this) {
    RegisterStep.PHONE -> stringResource(R.string.auth_create_account)
    RegisterStep.VERIFY -> stringResource(R.string.auth_verify_mobile)
    RegisterStep.DETAILS -> stringResource(R.string.auth_your_details)
    RegisterStep.ACCOUNT_EXISTS -> stringResource(R.string.auth_account_exists_title)
}

@Composable
private fun RegisterStep.description(destination: String) = when (this) {
    RegisterStep.PHONE -> stringResource(R.string.auth_register_description)
    RegisterStep.VERIFY -> stringResource(R.string.auth_verification_description, destination)
    RegisterStep.DETAILS -> stringResource(R.string.auth_details_description)
    RegisterStep.ACCOUNT_EXISTS -> stringResource(R.string.auth_account_exists_description)
}

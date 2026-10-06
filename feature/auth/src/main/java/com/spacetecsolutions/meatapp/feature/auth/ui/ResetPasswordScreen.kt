package com.spacetecsolutions.meatapp.feature.auth.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.feature.auth.R
import com.spacetecsolutions.meatapp.feature.auth.presentation.ResetPasswordViewModel
import com.spacetecsolutions.meatapp.feature.auth.presentation.ResetStep
import com.spacetecsolutions.meatapp.feature.auth.presentation.AuthFeedback

@Composable
internal fun ResetPasswordScreen(
    appName: String,
    @DrawableRes bgRes: Int,
    @DrawableRes logoRes: Int,
    onLogin: () -> Unit,
    onFeedback: (AuthFeedback) -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.feedback) {
        state.feedback?.let { onFeedback(it); viewModel.consumeFeedback() }
    }
    AnimatedContent(state.step, transitionSpec = { fadeIn(tween(AppMotion.MICRO_MILLIS)) togetherWith fadeOut() }) { step ->
        AuthFormScaffold(
            appName = appName,
            bgRes = bgRes,
            logoRes = logoRes,
            title = step.title(),
            description = step.description(state.maskedDestination),
        ) {
            when (step) {
                ResetStep.REQUEST -> {
                    MobileField(state.mobile, viewModel::updateMobile, stringResource(R.string.auth_mobile), state.mobileError, !state.loading)
                    LoadingButton(stringResource(R.string.auth_send_code), state.loading, viewModel::requestCode)
                }
                ResetStep.VERIFY -> {
                    OtpField(state.verificationCode, viewModel::updateCode, !state.loading, state.codeError)
                    LoadingButton(stringResource(R.string.auth_verify), state.loading, viewModel::verifyCode)
                    TextButton(onClick = viewModel::requestCode, enabled = !state.loading) {
                        Text(stringResource(R.string.auth_resend_otp))
                    }
                }
                ResetStep.NEW_PASSWORD -> {
                    PasswordField(state.newPassword, viewModel::updatePassword, stringResource(R.string.auth_new_password), state.passwordError, !state.loading)
                    PasswordField(state.confirmPassword, viewModel::updateConfirmation, stringResource(R.string.auth_confirm_password), state.confirmationError, !state.loading)
                    LoadingButton(stringResource(R.string.auth_update_password), state.loading, viewModel::resetPassword)
                }
                ResetStep.COMPLETE -> LoadingButton(stringResource(R.string.auth_back_to_login), false, onLogin)
            }
            if (step != ResetStep.COMPLETE) {
                TextButton(onClick = onLogin, enabled = !state.loading) { Text(stringResource(R.string.auth_back_to_login)) }
            }
        }
    }
}

@Composable
private fun ResetStep.title(): String = when (this) {
    ResetStep.REQUEST -> stringResource(R.string.auth_reset_password)
    ResetStep.VERIFY -> stringResource(R.string.auth_verification)
    ResetStep.NEW_PASSWORD -> stringResource(R.string.auth_new_password)
    ResetStep.COMPLETE -> stringResource(R.string.auth_reset_complete)
}

@Composable
private fun ResetStep.description(destination: String): String = when (this) {
    ResetStep.REQUEST -> stringResource(R.string.auth_reset_description)
    ResetStep.VERIFY -> stringResource(R.string.auth_verification_description, destination)
    ResetStep.NEW_PASSWORD -> stringResource(R.string.auth_new_password_description)
    ResetStep.COMPLETE -> stringResource(R.string.auth_reset_complete_description)
}

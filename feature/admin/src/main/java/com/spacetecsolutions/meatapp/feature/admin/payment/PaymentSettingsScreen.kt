package com.spacetecsolutions.meatapp.feature.admin.payment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.AppBackTopBar
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

@Composable
fun PaymentSettingsRoute(
    onBack: () -> Unit,
    onMessage: (PaymentSettingsMessage) -> Unit,
    viewModel: PaymentSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) { state.message?.let { onMessage(it); viewModel.consumeMessage() } }
    PaymentSettingsScreen(state, onBack, viewModel)
}

@Composable
private fun PaymentSettingsScreen(
    state: PaymentSettingsUiState,
    onBack: () -> Unit,
    actions: PaymentSettingsViewModel,
) {
    Scaffold(topBar = { AppBackTopBar("Payment Settings", onBack) }) { padding ->
        when {
            state.loading -> ContentStateView(ContentState.Loading, contentPadding = padding)
            state.config == null -> ContentStateView(
                ContentState.Error(description = state.error), contentPadding = padding, onAction = actions::refresh)
            !state.permissions.any -> ContentStateView(
                ContentState.Empty(
                    title = "No payment methods available",
                    description = "Super Admin has not permitted any payment methods.",
                ),
                contentPadding = padding,
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).wrapContentWidth()
                    .widthIn(max = AppDimensions.contentMaxWidth),
                contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            ) {
                item {
                    Text("Only methods permitted by Super Admin are shown.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.permissions.cod) item {
                    PaymentMethodCard(AppIcons.Cash, "Cash on Delivery", "Always available",
                        true, false, {})
                }
                if (state.permissions.razorpay) item {
                    val configured = state.config.razorpayConfigured
                    PaymentMethodCard(
                        AppIcons.Card, "Razorpay",
                        if (configured) "Backend configuration verified" else "Backend configuration required",
                        state.config.razorpayEnabled && configured,
                        !state.saving && configured,
                        actions::setRazorpay,
                        configured = configured,
                    )
                    if (!configured) OutlinedButton(
                        onClick = actions::verifyRazorpay,
                        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.small),
                        enabled = !state.saving,
                    ) { Text("Verify Razorpay setup") }
                }
                if (state.permissions.upi) item {
                    PaymentMethodCard(AppIcons.Upi, "UPI", "Accept supported UPI payments",
                        state.config.upiEnabled, !state.saving, actions::setUpi)
                }
                item {
                    Button(actions::save, Modifier.fillMaxWidth(), enabled = !state.saving) {
                        if (state.saving) CircularProgressIndicator(Modifier.size(AppSpacing.large))
                        else { Icon(AppIcons.Save, null); Spacer(Modifier.width(AppSpacing.small)); Text("Save settings") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    change: (Boolean) -> Unit,
    configured: Boolean? = null,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(AppSpacing.medium)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.medium)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedVisibility(configured != null) {
                    Text(if (configured == true) "Configured" else "Not configured",
                        color = if (configured == true) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            Switch(
                checked,
                change,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF22A652),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFB7BBC1),
                    uncheckedBorderColor = Color.Transparent,
                    disabledCheckedThumbColor = Color.White.copy(alpha = .9f),
                    disabledCheckedTrackColor = Color(0xFF22A652).copy(alpha = .45f),
                    disabledUncheckedThumbColor = Color.White.copy(alpha = .9f),
                    disabledUncheckedTrackColor = Color(0xFFB7BBC1).copy(alpha = .55f),
                    disabledUncheckedBorderColor = Color.Transparent,
                )
            )
        }
    }
}

package com.spacetecsolutions.meatapp.feature.superadmin.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.AppBackTopBar
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.FeatureConfig

@Composable
fun FeatureManagementRoute(
    onBack: () -> Unit,
    onMessage: (FeatureMessage) -> Unit,
    viewModel: FeatureManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) {
        state.message?.let { onMessage(it); viewModel.consumeMessage() }
    }
    FeatureManagementScreen(
        state, onBack, viewModel::toggle, viewModel::updateMaxProducts,
        viewModel::save, viewModel::discardChanges, viewModel::refresh,
    )
}

@Composable
fun PaymentMethodsRoute(
    onBack: () -> Unit,
    onMessage: (FeatureMessage) -> Unit,
    viewModel: FeatureManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) {
        state.message?.let { onMessage(it); viewModel.consumeMessage() }
    }
    ManagedSettingsScreen(
        title = "Payment Methods",
        state = state,
        onBack = onBack,
        rows = state.draft?.let(::paymentRows).orEmpty(),
        onToggle = viewModel::toggle,
        onSave = viewModel::save,
        onDiscard = viewModel::discardChanges,
        onRefresh = viewModel::refresh,
    )
}

@Composable
private fun FeatureManagementScreen(
    state: FeatureManagementUiState,
    onBack: () -> Unit,
    onToggle: (ManagedFeature, Boolean) -> Unit,
    onMaximumChange: (String) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onRefresh: () -> Unit,
) {
    ManagedSettingsScreen(
        title = "Feature Management",
        state = state,
        onBack = onBack,
        rows = state.draft?.let(::featureRows).orEmpty(),
        onToggle = { feature, checked ->
            onToggle(feature, checked)
            if (feature == ManagedFeature.OFFERS) onToggle(ManagedFeature.COUPONS, checked)
        },
        onSave = onSave,
        onDiscard = onDiscard,
        onRefresh = onRefresh,
        productLimit = { padding ->
            FeatureList(
                state, padding,
                { feature, checked ->
                    onToggle(feature, checked)
                    if (feature == ManagedFeature.OFFERS) onToggle(ManagedFeature.COUPONS, checked)
                },
                onMaximumChange,
            )
        },
    )
}

@Composable
private fun ManagedSettingsScreen(
    title: String,
    state: FeatureManagementUiState,
    onBack: () -> Unit,
    rows: List<FeatureRowModel>,
    onToggle: (ManagedFeature, Boolean) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onRefresh: () -> Unit,
    productLimit: (@Composable (PaddingValues) -> Unit)? = null,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AppBackTopBar(title, onBack) },
        bottomBar = { AnimatedVisibility(state.dirty) { SaveBar(state.saving, onDiscard, onSave) } },
    ) { padding ->
        when {
            state.loading -> ContentStateView(ContentState.Loading, contentPadding = padding)
            state.error != null && state.draft == null -> ContentStateView(
                ContentState.Error(description = state.error), contentPadding = padding, onAction = onRefresh,
            )
            state.draft != null && productLimit != null -> productLimit(padding)
            state.draft != null -> SettingsRows(rows, !state.saving, padding, onToggle)
        }
    }
}

@Composable
private fun FeatureList(
    state: FeatureManagementUiState,
    padding: PaddingValues,
    onToggle: (ManagedFeature, Boolean) -> Unit,
    onMaximumChange: (String) -> Unit,
) {
    val config = state.draft ?: return
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = AppDimensions.formMaxWidth),
        contentPadding = PaddingValues(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
    ) {
        items(featureRows(config), key = { it.feature }) { row ->
            FeatureRow(row, !state.saving) { onToggle(row.feature, it) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
        }
        item {
            Column(Modifier.padding(top = AppSpacing.medium)) {
                Text("Product Limit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = state.maxProductsText,
                    onValueChange = onMaximumChange,
                    modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.extraSmall),
                    leadingIcon = { Icon(AppIcons.ProductLimit, null) },
                    supportingText = { Text(state.maxProductsError ?: "Maximum products this client can publish") },
                    isError = state.maxProductsError != null,
                    enabled = !state.saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = AppShapes.extraSmall,
                )
            }
        }
    }
}

@Composable
private fun SettingsRows(
    rows: List<FeatureRowModel>, enabled: Boolean, padding: PaddingValues,
    onToggle: (ManagedFeature, Boolean) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = AppDimensions.formMaxWidth),
        contentPadding = PaddingValues(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
    ) {
        items(rows, key = { it.feature }) { row ->
            FeatureRow(row, enabled) { onToggle(row.feature, it) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
        }
    }
}

@Composable
private fun FeatureRow(row: FeatureRowModel, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(vertical = AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(Modifier.size(36.dp), shape = CircleShape, color = row.tint.copy(alpha = .11f)) {
            Icon(row.icon, null, Modifier.padding(9.dp), tint = row.tint)
        }
        Column(Modifier.padding(horizontal = AppSpacing.compact).weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = if (row.feature == ManagedFeature.COD || row.feature == ManagedFeature.SCHEDULED_DELIVERY) true else row.checked,
            onCheckedChange = onChange,
            enabled = enabled && row.feature != ManagedFeature.COD && row.feature != ManagedFeature.SCHEDULED_DELIVERY,
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
            ),
        )
    }
}

@Composable
private fun SaveBar(saving: Boolean, onDiscard: () -> Unit, onSave: () -> Unit) {
    Surface(shadowElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(AppSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            OutlinedButton(onDiscard, Modifier.weight(1f), enabled = !saving) { Text("Discard") }
            Button(onSave, Modifier.weight(1f), enabled = !saving) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Save changes")
            }
        }
    }
}

private data class FeatureRowModel(
    val feature: ManagedFeature,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val checked: Boolean,
)

private fun featureRows(config: FeatureConfig) = listOf(
    FeatureRowModel(ManagedFeature.IN_APP_NOTIFICATIONS, "In-App Notifications", "Show Customer inbox and bell (push stays on)", AppIcons.Notifications, Success, config.inAppNotificationsEnabled),
    FeatureRowModel(ManagedFeature.REALTIME_TRACKING, "Real-time Tracking", "Live order tracking", AppIcons.LiveTracking, Success, config.realtimeTrackingAllowed),
    FeatureRowModel(ManagedFeature.DELIVERY_STAFF, "Delivery Staff Management", "Create and assign delivery users", AppIcons.DeliveryPerson, Success, config.deliveryStaffManagementAllowed),
    FeatureRowModel(ManagedFeature.STAFF_MANAGEMENT, "Staff Management", "Create employees and control permissions", AppIcons.Customers, Success, config.staffManagementAllowed),
    FeatureRowModel(ManagedFeature.SUBCATEGORIES, "Product Subcategories", "Optional category grouping for products", AppIcons.Categories, Success, config.subcategoriesAllowed),
    FeatureRowModel(ManagedFeature.SCHEDULED_DELIVERY, "Delivery Scheduler", "Schedule delivery time", AppIcons.Calendar, Color(0xFFE79813), config.scheduledDeliveryAllowed),
    FeatureRowModel(ManagedFeature.OFFERS, "Coupons & Offers", "Discount coupons", AppIcons.Coupon, Color(0xFFE79813), config.offersAllowed && config.couponsAllowed),
)

private fun paymentRows(config: FeatureConfig) = listOf(
    FeatureRowModel(ManagedFeature.COD, "Cash on Delivery", "Pay with cash on delivery", AppIcons.Cash, Success, config.codAllowed),
    FeatureRowModel(ManagedFeature.RAZORPAY, "Razorpay", "Online payments via Razorpay", AppIcons.Card, Color(0xFF3F73B9), config.razorpayAllowed),
    FeatureRowModel(ManagedFeature.UPI, "UPI", "Pay using UPI", AppIcons.Upi, Color(0xFF3F73B9), config.upiAllowed),
)

package com.spacetecsolutions.meatapp.feature.admin.promotion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.*

@Composable
fun PromotionManagementRoute(
    offersAllowed: Boolean, couponsAllowed: Boolean, onBack: () -> Unit,
    onMessage: (PromotionMessage) -> Unit,
    viewModel: PromotionManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(offersAllowed, couponsAllowed) { viewModel.load(offersAllowed, couponsAllowed) }
    LaunchedEffect(state.message) { state.message?.let { onMessage(it); viewModel.consumeMessage() } }
    PromotionManagementScreen(state, onBack, viewModel)
}

@Composable
private fun PromotionManagementScreen(
    state: PromotionManagementUiState, back: () -> Unit, actions: PromotionManagementViewModel,
) {
    Column(Modifier.fillMaxSize()) {
        AppBackTopBar("Offers & Coupons", back)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { actions.edit(null) }) {
                Icon(AppIcons.Add, contentDescription = null)
                Spacer(Modifier.width(AppSpacing.extraSmall))
                Text("Create promotion")
            }
        }
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null -> ContentStateView(ContentState.Error(description = state.error), onAction = actions::refresh)
            state.promotions.isEmpty() -> ContentStateView(ContentState.Empty(
                title = "No promotions", description = "Create an offer or coupon for customers.",
                actionLabel = "Create promotion",
            ), onAction = { actions.edit(null) })
            else -> LazyColumn(Modifier.fillMaxSize().wrapContentWidth().widthIn(max = AppDimensions.contentMaxWidth),
                contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                items(state.promotions.sortedByDescending(Promotion::validUntilEpochMillis), key = Promotion::id) { promotion ->
                    PromotionCard(promotion, state.busyId == promotion.id, actions::edit, actions::toggle)
                }
            }
        }
    }
    state.form?.let { PromotionFormDialog(it, state.offersAllowed, state.couponsAllowed,
        actions::update, actions::save, actions::dismissForm) }
}

@Composable
private fun PromotionCard(
    promotion: Promotion, busy: Boolean, edit: (Promotion) -> Unit, toggle: (Promotion) -> Unit,
) = ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppSpacing.medium),
    verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(promotion.name, style = MaterialTheme.typography.titleMedium)
        Text(if (promotion.active) "Active" else "Inactive",
            color = if (promotion.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
    Text("${promotion.kind.name} · ${promotion.discountLabel()}")
    promotion.code?.let { Text("Code: $it") }
    Text("Valid ${promotion.validFromEpochMillis.date()} – ${promotion.validUntilEpochMillis.date()}",
        style = MaterialTheme.typography.bodySmall)
    Text("Minimum ₹${promotion.minimumOrderMinor / 100.0}" +
        (promotion.maximumDiscountMinor?.let { " · Maximum discount ₹${it / 100.0}" } ?: ""))
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton({ edit(promotion) }, enabled = !busy) { Text("Edit") }
        TextButton({ toggle(promotion) }, enabled = !busy) { Text(if (promotion.active) "Deactivate" else "Activate") }
    }
} }

@Composable
private fun PromotionFormDialog(
    form: PromotionForm, offersAllowed: Boolean, couponsAllowed: Boolean,
    update: (PromotionForm) -> Unit, save: () -> Unit, dismiss: () -> Unit,
) = AlertDialog(onDismissRequest = dismiss, title = { Text(if (form.id == "new") "Create promotion" else "Edit promotion") },
    text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        item { OutlinedTextField(form.name, { update(form.copy(name = it.take(80))) }, Modifier.fillMaxWidth(), label = { Text("Name") }) }
        item { Row {
            if (offersAllowed) FilterChip(form.kind == PromotionKind.OFFER, { update(form.copy(kind = PromotionKind.OFFER, code = "")) }, { Text("Offer") })
            Spacer(Modifier.width(AppSpacing.small))
            if (couponsAllowed) FilterChip(form.kind == PromotionKind.COUPON, { update(form.copy(kind = PromotionKind.COUPON)) }, { Text("Coupon") })
        } }
        if (form.kind == PromotionKind.COUPON) item { OutlinedTextField(form.code,
            { update(form.copy(code = it.filter(Char::isLetterOrDigit).uppercase().take(24))) }, Modifier.fillMaxWidth(), label = { Text("Coupon code") }) }
        item { Row {
            FilterChip(form.type == DiscountType.PERCENTAGE, { update(form.copy(type = DiscountType.PERCENTAGE)) }, { Text("Percentage") })
            Spacer(Modifier.width(AppSpacing.small))
            FilterChip(form.type == DiscountType.FIXED, { update(form.copy(type = DiscountType.FIXED)) }, { Text("Fixed") })
        } }
        item { NumberField(if (form.type == DiscountType.PERCENTAGE) "Discount percent" else "Fixed discount",
            form.value) { update(form.copy(value = it)) } }
        item { NumberField("Minimum order", form.minimum) { update(form.copy(minimum = it)) } }
        item { NumberField("Maximum discount (optional)", form.maximum) { update(form.copy(maximum = it)) } }
        item { OutlinedTextField(form.validFrom, { update(form.copy(validFrom = it.take(10))) }, Modifier.fillMaxWidth(), label = { Text("Valid from (YYYY-MM-DD)") }) }
        item { OutlinedTextField(form.validUntil, { update(form.copy(validUntil = it.take(10))) }, Modifier.fillMaxWidth(), label = { Text("Valid until (YYYY-MM-DD)") }) }
    } }, confirmButton = { Button(save) { Text("Save") } },
    dismissButton = { TextButton(dismiss) { Text("Cancel") } })

@Composable
private fun NumberField(label: String, value: String, change: (String) -> Unit) = OutlinedTextField(
    value, { if (it.length <= 12 && it.matches(Regex("\\d*(\\.\\d{0,2})?"))) change(it) },
    Modifier.fillMaxWidth(), label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

private fun Promotion.discountLabel() = when (discountType) {
    DiscountType.PERCENTAGE -> "$discountValue% off"
    DiscountType.FIXED -> "₹${discountValue / 100.0} off"
}
private fun Long.date() = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(this))

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
        AppBackTopBar(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_offers_coupons_2c00e95), back)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { actions.edit(null) }) {
                Icon(AppIcons.Add, contentDescription = null)
                Spacer(Modifier.width(AppSpacing.extraSmall))
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_create_promotion_bd9428d))
            }
        }
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null -> ContentStateView(ContentState.Error(description = state.error), onAction = actions::refresh)
            state.promotions.isEmpty() -> ContentStateView(ContentState.Empty(
                title = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_no_promotions_72a4951), description = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_create_an_offer_or_coupon_58b3185),
                actionLabel = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_create_promotion_bd9428d),
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
        Text(if (promotion.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_active_a733b80) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_inactive_09af574),
            color = if (promotion.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_promotion_kind_name_promotion_discountlabel_633ac1f, promotion.kind.name, promotion.discountLabel()))
    promotion.code?.let { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_code_it_30d6355, it)) }
    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_valid_promotion_validfromepochmillis_date_pro_10727c8, promotion.validFromEpochMillis.date(), promotion.validUntilEpochMillis.date()),
        style = MaterialTheme.typography.bodySmall)
    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_minimum_promotion_minimumorderminor_100_0_a278faa, promotion.minimumOrderMinor / 100.0) +
        (promotion.maximumDiscountMinor?.let { " · Maximum discount ₹${it / 100.0}" } ?: ""))
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton({ edit(promotion) }, enabled = !busy) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_edit_5301648)) }
        TextButton({ toggle(promotion) }, enabled = !busy) { Text(if (promotion.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_deactivate_d65ded9) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_activate_92ef083)) }
    }
} }

@Composable
private fun PromotionFormDialog(
    form: PromotionForm, offersAllowed: Boolean, couponsAllowed: Boolean,
    update: (PromotionForm) -> Unit, save: () -> Unit, dismiss: () -> Unit,
) = AlertDialog(onDismissRequest = dismiss, title = { Text(if (form.id == "new") androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_create_promotion_bd9428d) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_edit_promotion_cd1c9af)) },
    text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        item { OutlinedTextField(form.name, { update(form.copy(name = it.take(80))) }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_name_709a232)) }) }
        item { Row {
            if (offersAllowed) FilterChip(form.kind == PromotionKind.OFFER, { update(form.copy(kind = PromotionKind.OFFER, code = "")) }, { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_offer_3898b9a)) })
            Spacer(Modifier.width(AppSpacing.small))
            if (couponsAllowed) FilterChip(form.kind == PromotionKind.COUPON, { update(form.copy(kind = PromotionKind.COUPON)) }, { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_coupon_31943e5)) })
        } }
        if (form.kind == PromotionKind.COUPON) item { OutlinedTextField(form.code,
            { update(form.copy(code = it.filter(Char::isLetterOrDigit).uppercase().take(24))) }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_coupon_code_b227f58)) }) }
        item { Row {
            FilterChip(form.type == DiscountType.PERCENTAGE, { update(form.copy(type = DiscountType.PERCENTAGE)) }, { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_percentage_c4519ac)) })
            Spacer(Modifier.width(AppSpacing.small))
            FilterChip(form.type == DiscountType.FIXED, { update(form.copy(type = DiscountType.FIXED)) }, { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_fixed_ab98f5b)) })
        } }
        item { NumberField(if (form.type == DiscountType.PERCENTAGE) "Discount percent" else "Fixed discount",
            form.value) { update(form.copy(value = it)) } }
        item { NumberField("Minimum order", form.minimum) { update(form.copy(minimum = it)) } }
        item { NumberField("Maximum discount (optional)", form.maximum) { update(form.copy(maximum = it)) } }
        item { OutlinedTextField(form.validFrom, { update(form.copy(validFrom = it.take(10))) }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_valid_from_yyyy_mm_dd_d03f44c)) }) }
        item { OutlinedTextField(form.validUntil, { update(form.copy(validUntil = it.take(10))) }, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_valid_until_yyyy_mm_dd_06879d8)) }) }
    } }, confirmButton = { Button(save) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_save_efc007a)) } },
    dismissButton = { TextButton(dismiss) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_cancel_77dfd21)) } })

@Composable
private fun NumberField(label: String, value: String, change: (String) -> Unit) = OutlinedTextField(
    value, { if (it.length <= 12 && it.matches(Regex("\\d*(\\.\\d{0,2})?"))) change(it) },
    Modifier.fillMaxWidth(), label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

private fun Promotion.discountLabel() = when (discountType) {
    DiscountType.PERCENTAGE -> "$discountValue% off"
    DiscountType.FIXED -> "₹${discountValue / 100.0} off"
}
private fun Long.date() = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(this))

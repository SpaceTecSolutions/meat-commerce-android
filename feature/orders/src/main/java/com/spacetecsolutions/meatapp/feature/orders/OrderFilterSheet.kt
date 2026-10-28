package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod
import com.spacetecsolutions.meatapp.core.model.PaymentStatus
import com.spacetecsolutions.meatapp.core.model.ProductCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrderFilterSheet(
    selectedMethod: CheckoutPaymentMethod?,
    selectedPaymentStatus: PaymentStatus?,
    categories: List<ProductCategory>,
    selectedCategoryId: String?,
    onApply: (CheckoutPaymentMethod?, PaymentStatus?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val supportedMethods = listOf(CheckoutPaymentMethod.COD)
    val codPaymentStatuses = listOf(PaymentStatus.PENDING, PaymentStatus.COLLECTED)
    val codMethodLabel = stringResource(
        R.string.invoice_payment_cod,
    )
    var method by remember(selectedMethod) {
        mutableStateOf(selectedMethod?.takeIf { it in supportedMethods })
    }
    var status by remember(selectedPaymentStatus) {
        mutableStateOf(selectedPaymentStatus?.takeIf { it in codPaymentStatuses })
    }
    var categoryId by remember { mutableStateOf(selectedCategoryId) }
    val codStatusLabels = mapOf(
        PaymentStatus.PENDING to stringResource(R.string.ui_to_collect_2e62dd1),
        PaymentStatus.COLLECTED to stringResource(R.string.ui_collected_7d5d140),
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium)
                .navigationBarsPadding().padding(bottom = AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            Text(stringResource(R.string.ui_filter_orders_4b61250), style = MaterialTheme.typography.titleLarge)
            FilterGroup(
                stringResource(R.string.invoice_payment_method),
                supportedMethods, method,
                { codMethodLabel },
            ) { method = it }
            FilterGroup(
                stringResource(R.string.ui_cod_payment_status_9dc6ad2),
                codPaymentStatuses, status, { codStatusLabels[it] ?: it.name.pretty() },
            ) { status = it }
            if (categories.isNotEmpty()) FilterGroup(
                "Product category", categories, categories.firstOrNull { it.id == categoryId }, { it.name },
            ) {
                categoryId = it?.id
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                OutlinedButton({ method = null; status = null; categoryId = null }, Modifier.weight(1f)) { Text(stringResource(R.string.ui_clear_719ea39)) }
                Button({ onApply(method, status, categoryId) }, Modifier.weight(1f)) { Text(stringResource(R.string.ui_apply_cfea419)) }
            }
        }
    }
}

@Composable
private fun <T> FilterGroup(
    title: String,
    choices: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            item {
                FilterChip(selected == null, { onSelect(null) }, label = { Text(stringResource(R.string.ui_any_322444d)) })
            }
            items(choices) { choice ->
                FilterChip(selected == choice, { onSelect(choice) }, label = { Text(label(choice)) })
            }
        }
    }
}

private fun String.pretty() = lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)

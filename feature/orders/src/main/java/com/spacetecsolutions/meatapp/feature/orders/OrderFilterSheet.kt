package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    var method by remember { mutableStateOf(selectedMethod) }
    var status by remember { mutableStateOf(selectedPaymentStatus) }
    var categoryId by remember { mutableStateOf(selectedCategoryId) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium)
                .navigationBarsPadding().padding(bottom = AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            Text("Filter Orders", style = MaterialTheme.typography.titleLarge)
            FilterGroup("Payment method", CheckoutPaymentMethod.entries, method, { it.name }) { method = it }
            FilterGroup("Payment status", PaymentStatus.entries, status, { it.name.pretty() }) { status = it }
            if (categories.isNotEmpty()) FilterGroup(
                "Product category", categories, categories.firstOrNull { it.id == categoryId }, { it.name },
            ) {
                categoryId = it?.id
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                OutlinedButton({ method = null; status = null; categoryId = null }, Modifier.weight(1f)) { Text("Clear") }
                Button({ onApply(method, status, categoryId) }, Modifier.weight(1f)) { Text("Apply") }
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
                FilterChip(selected == null, { onSelect(null) }, label = { Text("Any") })
            }
            items(choices) { choice ->
                FilterChip(selected == choice, { onSelect(choice) }, label = { Text(label(choice)) })
            }
        }
    }
}

private fun String.pretty() = lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)

package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.model.CodOrder
import java.text.NumberFormat
import java.util.Currency

@Composable
internal fun CollectionDialog(
    order: CodOrder,
    amount: String,
    change: (String) -> Unit,
    complete: () -> Unit,
    reportMismatch: () -> Unit,
    dismiss: () -> Unit,
) {
    val isCod = order.paymentMethod == com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod.COD
    val actual = amount.toMinorUnitsOrNull()
    val sufficient = actual != null && actual >= order.amountDueMinor
    val changeDue = (actual ?: 0L) - order.amountDueMinor
    Dialog(onDismissRequest = dismiss) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface, shadowElevation = 16.dp) {
            Column(Modifier.fillMaxWidth().padding(AppSpacing.large),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                Text("Reached customer", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("Confirm payment and complete the delivery",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isCod) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)) {
                        Row(Modifier.fillMaxWidth().padding(AppSpacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("COD expected")
                            Text(order.amountDueMinor.money(order.currencyCode),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                    OutlinedTextField(amount, change, Modifier.fillMaxWidth(), label = { Text("Amount collected") },
                        prefix = { Text("₹") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = actual != null && !sufficient,
                        supportingText = {
                            when {
                                actual != null && !sufficient -> Text("Collected amount cannot be less than the final bill.")
                                sufficient && changeDue > 0 -> Text("Return ${changeDue.money(order.currencyCode)} to the customer.")
                                sufficient -> Text("Exact amount received.")
                            }
                        })
                    Text("Confirm the cash received before completing delivery.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("${order.paymentMethod.name} payment", style = MaterialTheme.typography.titleSmall)
                    Text("Payment status: ${order.paymentStatus.name}")
                    Text("Online payment status is read-only. Delivery staff cannot change it.",
                        style = MaterialTheme.typography.bodySmall)
                    if (order.paymentStatus != com.spacetecsolutions.meatapp.core.model.PaymentStatus.PAID) {
                        Text("Delivery cannot be completed until the backend confirms PAID.",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = dismiss, modifier = Modifier.weight(1f)) { Text("Back") }
                    if (isCod && actual != null && !sufficient) {
                        Button(onClick = reportMismatch, modifier = Modifier.weight(1f)) {
                            Text("Report mismatch")
                        }
                    } else {
                        Button(onClick = complete, modifier = Modifier.weight(1f),
                            enabled = if (isCod) sufficient else order.paymentStatus == com.spacetecsolutions.meatapp.core.model.PaymentStatus.PAID) {
                            Text("Complete delivery", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CancelDialog(reason: String, change: (String) -> Unit, confirm: () -> Unit, dismiss: () -> Unit) {
    val reasons = listOf("Product unavailable", "Customer requested", "Delivery unavailable",
        "Address issue", "Duplicate order", "Other")
    var selected by remember { mutableStateOf(reason.takeIf { it in reasons }.orEmpty()) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Reject or cancel order?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
                reasons.forEach { option ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected == option, onClick = {
                            selected = option
                            if (option != "Other") change(option) else change("")
                        })
                        Text(option)
                    }
                }
                if (selected == "Other") OutlinedTextField(
                    reason, change, Modifier.fillMaxWidth(), label = { Text("Reason") },
                    supportingText = { Text("${reason.length}/200") }, maxLines = 3,
                )
            }
        },
        confirmButton = { Button(confirm, enabled = reason.isNotBlank()) { Text("Confirm cancellation") } },
        dismissButton = { TextButton(dismiss) { Text("Keep order") } },
    )
}

internal fun Long.money(currencyCode: String) = NumberFormat.getCurrencyInstance().apply {
    currency = Currency.getInstance(currencyCode)
}.format(this / 100.0)

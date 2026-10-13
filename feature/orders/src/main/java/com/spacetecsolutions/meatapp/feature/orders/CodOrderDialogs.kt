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
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_reached_customer_96f0200), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_confirm_payment_and_complete_the_21ddd1d),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isCod) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)) {
                        Row(Modifier.fillMaxWidth().padding(AppSpacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_cod_expected_bc0b6b1))
                            Text(order.amountDueMinor.money(order.currencyCode),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                    OutlinedTextField(amount, change, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_amount_collected_368837e)) },
                        prefix = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_text_ba8d95b)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = actual != null && !sufficient,
                        supportingText = {
                            when {
                                actual != null && !sufficient -> Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_collected_amount_cannot_be_less_af8468a))
                                sufficient && changeDue > 0 -> Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_return_changedue_money_order_currencycode_c2c1f5a, changeDue.money(order.currencyCode)))
                                sufficient -> Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_exact_amount_received_16c6888))
                            }
                        })
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_confirm_the_cash_received_before_cb1977d),
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_order_paymentmethod_name_payment_558efaa, order.paymentMethod.name), style = MaterialTheme.typography.titleSmall)
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_payment_status_order_paymentstatus_name_834462d, order.paymentStatus.name))
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_online_payment_status_is_read_e376a49),
                        style = MaterialTheme.typography.bodySmall)
                    if (order.paymentStatus != com.spacetecsolutions.meatapp.core.model.PaymentStatus.PAID) {
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_delivery_cannot_be_completed_until_6f3b740),
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = dismiss, modifier = Modifier.weight(1f)) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_back_b52b36b)) }
                    if (isCod && actual != null && !sufficient) {
                        Button(onClick = reportMismatch, modifier = Modifier.weight(1f)) {
                            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_report_mismatch_2cbbf09))
                        }
                    } else {
                        Button(onClick = complete, modifier = Modifier.weight(1f),
                            enabled = if (isCod) sufficient else order.paymentStatus == com.spacetecsolutions.meatapp.core.model.PaymentStatus.PAID) {
                            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_complete_delivery_fb1dae9), maxLines = 1)
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
        title = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_reject_or_cancel_order_f256105)) },
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
                    reason, change, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_reason_f219cc0)) },
                    supportingText = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_reason_length_200_18e3c58, reason.length)) }, maxLines = 3,
                )
            }
        },
        confirmButton = { Button(confirm, enabled = reason.isNotBlank()) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_confirm_cancellation_90977a5)) } },
        dismissButton = { TextButton(dismiss) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_keep_order_3017312)) } },
    )
}

internal fun Long.money(currencyCode: String) = NumberFormat.getCurrencyInstance().apply {
    currency = Currency.getInstance(currencyCode)
}.format(this / 100.0)

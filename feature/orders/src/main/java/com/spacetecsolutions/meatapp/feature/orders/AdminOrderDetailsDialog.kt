package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.CodOrder

@Composable
internal fun AdminOrderDetailsDialog(
    order: CodOrder,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onStartPreparing: () -> Unit,
    onMarkReady: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_order_order_displaynumber_5a620eb, order.displayNumber)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = AppDimensions.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            ) {
                item { DetailSection("Customer") {
                    Text(order.customerName, style = MaterialTheme.typography.titleSmall)
                    Text(order.customerMobile)
                    Text(order.addressSummary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
                item { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_items_44d25b5), style = MaterialTheme.typography.titleMedium) }
                items(order.items, key = { it.productId }) { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_item_quantitylabel_item_unitpriceminor_money_4e27373, item.quantityLabel, item.unitPriceMinor.money(order.currencyCode)),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(item.lineTotalMinor.money(order.currencyCode))
                    }
                }
                item { DetailSection("Payment") {
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_order_paymentmethod_name_order_paymentstatus_8160e87, order.paymentMethod.name, order.paymentStatus.name))
                } }
                item { DetailSection("Totals") {
                    TotalRow("Subtotal", order.subtotalMinor.money(order.currencyCode))
                    TotalRow("Discount", "−${order.discountMinor.money(order.currencyCode)}")
                    TotalRow("Delivery", order.deliveryFeeMinor.money(order.currencyCode))
                    HorizontalDivider()
                    TotalRow("Total", order.totalMinor.money(order.currencyCode))
                } }
                if (order.instructions.isNotBlank()) item {
                    DetailSection("Instructions") { Text(order.instructions) }
                }
                if (order.readyForDeliveryAssignment) item {
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_ready_for_delivery_assignment_afc413b), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall)
                }
                if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            when {
                order.canConfirm -> Button(onConfirm, enabled = !busy) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_confirm_04a2122)) }
                order.canStartPreparing -> Button(onStartPreparing, enabled = !busy) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_start_preparing_b18be6d)) }
                order.canMarkReadyForDelivery -> Button(onMarkReady, enabled = !busy) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_mark_ready_c4329a4)) }
            }
        },
        dismissButton = {
            if (order.canReject) TextButton(onReject, enabled = !busy) {
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_reject_2b03b59), color = MaterialTheme.colorScheme.error)
            } else TextButton(onDismiss) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_close_bbfa773)) }
        },
    )
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

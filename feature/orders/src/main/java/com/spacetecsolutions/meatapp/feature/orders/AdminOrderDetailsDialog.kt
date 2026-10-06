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
        title = { Text("Order ${order.displayNumber}") },
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
                item { Text("Items", style = MaterialTheme.typography.titleMedium) }
                items(order.items, key = { it.productId }) { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text("${item.quantity} × ${item.unitPriceMinor.money(order.currencyCode)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(item.lineTotalMinor.money(order.currencyCode))
                    }
                }
                item { DetailSection("Payment") {
                    Text("${order.paymentMethod.name} · ${order.paymentStatus.name}")
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
                    Text("Ready for delivery assignment", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall)
                }
                if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            when {
                order.canConfirm -> Button(onConfirm, enabled = !busy) { Text("Confirm") }
                order.canStartPreparing -> Button(onStartPreparing, enabled = !busy) { Text("Start Preparing") }
                order.canMarkReadyForDelivery -> Button(onMarkReady, enabled = !busy) { Text("Mark Ready") }
            }
        },
        dismissButton = {
            if (order.canReject) TextButton(onReject, enabled = !busy) {
                Text("Reject", color = MaterialTheme.colorScheme.error)
            } else TextButton(onDismiss) { Text("Close") }
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

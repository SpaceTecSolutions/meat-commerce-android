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
internal fun DeliveryOrderDetailsDialog(
    order: CodOrder,
    trackingAllowed: Boolean = false,
    dismiss: () -> Unit,
    navigate: () -> Unit = {},
    startDelivery: () -> Unit = {},
    canResumeTracking: Boolean = false,
    resumeTracking: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Order ${order.displayNumber}") },
        text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
            item {
                Text(order.customerName, style = MaterialTheme.typography.titleMedium)
                Text(order.customerMobile)
                Text(order.addressSummary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { HorizontalDivider() }
            items(order.items, key = { it.productId }) { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                        Text("Quantity ${item.quantity}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(item.lineTotalMinor.money(order.currencyCode))
                }
            }
            if (order.instructions.isNotBlank()) item {
                Text("Instructions", style = MaterialTheme.typography.titleSmall)
                Text(order.instructions)
            }
            item {
                Text("Payment: ${order.paymentMethod.name} · ${order.paymentStatus.name}")
                Text("Amount due: ${order.amountDueMinor.money(order.currencyCode)}",
                    style = MaterialTheme.typography.titleMedium)
            }
            if (trackingAllowed && order.canStartDelivery) item {
                Text("Live tracking will be prepared when delivery starts.",
                    color = MaterialTheme.colorScheme.primary)
            }
        } },
        confirmButton = {
            if (order.canStartDelivery) Button(onClick = startDelivery) { Text("Start Delivery") }
            else if (canResumeTracking) Button(onClick = resumeTracking) { Text("Resume Live Tracking") }
            else Button(onClick = dismiss) { Text("Close") }
        },
        dismissButton = {
            if (order.canStartDelivery) OutlinedButton(onClick = navigate) { Text("Navigate") }
        },
    )
}

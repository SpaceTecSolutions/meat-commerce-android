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
        title = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_order_order_displaynumber_5a620eb, order.displayNumber)) },
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
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_quantity_item_quantitylabel_781ea8e, item.quantityLabel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(item.lineTotalMinor.money(order.currencyCode))
                }
            }
            if (order.instructions.isNotBlank()) item {
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_instructions_ed58f29), style = MaterialTheme.typography.titleSmall)
                Text(order.instructions)
            }
            item {
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_payment_order_paymentmethod_name_order_8e99c10, order.paymentMethod.name, order.paymentStatus.name))
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_amount_due_order_amountdueminor_money_5f3936e, order.amountDueMinor.money(order.currencyCode)),
                    style = MaterialTheme.typography.titleMedium)
            }
            if (trackingAllowed && order.canStartDelivery) item {
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_live_tracking_will_be_prepared_945417d),
                    color = MaterialTheme.colorScheme.primary)
            }
        } },
        confirmButton = {
            if (order.canStartDelivery) Button(onClick = startDelivery) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_start_delivery_a40db5d)) }
            else if (canResumeTracking) Button(onClick = resumeTracking) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_resume_live_tracking_50df755)) }
            else Button(onClick = dismiss) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_close_bbfa773)) }
        },
        dismissButton = {
            if (order.canStartDelivery) OutlinedButton(onClick = navigate) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_navigate_1eee3af)) }
        },
    )
}

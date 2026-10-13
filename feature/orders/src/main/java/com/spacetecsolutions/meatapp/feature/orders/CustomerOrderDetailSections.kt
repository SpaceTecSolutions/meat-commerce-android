package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.OrderStatus
import com.spacetecsolutions.meatapp.core.model.PaymentStatus

@Composable
internal fun CustomerAddressDetail(order: CodOrder, model: CustomerOrderUiModel,
    onNavigate: () -> Unit) {
    CustomerDetailSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CustomerDetailLabel("DELIVERY ADDRESS")
            Spacer(Modifier.weight(1f))
            if (order.addressSummary.isNotBlank()) TextButton(onNavigate,
                contentPadding = PaddingValues(0.dp)) {
                Icon(AppIcons.Location, null, Modifier.size(16.dp), tint = T.red)
                Spacer(Modifier.width(3.dp)); Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_view_map_7e06a4c), color = T.red)
            }
        }
        if (order.addressLabel.isNotBlank()) Surface(color = T.soft,
            shape = RoundedCornerShape(5.dp)) {
            Text(order.addressLabel.uppercase(),
                Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall, color = T.ink)
        }
        Text(order.addressSummary, style = MaterialTheme.typography.bodySmall, color = T.ink)
        if (model.formattedDeliveryTime.isNotBlank()) {
            HorizontalDivider(color = T.border)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (order.orderStatus == OrderStatus.DELIVERED) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_delivered_slot_f787da3) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_estimated_delivery_1166329), style = MaterialTheme.typography.bodySmall,
                    color = T.muted)
                Text(model.formattedDeliveryTime,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold, color = T.ink)
            }
            order.deliverySlotChangeReason?.takeIf(String::isNotBlank)?.let { reason ->
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_delivery_time_changed_reason_b8eaf1e, reason), style = MaterialTheme.typography.bodySmall,
                    color = T.muted)
            }
        }
    }
}

@Composable
internal fun CustomerPaymentDetail(order: CodOrder) {
    CustomerDetailSurface {
        CustomerDetailLabel("PAYMENT DETAILS")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PaymentTile("Payment Method", when (order.paymentMethod) {
                CheckoutPaymentMethod.COD -> "Cash on Delivery"
                CheckoutPaymentMethod.RAZORPAY -> "Razorpay"
                CheckoutPaymentMethod.UPI -> "UPI"
            }, Modifier.weight(1f))
            PaymentTile("Payment Status",
                order.paymentStatus.name.lowercase().replaceFirstChar(Char::titlecase),
                Modifier.weight(1f),
                if (order.paymentStatus == PaymentStatus.PAID) T.green
                else if (order.paymentStatus == PaymentStatus.FAILED) T.red else T.amber)
        }
    }
}

@Composable
private fun PaymentTile(label: String, value: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color = T.ink) {
    Surface(modifier, color = T.canvas, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, T.border)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = T.muted)
            Text(value, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, color = color, maxLines = 2)
        }
    }
}

@Composable
internal fun CustomerOrderItemsDetail(order: CodOrder) {
    var expanded by rememberSaveable(order.id) { mutableStateOf(false) }
    val visible = if (expanded || order.items.size <= 3) order.items else order.items.take(3)
    CustomerDetailSurface {
        Column(Modifier.animateContentSize(tween(AppMotion.STANDARD_MILLIS))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CustomerDetailLabel(if (order.orderStatus == OrderStatus.CANCELLED)
                    "CANCELLED ITEMS" else "ORDER ITEMS")
                Spacer(Modifier.weight(1f))
                Surface(color = T.soft, shape = RoundedCornerShape(50)) {
                    Text(order.items.size.toString() + " items",
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall, color = T.muted)
                }
            }
            Spacer(Modifier.height(8.dp))
            visible.forEachIndexed { index, item ->
                CustomerOrderItemRow(item, order.currencyCode)
                if (index != visible.lastIndex) HorizontalDivider(color = T.border)
            }
            if (order.items.size > 3) TextButton({ expanded = !expanded },
                Modifier.fillMaxWidth()) {
                Text(if (expanded) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_show_less_08d205d) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_show_more_84e9fe0) +
                    (order.items.size - 3) + " more)", color = T.red)
            }
        }
    }
}

@Composable
internal fun CustomerBillDetail(order: CodOrder) {
    CustomerDetailSurface {
        CustomerDetailLabel("BILL SUMMARY")
        HorizontalDivider(color = T.border)
        OrderSummaryRow("Item Total", order.subtotalMinor.money(order.currencyCode))
        if (order.discountMinor > 0) OrderSummaryRow("Discount",
            "−" + order.discountMinor.money(order.currencyCode))
        OrderSummaryRow("Delivery Charge", order.deliveryFeeMinor.money(order.currencyCode))
        if (order.taxMinor > 0) OrderSummaryRow("Tax", order.taxMinor.money(order.currencyCode))
        HorizontalDivider(color = T.border)
        OrderSummaryRow(when {
            order.paymentStatus == PaymentStatus.PAID -> "Paid"
            order.paymentMethod == CheckoutPaymentMethod.COD -> "To Pay (COD)"
            else -> "Total"
        }, order.totalMinor.money(order.currencyCode), emphasized = true)
    }
}

@Composable
internal fun CustomerDetailHelp(onHelp: () -> Unit) {
    CustomerDetailSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Call, null, Modifier.size(20.dp), tint = T.muted)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_need_help_with_this_order_e065805), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = T.ink)
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_contact_our_support_team_6456b67), style = MaterialTheme.typography.bodySmall,
                    color = T.muted)
            }
            TextButton(onHelp) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_call_us_84207ab), color = T.ink) }
        }
    }
}

@Composable
internal fun CustomerDownloadInvoice() {
    CustomerDetailSurface {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_download_invoice_5366b40))
            Spacer(Modifier.width(10.dp))
            Icon(AppIcons.DownloadInvoice, null, Modifier.size(20.dp), tint = T.red)
        }
    }
}

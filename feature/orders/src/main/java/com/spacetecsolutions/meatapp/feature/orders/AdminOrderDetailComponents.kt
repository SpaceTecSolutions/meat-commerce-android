package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.RoundedSquareImage
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.OrderItemSnapshot

@Composable
internal fun CustomerInfoSection(order: CodOrder, call: (String) -> Unit, modifier: Modifier = Modifier) =
    DetailSurface("Customer", AppIcons.Customers, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(AppIcons.Profile, null, Modifier.size(22.dp)) }
            }
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.medium)) {
                Text(order.customerName, fontWeight = FontWeight.SemiBold)
                Text(order.customerMobile, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (order.customerMobile.isNotBlank()) IconButton({ call(order.customerMobile) }) {
                Icon(AppIcons.Call, "Call ${order.customerName}", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

@Composable
internal fun DeliveryAddressSection(order: CodOrder, navigate: (String) -> Unit, modifier: Modifier = Modifier) =
    DetailSurface("Delivery Address", AppIcons.Location, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.HomeAddress, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.medium)) {
                Text(order.addressLabel, fontWeight = FontWeight.SemiBold)
                Text(order.addressSummary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton({ navigate(order.addressSummary) }) { Icon(AppIcons.Location, "Open address in maps",
                tint = MaterialTheme.colorScheme.primary) }
        }
    }

@Composable
internal fun OrderItemRow(item: OrderItemSnapshot, currencyCode: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundedSquareImage(item.imageUrl, item.name, 44.dp, cornerRadius = 8.dp)
        Column(Modifier.weight(1f).padding(horizontal = AppSpacing.medium)) {
            Text(item.name, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val attributes = item.attributes.values.joinToString(" · ")
            Text(listOf(item.unit.name.pretty(), attributes).filter(String::isNotBlank).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("×${item.quantity}", Modifier.padding(horizontal = AppSpacing.small),
            style = MaterialTheme.typography.labelLarge)
        Text(item.lineTotalMinor.money(currencyCode), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun OrderBillSummary(order: CodOrder) = DetailSurface("Bill Summary", AppIcons.Orders) {
    BillRow("Item Total", order.subtotalMinor.money(order.currencyCode))
    if (order.discountMinor > 0) BillRow("Discount", "−${order.discountMinor.money(order.currencyCode)}")
    BillRow("Delivery Charge", order.deliveryFeeMinor.money(order.currencyCode))
    if (order.taxMinor > 0) BillRow("Tax", order.taxMinor.money(order.currencyCode))
    HorizontalDivider(Modifier.padding(vertical = AppSpacing.small))
    BillRow(if (order.paymentMethod.name == "COD") "To Pay (COD)" else "Total Paid",
        order.totalMinor.money(order.currencyCode), true)
}

@Composable
internal fun PaymentDetailsSection(order: CodOrder) = DetailSurface("Payment Details", AppIcons.Cash) {
    BillRow("Payment Method", order.paymentMethod.name.pretty())
    BillRow("Payment Status", order.paymentStatus.name.pretty())
}

@Composable
internal fun DeliverySlotSection(order: CodOrder) = DetailSurface("Delivery Time", AppIcons.Calendar) {
    order.deliverySlotDateLabel?.let { Text(it, fontWeight = FontWeight.SemiBold) }
    order.deliverySlotTimeLabel?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
internal fun CustomerNoteSection(note: String) = DetailSurface("Customer Note", AppIcons.Message) {
    Text("“$note”", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun DetailSurface(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.fillMaxWidth(), shape = AppShapes.medium, color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(AppSpacing.medium), verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, Modifier.padding(start = AppSpacing.small), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (emphasized) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (emphasized) FontWeight.Bold else null)
        Text(value, color = if (emphasized) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            fontWeight = FontWeight.Bold)
    }
}

private fun String.pretty() = lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)

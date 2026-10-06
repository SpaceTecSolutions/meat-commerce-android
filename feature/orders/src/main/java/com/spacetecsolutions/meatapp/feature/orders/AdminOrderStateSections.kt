package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.RoundedSquareImage
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.OrderItemSnapshot
import com.spacetecsolutions.meatapp.core.model.OrderStatus

@Composable
internal fun AdminOrderProgress(order: CodOrder) {
    if (order.orderStatus == OrderStatus.CANCELLED) return
    val stages = listOf("Placed", "Confirmed", "Preparing", "Out Delivery", "Delivered")
    val current = when (order.orderStatus) {
        OrderStatus.PENDING -> 0
        OrderStatus.CONFIRMED -> 1
        OrderStatus.PREPARING -> 2
        OrderStatus.OUT_FOR_DELIVERY -> 3
        OrderStatus.DELIVERED -> 4
        OrderStatus.CANCELLED -> 0
    }
    Surface(shape = RoundedCornerShape(18.dp), color = T.surface,
        border = BorderStroke(1.dp, T.border), shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            stages.forEachIndexed { index, label ->
                val reached = index <= current
                val color = when {
                    !reached -> T.muted
                    order.orderStatus == OrderStatus.DELIVERED -> T.green
                    index == current && current == 2 -> T.amber
                    else -> T.red
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Surface(shape = RoundedCornerShape(50),
                        color = if (reached) color else T.canvas,
                        border = BorderStroke(2.dp, if (reached) color else T.border)) {
                        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                            if (reached) Icon(AppIcons.Check, null, Modifier.size(14.dp), tint = T.surface)
                        }
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (index == current) FontWeight.Bold else FontWeight.Normal,
                        color = color, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}

@Composable
internal fun AdminCancelledHero(order: CodOrder) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = T.red, shape = RoundedCornerShape(50)) {
            Icon(AppIcons.Close, null, Modifier.padding(18.dp).size(30.dp), tint = T.surface)
        }
        Text("ORDER CANCELLED", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = T.red)
        Surface(shape = RoundedCornerShape(16.dp), color = T.rose,
            border = BorderStroke(1.dp, T.red.copy(alpha = .25f))) {
            Column(Modifier.fillMaxWidth().padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Order cancelled", fontWeight = FontWeight.Bold, color = T.red)
                Text("No further delivery actions can be taken.",
                    style = MaterialTheme.typography.bodySmall, color = T.ink)
                order.cancelledAtEpochMillis?.let { Text(it.orderDateTime(),
                    style = MaterialTheme.typography.labelSmall, color = T.muted) }
            }
        }
    }
}

@Composable
internal fun AdminDeliveredHero(order: CodOrder) {
    Surface(shape = RoundedCornerShape(16.dp), color = T.green,
        shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Check, null, tint = T.surface)
            Column {
                Text("Order Delivered Successfully", fontWeight = FontWeight.Bold,
                    color = T.surface)
                order.deliveredAtEpochMillis?.let { Text("Delivered ${it.orderDateTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = T.surface.copy(alpha = .9f)) }
            }
        }
    }
}

@Composable
internal fun AdminOrderItemsSection(order: CodOrder) {
    var expanded by rememberSaveable(order.id) { mutableStateOf(false) }
    val visible = if (expanded || order.items.size <= 3) order.items else order.items.take(3)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = T.surface,
        border = BorderStroke(1.dp, T.border), shadowElevation = 1.dp) {
        Column(Modifier.padding(14.dp).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Order Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Surface(Modifier.padding(start = 6.dp), shape = RoundedCornerShape(50), color = T.canvas) {
                    Text("${order.items.size} items", Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall, color = T.muted)
                }
                Spacer(Modifier.weight(1f))
                Text("Butcher Verified", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold, color = T.red)
            }
            OrderItemsTableHeader()
            visible.forEachIndexed { index, item ->
                AdminOrderTableRow(item, order.currencyCode)
                if (index != visible.lastIndex) HorizontalDivider(color = T.border)
            }
            if (order.items.size > 3) OutlinedButton({ expanded = !expanded },
                Modifier.fillMaxWidth()) {
                Text(if (expanded) "Show Less" else "Show ${order.items.size - 3} more items")
            }
        }
    }
}

@Composable
private fun OrderItemsTableHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("Item", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = T.muted)
        Text("Qty", Modifier.width(34.dp), style = MaterialTheme.typography.labelSmall,
            color = T.muted, textAlign = TextAlign.Center)
        Text("Unit", Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall,
            color = T.muted, textAlign = TextAlign.Center)
        Text("Total", Modifier.width(78.dp), style = MaterialTheme.typography.labelSmall,
            color = T.muted, textAlign = TextAlign.End)
    }
}

@Composable
private fun AdminOrderTableRow(item: OrderItemSnapshot, currencyCode: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            RoundedSquareImage(item.imageUrl, item.name, 42.dp, cornerRadius = 8.dp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(item.name, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${item.unitPriceMinor.money(currencyCode)} / ${item.unit.name.lowercase()}",
                    style = MaterialTheme.typography.labelSmall, color = T.muted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(item.quantity.toString(), Modifier.width(34.dp), style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center)
        Text(item.unit.name.lowercase(), Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center, color = T.muted)
        Text(item.lineTotalMinor.money(currencyCode), Modifier.width(78.dp),
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End)
    }
}

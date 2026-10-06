package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.RoundedSquareImage
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.OrderItemSnapshot
import com.spacetecsolutions.meatapp.core.model.OrderStatus

@Composable
internal fun CustomerOrderStatusTabs(selected: CustomerOrderTab,
    select: (CustomerOrderTab) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        CustomerOrderTab.entries.forEach { tab ->
            val active = selected == tab
            val background by animateColorAsState(if (active) T.red else T.soft,
                tween(AppMotion.MICRO_MILLIS), label = "order-tab")
            Surface(onClick = { select(tab) }, Modifier.weight(
                if (tab == CustomerOrderTab.ALL) .7f else 1f).height(42.dp),
                shape = RoundedCornerShape(50), color = background) {
                Box(contentAlignment = Alignment.Center) {
                    Text(tab.label(), style = MaterialTheme.typography.labelMedium,
                        maxLines = 1, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) T.surface else T.ink)
                }
            }
        }
    }
}

@Composable
internal fun CustomerOrderStatusChip(model: CustomerOrderUiModel) {
    val order = model.order
    val foreground = when {
        model.isDelayed -> T.amber
        order.orderStatus == OrderStatus.DELIVERED -> T.green
        order.orderStatus == OrderStatus.CANCELLED -> T.red
        else -> order.orderStatus.strongColor()
    }
    val background = when {
        model.isDelayed -> T.amberSurface
        order.orderStatus == OrderStatus.DELIVERED -> T.greenSurface
        order.orderStatus == OrderStatus.CANCELLED -> T.rose
        else -> order.orderStatus.softColor()
    }
    Surface(color = background, shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, foreground.copy(alpha = .25f))) {
        Text(model.displayStatus, Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            color = foreground, maxLines = 1)
    }
}

@Composable
internal fun CustomerOrderCard(model: CustomerOrderUiModel, onClick: (CodOrder) -> Unit,
    onTrack: (() -> Unit)? = null, onReorder: (() -> Unit)? = null,
    modifier: Modifier = Modifier) {
    val order = model.order
    val outForDelivery = order.orderStatus == OrderStatus.OUT_FOR_DELIVERY
    val accent = when {
        model.isDelayed -> T.amber
        order.orderStatus == OrderStatus.DELIVERED -> T.green
        order.orderStatus == OrderStatus.CANCELLED -> T.red
        else -> T.red
    }
    Card(modifier = modifier.fillMaxWidth().semantics {
        contentDescription = order.numberLabel() + ", " + model.displayStatus + ", " +
            order.totalMinor.money(order.currencyCode)
    }.clickable { onClick(order) }, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = T.surface),
        border = BorderStroke(1.dp, when {
            outForDelivery -> T.red.copy(alpha = .32f)
            model.isDelayed -> T.amber.copy(alpha = .4f)
            else -> T.border
        }),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(order.numberLabel(), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = T.ink, maxLines = 1)
                Box(Modifier.weight(1f)) { CustomerOrderStatusChip(model) }
                Text(order.totalMinor.money(order.currencyCode),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (order.orderStatus == OrderStatus.CANCELLED) T.muted else T.ink)
            }
            Text(model.formattedCreatedDate, style = MaterialTheme.typography.bodySmall,
                color = T.muted)
            if (outForDelivery) Surface(color = T.rose,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, T.red.copy(alpha = .16f))) {
                Row(Modifier.fillMaxWidth().padding(11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(AppIcons.DeliveryPerson, null, Modifier.size(20.dp), tint = T.red)
                    Column {
                        Text(if (model.isDelayed) "Delivery is running late" else "Rider is on the way",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold, color = T.ink)
                        if (model.formattedDeliveryTime.isNotBlank())
                            Text("Slot: " + model.formattedDeliveryTime,
                                style = MaterialTheme.typography.labelSmall, color = T.muted)
                    }
                }
            }
            if (model.isDelayed && !outForDelivery) Surface(color = T.amberSurface,
                shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp,
                    T.amber.copy(alpha = .25f))) {
                Column(Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(model.statusDetail ?: "Delivery time has passed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = T.amber)
                    if (model.formattedDeliveryTime.isNotBlank())
                        Text("Slot: " + model.formattedDeliveryTime,
                            style = MaterialTheme.typography.bodySmall, color = T.amber)
                }
            } else if (!outForDelivery && model.formattedDeliveryTime.isNotBlank()) {
                Text(model.formattedDeliveryTime, style = MaterialTheme.typography.bodySmall,
                    color = accent, maxLines = 2)
            }
            if (order.items.isNotEmpty()) {
                HorizontalDivider(color = T.border)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundedSquareImage(order.items.first().imageUrl, order.items.first().name,
                        42.dp, cornerRadius = 10.dp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(order.items.first().name, style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, color = T.ink)
                        Text(order.items.size.toString() +
                            if (order.items.size == 1) " item" else " items",
                            style = MaterialTheme.typography.labelSmall, color = T.muted)
                    }
                    when {
                        onTrack != null -> Button(onTrack,
                            colors = ButtonDefaults.buttonColors(containerColor = T.red),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Text("Track Live", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(4.dp))
                            Icon(AppIcons.ArrowRight, null, Modifier.size(15.dp))
                        }
                        onReorder != null -> TextButton(onReorder) { Text("Reorder", color = T.red) }
                        else -> Text("View Details ›", style = MaterialTheme.typography.labelMedium,
                            color = T.muted)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CustomerOrderItemRow(item: OrderItemSnapshot, currencyCode: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RoundedSquareImage(item.imageUrl, item.name, 48.dp, cornerRadius = 10.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 2,
                overflow = TextOverflow.Ellipsis, color = T.ink)
            Text(item.quantity.toString() + " " + item.unit.shortLabel(),
                style = MaterialTheme.typography.bodySmall, color = T.muted)
        }
        Spacer(Modifier.width(8.dp))
        Text(item.lineTotalMinor.money(currencyCode),
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = T.ink)
    }
}

@Composable
internal fun OrderSummaryRow(label: String, value: String, emphasized: Boolean = false,
    valueColor: Color = if (emphasized) T.red else T.ink) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (emphasized) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.bodySmall, color = if (emphasized) T.red else T.muted,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text(value, style = if (emphasized) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

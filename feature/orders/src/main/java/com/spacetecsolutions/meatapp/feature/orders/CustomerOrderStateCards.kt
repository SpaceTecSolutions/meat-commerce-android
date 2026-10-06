package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.OrderStatus
import com.spacetecsolutions.meatapp.core.model.UserRole

@Composable
internal fun CustomerDetailSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = T.surface,
        border = BorderStroke(1.dp, T.border), shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun CustomerDetailLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold, color = T.muted)
}

@Composable
internal fun CustomerDetailHeader(model: CustomerOrderUiModel) {
    CustomerDetailSurface {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                CustomerDetailLabel("ORDER ID")
                Text(model.order.numberLabel(), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = T.ink)
                Text("Placed: " + model.formattedCreatedDate,
                    style = MaterialTheme.typography.bodySmall, color = T.muted)
            }
            CustomerOrderStatusChip(model)
        }
        if (model.order.orderStatus == OrderStatus.CANCELLED &&
            model.order.cancelledByRole == UserRole.CUSTOMER)
            Text("Cancelled by You", style = MaterialTheme.typography.bodySmall, color = T.red)
    }
}

@Composable
internal fun CustomerActiveState(order: CodOrder, model: CustomerOrderUiModel,
    onHelp: () -> Unit, onTrack: () -> Unit) {
    CustomerDetailSurface {
        if (model.isDelayed) Surface(color = T.amberSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, T.amber.copy(alpha = .35f))) {
            Column(Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (order.orderStatus == OrderStatus.OUT_FOR_DELIVERY)
                    "Delivery is running late" else "Delivery is delayed",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, color = T.amber)
                Text(if (order.orderStatus == OrderStatus.OUT_FOR_DELIVERY)
                    "Your selected delivery time has passed. Your order is still on the way."
                    else "Your selected delivery time has passed. The shop is still processing this order.",
                    style = MaterialTheme.typography.bodySmall, color = T.ink)
                TextButton(onHelp, contentPadding = PaddingValues(0.dp)) {
                    Text("Contact Support", color = T.red)
                    Icon(AppIcons.ArrowRight, null, Modifier.size(16.dp), tint = T.red)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom) {
            Text("Order Status Timeline", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = T.ink)
            if (model.formattedDeliveryTime.isNotBlank()) Text(model.formattedDeliveryTime,
                Modifier.widthIn(max = 145.dp), style = MaterialTheme.typography.labelSmall,
                color = T.muted)
        }
        val timeline = order.timelineItems()
        timeline.forEachIndexed { index, stage ->
            CustomerProgressRow(stage, index == timeline.lastIndex)
        }
        val liveReady = model.isLiveTrackingAvailable && order.hasTrackingDestination
        if (liveReady || (model.canTrack && order.orderStatus != OrderStatus.OUT_FOR_DELIVERY))
            OutlinedButton(onTrack,
            Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(11.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (liveReady) T.red else T.surface,
                contentColor = if (liveReady) T.surface else T.ink)) {
            Icon(AppIcons.LiveTracking, null, Modifier.size(18.dp),
                tint = if (liveReady) T.surface else T.red)
            Spacer(Modifier.width(8.dp))
            Text(if (liveReady) "Live Tracking" else "View Order Status")
        }
    }
}

@Composable
private fun CustomerProgressRow(stage: CustomerTimelineItem, last: Boolean) {
    val color = when (stage.state) {
        TimelineVisualState.COMPLETED -> T.green
        TimelineVisualState.CURRENT, TimelineVisualState.CANCELLED -> T.red
        TimelineVisualState.UPCOMING -> T.border
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(24.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(20.dp), shape = RoundedCornerShape(50),
                color = if (stage.state == TimelineVisualState.UPCOMING) T.surface else color,
                border = BorderStroke(2.dp, color)) {
                Box(contentAlignment = Alignment.Center) {
                    if (stage.state != TimelineVisualState.UPCOMING)
                        Icon(if (stage.state == TimelineVisualState.COMPLETED) AppIcons.Check
                            else AppIcons.Pending, null, Modifier.size(13.dp), tint = T.surface)
                }
            }
            if (!last) Surface(Modifier.width(2.dp).weight(1f), color = color) {}
        }
        Column(Modifier.weight(1f).padding(start = 9.dp, bottom = 10.dp)) {
            Text(stage.label, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (stage.state == TimelineVisualState.UPCOMING) T.muted else T.ink)
            Text(stage.timestamp?.timelineDateTime()
                ?: if (stage.state == TimelineVisualState.UPCOMING) "Upcoming" else "",
                style = MaterialTheme.typography.labelSmall, color = T.muted)
        }
    }
}

@Composable
internal fun CustomerDeliveredState(order: CodOrder, model: CustomerOrderUiModel) {
    CustomerDetailSurface {
        Surface(color = T.greenSurface, shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, T.green.copy(alpha = .2f))) {
            Row(Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(AppIcons.Check, null, tint = T.green)
                Column {
                    Text("Delivered safely", fontWeight = FontWeight.Bold, color = T.ink)
                    order.deliveredAtEpochMillis?.let {
                        Text("Delivered " + CustomerOrderDateFormatter.created(it, System.currentTimeMillis()),
                            style = MaterialTheme.typography.bodySmall, color = T.green)
                    }
                    Text("Your order has been delivered.",
                        style = MaterialTheme.typography.bodySmall, color = T.muted)
                }
            }
        }
        CustomerDetailLabel("ORDER FULFILLED")
        if (model.formattedDeliveryTime.isNotBlank())
            Text("Delivery slot: " + model.formattedDeliveryTime,
                style = MaterialTheme.typography.bodySmall, color = T.muted)
    }
}

@Composable
internal fun CustomerCancelledState(order: CodOrder, model: CustomerOrderUiModel) {
    val byCustomer = order.cancelledByRole == UserRole.CUSTOMER
    CustomerDetailSurface {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = T.rose, shape = RoundedCornerShape(50)) {
                Icon(AppIcons.Close, null, Modifier.padding(12.dp).size(20.dp), tint = T.red)
            }
            Column {
                Text(if (byCustomer) "Order Cancelled by You" else "Order Cancelled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = T.ink)
                order.cancelledAtEpochMillis?.let {
                    Text("Cancelled " + CustomerOrderDateFormatter.created(it, System.currentTimeMillis()),
                        style = MaterialTheme.typography.bodySmall, color = T.muted)
                }
                order.cancelReason?.takeIf(String::isNotBlank)?.let {
                    Text("Reason: " + it, style = MaterialTheme.typography.bodySmall, color = T.ink)
                }
            }
        }
        if (model.formattedDeliveryTime.isNotBlank()) Text(
            "Delivery slot: " + model.formattedDeliveryTime,
            style = MaterialTheme.typography.bodySmall, color = T.muted)
    }
}

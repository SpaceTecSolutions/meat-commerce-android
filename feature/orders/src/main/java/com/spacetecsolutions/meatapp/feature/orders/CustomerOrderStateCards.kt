package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
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
                Text(stringResource(R.string.ui_placed_6b959c2) + model.formattedCreatedDate,
                    style = MaterialTheme.typography.bodySmall, color = T.muted)
            }
            CustomerOrderStatusChip(model)
        }
        if (model.order.orderStatus == OrderStatus.CANCELLED &&
            model.order.cancelledByRole == UserRole.CUSTOMER)
            Text(stringResource(R.string.ui_cancelled_by_you_989a848), style = MaterialTheme.typography.bodySmall, color = T.red)
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
                    stringResource(R.string.ui_delivery_is_running_late_fb20402) else stringResource(R.string.ui_delivery_is_delayed_1c8d0fb),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, color = T.amber)
                Text(if (order.orderStatus == OrderStatus.OUT_FOR_DELIVERY)
                    stringResource(R.string.ui_your_selected_delivery_time_has_08d24e7) else stringResource(R.string.ui_your_selected_delivery_time_has_3fb9780),
                    style = MaterialTheme.typography.bodySmall, color = T.ink)
                TextButton(onHelp, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.ui_contact_support_fe1a292), color = T.red)
                    Icon(AppIcons.ArrowRight, null, Modifier.size(16.dp), tint = T.red)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.ui_order_status_timeline_9851e57), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = T.ink)
            /*if (model.formattedDeliveryTime.isNotBlank()) Text(model.formattedDeliveryTime,
                Modifier.widthIn(max = 145.dp), style = MaterialTheme.typography.labelSmall,
                color = T.muted)*/
        }
        val timeline = order.timelineItems()
        timeline.forEachIndexed { index, stage ->
            CustomerProgressRow(stage, index == timeline.lastIndex)
        }
        val liveReady = model.isLiveTrackingAvailable && order.hasTrackingDestination
        if (liveReady /*|| (model.canTrack && order.orderStatus != OrderStatus.OUT_FOR_DELIVERY)*/)
            OutlinedButton(onTrack,
            Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(11.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (liveReady) T.red else T.surface,
                contentColor = if (liveReady) T.surface else T.ink)) {
            Icon(AppIcons.LiveTracking, null, Modifier.size(18.dp),
                tint = if (liveReady) T.surface else T.red)
            Spacer(Modifier.width(8.dp))
            Text(if (liveReady) stringResource(R.string.ui_live_tracking_d6056f9) else stringResource(R.string.ui_view_order_status_a4d764c))
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
    val pulse = rememberInfiniteTransition(label = "checkout step marker")
    val arrowScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse),
        label = "checkout arrow scale",
    )
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(24.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(20.dp).scale(if (stage.state == TimelineVisualState.UPCOMING) arrowScale else 1f), shape = RoundedCornerShape(50),
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
                    Text(stringResource(R.string.ui_delivered_safely_841356a), fontWeight = FontWeight.Bold, color = T.ink)
                    order.deliveredAtEpochMillis?.let {
                        Text(stringResource(R.string.ui_delivered_bb5df27) + CustomerOrderDateFormatter.created(it, System.currentTimeMillis()),
                            style = MaterialTheme.typography.bodySmall, color = T.green)
                    }
                    Text(stringResource(R.string.ui_your_order_has_been_delivered_f8f9a04),
                        style = MaterialTheme.typography.bodySmall, color = T.muted)
                }
            }
        }
        CustomerDetailLabel("ORDER FULFILLED")
        if (model.formattedDeliveryTime.isNotBlank())
            Text(stringResource(R.string.ui_delivery_slot_78663db) + model.formattedDeliveryTime,
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
                Text(if (byCustomer) stringResource(R.string.ui_order_cancelled_by_you_5d72a6f) else stringResource(R.string.ui_order_cancelled_1f1445f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = T.ink)
                order.cancelledAtEpochMillis?.let {
                    Text(stringResource(R.string.ui_cancelled_6bae6a7) + CustomerOrderDateFormatter.created(it, System.currentTimeMillis()),
                        style = MaterialTheme.typography.bodySmall, color = T.muted)
                }
                order.cancelReason?.takeIf(String::isNotBlank)?.let {
                    Text(stringResource(R.string.ui_reason_3cf4a40) + it, style = MaterialTheme.typography.bodySmall, color = T.ink)
                }
            }
        }
        if (model.formattedDeliveryTime.isNotBlank()) Text(
            "Delivery slot: " + model.formattedDeliveryTime,
            style = MaterialTheme.typography.bodySmall, color = T.muted)
    }
}

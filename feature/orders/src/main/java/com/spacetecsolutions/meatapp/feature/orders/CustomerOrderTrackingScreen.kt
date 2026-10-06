package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.designsystem.theme.Success
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.DeliveryLocation
import com.spacetecsolutions.meatapp.core.model.DeliveryRoute
import com.spacetecsolutions.meatapp.core.model.DeliveryOtp
import com.spacetecsolutions.meatapp.core.model.OrderStatus

@Composable
internal fun CustomerOrderTrackingScreen(
    order: CodOrder,
    liveLocation: DeliveryLocation?,
    liveRoute: DeliveryRoute?,
    liveRouteError: Boolean,
    liveRouteLoading: Boolean,
    liveTrackingError: Boolean,
    deliveryOtp: DeliveryOtp?,
    deliveryOtpError: Boolean,
    nowEpochMillis: Long,
    onBack: () -> Unit,
    onContact: () -> Unit,
    onRetryOtp: () -> Unit,
) {
    if (order.realtimeTrackingAvailable && order.hasTrackingDestination) {
        CustomerLiveTrackingScreen(
            order = order,
            riderLocation = liveLocation,
            route = liveRoute,
            routeFailed = liveRouteError,
            routeLoading = liveRouteLoading,
            locationFailed = liveTrackingError,
            deliveryOtp = deliveryOtp,
            deliveryOtpError = deliveryOtpError,
            nowEpochMillis = nowEpochMillis,
            onBack = onBack,
            onContact = onContact,
            onRetryOtp = onRetryOtp,
        )
        return
    }
    val timeline = order.timelineItems()
    Column(Modifier.fillMaxSize()) {
        TrackingTopBar(onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth),
                contentPadding = PaddingValues(
                    start = AppSpacing.medium,
                    end = AppSpacing.medium,
                    bottom = AppSpacing.large,
                ),
            ) {
                item { TrackingHeader(order, nowEpochMillis) }
                item { EstimatedDeliverySection(order, nowEpochMillis) }
                item { DeliveryInfo(order, nowEpochMillis) }
                item {
                    Column(Modifier.padding(vertical = AppSpacing.medium)) {
                        timeline.forEachIndexed { index, item ->
                            OrderTimelineItem(item, index == timeline.lastIndex)
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onContact,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = AppShapes.medium,
                    ) {
                        Text("Need Help? Contact Us", modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold)
                        Icon(AppIcons.Call, contentDescription = "Contact support")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackingTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Order Tracking", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold) },
        navigationIcon = { IconButton(onBack) { Icon(AppIcons.Back, contentDescription = "Back") } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        expandedHeight = 56.dp,
    )
}

@Composable
private fun TrackingHeader(order: CodOrder, nowEpochMillis: Long) {
    val model = order.toCustomerOrderUi(nowEpochMillis)
    Row(
        Modifier.fillMaxWidth().padding(vertical = AppSpacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(order.numberLabel(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AnimatedContent(
            targetState = order.orderStatus,
            transitionSpec = { androidx.compose.animation.fadeIn(tween(AppMotion.MICRO_MILLIS)) togetherWith
                androidx.compose.animation.fadeOut(tween(AppMotion.QUICK_MILLIS)) },
            label = "tracking-status",
        ) {
            if (model.isDelayed && it != OrderStatus.OUT_FOR_DELIVERY) {
                com.spacetecsolutions.meatapp.core.designsystem.component.StatusChip(
                    model.displayStatus,
                    tone = com.spacetecsolutions.meatapp.core.designsystem.component.StatusTone.WARNING,
                )
            } else OrderStatusChip(it)
        }
    }
}

@Composable
private fun EstimatedDeliverySection(order: CodOrder, nowEpochMillis: Long) {
    Column(
        Modifier.fillMaxWidth().padding(top = AppSpacing.medium, bottom = AppSpacing.small),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
    ) {
        Text("Estimated Delivery", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            CustomerOrderDateFormatter.delivery(order, nowEpochMillis).ifBlank { "Timing unavailable" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DeliveryInfo(order: CodOrder, nowEpochMillis: Long) {
    val model = order.toCustomerOrderUi(nowEpochMillis)
    val copy = when {
        model.isDelayed && order.orderStatus == OrderStatus.OUT_FOR_DELIVERY ->
            "Your order is on the way but is running late. Live tracking will continue until delivery."
        model.isDelayed -> "Delivery is delayed. Your selected delivery time has passed."
        else -> when (order.orderStatus) {
        OrderStatus.PENDING -> "Waiting for shop confirmation."
        OrderStatus.OUT_FOR_DELIVERY -> "We deliver according to the time slot.\nYou will receive a call before delivery."
        else -> order.trackingMessage()
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.small),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = AppShapes.small,
    ) {
        Text(copy, Modifier.padding(AppSpacing.compact), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OrderTimelineItem(item: CustomerTimelineItem, last: Boolean) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(
            modifier = Modifier.width(28.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimelineIndicator(item.state)
            if (!last) Box(
                Modifier.width(2.dp).weight(1f).padding(vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(Modifier.fillMaxSize(), color = timelineColor(item.state)) {}
            }
        }
        Column(
            Modifier.weight(1f).padding(start = AppSpacing.small, bottom = AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
        ) {
            Text(item.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                item.timestamp?.timelineDateTime() ?: if (item.state == TimelineVisualState.UPCOMING) "Upcoming" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimelineIndicator(state: TimelineVisualState) {
    val color = timelineColor(state)
    Surface(
        modifier = Modifier.size(20.dp),
        shape = AppShapes.large,
        color = if (state == TimelineVisualState.UPCOMING) MaterialTheme.colorScheme.surface else color,
        border = BorderStroke(2.dp, color),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (state) {
                TimelineVisualState.COMPLETED -> Icon(AppIcons.Check, null, Modifier.size(13.dp), Color.White)
                TimelineVisualState.CANCELLED -> Icon(AppIcons.Close, null, Modifier.size(13.dp), Color.White)
                TimelineVisualState.CURRENT -> Icon(AppIcons.Pending, null, Modifier.size(11.dp), Color.White)
                TimelineVisualState.UPCOMING -> Unit
            }
        }
    }
}

@Composable
private fun timelineColor(state: TimelineVisualState) = when (state) {
    TimelineVisualState.COMPLETED -> Success
    TimelineVisualState.CURRENT, TimelineVisualState.CANCELLED -> MaterialTheme.colorScheme.primary
    TimelineVisualState.UPCOMING -> MaterialTheme.colorScheme.outline
}

package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.DeliveryLocation
import com.spacetecsolutions.meatapp.core.model.DeliveryRoute
import com.spacetecsolutions.meatapp.core.model.DeliveryOtp
import com.spacetecsolutions.meatapp.core.model.UserRole

@Composable
internal fun TrackingDeliveryPanel(
    order: CodOrder,
    rider: DeliveryLocation?,
    route: DeliveryRoute?,
    stale: Boolean,
    nowEpochMillis: Long,
    deliveryOtp: DeliveryOtp?,
    deliveryOtpError: Boolean,
    onRetryOtp: () -> Unit,
    onContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, color = T.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 10.dp) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp),
                color = T.border, shape = RoundedCornerShape(50)) {}
            RiderContactCard(order, rider, stale, nowEpochMillis, onContact)
            CustomerDeliveryOtpCard(deliveryOtp, deliveryOtpError, onRetryOtp)
            if (route != null && route.distanceMeters > 0 && !stale) Row(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackingMetric("DISTANCE LEFT", route.distanceMeters.distanceLabel(),
                    Modifier.weight(1f))
                if (route.durationSeconds > 0) TrackingMetric("ESTIMATED ARRIVAL",
                    "~${(route.durationSeconds / 60).coerceAtLeast(1)} min",
                    Modifier.weight(1f))
            }
            Surface(color = T.canvas, shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, T.border)) {
                Row(Modifier.fillMaxWidth().padding(11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(AppIcons.Location, null, Modifier.size(18.dp), tint = T.red)
                    Column(Modifier.weight(1f)) {
                        Text(order.addressLabel.ifBlank { "Delivery location" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = T.ink)
                        Text(order.addressSummary, style = MaterialTheme.typography.labelSmall,
                            color = T.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            TrackingOrderStatus(order, nowEpochMillis)
        }
    }
}

@Composable
private fun RiderContactCard(order: CodOrder, rider: DeliveryLocation?, stale: Boolean,
    nowEpochMillis: Long, onContact: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = T.canvas,
        border = BorderStroke(1.dp, T.border)) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = T.red) {
                Icon(AppIcons.DeliveryPerson, null, Modifier.padding(10.dp).size(22.dp),
                    tint = T.surface)
            }
            Column(Modifier.weight(1f)) {
                Text(deliveryName(order), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, color = T.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (order.assignedDeliveryRole == UserRole.ADMIN) "Shop administrator"
                    else "Delivery partner", style = MaterialTheme.typography.labelSmall,
                    color = T.muted)
                Text(when {
                    rider == null -> "Waiting for location"
                    stale -> "Location update delayed"
                    else -> locationAgeLabel(nowEpochMillis - rider.recordedAtEpochMillis)
                }, style = MaterialTheme.typography.labelSmall,
                    color = if (rider != null && !stale) T.green else T.amber)
            }
            if (!order.deliveryContactMobile.isNullOrBlank())
                FilledTonalIconButton(onContact,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = T.greenSurface, contentColor = T.green)) {
                    Icon(AppIcons.Call, "Call delivery partner")
                }
        }
    }
}

@Composable
private fun TrackingMetric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = T.rose, shape = RoundedCornerShape(13.dp)) {
        Column(Modifier.padding(11.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = T.muted)
            Text(value, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = T.red)
        }
    }
}

@Composable
private fun TrackingOrderStatus(order: CodOrder, nowEpochMillis: Long) {
    val model = order.toCustomerOrderUi(nowEpochMillis)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("LIVE ORDER STATUS", Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = T.ink)
            CustomerOrderStatusChip(model)
        }
        if (model.isDelayed) Text("Delivery is running late",
            style = MaterialTheme.typography.labelSmall, color = T.amber)
        order.timelineItems().filter { it.state != TimelineVisualState.UPCOMING }
            .forEach { stage -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(if (stage.state == TimelineVisualState.CURRENT) AppIcons.Pending
                    else AppIcons.Check, null, Modifier.size(16.dp),
                    tint = if (stage.state == TimelineVisualState.CURRENT) T.red else T.green)
                Text(stage.label, style = MaterialTheme.typography.bodySmall,
                    color = if (stage.state == TimelineVisualState.CURRENT) T.red else T.ink)
                stage.timestamp?.let { Text(it.timelineDateTime(),
                    style = MaterialTheme.typography.labelSmall, color = T.muted) }
            } }
    }
}

private fun deliveryName(order: CodOrder) = order.deliveryContactName?.takeIf(String::isNotBlank)
    ?: if (order.assignedDeliveryRole == UserRole.ADMIN) "Admin delivery" else "Delivery partner"
private fun Long.distanceLabel() = if (this < 1000) "$this m" else "%.1f km".format(this / 1000.0)
private fun locationAgeLabel(ageMillis: Long): String = when {
    ageMillis < 15_000 -> "Live · just now"
    ageMillis < 60_000 -> "Updated ${ageMillis.coerceAtLeast(0) / 1_000}s ago"
    else -> "Updated ${(ageMillis.coerceAtLeast(0) / 60_000).coerceAtLeast(1)}m ago"
}

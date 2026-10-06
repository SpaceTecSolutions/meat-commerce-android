package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.DeliveryLocation
import com.spacetecsolutions.meatapp.core.model.DeliveryRoute
import com.spacetecsolutions.meatapp.core.model.DeliveryOtp

@Composable
internal fun CustomerLiveTrackingScreen(
    order: CodOrder,
    riderLocation: DeliveryLocation?,
    route: DeliveryRoute?,
    routeFailed: Boolean,
    routeLoading: Boolean,
    locationFailed: Boolean,
    deliveryOtp: DeliveryOtp?,
    deliveryOtpError: Boolean,
    nowEpochMillis: Long,
    onBack: () -> Unit,
    onContact: () -> Unit,
    onRetryOtp: () -> Unit,
) {
    val stale = riderLocation != null &&
        nowEpochMillis - riderLocation.recordedAtEpochMillis > STALE_LOCATION_MILLIS
    var recenterSignal by remember { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelMaxHeight = maxHeight * .45f
        Column(Modifier.fillMaxSize()) {
            LiveTrackingHeader(order, onBack, onContact)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CustomerDeliveryMap(
                    riderLocation = riderLocation,
                    deliveryRoute = route,
                    destinationLatitude = order.deliveryDestinationLatitude,
                    destinationLongitude = order.deliveryDestinationLongitude,
                    recenterSignal = recenterSignal,
                    modifier = Modifier.fillMaxSize(),
                )
                if (route != null && route.durationSeconds > 0 && !stale) {
                    Surface(Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                        shape = RoundedCornerShape(50), color = T.surface,
                        shadowElevation = 4.dp) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = T.green, shape = RoundedCornerShape(50)) {
                                Spacer(Modifier.size(10.dp))
                            }
                            Text("On the way", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold, color = T.ink)
                            Text("|", color = T.border)
                            Text("Arriving in ~${(route.durationSeconds / 60).coerceAtLeast(1)} min",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold, color = T.red)
                        }
                    }
                }
                TrackingStatePill(
                    riderMissing = riderLocation == null,
                    locationStale = stale,
                    locationFailed = locationFailed,
                    routeLoading = routeLoading && route == null,
                    routeFailed = routeFailed && route == null,
                    modifier = Modifier.align(Alignment.TopCenter)
                        .padding(top = if (route != null && !stale) 62.dp else 12.dp),
                )
                SmallFloatingActionButton(
                    onClick = { recenterSignal++ },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    containerColor = T.surface, contentColor = T.red,
                ) { Icon(AppIcons.LiveTracking, "Recenter map") }
            }
            TrackingDeliveryPanel(order, riderLocation, route, stale, nowEpochMillis,
                deliveryOtp, deliveryOtpError, onRetryOtp, onContact,
                Modifier.fillMaxWidth().heightIn(max = panelMaxHeight))
        }
    }
}

@Composable
private fun LiveTrackingHeader(order: CodOrder, onBack: () -> Unit, onHelp: () -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(AppIcons.Back, "Back", tint = T.ink) }
            Spacer(Modifier.weight(1f))
            Text(order.numberLabel(), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = T.ink)
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(50), color = T.rose,
                border = BorderStroke(1.dp, T.red.copy(alpha = .25f))) {
                Text("LIVE", Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, color = T.red)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onHelp) { Text("Help", color = T.ink) }
        }
        Text("Order is on the way", Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, color = T.ink)
        Text("Your delivery is heading towards the selected address",
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center, color = T.muted)
    }
}

@Composable
private fun TrackingStatePill(
    riderMissing: Boolean,
    locationStale: Boolean,
    locationFailed: Boolean,
    routeLoading: Boolean,
    routeFailed: Boolean,
    modifier: Modifier,
) {
    val message = when {
        locationFailed -> "Live location is temporarily unavailable"
        locationStale -> "Location update delayed"
        riderMissing -> "Preparing live location…"
        routeFailed -> "Unable to load route · Live location is available"
        routeLoading -> "Calculating delivery route…"
        else -> null
    } ?: return
    Surface(modifier, shape = RoundedCornerShape(50), color = T.surface,
        shadowElevation = 4.dp) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (riderMissing || routeLoading) CircularProgressIndicator(Modifier.size(16.dp),
                strokeWidth = 2.dp, color = T.red)
            else Icon(AppIcons.LiveTracking, null, Modifier.size(16.dp), tint = T.red)
            AnimatedContent(message, transitionSpec = {
                fadeIn(tween(AppMotion.STANDARD_MILLIS)) togetherWith
                    fadeOut(tween(AppMotion.QUICK_MILLIS))
            }, label = "tracking-state") { Text(it, style = MaterialTheme.typography.labelSmall,
                color = T.ink) }
        }
    }
}

private const val STALE_LOCATION_MILLIS = 60_000L

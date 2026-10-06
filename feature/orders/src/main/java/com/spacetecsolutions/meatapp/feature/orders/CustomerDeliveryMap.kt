package com.spacetecsolutions.meatapp.feature.orders

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.DeliveryLocation
import com.spacetecsolutions.meatapp.core.model.DeliveryRoute

@Composable
internal fun CustomerDeliveryMap(
    riderLocation: DeliveryLocation?,
    deliveryRoute: DeliveryRoute?,
    destinationLatitude: Double?,
    destinationLongitude: Double?,
    recenterSignal: Int,
    modifier: Modifier,
) {
    val routeColor = MaterialTheme.colorScheme.primary.toArgb()
    val context = LocalContext.current
    val bikeBitmap = remember(context, routeColor) { deliveryBikeMarkerBitmap(context, routeColor) }
    val mapView = remember(context) { MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<GoogleMap?>(null) }
    var riderMarker by remember { mutableStateOf<Marker?>(null) }
    var destinationMarker by remember { mutableStateOf<Marker?>(null) }
    var riderAnimator by remember { mutableStateOf<ValueAnimator?>(null) }
    var routeLine by remember { mutableStateOf<Polyline?>(null) }
    var routeOutline by remember { mutableStateOf<Polyline?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var cameraInitialized by remember { mutableStateOf(false) }
    var fittedBothLocations by remember { mutableStateOf(false) }
    var userMovedCamera by remember { mutableStateOf(false) }

    DisposableEffect(mapView) {
        mapView.onStart(); mapView.onResume()
        onDispose {
            riderAnimator?.cancel()
            mapView.onPause(); mapView.onStop(); mapView.onDestroy()
        }
    }
    Box(modifier) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { ready ->
                        ready.uiSettings.apply {
                            isMapToolbarEnabled = false
                            isMyLocationButtonEnabled = false
                            isCompassEnabled = false
                            isZoomControlsEnabled = false
                            isRotateGesturesEnabled = false
                        }
                        ready.setOnCameraMoveStartedListener { reason ->
                            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                                userMovedCamera = true
                            }
                        }
                        map = ready
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (map == null) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier)
                }
            }
        }
    }

    LaunchedEffect(map, destinationLatitude, destinationLongitude) {
        val ready = map ?: return@LaunchedEffect
        if (destinationLatitude == null || destinationLongitude == null) return@LaunchedEffect
        val destination = LatLng(destinationLatitude, destinationLongitude)
        if (destinationMarker == null) {
            destinationMarker = ready.addMarker(
                MarkerOptions().position(destination).title("Delivery location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)),
            )
        }
    }
    LaunchedEffect(map, riderLocation) {
        val ready = map ?: return@LaunchedEffect
        val rider = riderLocation?.let { LatLng(it.latitude, it.longitude) } ?: return@LaunchedEffect
        val marker = riderMarker
        if (marker == null) {
            riderMarker = ready.addMarker(
                MarkerOptions().position(rider).title("Delivery partner")
                    .icon(BitmapDescriptorFactory.fromBitmap(bikeBitmap)).anchor(.5f, .92f)
                    .rotation(riderLocation.headingDegrees ?: 0f).flat(true),
            )
        } else {
            riderAnimator?.cancel()
            riderAnimator = animateRiderMarker(marker, rider, riderLocation.headingDegrees)
        }
    }
    LaunchedEffect(map, deliveryRoute) {
        val ready = map ?: return@LaunchedEffect
        val decoded = deliveryRoute?.encodedPolyline?.let(::decodeRoutePolyline).orEmpty()
        if (decoded.size < 2) return@LaunchedEffect
        val newOutline = ready.addPolyline(
            PolylineOptions().addAll(decoded).color(0xD9FFFFFF.toInt()).width(14f)
                .jointType(JointType.ROUND),
        )
        val newLine = ready.addPolyline(
            PolylineOptions().addAll(decoded).color(routeColor).width(8f)
                .jointType(JointType.ROUND),
        )
        routeOutline?.remove(); routeLine?.remove()
        routeOutline = newOutline; routeLine = newLine; routePoints = decoded
        if (!userMovedCamera && !fittedBothLocations) {
            fitTrackingCamera(ready, decoded, riderLocation, destinationLatitude, destinationLongitude)
            cameraInitialized = true
            fittedBothLocations = riderLocation != null && destinationLatitude != null &&
                destinationLongitude != null
        }
    }
    LaunchedEffect(map, riderLocation, destinationLatitude, destinationLongitude,
        cameraInitialized, fittedBothLocations) {
        val ready = map ?: return@LaunchedEffect
        if (riderLocation == null && (destinationLatitude == null || destinationLongitude == null)) return@LaunchedEffect
        if (!cameraInitialized || (riderLocation != null && destinationLatitude != null &&
                destinationLongitude != null && !fittedBothLocations && !userMovedCamera)) {
            mapView.post {
                fitTrackingCamera(ready, routePoints, riderLocation,
                    destinationLatitude, destinationLongitude)
                cameraInitialized = true
                fittedBothLocations = riderLocation != null && destinationLatitude != null &&
                    destinationLongitude != null
            }
        }
    }
    LaunchedEffect(map, recenterSignal) {
        if (recenterSignal == 0) return@LaunchedEffect
        val ready = map ?: return@LaunchedEffect
        userMovedCamera = false
        fitTrackingCamera(ready, routePoints, riderLocation, destinationLatitude, destinationLongitude)
        fittedBothLocations = riderLocation != null && destinationLatitude != null &&
            destinationLongitude != null
    }
}

private fun fitTrackingCamera(
    map: GoogleMap,
    route: List<LatLng>,
    rider: DeliveryLocation?,
    destinationLatitude: Double?,
    destinationLongitude: Double?,
) {
    val points = route.ifEmpty {
        listOfNotNull(
            rider?.let { LatLng(it.latitude, it.longitude) },
            if (destinationLatitude != null && destinationLongitude != null) {
                LatLng(destinationLatitude, destinationLongitude)
            } else null,
        )
    }
    if (points.size > 1) {
        val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
        runCatching { map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 92), 450, null) }
    } else points.firstOrNull()?.let {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f), 350, null)
    }
}

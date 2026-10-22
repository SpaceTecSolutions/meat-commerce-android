package com.spacetecsolutions.meatapp.feature.address

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.spacetecsolutions.meatapp.core.designsystem.component.HideAppBottomBar
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerAddressTokens as T
import com.spacetecsolutions.meatapp.core.model.DetectedAddress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddressMapPicker(form: AddressFormState, preview: DetectedAddress?,
    loading: Boolean, error: String?, dismiss: () -> Unit,
    onIdle: (Double, Double) -> Unit, onMove: () -> Unit,
    onPlaceSelected: (DetectedAddress) -> Unit, confirm: () -> Unit,
    useCurrentLocation: () -> Unit) {
    val context = LocalContext.current
    var map by remember { mutableStateOf<GoogleMap?>(null) }
    var mapMoved by remember { mutableStateOf(false) }
    var programmaticCameraMove by remember { mutableStateOf(false) }
    val view = remember { MapView(context).apply { onCreate(null) } }
    DisposableEffect(view) {
        view.onStart(); view.onResume()
        onDispose { view.onPause(); view.onStop(); view.onDestroy() }
    }
    BackHandler(onBack = dismiss)
    HideAppBottomBar()
    Scaffold(containerColor = T.canvas, topBar = {
        TopAppBar(title = {
            AddressLocationSearchBar(Modifier.fillMaxWidth()) { selected ->
                onPlaceSelected(selected)
            }
        }, navigationIcon = { IconButton(dismiss) { Icon(AppIcons.Back, stringResource(R.string.ui_back_b52b36b)) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = T.surface))
    }, bottomBar = {
        Surface(color = T.surface, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ui_selected_delivery_pin_a940f31), Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall, color = T.muted)
                    if (preview != null) Text(stringResource(R.string.ui_location_resolved_8876579),
                        style = MaterialTheme.typography.labelSmall, color = T.success)
                }
                Surface(color = T.canvas, shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, T.border)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Location, null, tint = T.red)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(preview?.area?.ifBlank { preview.city }?.ifBlank { "Delivery location" }
                                ?: if (loading) "Resolving pin..." else "Move map to choose a pin",
                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text(preview?.address ?: stringResource(R.string.ui_search_or_move_the_map_b35168c),
                                style = MaterialTheme.typography.bodySmall, color = T.muted,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = T.red)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
                Button(confirm, Modifier.fillMaxWidth().height(48.dp),
                    enabled = preview != null && !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = T.red),
                    shape = RoundedCornerShape(10.dp)) {
                    Icon(AppIcons.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_confirm_location_proceed_32c8f1d))
                }
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(factory = { view.apply { getMapAsync { ready ->
                map = ready
                ready.uiSettings.isMapToolbarEnabled = false
                ready.uiSettings.isMyLocationButtonEnabled = false
                ready.uiSettings.isZoomControlsEnabled = false
                val initial = if (form.latitude != null && form.longitude != null)
                    LatLng(form.latitude, form.longitude) else LatLng(12.9716, 77.5946)
                ready.moveCamera(CameraUpdateFactory.newLatLngZoom(initial,
                    if (form.latitude == null) 11f else 17f))
                ready.setOnCameraMoveStartedListener {
                    mapMoved = true
                    if (!programmaticCameraMove) onMove()
                }
                ready.setOnCameraIdleListener {
                    if (programmaticCameraMove) {
                        programmaticCameraMove = false
                    } else if (mapMoved || (form.latitude != null && form.longitude != null)) {
                        val point = ready.cameraPosition.target
                        onIdle(point.latitude, point.longitude)
                    }
                }
                if (form.latitude != null && form.longitude != null)
                    onIdle(form.latitude, form.longitude)
            } } }, modifier = Modifier.fillMaxSize())
            Icon(AppIcons.Location, stringResource(R.string.ui_selected_delivery_pin_f82b203),
                Modifier.align(Alignment.Center).padding(bottom = 30.dp).size(48.dp),
                tint = T.red)
            Surface(Modifier.align(Alignment.TopCenter).padding(top = 12.dp), color = T.ink,
                shape = RoundedCornerShape(50)) {
                Text(stringResource(R.string.ui_drag_map_to_adjust_delivery_9db6fee), Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall, color = T.surface)
            }
            SmallFloatingActionButton(useCurrentLocation,
                Modifier.align(Alignment.BottomEnd).padding(end = 12.dp),
                containerColor = T.surface, contentColor = T.red) {
                Icon(AppIcons.LiveTracking, stringResource(R.string.ui_use_current_location_622c745))
            }
        }
    }
    LaunchedEffect(preview?.latitude, preview?.longitude, map) {
        if (preview != null && map != null) {
            val target = map!!.cameraPosition.target
            if (kotlin.math.abs(target.latitude - preview.latitude) > .002 ||
                kotlin.math.abs(target.longitude - preview.longitude) > .002) {
                programmaticCameraMove = true
                map!!.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(preview.latitude, preview.longitude), 17f))
            }
        }
    }
}

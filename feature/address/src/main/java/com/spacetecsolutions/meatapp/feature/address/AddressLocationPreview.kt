package com.spacetecsolutions.meatapp.feature.address

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerAddressTokens as T
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons

@Composable
internal fun AddressLocationPreview(form: AddressFormState, enabled: Boolean, search: () -> Unit, locate: () -> Unit) {
    val context = LocalContext.current
    val view = remember { MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<GoogleMap?>(null) }
    DisposableEffect(view) {
        view.onStart(); view.onResume()
        onDispose { view.onPause(); view.onStop(); view.onDestroy() }
    }
    LaunchedEffect(map, form.latitude, form.longitude) {
        val ready = map ?: return@LaunchedEffect
        ready.clear()
        val point = if (form.latitude != null && form.longitude != null) LatLng(form.latitude, form.longitude) else null
        // Bengaluru is only the browsing area, never a guessed delivery destination.
        ready.moveCamera(CameraUpdateFactory.newLatLngZoom(point ?: LatLng(12.9716, 77.5946), if (point == null) 10f else 17f))
    }
    Box(Modifier.fillMaxWidth().height(238.dp).clip(RoundedCornerShape(16.dp))) {
        AndroidView(factory = { view.apply { getMapAsync { ready ->
            map = ready
            ready.uiSettings.setAllGesturesEnabled(false)
            ready.uiSettings.isMapToolbarEnabled = false
        } } }, modifier = Modifier.fillMaxSize())
        OutlinedButton(search, Modifier.align(Alignment.TopCenter).padding(10.dp).fillMaxWidth(), enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = T.surface, contentColor = T.ink)) {
            Icon(AppIcons.Search, null, tint = T.red); Spacer(Modifier.width(8.dp))
            Text("Search address or landmark...", Modifier.weight(1f), maxLines = 1)
        }
        if (form.latitude != null && form.longitude != null)
            Icon(AppIcons.Location, "Selected delivery location",
                Modifier.align(Alignment.Center).size(34.dp), tint = T.red)
        SmallFloatingActionButton({ if (enabled) locate() }, Modifier.align(Alignment.BottomEnd).padding(12.dp),
            containerColor = T.surface) {
            Icon(AppIcons.LiveTracking, "Use current location", tint = T.red)
        }
    }
}

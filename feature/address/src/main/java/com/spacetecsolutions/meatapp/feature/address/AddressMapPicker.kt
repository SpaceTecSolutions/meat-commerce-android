package com.spacetecsolutions.meatapp.feature.address

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerAddressTokens as T
import com.spacetecsolutions.meatapp.core.model.DetectedAddress
import java.util.ServiceConfigurationError
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddressMapPicker(form: AddressFormState, preview: DetectedAddress?,
    loading: Boolean, error: String?, dismiss: () -> Unit,
    onIdle: (Double, Double) -> Unit, onMove: () -> Unit, confirm: () -> Unit,
    useCurrentLocation: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var map by remember { mutableStateOf<GoogleMap?>(null) }
    var mapMoved by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    val view = remember { MapView(context).apply { onCreate(null) } }
    val autocomplete = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intent = result.data
        if (result.resultCode == Activity.RESULT_OK && intent != null) {
            val prediction = PlaceAutocomplete.getPredictionFromIntent(intent)
            if (prediction != null) scope.launch {
                searching = true; searchError = null
                try {
                    val request = FetchPlaceRequest.builder(prediction.placeId,
                        listOf(Place.Field.LOCATION))
                        .setSessionToken(PlaceAutocomplete.getSessionTokenFromIntent(intent)).build()
                    val point = Places.createClient(context).fetchPlace(request).await().place.location
                    if (point != null) map?.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17f))
                    else searchError = "This result has no map location. Try another."
                } catch (_: ServiceConfigurationError) {
                    searchError = "Search is temporarily unavailable. Move the map or retry."
                } catch (_: Exception) {
                    searchError = "Search is temporarily unavailable. Move the map or retry."
                } finally { searching = false }
            }
        }
    }
    fun launchSearch() {
        runCatching {
            synchronized(Places::class.java) {
                if (!Places.isInitialized()) {
                    val key = context.packageManager.getApplicationInfo(context.packageName,
                        PackageManager.GET_META_DATA).metaData
                        ?.getString("com.google.android.geo.API_KEY").orEmpty()
                    require(key.isNotBlank())
                    Places.initializeWithNewPlacesApiEnabled(context, key)
                }
            }
            autocomplete.launch(PlaceAutocomplete.IntentBuilder().setCountries(listOf("IN")).build(context))
        }.onFailure { searchError = "Google search is not configured. Move the map instead." }
    }
    DisposableEffect(view) {
        view.onStart(); view.onResume()
        onDispose { view.onPause(); view.onStop(); view.onDestroy() }
    }
    androidx.activity.compose.BackHandler(onBack = dismiss)
    com.spacetecsolutions.meatapp.core.designsystem.component.HideAppBottomBar()
    Scaffold(containerColor = T.canvas, topBar = {
        TopAppBar(title = { Column {
            Text("Choose Location", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text("Move pin or search address", style = MaterialTheme.typography.labelSmall,
                color = T.muted)
        } }, navigationIcon = { IconButton(dismiss) { Icon(AppIcons.Back, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = T.surface))
    }, bottomBar = {
        Surface(color = T.surface, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SELECTED DELIVERY PIN", Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall, color = T.muted)
                    if (preview != null) Text("Location resolved",
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
                            Text(preview?.address ?: "Search or move the map to your delivery location.",
                                style = MaterialTheme.typography.bodySmall, color = T.muted,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (loading || searching) LinearProgressIndicator(Modifier.fillMaxWidth(), color = T.red)
                (error ?: searchError)?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
                Button(confirm, Modifier.fillMaxWidth().height(48.dp),
                    enabled = preview != null && !loading && !searching,
                    colors = ButtonDefaults.buttonColors(containerColor = T.red),
                    shape = RoundedCornerShape(10.dp)) {
                    Icon(AppIcons.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Confirm Location & Proceed")
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
                ready.setOnCameraMoveStartedListener { mapMoved = true; onMove() }
                ready.setOnCameraIdleListener {
                    if (mapMoved || (form.latitude != null && form.longitude != null)) {
                        val point = ready.cameraPosition.target
                        onIdle(point.latitude, point.longitude)
                    }
                }
                if (form.latitude != null && form.longitude != null)
                    onIdle(form.latitude, form.longitude)
            } } }, modifier = Modifier.fillMaxSize())
            Icon(AppIcons.Location, "Selected delivery pin",
                Modifier.align(Alignment.Center).padding(bottom = 30.dp).size(48.dp),
                tint = T.red)
            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(::launchSearch, Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = T.surface, contentColor = T.ink),
                    shape = RoundedCornerShape(14.dp), elevation = ButtonDefaults.buttonElevation(3.dp)) {
                    Icon(AppIcons.Search, null, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp))
                    Text("Search address or landmark...", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium, color = T.muted)
                }
                Surface(Modifier.align(Alignment.CenterHorizontally), color = T.ink,
                    shape = RoundedCornerShape(50)) {
                    Text("Drag map to adjust delivery pin", Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall, color = T.surface)
                }
            }
            SmallFloatingActionButton(useCurrentLocation,
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                containerColor = T.surface, contentColor = T.red) {
                Icon(AppIcons.LiveTracking, "Use current location")
            }
        }
    }
    LaunchedEffect(preview?.latitude, preview?.longitude, map) {
        if (preview != null && map != null) {
            val target = map!!.cameraPosition.target
            if (kotlin.math.abs(target.latitude - preview.latitude) > .002 ||
                kotlin.math.abs(target.longitude - preview.longitude) > .002)
                map!!.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(preview.latitude, preview.longitude), 17f))
        }
    }
}

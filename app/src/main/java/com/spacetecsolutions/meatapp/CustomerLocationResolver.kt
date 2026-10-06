package com.spacetecsolutions.meatapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.spacetecsolutions.meatapp.core.model.DetectedAddress
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal suspend fun resolveCustomerLocation(context: Context): DetectedAddress? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) return null
    val location = withTimeoutOrNull(10_000) {
        val cancellation = CancellationTokenSource()
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token).await()
    } ?: return null
    val address = reverseGeocode(context, location.latitude, location.longitude)
    return DetectedAddress(
        address = address?.getAddressLine(0).orEmpty(),
        area = address?.subLocality ?: address?.locality.orEmpty(),
        city = address?.locality ?: address?.subAdminArea.orEmpty(),
        state = address?.adminArea.orEmpty(),
        postalCode = address?.postalCode.orEmpty(),
        latitude = location.latitude,
        longitude = location.longitude,
    )
}

@Suppress("DEPRECATION")
private suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double) =
    withContext(Dispatchers.IO) {
        runCatching { Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)?.firstOrNull() }
            .getOrNull()
    }

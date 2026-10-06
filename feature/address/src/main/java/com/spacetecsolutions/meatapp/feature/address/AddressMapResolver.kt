package com.spacetecsolutions.meatapp.feature.address

import android.content.Context
import android.location.Geocoder
import android.content.pm.PackageManager
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.spacetecsolutions.meatapp.core.model.DetectedAddress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

class AddressMapResolver @Inject constructor(@param:ApplicationContext private val context: Context) {
    sealed interface Lookup {
        data class Found(val location: DetectedAddress) : Lookup
        data object NotFound : Lookup
        data object Unavailable : Lookup
    }

    /** Google Places Text Search (New); one lookup only when Save/Update is pressed. */
    suspend fun verifyManual(form: AddressFormState): Lookup = withContext(Dispatchers.IO) {
        try {
            val key = context.packageManager.getApplicationInfo(context.packageName,
                PackageManager.GET_META_DATA).metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
            if (key.isBlank()) return@withContext Lookup.Unavailable
            synchronized(Places::class.java) {
                if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(context, key)
            }
            val geographicAddress = form.address.trim().replace(
                Regex("^(flat|floor|door|house|unit)\\s*[^,]*,\\s*", RegexOption.IGNORE_CASE), "")
            val query = listOf(geographicAddress, form.city, form.state, form.postalCode, "India")
                .filter(String::isNotBlank).joinToString(", ")
            val request = SearchByTextRequest.builder(query,
                listOf(Place.Field.LOCATION, Place.Field.FORMATTED_ADDRESS,
                    Place.Field.ADDRESS_COMPONENTS)).setRegionCode("IN")
                .setMaxResultCount(1).build()
            val place = Places.createClient(context).searchByText(request).await().places.firstOrNull()
                ?: return@withContext Lookup.NotFound
            val point = place.location ?: return@withContext Lookup.NotFound
            val resolvedPin = place.addressComponents?.asList()?.firstOrNull {
                "postal_code" in it.types
            }?.name
            if (resolvedPin != null && resolvedPin != form.postalCode.trim())
                return@withContext Lookup.NotFound
            if (point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0)
                return@withContext Lookup.NotFound
            Lookup.Found(DetectedAddress(address = place.formattedAddress.orEmpty(), city = form.city,
                state = form.state, postalCode = form.postalCode,
                latitude = point.latitude, longitude = point.longitude))
        } catch (_: Exception) { Lookup.Unavailable }
    }

    @Suppress("DEPRECATION")
    suspend fun resolve(latitude: Double, longitude: Double): DetectedAddress = withContext(Dispatchers.IO) {
        val address = runCatching {
            Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)?.firstOrNull()
        }.getOrNull()
        DetectedAddress(address = address?.getAddressLine(0).orEmpty(),
            area = address?.subLocality.orEmpty(), city = address?.locality.orEmpty(),
            state = address?.adminArea.orEmpty(), postalCode = address?.postalCode.orEmpty(),
            latitude = latitude, longitude = longitude)
    }
}

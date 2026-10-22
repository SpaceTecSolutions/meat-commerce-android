package com.spacetecsolutions.meatapp.feature.address

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.PlacesStatusCodes
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerAddressTokens as T
import com.spacetecsolutions.meatapp.core.model.DetectedAddress
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.ServiceConfigurationError

private const val TAG = "AddressPlaceSearch"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddressLocationSearchBar(
    modifier: Modifier = Modifier,
    onSelected: (DetectedAddress) -> Unit,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var completedQuery by remember { mutableStateOf("") }
    var failure by remember { mutableIntStateOf(0) }
    var retrySequence by remember { mutableIntStateOf(0) }
    var requestSequence by remember { mutableIntStateOf(0) }
    var selectedAddressQuery by remember { mutableStateOf<String?>(null) }
    var dismissedQuery by remember { mutableStateOf<String?>(null) }
    var sessionToken by remember { mutableStateOf(AutocompleteSessionToken.newInstance()) }
    val clientResult = remember(context) { runCatching { createPlacesClient(context) } }
    val client = clientResult.getOrNull()

    LaunchedEffect(clientResult) {
        clientResult.exceptionOrNull()?.let {
            failure = Int.MIN_VALUE
            Log.e(TAG, "Places SDK initialization failed", it)
        }
    }
    LaunchedEffect(query, retrySequence, client) {
        val normalized = query.trim()
        val requestId = ++requestSequence
        if (selectedAddressQuery == normalized) {
            predictions = emptyList()
            loading = false
            failure = 0
            completedQuery = normalized
            return@LaunchedEffect
        }
        dismissedQuery = null
        if (normalized.length < 2) {
            predictions = emptyList()
            loading = false
            failure = 0
            completedQuery = ""
            return@LaunchedEffect
        }
        if (completedQuery != normalized) {
            predictions = emptyList()
            completedQuery = ""
        }
        delay(300)
        loading = true
        failure = 0
        try {
            val placesClient = client ?: throw IllegalStateException("Places client is not initialized")
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(normalized)
                .setCountries(listOf("IN"))
                .setSessionToken(sessionToken)
                .build()
            val response = placesClient.findAutocompletePredictions(request).await()
            if (requestId == requestSequence) {
                predictions = response.autocompletePredictions
                completedQuery = normalized
            }
        } catch (exception: Exception) {
            if (requestId == requestSequence) {
                predictions = emptyList()
                failure = if (client == null) Int.MIN_VALUE else exception.toPlacesStatusCode()
                logPlacesFailure("autocomplete", exception)
            }
        } catch (error: ServiceConfigurationError) {
            if (requestId == requestSequence) {
                predictions = emptyList()
                failure = Int.MIN_VALUE
                Log.e(TAG, "Places SDK provider initialization failed", error)
            }
        } finally {
            if (requestId == requestSequence) loading = false
        }
    }

    fun choose(prediction: AutocompletePrediction) {
        if (selecting) return
        selecting = true
        keyboard?.hide()
        scope.launch {
            try {
                val placesClient = client ?: throw IllegalStateException("Places client is not initialized")
                val request = FetchPlaceRequest.builder(prediction.placeId,
                    listOf(Place.Field.LOCATION, Place.Field.FORMATTED_ADDRESS,
                        Place.Field.ADDRESS_COMPONENTS))
                    .setSessionToken(sessionToken)
                    .build()
                val place = placesClient.fetchPlace(request).await().place
                val point = place.location
                if (point == null) {
                    failure = PlacesStatusCodes.INVALID_REQUEST
                    return@launch
                }
                val components = place.addressComponents?.asList().orEmpty()
                fun component(type: String) = components.firstOrNull { type in it.types }?.name.orEmpty()
                val selected = DetectedAddress(
                    address = place.formattedAddress.orEmpty().ifBlank {
                        prediction.getFullText(null).toString()
                    },
                    area = component("sublocality_level_1").ifBlank { component("neighborhood") },
                    city = component("locality").ifBlank { component("postal_town") },
                    state = component("administrative_area_level_1"),
                    postalCode = component("postal_code"),
                    latitude = point.latitude,
                    longitude = point.longitude,
                )
                query = selected.address
                selectedAddressQuery = selected.address.trim()
                predictions = emptyList()
                completedQuery = selected.address.trim()
                sessionToken = AutocompleteSessionToken.newInstance()
                onSelected(selected)
            } catch (exception: Exception) {
                failure = if (client == null) Int.MIN_VALUE else exception.toPlacesStatusCode()
                logPlacesFailure("place details", exception)
            } catch (error: ServiceConfigurationError) {
                failure = Int.MIN_VALUE
                Log.e(TAG, "Places SDK provider initialization failed while fetching details", error)
            } finally {
                selecting = false
            }
        }
    }

    val expanded = dismissedQuery != query.trim() && selectedAddressQuery != query.trim() &&
        (loading || selecting || failure != 0 ||
            (query.trim().length >= 2 && completedQuery == query.trim()))
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!it) dismissedQuery = query.trim() },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                selectedAddressQuery = null
                dismissedQuery = null
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            leadingIcon = { Icon(AppIcons.Search, null, tint = T.muted) },
            placeholder = { Text(stringResource(R.string.ui_search_address_or_landmark_dcd28a7),
                maxLines = 1, style = MaterialTheme.typography.bodySmall) },
            trailingIcon = {
                if (loading || selecting) CircularProgressIndicator(Modifier.padding(10.dp),
                    color = T.red, strokeWidth = 2.dp)
                else if (query.isNotEmpty()) IconButton(onClick = {
                    query = ""
                    failure = 0
                    predictions = emptyList()
                    completedQuery = ""
                    selectedAddressQuery = null
                    dismissedQuery = null
                    sessionToken = AutocompleteSessionToken.newInstance()
                }) { Icon(AppIcons.Close, stringResource(R.string.ui_clear_search)) }
            },
            shape = RoundedCornerShape(50),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = T.soft, unfocusedContainerColor = T.soft,
            ),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = {
            predictions = emptyList()
            dismissedQuery = query.trim()
        }) {
            when {
                loading || selecting -> DropdownMenuItem(
                    text = { Text(stringResource(R.string.ui_searching_locations)) },
                    onClick = {}, enabled = false,
                    leadingIcon = { CircularProgressIndicator(Modifier.padding(4.dp),
                        color = T.red, strokeWidth = 2.dp) },
                )
                failure != 0 -> DropdownMenuItem(
                    text = { Text(searchErrorText(failure), color = MaterialTheme.colorScheme.error) },
                    onClick = { retrySequence++ },
                    leadingIcon = { Icon(AppIcons.Retry, null, tint = T.red) },
                )
                predictions.isEmpty() -> DropdownMenuItem(
                    text = { Text(stringResource(R.string.ui_no_location_results), color = T.muted) },
                    onClick = {}, enabled = false,
                )
                else -> predictions.forEach { prediction ->
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(prediction.getPrimaryText(null).toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = T.ink, maxLines = 1)
                                Text(prediction.getSecondaryText(null).toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = T.muted, maxLines = 2)
                            }
                        },
                        onClick = { choose(prediction) },
                    )
                }
            }
        }
    }
}

private fun createPlacesClient(context: Context): PlacesClient {
    val key = context.packageManager.getApplicationInfo(context.packageName,
        PackageManager.GET_META_DATA).metaData
        ?.getString("com.google.android.geo.API_KEY").orEmpty()
    require(key.isNotBlank()) { "Maps/Places API key is missing" }
    synchronized(Places::class.java) {
        if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(context, key)
    }
    return Places.createClient(context)
}

private fun Exception.toPlacesStatusCode(): Int = when {
    this is ApiException -> statusCode
    this is IOException -> CommonStatusCodes.NETWORK_ERROR
    else -> Int.MAX_VALUE
}

private fun logPlacesFailure(operation: String, exception: Exception) {
    val code = (exception as? ApiException)?.statusCode
    Log.e(TAG, "$operation failed; status=${code?.let(PlacesStatusCodes::getStatusCodeString) ?: "non-API exception"}; message=${exception.message}", exception)
}

@Composable
private fun searchErrorText(statusCode: Int): String = when {
    statusCode == Int.MIN_VALUE -> stringResource(R.string.address_places_sdk_unavailable)
    statusCode == CommonStatusCodes.NETWORK_ERROR || statusCode == CommonStatusCodes.TIMEOUT ->
        stringResource(R.string.address_places_network_error)
    statusCode == PlacesStatusCodes.REQUEST_DENIED -> stringResource(R.string.address_places_access_denied)
    statusCode == PlacesStatusCodes.OVER_QUERY_LIMIT -> stringResource(R.string.address_places_quota_error)
    else -> stringResource(R.string.address_places_search_error)
}

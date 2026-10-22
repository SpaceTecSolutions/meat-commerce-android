package com.spacetecsolutions.meatapp.feature.address

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerAddressTokens as T
import com.spacetecsolutions.meatapp.core.model.CustomerAddress
import com.spacetecsolutions.meatapp.core.model.DetectedAddress
import kotlinx.coroutines.launch

enum class AddressInitialAction { EDIT, DELETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressRoute(onBack: () -> Unit, onMessage: (AddressMessage) -> Unit,
    onSelect: ((String) -> Unit)? = null, onSelectAddress: ((CustomerAddress) -> Unit)? = null,
    resolveCurrentLocation: suspend () -> DetectedAddress? = { null },
    requestCurrentLocationOnStart: Boolean = false,
    onCurrentLocationRequestConsumed: () -> Unit = {},
    initialSelectedAddressId: String? = null,
    initialAction: AddressInitialAction? = null,
    onInitialActionConsumed: () -> Unit = {},
    viewModel: AddressViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(initialSelectedAddressId) { viewModel.initializeSelection(initialSelectedAddressId) }
    LaunchedEffect(initialAction, initialSelectedAddressId, state.loading) {
        if (!state.loading && initialAction != null && initialSelectedAddressId != null) {
            state.addresses.firstOrNull { it.id == initialSelectedAddressId }?.let { address ->
                when (initialAction) {
                    AddressInitialAction.EDIT -> viewModel.edit(address)
                    AddressInitialAction.DELETE -> viewModel.confirmDelete(address)
                }
            }
            onInitialActionConsumed()
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var showLocationSettings by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    fun detectLocation() = scope.launch {
        resolveCurrentLocation()?.let(viewModel::previewCurrentLocation) ?: viewModel.locationUnavailable()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) detectLocation() else showLocationSettings = true
    }
    val useCurrentLocation: () -> Unit = {
        showMapPicker = true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) detectLocation()
        else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        Unit
    }
    LaunchedEffect(requestCurrentLocationOnStart) {
        if (requestCurrentLocationOnStart) {
            viewModel.add(); useCurrentLocation(); onCurrentLocationRequestConsumed()
        }
    }
    state.message?.let { message ->
        LaunchedEffect(message) { onMessage(message); viewModel.consumeMessage() }
    }
    if (showLocationSettings) AlertDialog(onDismissRequest = { showLocationSettings = false },
        title = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_location_permission_is_off_3f1770f)) },
        text = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_you_can_still_search_or_d88c06b)) },
        confirmButton = { TextButton({
            showLocationSettings = false
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + context.packageName)))
        }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_open_settings_134635e)) } },
        dismissButton = { TextButton({ showLocationSettings = false }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_continue_manually_05acb97)) } })
    state.form?.let { form ->
        if (showMapPicker) AddressMapPicker(form, state.mapPreview, state.mapLoading, state.mapError,
            dismiss = { viewModel.clearMapPreview(); showMapPicker = false },
            onIdle = viewModel::previewMapLocation, onMove = viewModel::clearMapPreview,
            onPlaceSelected = viewModel::previewCurrentLocation,
            confirm = { viewModel.confirmMapLocation(); showMapPicker = false },
            useCurrentLocation = useCurrentLocation)
        else AddressFormDialog(form, state.submitting, state.verification,
            viewModel::updateForm, viewModel::dismissForm, viewModel::save, useCurrentLocation,
            { showMapPicker = true },
            onSearchLocation = { showMapPicker = true },
            onDelete = if (form.editing) ({ state.addresses.firstOrNull { it.id == form.id }?.let {
                address -> viewModel.dismissForm(); viewModel.confirmDelete(address)
            } }) else null)
        return
    }
    state.deleteConfirmation?.let { address ->
        AlertDialog(onDismissRequest = { viewModel.confirmDelete(null) },
            title = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_delete_address_888fcb1)) },
            text = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_this_019dbc8) + address.type.name.lowercase() + " address will be removed.") },
            confirmButton = { Button(viewModel::delete, enabled = !state.submitting,
                colors = ButtonDefaults.buttonColors(containerColor = T.red)) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_delete_f6fdbe4)) } },
            dismissButton = { TextButton({ viewModel.confirmDelete(null) }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_cancel_77dfd21)) } })
    }
    val selectionMode = onSelect != null || onSelectAddress != null
    Scaffold(containerColor = T.canvas, topBar = {
        TopAppBar(title = { Text(if (selectionMode) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_select_delivery_address_af45abe) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_my_addresses_b0239f5),
            fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onBack) { Icon(AppIcons.Back, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_back_b52b36b)) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = T.surface))
    }, bottomBar = {
        Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
            Button(viewModel::add, Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = T.red)) {
                Icon(AppIcons.Add, null); Spacer(Modifier.width(8.dp)); Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_add_new_address_3e89e38))
            }
        }
    }) { padding ->
        when {
            state.loading -> ContentStateView(ContentState.Loading, Modifier.padding(padding))
            state.error != null -> ContentStateView(ContentState.Error(description = state.error),
                Modifier.padding(padding), onAction = viewModel::refresh)
            state.addresses.isEmpty() -> ContentStateView(ContentState.Empty(
                title = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_no_saved_addresses_ec37dcd), description = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.address.R.string.ui_add_an_address_for_delivery_947e319)),
                Modifier.padding(padding), onAction = viewModel::add)
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)
                .wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = AppDimensions.contentMaxWidth),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text(state.addresses.size.toString() + " saved " +
                        if (state.addresses.size == 1) "location" else "locations",
                        style = MaterialTheme.typography.bodyMedium, color = T.muted)
                }
                items(state.addresses, key = CustomerAddress::id) { address ->
                    SwipeDeleteContainer(!state.submitting, { viewModel.confirmDelete(address) }) {
                        SelectableAddressCard(address, state.selectedAddressId == address.id,
                            state.submitting, selectionMode,
                            { viewModel.select(address.id); onSelect?.invoke(address.id)
                                onSelectAddress?.invoke(address) },
                            { viewModel.edit(address) }, { viewModel.confirmDelete(address) },
                            { viewModel.setDefault(address) })
                    }
                }
            }
        }
    }
}

package com.spacetecsolutions.meatapp.feature.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerAddressRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val repository: CustomerAddressRepository,
    private val validator: AddressValidator,
    private val mapResolver: AddressMapResolver,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AddressUiState())
    val state = mutableState.asStateFlow()
    private var mapResolveJob: Job? = null
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.getAddresses()) {
            is AppResult.Success -> mutableState.update { current -> current.copy(
                loading = false, addresses = result.value,
                selectedAddressId = current.selectedAddressId?.takeIf { id -> result.value.any { it.id == id } }
                    ?: result.value.firstOrNull { it.isDefault }?.id ?: result.value.firstOrNull()?.id,
            ) }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, error = result.error.displayMessage())
            }
        }
    }

    fun add() = mutableState.update {
        it.copy(form = AddressFormState(makeDefault = it.addresses.isEmpty()),
            verification = AddressVerification.IDLE, mapPreview = null, mapError = null)
    }

    fun edit(address: CustomerAddress) = mutableState.update {
        it.copy(form = AddressFormState(
            id = address.id, type = address.type, name = address.name, mobile = address.mobile,
            address = address.address, landmark = address.landmark, city = address.city,
            state = address.state, postalCode = address.postalCode,
            makeDefault = address.isDefault, expectedRevision = address.revision,
            latitude = address.latitude, longitude = address.longitude,
            verifiedLocation = address.latitude != null && address.longitude != null,
        ))
    }

    fun updateForm(transform: (AddressFormState) -> AddressFormState) = mutableState.update { state ->
        state.form?.let { previous ->
            val changed = transform(previous)
            val locationChanged = changed.geographicKey() != previous.geographicKey()
            state.copy(form = changed.copy(errors = emptyMap(),
                latitude = if (locationChanged) null else changed.latitude,
                longitude = if (locationChanged) null else changed.longitude,
                verifiedLocation = !locationChanged && previous.verifiedLocation),
                verification = if (locationChanged) AddressVerification.IDLE else state.verification)
        } ?: state
    }

    fun dismissForm() = mutableState.update { it.copy(form = null, mapPreview = null) }
    fun select(addressId: String) = mutableState.update { it.copy(selectedAddressId = addressId) }
    fun initializeSelection(addressId: String?) {
        if (!addressId.isNullOrBlank()) mutableState.update { it.copy(selectedAddressId = addressId) }
    }
    fun applyDetectedAddress(value: DetectedAddress) = mutableState.update { state ->
        val form = state.form ?: AddressFormState(makeDefault = state.addresses.isEmpty())
        val houseDetails = form.address.substringBefore(',').trim().takeIf {
            it.matches(Regex("^(flat|floor|door|house|unit)\\b.*", RegexOption.IGNORE_CASE))
        }
        val resolvedAddress = value.address.ifBlank { value.area }
        state.copy(form = form.copy(
            address = listOfNotNull(houseDetails, resolvedAddress.takeIf(String::isNotBlank))
                .joinToString(", "), city = value.city,
            state = value.state, postalCode = value.postalCode,
            latitude = value.latitude, longitude = value.longitude, verifiedLocation = true,
            errors = emptyMap(),
        ), message = AddressMessage("Location detected. Review the details before saving.", true))
    }
    fun locationUnavailable() {
        clearMapPreview()
        mutableState.update { it.copy(mapError = "Unable to detect location. Search or move the map.",
            message = AddressMessage("Unable to detect location. Search or select a point on the map.", false)) }
    }
    fun previewMapLocation(latitude: Double, longitude: Double) {
        mapResolveJob?.cancel()
        mapResolveJob = viewModelScope.launch {
        mutableState.update { it.copy(mapLoading = true, mapError = null, mapPreview = null) }
        val value = mapResolver.resolve(latitude, longitude)
        mutableState.update { it.copy(mapLoading = false,
            mapPreview = value.takeIf { found -> found.address.isNotBlank() },
            mapError = if (value.address.isBlank()) "Unable to resolve this pin. Try moving the map." else null) }
        }
    }
    fun previewCurrentLocation(value: DetectedAddress) {
        mapResolveJob?.cancel()
        mutableState.update { it.copy(mapLoading = false,
            mapPreview = value.takeIf { found -> found.address.isNotBlank() },
            mapError = if (value.address.isBlank()) "Unable to resolve your location yet." else null)
        }
    }
    fun clearMapPreview() {
        mapResolveJob?.cancel()
        mutableState.update { it.copy(mapPreview = null, mapLoading = false, mapError = null) }
    }
    fun confirmMapLocation() {
        val value = state.value.mapPreview ?: return
        applyDetectedAddress(value)
        clearMapPreview()
    }
    fun confirmDelete(address: CustomerAddress?) = mutableState.update { it.copy(deleteConfirmation = address) }
    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    fun save() = viewModelScope.launch {
        val form = state.value.form ?: return@launch
        val errors = validator.validate(form)
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(form = form.copy(errors = errors)) }
            return@launch
        }
        if (state.value.submitting) return@launch
        mutableState.update { it.copy(submitting = true) }
        var verified = form
        if (!form.verifiedLocation || !form.hasValidCoordinates()) {
            mutableState.update { it.copy(verification = AddressVerification.VALIDATING) }
            when (val lookup = mapResolver.verifyManual(form)) {
                is AddressMapResolver.Lookup.Found -> {
                    verified = form.copy(latitude = lookup.location.latitude,
                        longitude = lookup.location.longitude, verifiedLocation = true)
                    mutableState.update { it.copy(form = verified, verification = AddressVerification.VALID) }
                }
                AddressMapResolver.Lookup.NotFound -> {
                    mutableState.update { it.copy(submitting = false,
                        verification = AddressVerification.NOT_FOUND,
                        form = form.copy(errors = mapOf("address" to
                            "We couldn't find this address on Google Maps. Check it or choose on the map."))) }
                    return@launch
                }
                AddressMapResolver.Lookup.Unavailable -> {
                    mutableState.update { it.copy(submitting = false,
                        verification = AddressVerification.TEMPORARY_ERROR,
                        form = form.copy(errors = mapOf("address" to
                            "Unable to verify this address right now. Check your connection and retry."))) }
                    return@launch
                }
            }
        }
        val input = verified.toInput(validator.normalizedMobile(verified.mobile))
        val result = if (form.editing) repository.update(input) else repository.create(input)
        handleMutation(result, if (form.editing) "Address updated" else "Address added")
    }

    fun delete() = viewModelScope.launch {
        val address = state.value.deleteConfirmation ?: return@launch
        mutableState.update { it.copy(submitting = true) }
        handleMutation(repository.delete(address.id, address.revision), "Address deleted")
    }

    fun setDefault(address: CustomerAddress) = viewModelScope.launch {
        if (address.isDefault) return@launch
        mutableState.update { it.copy(submitting = true) }
        handleMutation(repository.setDefault(address.id, address.revision), "Default address updated")
    }

    private fun handleMutation(result: AppResult<List<CustomerAddress>>, success: String) {
        when (result) {
            is AppResult.Success -> mutableState.update {
                it.copy(submitting = false, addresses = result.value, form = null,
                    deleteConfirmation = null, message = AddressMessage(success, true))
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(submitting = false, message = AddressMessage(result.error.displayMessage(), false))
            }
        }
    }
}

private fun AddressFormState.toInput(mobile: String) = CustomerAddressInput(
    id, type, name.trim(), mobile, address.trim(), landmark.trim(), city.trim(), state.trim(),
    postalCode.trim(), makeDefault, expectedRevision, latitude, longitude,
)

/** Apartment/unit detail does not move the geographic pin; area/city/PIN changes do. */
private fun AddressFormState.geographicKey() = listOf(
    address.trim().replace(Regex("^(flat|floor|door|house|unit)\\s*[^,]*,\\s*",
        RegexOption.IGNORE_CASE), "").lowercase(), city.trim().lowercase(),
    state.trim().lowercase(), postalCode.trim(),
).joinToString("|")

private fun AddressFormState.hasValidCoordinates() = latitude?.isFinite() == true &&
    longitude?.isFinite() == true && latitude in -90.0..90.0 && longitude in -180.0..180.0

private fun AppError.displayMessage() = when (this) {
    AppError.Network, AppError.Offline -> "You're offline. Check your connection and try again."
    AppError.NotFound -> "This address no longer exists"
    is AppError.Validation -> message
    else -> "We couldn't update the address. Please try again."
}

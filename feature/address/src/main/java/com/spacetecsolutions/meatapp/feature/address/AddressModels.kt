package com.spacetecsolutions.meatapp.feature.address

import com.spacetecsolutions.meatapp.core.model.*

data class AddressFormState(
    val id: String? = null,
    val type: AddressType = AddressType.HOME,
    val name: String = "",
    val mobile: String = "",
    val address: String = "",
    val landmark: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val makeDefault: Boolean = false,
    val expectedRevision: Long? = null,
    val errors: Map<String, String> = emptyMap(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val verifiedLocation: Boolean = false,
) { val editing: Boolean get() = id != null }

enum class AddressVerification { IDLE, VALIDATING, VALID, NOT_FOUND, TEMPORARY_ERROR }

data class AddressUiState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val addresses: List<CustomerAddress> = emptyList(),
    val form: AddressFormState? = null,
    val deleteConfirmation: CustomerAddress? = null,
    val error: String? = null,
    val message: AddressMessage? = null,
    val selectedAddressId: String? = null,
    val verification: AddressVerification = AddressVerification.IDLE,
    val mapPreview: DetectedAddress? = null,
    val mapLoading: Boolean = false,
    val mapError: String? = null,
)

data class AddressMessage(val text: String, val success: Boolean)

package com.spacetecsolutions.meatapp.core.model

enum class AddressType { HOME, WORK, OTHER }

data class CustomerAddress(
    val id: String,
    val type: AddressType,
    val name: String,
    val mobile: String,
    val address: String,
    val landmark: String = "",
    val city: String,
    val state: String,
    val postalCode: String,
    val isDefault: Boolean = false,
    val revision: Long = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class CustomerAddressInput(
    val id: String? = null,
    val type: AddressType,
    val name: String,
    val mobile: String,
    val address: String,
    val landmark: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val makeDefault: Boolean,
    val expectedRevision: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class DetectedAddress(
    val address: String = "",
    val area: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val latitude: Double,
    val longitude: Double,
)

package com.spacetecsolutions.meatapp.feature.address

import com.spacetecsolutions.meatapp.core.model.PhoneNumberConfig
import javax.inject.Inject

class AddressValidator @Inject constructor(private val phoneConfig: PhoneNumberConfig) {
    fun validate(form: AddressFormState): Map<String, String> = buildMap {
        required("name", form.name, 2, 80, "Name")?.let { put("name", it) }
        mobile(form.mobile)?.let { put("mobile", it) }
        required("address", form.address, 5, 250, "Address")?.let { put("address", it) }
        if (form.landmark.length > 100) put("landmark", "Landmark is too long")
        required("city", form.city, 2, 80, "City")?.let { put("city", it) }
        required("state", form.state, 2, 80, "State")?.let { put("state", it) }
        if (!form.postalCode.matches(Regex("[1-9][0-9]{5}"))) put("postalCode", "Enter a valid postal code")
    }

    fun normalizedMobile(value: String): String {
        val compact = value.filterNot { it.isWhitespace() || it in "-()" }
        val national = when {
            compact.startsWith(phoneConfig.countryCode) -> compact.removePrefix(phoneConfig.countryCode)
            compact.startsWith("0") -> compact.drop(1)
            else -> compact
        }
        return phoneConfig.countryCode + national
    }

    private fun mobile(value: String): String? {
        val normalized = normalizedMobile(value).removePrefix(phoneConfig.countryCode)
        return if (normalized.length == phoneConfig.nationalNumberLength && normalized.all(Char::isDigit)) null
        else "Enter a valid mobile number"
    }

    private fun required(key: String, value: String, min: Int, max: Int, label: String): String? = when {
        value.isBlank() -> "$label is required"
        value.trim().length < min -> "Enter a valid ${label.lowercase()}"
        value.trim().length > max -> "$label is too long"
        else -> null
    }
}

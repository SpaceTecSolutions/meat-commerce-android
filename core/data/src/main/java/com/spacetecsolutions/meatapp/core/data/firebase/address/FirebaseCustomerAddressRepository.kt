package com.spacetecsolutions.meatapp.core.data.firebase.address

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerAddressRepository
import com.spacetecsolutions.meatapp.core.model.*
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

internal class FirebaseCustomerAddressRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : CustomerAddressRepository {
    override suspend fun getAddresses() = call("customerGetAddresses", emptyMap())
    override suspend fun create(input: CustomerAddressInput) =
        call("customerCreateAddress", input.payload(false))
    override suspend fun update(input: CustomerAddressInput) =
        call("customerUpdateAddress", input.payload(true))
    override suspend fun delete(addressId: String, expectedRevision: Long) = call(
        "customerDeleteAddress", mapOf("addressId" to addressId, "expectedRevision" to expectedRevision),
    )
    override suspend fun setDefault(addressId: String, expectedRevision: Long) = call(
        "customerSetDefaultAddress",
        mapOf("addressId" to addressId, "expectedRevision" to expectedRevision),
    )

    private suspend fun call(name: String, payload: Map<String, Any>): AppResult<List<CustomerAddress>> = try {
        val response = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid address response")
        AppResult.Success(response.addresses())
    } catch (error: Exception) {
        AppResult.Failure(error.toAddressError())
    }
}

private fun CustomerAddressInput.payload(includeId: Boolean) = buildMap<String, Any> {
    if (includeId) put("addressId", requireNotNull(id))
    put("type", type.name); put("name", name); put("mobile", mobile); put("address", address)
    put("landmark", landmark); put("city", city); put("state", state); put("postalCode", postalCode)
    put("makeDefault", makeDefault)
    expectedRevision?.let { put("expectedRevision", it) }
    latitude?.let { put("latitude", it) }; longitude?.let { put("longitude", it) }
}

private fun Map<*, *>.addresses() = (this["addresses"] as? List<*>).orEmpty().mapNotNull {
    (it as? Map<*, *>)?.toAddress()
}
private fun Map<*, *>.toAddress() = CustomerAddress(
    id = string("id"), type = runCatching { AddressType.valueOf(string("type")) }
        .getOrDefault(AddressType.OTHER),
    name = string("name"), mobile = string("mobile"), address = string("address"),
    landmark = this["landmark"] as? String ?: "", city = string("city"), state = string("state"),
    postalCode = string("postalCode"), isDefault = this["isDefault"] as? Boolean ?: false,
    revision = (this["revision"] as? Number)?.toLong() ?: 0,
    latitude = (this["latitude"] as? Number)?.toDouble(),
    longitude = (this["longitude"] as? Number)?.toDouble(),
)
private fun Map<*, *>.string(key: String) = this[key] as? String ?: error("Missing $key")

private fun Throwable.toAddressError(): AppError {
    val exception = this as? FirebaseFunctionsException
    return when (exception?.code) {
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        FirebaseFunctionsException.Code.NOT_FOUND -> AppError.NotFound
        FirebaseFunctionsException.Code.INVALID_ARGUMENT -> AppError.Validation(message = "Check the address details")
        FirebaseFunctionsException.Code.ABORTED -> AppError.Validation(message = "The address changed. Refresh and try again")
        FirebaseFunctionsException.Code.UNAVAILABLE,
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
        FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

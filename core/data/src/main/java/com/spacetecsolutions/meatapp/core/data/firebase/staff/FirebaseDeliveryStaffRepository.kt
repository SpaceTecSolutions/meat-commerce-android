package com.spacetecsolutions.meatapp.core.data.firebase.staff

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.DeliveryStaffRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseDeliveryStaffRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : DeliveryStaffRepository {
    override suspend fun getStaff() = call("adminListDeliveryStaff", emptyMap()) { response ->
        (response["staff"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.toStaff() }
    }

    override suspend fun create(request: CreateDeliveryStaffRequest) = call(
        "adminCreateDeliveryStaff",
        mapOf("displayName" to request.displayName, "mobileNumber" to request.mobileNumber,
            "password" to request.password), Map<*, *>::staffRequired,
    )

    override suspend fun update(request: UpdateDeliveryStaffRequest) = call(
        "adminUpdateDeliveryStaff",
        mapOf("userId" to request.userId, "displayName" to request.displayName,
            "mobileNumber" to request.mobileNumber), Map<*, *>::staffRequired,
    )

    override suspend fun setActive(userId: String, active: Boolean) = call(
        "adminSetDeliveryStaffActive", mapOf("userId" to userId, "active" to active),
        Map<*, *>::staffRequired,
    )

    private suspend fun <T> call(
        name: String, payload: Map<String, Any>, transform: (Map<*, *>) -> T,
    ): AppResult<T> = try {
        val response = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid delivery staff response")
        AppResult.Success(transform(response))
    } catch (error: Exception) { AppResult.Failure(error.toStaffError()) }
}

private fun Map<*, *>.staffRequired() = (this["staff"] as? Map<*, *> ?: this).toStaff()
    ?: error("Invalid delivery user")
private fun Map<*, *>.toStaff(): User? {
    if (this["role"] != UserRole.DELIVERY.name) return null
    return User(
        id = this["id"] as? String ?: return null,
        displayName = this["displayName"] as? String ?: return null,
        mobileNumber = this["mobileNumber"] as? String ?: return null,
        role = UserRole.DELIVERY, shopId = this["shopId"] as? String,
        active = this["active"] as? Boolean ?: false,
        createdAtEpochMillis = (this["createdAtEpochMillis"] as? Number)?.toLong() ?: 0,
        updatedAtEpochMillis = (this["updatedAtEpochMillis"] as? Number)?.toLong() ?: 0,
    )
}

private fun Throwable.toStaffError(): AppError {
    val exception = this as? FirebaseFunctionsException
    val reason = (exception?.details as? Map<*, *>)?.get("reason") as? String
    return when {
        reason == "FEATURE_DISABLED" -> AppError.Forbidden
        reason == "DUPLICATE_MOBILE" -> AppError.DuplicateAccount
        exception?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        exception?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        exception?.code == FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
            AppError.Validation(message = "Check the delivery user details")
        exception?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

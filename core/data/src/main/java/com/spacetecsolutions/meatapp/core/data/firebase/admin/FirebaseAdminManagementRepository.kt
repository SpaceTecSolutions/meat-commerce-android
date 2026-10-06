package com.spacetecsolutions.meatapp.core.data.firebase.admin

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.AdminManagementRepository
import com.spacetecsolutions.meatapp.core.model.CreateAdminRequest
import com.spacetecsolutions.meatapp.core.model.UpdateAdminRequest
import com.spacetecsolutions.meatapp.core.model.User
import com.spacetecsolutions.meatapp.core.model.UserRole
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseAdminManagementRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : AdminManagementRepository {
    override suspend fun getAdmins(): AppResult<List<User>> = call("superAdminListAdmins", emptyMap()) {
        val records = it["admins"] as? List<*> ?: emptyList<Any>()
        records.mapNotNull { record -> (record as? Map<*, *>)?.toAdmin() }
    }

    override suspend fun createAdmin(request: CreateAdminRequest): AppResult<User> = call(
        "superAdminCreateAdmin",
        mapOf(
            "displayName" to request.displayName,
            "mobileNumber" to request.mobileNumber,
            "password" to request.password,
        ),
        Map<*, *>::toAdminRequired,
    )

    override suspend fun updateAdmin(request: UpdateAdminRequest): AppResult<User> = call(
        "superAdminUpdateAdmin",
        mapOf(
            "userId" to request.userId,
            "displayName" to request.displayName,
            "mobileNumber" to request.mobileNumber,
        ),
        Map<*, *>::toAdminRequired,
    )

    override suspend fun setAdminActive(userId: String, active: Boolean): AppResult<User> = call(
        "superAdminSetAdminActive",
        mapOf("userId" to userId, "active" to active),
        Map<*, *>::toAdminRequired,
    )

    private suspend fun <T> call(
        name: String,
        payload: Map<String, Any>,
        transform: (Map<*, *>) -> T,
    ): AppResult<T> = try {
        val response = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: return AppResult.Failure(AppError.Unknown())
        AppResult.Success(transform(response))
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }
}

private fun Map<*, *>.toAdminRequired(): User =
    ((this["admin"] as? Map<*, *>) ?: this).toAdmin() ?: error("Invalid ADMIN response")

private fun Map<*, *>.toAdmin(): User? {
    val id = this["id"] as? String ?: return null
    val role = this["role"] as? String
    if (role != UserRole.ADMIN.name) return null
    return User(
        id = id,
        displayName = this["displayName"] as? String ?: return null,
        mobileNumber = this["mobileNumber"] as? String ?: return null,
        role = UserRole.ADMIN,
        shopId = this["shopId"] as? String,
        active = this["active"] as? Boolean ?: false,
        createdAtEpochMillis = (this["createdAtEpochMillis"] as? Number)?.toLong() ?: 0L,
        updatedAtEpochMillis = (this["updatedAtEpochMillis"] as? Number)?.toLong() ?: 0L,
    )
}

private fun Throwable.toAppError(): AppError {
    val exception = this as? FirebaseFunctionsException
    val reason = (exception?.details as? Map<*, *>)?.get("reason") as? String
    return when {
        reason == "DUPLICATE_MOBILE" -> AppError.DuplicateAccount
        reason == "ADMIN_DISABLED" -> AppError.DisabledUser
        exception?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        exception?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        exception?.code == FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
            AppError.Validation(message = "Check the entered Admin details")
        exception?.code == FirebaseFunctionsException.Code.ALREADY_EXISTS -> AppError.DuplicateAccount
        exception?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

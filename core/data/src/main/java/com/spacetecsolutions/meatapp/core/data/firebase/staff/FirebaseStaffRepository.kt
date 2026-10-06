package com.spacetecsolutions.meatapp.core.data.firebase.staff

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.*
import com.spacetecsolutions.meatapp.core.model.*
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

internal class FirebaseStaffRepository @Inject constructor(private val functions: FirebaseFunctions) : StaffRepository {
    override suspend fun getStaff() = call("adminListStaff", emptyMap()) { response ->
        (response["staff"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.staff() }
    }
    override suspend fun saveStaff(input: StaffInput) = call(if (input.userId == null) "adminCreateStaff" else "adminUpdateStaff",
        buildMap { input.userId?.let { put("userId", it) }; put("displayName", input.displayName)
            put("mobileNumber", input.mobileNumber); input.email?.let { put("email", it) }; put("active", input.active)
            put("permissions", input.permissions.map(StaffPermission::name)) }) { it.staffRequired() }
    private suspend fun <T> call(name: String, payload: Map<String, Any>, transform: (Map<*, *>) -> T): AppResult<T> = try {
        val response = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *> ?: error("Invalid staff response")
        AppResult.Success(transform(response))
    } catch (error: Exception) { val e = error as? FirebaseFunctionsException; AppResult.Failure(when (e?.code) {
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        FirebaseFunctionsException.Code.ALREADY_EXISTS -> AppError.DuplicateAccount
        FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Validation(message = e?.message ?: "Unable to update staff")
    }) }
}
private fun Map<*, *>.staffRequired() = (this["staff"] as? Map<*, *> ?: this).staff() ?: error("Invalid staff")
private fun Map<*, *>.staff(): User? { if (this["role"] != "STAFF") return null
    return User(id = this["id"] as? String ?: return null, mobileNumber = this["mobileNumber"] as? String ?: return null,
        displayName = this["displayName"] as? String ?: return null, role = UserRole.STAFF, shopId = this["shopId"] as? String,
        email = this["email"] as? String, active = this["active"] as? Boolean ?: false,
        permissions = (this["permissions"] as? List<*>)?.mapNotNull { raw -> (raw as? String)?.let { name ->
            StaffPermission.entries.firstOrNull { it.name == name } } }?.toSet().orEmpty())
}

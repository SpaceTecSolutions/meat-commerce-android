package com.spacetecsolutions.meatapp.core.data.firebase.staff

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.*
import com.spacetecsolutions.meatapp.core.model.*
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

internal class FirebaseStaffRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
) : StaffRepository {
    override suspend fun getStaff() = call("adminListStaff", emptyMap()) { response ->
        (response["staff"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.staff() }
    }
    override suspend fun saveStaff(input: StaffInput) = call(if (input.userId == null) "adminCreateStaff" else "adminUpdateStaff",
        buildMap { input.userId?.let { put("userId", it) }; put("displayName", input.displayName)
            put("mobileNumber", input.mobileNumber); input.email?.let { put("email", it) }; put("active", input.active)
            put("permissions", input.permissions.map(StaffPermission::name)) }, refreshAuth = true) { it.staffRequired() }
    private suspend fun <T> call(name: String, payload: Map<String, Any>, refreshAuth: Boolean = false,
        transform: (Map<*, *>) -> T): AppResult<T> = try {
        if (refreshAuth) {
            val user = auth.currentUser ?: return AppResult.Failure(AppError.Unauthorized)
            user.getIdToken(true).await()
        }
        val response = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *> ?: error("Invalid staff response")
        AppResult.Success(transform(response))
    } catch (error: Exception) {
        val e = error as? FirebaseFunctionsException
        Log.e("FirebaseStaffRepository", "$name failed (${e?.code ?: error.javaClass.simpleName}): ${e?.message ?: error.message}")
        AppResult.Failure(when {
            error is FirebaseAuthException -> AppError.Unauthorized
            e?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
            e?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
            e?.code == FirebaseFunctionsException.Code.ALREADY_EXISTS -> AppError.DuplicateAccount
            e?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
            else -> AppError.Validation(message = e?.message ?: "Unable to update staff")
        })
    }
}
private fun Map<*, *>.staffRequired() = (this["staff"] as? Map<*, *> ?: this).staff() ?: error("Invalid staff")
private fun Map<*, *>.staff(): User? { if (this["role"] != "STAFF") return null
    return User(id = this["id"] as? String ?: return null, mobileNumber = this["mobileNumber"] as? String ?: return null,
        displayName = this["displayName"] as? String ?: return null, role = UserRole.STAFF, shopId = this["shopId"] as? String,
        email = this["email"] as? String, active = this["active"] as? Boolean ?: false,
        permissions = (this["permissions"] as? List<*>)?.mapNotNull { raw -> (raw as? String)?.let { name ->
            StaffPermission.entries.firstOrNull { it.name == name } } }?.toSet().orEmpty())
}

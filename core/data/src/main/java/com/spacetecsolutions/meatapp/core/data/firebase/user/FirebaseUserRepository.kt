package com.spacetecsolutions.meatapp.core.data.firebase.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.UserRepository
import com.spacetecsolutions.meatapp.core.model.User
import com.spacetecsolutions.meatapp.core.model.UserRole
import com.spacetecsolutions.meatapp.core.model.StaffPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseUserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : UserRepository {
    override fun observeAuthenticatedUser(): Flow<AppResult<User?>> = callbackFlow {
        var profileRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            profileRegistration?.remove()
            val userId = firebaseAuth.currentUser?.uid
            if (userId == null) {
                trySend(AppResult.Success(null))
            } else {
                profileRegistration = userDocument(userId).addSnapshotListener { snapshot, error ->
                    val result = when {
                        error != null -> AppResult.Failure(AppError.Network)
                        snapshot == null || !snapshot.exists() -> AppResult.Failure(AppError.NotFound)
                        else -> snapshot.toUser()?.let { AppResult.Success(it) }
                            ?: AppResult.Failure(AppError.Unknown())
                    }
                    trySend(result)
                }
            }
        }
        auth.addAuthStateListener(authListener)
        awaitClose {
            profileRegistration?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    override suspend fun getUser(userId: String): AppResult<User> = try {
        val snapshot = userDocument(userId).get().await()
        snapshot.toUser()?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound)
    } catch (error: Exception) {
        AppResult.Failure(AppError.Network)
    }

    private fun userDocument(userId: String) = firestore.collection("users").document(userId)
}

private fun DocumentSnapshot.toUser(): User? {
    val role = getString("role")?.let { stored -> UserRole.entries.firstOrNull { it.name == stored } }
        ?: return null
    return User(
        id = id,
        mobileNumber = getString("mobileNumber").orEmpty(),
        displayName = getString("displayName").orEmpty(),
        firstName = getString("firstName") ?: getString("displayName").orEmpty().substringBefore(' '),
        lastName = getString("lastName") ?: getString("displayName").orEmpty().substringAfter(' ', ""),
        role = role,
        shopId = getString("shopId"),
        email = getString("email"),
        permissions = (get("permissions") as? List<*>)?.mapNotNull { raw ->
            (raw as? String)?.let { name -> StaffPermission.entries.firstOrNull { it.name == name } }
        }?.toSet().orEmpty(),
        active = getBoolean("active") ?: false,
        createdAtEpochMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        updatedAtEpochMillis = getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
    )
}

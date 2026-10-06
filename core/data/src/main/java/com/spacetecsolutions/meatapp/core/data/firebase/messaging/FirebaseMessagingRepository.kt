package com.spacetecsolutions.meatapp.core.data.firebase.messaging

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.MessagingRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

internal class FirebaseMessagingRepository @Inject constructor(
    private val messaging: FirebaseMessaging,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : MessagingRepository {
    override fun observeUnreadCount(): Flow<AppResult<Int>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(AppResult.Failure(AppError.Unauthorized)); close(); return@callbackFlow }
        val listener = firestore.collection("users").document(uid).collection("notifications")
            .whereEqualTo("readAt", null).addSnapshotListener { snapshot, error ->
                if (error != null) trySend(AppResult.Failure(error.messageError()))
                else trySend(AppResult.Success(snapshot?.size() ?: 0))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getRegistrationToken(): AppResult<String> = result { messaging.token.await() }

    override suspend fun registerDeviceToken(token: String): AppResult<Unit> = result {
        require(token.isNotBlank())
        call("registerNotificationDevice", mapOf("token" to token, "platform" to "ANDROID")); Unit
    }

    override suspend fun deleteRegistrationToken(): AppResult<Unit> = result {
        val token = runCatching { messaging.token.await() }.getOrNull()
        if (!token.isNullOrBlank()) call("unregisterNotificationDevice", mapOf("token" to token))
        messaging.deleteToken().await(); Unit
    }

    override suspend fun getNotificationHistory(limit: Int): AppResult<List<AppNotification>> = result {
        val data = call("getNotificationHistory", mapOf("limit" to limit.coerceIn(1, 100)))
        (data["notifications"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.notification() }
    }

    override suspend fun markNotificationRead(notificationId: String): AppResult<Unit> = result {
        call("markNotificationRead", mapOf("notificationId" to notificationId)); Unit
    }

    override suspend fun markAllNotificationsRead(): AppResult<Unit> = result {
        call("markAllNotificationsRead", emptyMap()); Unit
    }

    override suspend fun clearAllNotifications(): AppResult<Unit> = result {
        call("clearAllNotifications", emptyMap()); Unit
    }

    override fun observeNotificationHistory(limit: Int): Flow<AppResult<List<AppNotification>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(AppResult.Failure(AppError.Unauthorized)); close(); return@callbackFlow
        }
        val registration = firestore.collection("users").document(uid).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING).limit(limit.coerceIn(1, 100).toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) trySend(AppResult.Failure(error.messageError()))
                else trySend(AppResult.Success(snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.data?.plus("id" to doc.id)?.notification()
                }))
            }
        awaitClose { registration.remove() }
    }

    private suspend fun call(name: String, payload: Map<String, Any?>): Map<*, *> =
        functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *> ?: emptyMap<Any, Any>()

    private suspend fun <T> result(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (error: Exception) {
        AppResult.Failure(error.messageError())
    }
}

private fun Map<*, *>.notification(): AppNotification? = runCatching {
    AppNotification(
        id = this["id"] as String,
        event = enumOrDefault(this["event"], NotificationEvent.UNKNOWN),
        title = this["title"] as String,
        body = this["body"] as String,
        createdAtEpochMillis = epoch(this["createdAtEpochMillis"] ?: this["createdAt"]),
        readAtEpochMillis = nullableEpoch(this["readAtEpochMillis"] ?: this["readAt"]),
        orderId = this["orderId"] as? String,
        productId = this["productId"] as? String,
        reportPeriod = this["reportPeriod"] as? String,
        deepLinkRoute = this["deepLinkRoute"] as? String,
        category = enumOrDefault(this["category"], NotificationCategory.SYSTEM),
        priority = enumOrDefault(this["priority"], NotificationPriority.NORMAL),
    )
}.getOrNull()

private inline fun <reified T : Enum<T>> enumOrDefault(value: Any?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

private fun epoch(value: Any?): Long = when (value) {
    is Number -> value.toLong()
    is Timestamp -> value.toDate().time
    else -> 0L
}

private fun nullableEpoch(value: Any?): Long? = when (value) {
    is Number -> value.toLong()
    is Timestamp -> value.toDate().time
    else -> null
}

private fun Throwable.messageError(): AppError = when ((this as? FirebaseFunctionsException)?.code) {
    FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
    FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
    FirebaseFunctionsException.Code.NOT_FOUND -> AppError.NotFound
    FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
    else -> AppError.Unknown(this)
}

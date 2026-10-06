package com.spacetecsolutions.meatapp.core.data.firebase.order

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.isRealtimeTrackingAvailable
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** One query listener for every customer order; config listeners only carry the two tracking gates. */
internal class CustomerOrderRealtimeDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    fun observe(): Flow<AppResult<List<CodOrder>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(AppResult.Failure(AppError.Unauthorized))
            close()
            return@callbackFlow
        }
        val registrations = mutableListOf<ListenerRegistration>()
        var orderValues: List<Map<String, Any?>>? = null
        var superAllowed: Boolean? = null
        var adminEnabled: Boolean? = null

        fun emitCurrent() {
            val values = orderValues ?: return
            val superGate = superAllowed ?: return
            val adminGate = adminEnabled ?: return
            val orders = values.mapNotNull { raw ->
                runCatching {
                    val order = raw.toCodOrder()
                    order.copy(realtimeTrackingAvailable = order.isRealtimeTrackingAvailable(superGate, adminGate))
                }.onFailure { Log.w(TAG, "Ignoring malformed customer order", it) }.getOrNull()
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) orders.forEach { order ->
                Log.d(TAG, "Tracking ${order.displayNumber}: super=$superGate admin=$adminGate " +
                    "status=${order.orderStatus} role=${order.assignedDeliveryRole} " +
                    "session=${order.trackingLifecycle} available=${order.realtimeTrackingAvailable}")
            }
            trySend(AppResult.Success(orders))
        }

        fun attach(shopId: String) {
            registrations += firestore.collection("shops").document(shopId).collection("orders")
                .whereEqualTo("customerId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(250)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Customer order listener failed", error)
                        trySend(AppResult.Failure(AppError.Network))
                    } else {
                        orderValues = snapshot?.documents.orEmpty().mapNotNull { document ->
                            document.data?.toMutableMap()?.apply { put("id", document.id) }
                        }
                        emitCurrent()
                    }
                }
            registrations += firestore.collection("appConfig").document("features")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) trySend(AppResult.Failure(AppError.Network))
                    else { superAllowed = snapshot?.getBoolean("realtimeTrackingAllowed") == true; emitCurrent() }
                }
            registrations += firestore.collection("shops").document(shopId).collection("config").document("delivery")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) trySend(AppResult.Failure(AppError.Network))
                    else { adminEnabled = snapshot?.getBoolean("realtimeTrackingEnabled") == true; emitCurrent() }
                }
        }

        registrations += firestore.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            when {
                error != null -> trySend(AppResult.Failure(AppError.Network))
                snapshot?.getBoolean("active") != true || snapshot.getString("role") != "CUSTOMER" ->
                    trySend(AppResult.Failure(AppError.Forbidden))
                registrations.size == 1 -> attach(snapshot.getString("shopId") ?: "default")
            }
        }
        awaitClose { registrations.forEach(ListenerRegistration::remove) }
    }

    private companion object { const val TAG = "CustomerOrderRealtime" }
}

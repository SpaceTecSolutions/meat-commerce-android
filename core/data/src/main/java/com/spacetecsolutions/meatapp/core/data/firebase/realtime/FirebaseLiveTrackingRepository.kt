package com.spacetecsolutions.meatapp.core.data.firebase.realtime

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.LiveTrackingRepository
import com.spacetecsolutions.meatapp.core.model.CodActor
import com.spacetecsolutions.meatapp.core.model.DeliveryLocation
import com.spacetecsolutions.meatapp.core.model.DeliveryRoute
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseLiveTrackingRepository @Inject constructor(
    private val accessor: RealtimeTrackingDatabaseAccessor,
    private val functions: FirebaseFunctions,
) : LiveTrackingRepository {
    override suspend fun activate(
        actor: CodActor, orderId: String, sessionId: String,
    ): AppResult<Unit> = function("activateDeliveryTracking", mapOf(
        "actor" to actor.name, "orderId" to orderId, "trackingSessionId" to sessionId,
    ))

    override suspend fun publish(sessionId: String, location: DeliveryLocation): AppResult<Unit> = try {
        accessor.getWhenTrackingIsEnabled().reference
            .child(ACTIVE).child(sessionId).child("location")
            .setValue(mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracyMeters" to location.accuracyMeters,
                "speedMetersPerSecond" to location.speedMetersPerSecond,
                "headingDegrees" to location.headingDegrees,
                "recordedAtEpochMillis" to location.recordedAtEpochMillis,
            )).await()
        AppResult.Success(Unit)
    } catch (error: Exception) { AppResult.Failure(AppError.Network) }

    override fun observeCustomerLocation(
        orderId: String, sessionId: String,
    ): Flow<AppResult<DeliveryLocation>> = callbackFlow {
        // RTDB Rules validate the authenticated customer owns orderId/sessionId.
        val reference = accessor.getWhenTrackingIsEnabled().reference
            .child(ACTIVE).child(sessionId).child("location")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val location = snapshot.toLocation()
                trySend(location?.let { AppResult.Success(it) }
                    ?: AppResult.Failure(AppError.NotFound))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(AppResult.Failure(AppError.Network))
            }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

    override suspend fun getCustomerRoute(orderId: String): AppResult<DeliveryRoute> {
        return try {
            val response = functions.getHttpsCallable("customerGetTrackingRoute")
                .call(mapOf("orderId" to orderId)).await().data as? Map<*, *>
            val route = response?.get("route") as? Map<*, *>
                ?: return AppResult.Failure(AppError.NotFound)
            val polyline = route["encodedPolyline"] as? String
                ?: return AppResult.Failure(AppError.NotFound)
            AppResult.Success(DeliveryRoute(
                encodedPolyline = polyline,
                distanceMeters = (route["distanceMeters"] as? Number)?.toLong() ?: 0,
                durationSeconds = (route["durationSeconds"] as? Number)?.toLong() ?: 0,
                generatedAtEpochMillis = (route["generatedAtEpochMillis"] as? Number)?.toLong() ?: 0,
                destinationLatitude = (route["destinationLatitude"] as? Number)?.toDouble()
                    ?: return AppResult.Failure(AppError.NotFound),
                destinationLongitude = (route["destinationLongitude"] as? Number)?.toDouble()
                    ?: return AppResult.Failure(AppError.NotFound),
            ))
        } catch (error: Exception) { AppResult.Failure(AppError.Network) }
    }

    override suspend fun stop(orderId: String, sessionId: String): AppResult<Unit> {
        return function("stopDeliveryTracking", mapOf(
            "orderId" to orderId, "trackingSessionId" to sessionId,
        ))
    }

    private suspend fun function(name: String, payload: Map<String, Any>): AppResult<Unit> = try {
        functions.getHttpsCallable(name).call(payload).await()
        AppResult.Success(Unit)
    } catch (error: Exception) { AppResult.Failure(AppError.Forbidden) }

    private companion object { const val ACTIVE = "activeDeliveryTracking" }
}

private fun DataSnapshot.toLocation(): DeliveryLocation? = runCatching {
    DeliveryLocation(
        latitude = child("latitude").getValue(Double::class.java) ?: return null,
        longitude = child("longitude").getValue(Double::class.java) ?: return null,
        accuracyMeters = child("accuracyMeters").getValue(Double::class.java)?.toFloat() ?: return null,
        speedMetersPerSecond = child("speedMetersPerSecond").getValue(Double::class.java)?.toFloat(),
        headingDegrees = child("headingDegrees").getValue(Double::class.java)?.toFloat(),
        recordedAtEpochMillis = child("recordedAtEpochMillis").getValue(Long::class.java) ?: return null,
    )
}.getOrNull()

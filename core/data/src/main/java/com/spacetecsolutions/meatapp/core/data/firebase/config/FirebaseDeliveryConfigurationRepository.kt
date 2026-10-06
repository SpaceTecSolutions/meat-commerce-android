package com.spacetecsolutions.meatapp.core.data.firebase.config

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.DeliveryConfigurationRepository
import com.spacetecsolutions.meatapp.core.model.DeliveryConfig
import com.spacetecsolutions.meatapp.core.model.DeliverySlot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseDeliveryConfigurationRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : DeliveryConfigurationRepository {
    override suspend fun get() = call("adminGetDeliveryConfig", emptyMap())

    override suspend fun update(config: DeliveryConfig) = call(
        "adminUpdateDeliveryConfig",
        mapOf(
            "normalDeliveryEnabled" to config.normalDeliveryEnabled,
            "deliveryChargeMinor" to config.deliveryChargeMinor,
            "freeDeliveryThresholdMinor" to config.freeDeliveryThresholdMinor,
            "minimumOrderMinor" to config.minimumOrderMinor,
            "scheduledDeliveryEnabled" to config.scheduledDeliveryEnabled,
            "realtimeTrackingEnabled" to config.realtimeTrackingEnabled,
            "slots" to config.slots.map { slot ->
                mapOf("id" to slot.id, "label" to slot.label, "startMinutes" to slot.startMinutes,
                    "endMinutes" to slot.endMinutes, "active" to slot.active)
            },
            "expectedRevision" to config.revision,
        ),
    )

    private suspend fun call(name: String, payload: Map<String, Any?>): AppResult<DeliveryConfig> = try {
        val response = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: return AppResult.Failure(AppError.Unknown())
        val map = response["config"] as? Map<*, *> ?: response
        AppResult.Success(map.toDeliveryConfig())
    } catch (error: Exception) {
        val exception = error as? FirebaseFunctionsException
        val appError = when (exception?.code) {
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
            FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                AppError.Validation(message = "Check delivery settings")
            FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                AppError.Validation(message = exception.message ?: "This option is not permitted")
            FirebaseFunctionsException.Code.ABORTED ->
                AppError.Validation(message = "Settings changed. Refresh and retry")
            FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
            else -> AppError.Unknown(error)
        }
        AppResult.Failure(appError)
    }
}

private fun Map<*, *>.toDeliveryConfig() = DeliveryConfig(
    normalDeliveryEnabled = this["normalDeliveryEnabled"] as? Boolean ?: true,
    deliveryChargeMinor = (this["deliveryChargeMinor"] as? Number)?.toLong() ?: 0,
    freeDeliveryThresholdMinor = (this["freeDeliveryThresholdMinor"] as? Number)?.toLong(),
    minimumOrderMinor = (this["minimumOrderMinor"] as? Number)?.toLong() ?: 0,
    scheduledDeliveryEnabled = this["scheduledDeliveryEnabled"] as? Boolean ?: false,
    realtimeTrackingEnabled = this["realtimeTrackingEnabled"] as? Boolean ?: false,
    currencyCode = this["currencyCode"] as? String ?: "INR",
    revision = (this["revision"] as? Number)?.toLong() ?: 0,
    slots = (this["slots"] as? List<*>)?.mapNotNull { raw ->
        val slot = raw as? Map<*, *> ?: return@mapNotNull null
        runCatching { DeliverySlot(
            id = slot["id"] as? String ?: return@mapNotNull null,
            label = slot["label"] as? String ?: return@mapNotNull null,
            startMinutes = (slot["startMinutes"] as? Number)?.toInt() ?: return@mapNotNull null,
            endMinutes = (slot["endMinutes"] as? Number)?.toInt() ?: return@mapNotNull null,
            active = slot["active"] as? Boolean ?: true,
        ) }.getOrNull()
    }.orEmpty(),
)

package com.spacetecsolutions.meatapp.core.data.firebase.config

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.coroutines.AppDispatchers
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.data.preferences.FeatureConfigCache
import com.spacetecsolutions.meatapp.core.domain.repository.FeatureManagementRepository
import com.spacetecsolutions.meatapp.core.model.FeatureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseFeatureManagementRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore,
    private val cache: FeatureConfigCache,
    dispatchers: AppDispatchers,
) : FeatureManagementRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val mutableConfig = MutableStateFlow<AppResult<FeatureConfig>?>(null)
    override val featureConfig: StateFlow<AppResult<FeatureConfig>?> = mutableConfig

    init {
        scope.launch {
            cache.config.first()?.let { mutableConfig.value = AppResult.Success(it) }
            refresh()
        }
        scope.launch {
            callbackFlow {
                val registration = firestore.collection("appConfig").document("publicCustomerFeatures")
                    .addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null && snapshot.exists())
                            trySend(snapshot.getBoolean("inAppNotificationsEnabled") to snapshot.getLong("revision"))
                    }
                awaitClose { registration.remove() }
            }.collect { (enabled, revision): Pair<Boolean?, Long?> ->
                val existing = (mutableConfig.value as? AppResult.Success)?.value
                if (existing != null && revision != null && revision > existing.revision) {
                    // A public projection changed: reload the authoritative full revision.
                    refresh()
                } else if (enabled != null && existing != null &&
                    (revision == null || revision == existing.revision)) {
                    val config = existing.copy(inAppNotificationsEnabled = enabled)
                    cache.save(config)
                    mutableConfig.value = AppResult.Success(config)
                }
            }
        }
    }

    override suspend fun refresh(): AppResult<FeatureConfig> =
        call("getAllowedFeatureConfig", emptyMap()).also { result ->
            if (result is AppResult.Success) {
                cache.save(result.value)
                mutableConfig.value = result
            } else if (mutableConfig.value == null) {
                mutableConfig.value = result
            }
        }

    override suspend fun update(config: FeatureConfig): AppResult<FeatureConfig> =
        call(
            "superAdminUpdateFeatureConfig",
            mapOf(
                "deliveryStaffManagementAllowed" to config.deliveryStaffManagementAllowed,
                "staffManagementAllowed" to config.staffManagementAllowed,
                "subcategoriesAllowed" to config.subcategoriesAllowed,
                "realtimeTrackingAllowed" to config.realtimeTrackingAllowed,
                "codAllowed" to config.codAllowed,
                "razorpayAllowed" to config.razorpayAllowed,
                "upiAllowed" to config.upiAllowed,
                "offersAllowed" to config.offersAllowed,
                "couponsAllowed" to config.couponsAllowed,
                "scheduledDeliveryAllowed" to config.scheduledDeliveryAllowed,
                "inAppNotificationsEnabled" to config.inAppNotificationsEnabled,
                "maxProducts" to config.maxProducts,
                "expectedRevision" to config.revision,
            ),
        ).also { result ->
            if (result is AppResult.Success) {
                cache.save(result.value)
                mutableConfig.value = result
            }
        }

    private suspend fun call(name: String, payload: Map<String, Any>): AppResult<FeatureConfig> = try {
        val response = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: return AppResult.Failure(AppError.Unknown())
        val config = (response["config"] as? Map<*, *> ?: response).toFeatureConfig()
        AppResult.Success(config)
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }
}

private fun Map<*, *>.toFeatureConfig() = FeatureConfig(
    deliveryStaffManagementAllowed = this["deliveryStaffManagementAllowed"] as? Boolean ?: false,
    staffManagementAllowed = this["staffManagementAllowed"] as? Boolean ?: false,
    subcategoriesAllowed = this["subcategoriesAllowed"] as? Boolean ?: false,
    realtimeTrackingAllowed = this["realtimeTrackingAllowed"] as? Boolean ?: false,
    codAllowed = this["codAllowed"] as? Boolean ?: false,
    razorpayAllowed = this["razorpayAllowed"] as? Boolean ?: false,
    upiAllowed = this["upiAllowed"] as? Boolean ?: false,
    offersAllowed = this["offersAllowed"] as? Boolean ?: false,
    couponsAllowed = this["couponsAllowed"] as? Boolean ?: false,
    scheduledDeliveryAllowed = this["scheduledDeliveryAllowed"] as? Boolean ?: false,
    inAppNotificationsEnabled = this["inAppNotificationsEnabled"] as? Boolean ?: true,
    maxProducts = (this["maxProducts"] as? Number)?.toInt() ?: FeatureConfig.DEFAULT_MAX_PRODUCTS,
    revision = (this["revision"] as? Number)?.toLong() ?: 0L,
)

private fun Throwable.toAppError(): AppError {
    val exception = this as? FirebaseFunctionsException
    return when (exception?.code) {
        FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
            AppError.Validation(message = "Check the feature configuration")
        FirebaseFunctionsException.Code.ABORTED ->
            AppError.Validation(message = "Settings changed elsewhere. Refresh and try again")
        FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

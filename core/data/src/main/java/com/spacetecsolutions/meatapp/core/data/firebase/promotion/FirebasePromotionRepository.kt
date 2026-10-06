package com.spacetecsolutions.meatapp.core.data.firebase.promotion

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.PromotionRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebasePromotionRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : PromotionRepository {
    override suspend fun getAdminPromotions(): AppResult<List<Promotion>> = try {
        val data = call("adminGetPromotions")
        AppResult.Success((data["promotions"] as? List<*>).orEmpty().mapNotNull {
            (it as? Map<*, *>)?.toPromotion()
        })
    } catch (error: Exception) { AppResult.Failure(error.toPromotionError()) }

    override suspend fun savePromotion(promotion: Promotion) = promotionCall(
        "adminSavePromotion", promotion.toPayload() + ("expectedRevision" to promotion.revision),
    )

    override suspend fun setPromotionActive(id: String, active: Boolean, expectedRevision: Long) =
        promotionCall("adminSetPromotionActive", mapOf(
            "promotionId" to id, "active" to active, "expectedRevision" to expectedRevision,
        ))

    private suspend fun promotionCall(name: String, payload: Map<String, Any?>): AppResult<Promotion> = try {
        val data = call(name, payload)
        AppResult.Success((data["promotion"] as? Map<*, *> ?: data).toPromotion())
    } catch (error: Exception) { AppResult.Failure(error.toPromotionError()) }

    private suspend fun call(name: String, payload: Map<String, Any?> = emptyMap()) =
        functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid promotion response")
}

private fun Promotion.toPayload(): Map<String, Any?> = mapOf(
    "promotionId" to id.takeIf { it != "new" }, "name" to name, "code" to code,
    "kind" to kind.name, "discountType" to discountType.name, "discountValue" to discountValue,
    "minimumOrderMinor" to minimumOrderMinor, "maximumDiscountMinor" to maximumDiscountMinor,
    "validFromEpochMillis" to validFromEpochMillis, "validUntilEpochMillis" to validUntilEpochMillis,
    "active" to active,
)
internal fun Map<*, *>.toPromotion() = Promotion(
    id = this["id"] as? String ?: error("Missing promotion id"),
    name = this["name"] as? String ?: error("Missing promotion name"),
    code = this["code"] as? String, kind = enum("kind", PromotionKind.OFFER),
    discountType = enum("discountType", DiscountType.PERCENTAGE),
    discountValue = (this["discountValue"] as? Number)?.toLong() ?: 0,
    minimumOrderMinor = (this["minimumOrderMinor"] as? Number)?.toLong() ?: 0,
    maximumDiscountMinor = (this["maximumDiscountMinor"] as? Number)?.toLong(),
    validFromEpochMillis = (this["validFromEpochMillis"] as? Number)?.toLong() ?: 0,
    validUntilEpochMillis = (this["validUntilEpochMillis"] as? Number)?.toLong() ?: 0,
    active = this["active"] as? Boolean ?: true,
    revision = (this["revision"] as? Number)?.toLong() ?: 0,
)
private inline fun <reified T : Enum<T>> Map<*, *>.enum(key: String, fallback: T) =
    runCatching { enumValueOf<T>(this[key] as String) }.getOrDefault(fallback)
private fun Throwable.toPromotionError(): AppError {
    val exception = this as? FirebaseFunctionsException
    return when (exception?.code) {
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        FirebaseFunctionsException.Code.INVALID_ARGUMENT -> AppError.Validation(message = "Check promotion details")
        FirebaseFunctionsException.Code.FAILED_PRECONDITION -> AppError.Validation(message = exception.message ?: "Promotion is not allowed")
        FirebaseFunctionsException.Code.ABORTED -> AppError.Validation(message = "Promotion changed. Refresh and retry")
        FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

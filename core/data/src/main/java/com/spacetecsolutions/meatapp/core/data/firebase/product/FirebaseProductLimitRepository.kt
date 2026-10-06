package com.spacetecsolutions.meatapp.core.data.firebase.product

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.ProductLimitRepository
import com.spacetecsolutions.meatapp.core.model.ProductLimitStatus
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseProductLimitRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : ProductLimitRepository {
    override suspend fun getStatus(): AppResult<ProductLimitStatus> =
        call("superAdminGetProductLimitStatus", emptyMap())

    override suspend fun updateLimit(
        maxProducts: Int,
        expectedRevision: Long,
    ): AppResult<ProductLimitStatus> = call(
        "superAdminUpdateProductLimit",
        mapOf("maxProducts" to maxProducts, "expectedRevision" to expectedRevision),
    )

    private suspend fun call(
        name: String,
        payload: Map<String, Any>,
    ): AppResult<ProductLimitStatus> = try {
        val response = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: return AppResult.Failure(AppError.Unknown())
        val status = response["status"] as? Map<*, *> ?: response
        AppResult.Success(
            ProductLimitStatus(
                countedProducts = (status["countedProducts"] as? Number)?.toInt()
                    ?: error("Missing countedProducts"),
                maxProducts = (status["maxProducts"] as? Number)?.toInt()
                    ?: error("Missing maxProducts"),
                revision = (status["revision"] as? Number)?.toLong() ?: 0L,
            ),
        )
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }
}

private fun Throwable.toAppError(): AppError {
    val exception = this as? FirebaseFunctionsException
    val reason = (exception?.details as? Map<*, *>)?.get("reason") as? String
    return when {
        reason == "LIMIT_BELOW_CURRENT_COUNT" -> AppError.Validation(
            field = "maxProducts",
            message = "Archive products before lowering the limit below the current count",
        )
        exception?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        exception?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        exception?.code == FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
            AppError.Validation(field = "maxProducts", message = "Enter a valid product limit")
        exception?.code == FirebaseFunctionsException.Code.ABORTED ->
            AppError.Validation(message = "The catalog changed. Refresh and try again")
        exception?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

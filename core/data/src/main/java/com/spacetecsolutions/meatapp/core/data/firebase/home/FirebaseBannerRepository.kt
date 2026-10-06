package com.spacetecsolutions.meatapp.core.data.firebase.home

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.data.firebase.product.ProductImageCompressor
import com.spacetecsolutions.meatapp.core.domain.repository.BannerRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseBannerRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
    private val compressor: ProductImageCompressor,
) : BannerRepository {
    override suspend fun getBanners() = runResult {
        call("adminGetBanners").bannerList()
    }

    override suspend fun uploadImage(localUri: String) = runResult {
        val session = call("adminBeginBannerImageUpload")
        val path = session["storagePath"] as? String ?: error("Missing storage path")
        val token = session["uploadToken"] as? String ?: error("Missing upload token")
        val bytes = compressor.compress(localUri)
        require(bytes.size <= 5 * 1024 * 1024) { "Banner image is too large" }
        storage.reference.child(path).putBytes(
            bytes, StorageMetadata.Builder().setContentType("image/jpeg").build(),
        ).await()
        BannerImageUpload(token)
    }

    override suspend fun save(input: BannerInput) = runResult {
        val response = call(if (input.bannerId == null) "adminCreateBanner" else "adminUpdateBanner", input.payload())
        (response["banner"] as? Map<*, *> ?: response).toBanner()
    }

    override suspend fun setActive(id: String, active: Boolean, expectedRevision: Long) = runResult {
        val response = call("adminSetBannerActive", mapOf(
            "bannerId" to id, "active" to active, "expectedRevision" to expectedRevision,
        ))
        (response["banner"] as? Map<*, *> ?: response).toBanner()
    }

    override suspend fun delete(id: String, expectedRevision: Long) = runResult {
        call("adminDeleteBanner", mapOf("bannerId" to id, "expectedRevision" to expectedRevision)); Unit
    }

    private suspend fun call(name: String, payload: Map<String, Any?> = emptyMap()) =
        functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid banner response")

    private suspend fun <T> runResult(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (error: Exception) { AppResult.Failure(error.bannerError()) }
}

private fun BannerInput.payload(): Map<String, Any?> = mapOf(
    "bannerId" to bannerId, "title" to title, "subtitle" to subtitle,
    "imageUploadToken" to imageUploadToken, "actionRoute" to actionRoute, "active" to active,
    "sortOrder" to sortOrder, "contentAlignment" to contentAlignment.name,
    "buttonEnabled" to buttonEnabled, "buttonText" to buttonText,
    "expectedRevision" to expectedRevision,
    "textPlacement" to textPlacement?.payload(), "buttonPlacement" to buttonPlacement?.payload(),
    "subtitlePlacement" to subtitlePlacement?.payload(), "buttonColor" to buttonColor,
    "buttonTextColor" to buttonTextColor,
    "buttonShape" to buttonShape, "buttonArrow" to buttonArrow,
)

private fun BannerPlacement.payload() = mapOf("x" to x, "y" to y, "alignment" to alignment)
private fun Any?.placement(): BannerPlacement? = (this as? Map<*, *>)?.let {
    BannerPlacement((it["x"] as? Number)?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
        (it["y"] as? Number)?.toFloat()?.coerceIn(0f, 1f) ?: 0f, it["alignment"] as? String ?: "LEFT")
}

internal fun Map<*, *>.toBanner() = PromotionBanner(
    id = this["id"] as? String ?: error("Missing banner id"),
    title = this["title"] as? String ?: "", subtitle = this["subtitle"] as? String ?: "",
    imageUrl = this["imageUrl"] as? String, actionRoute = this["actionRoute"] as? String,
    active = this["active"] as? Boolean ?: false,
    sortOrder = (this["sortOrder"] as? Number)?.toInt() ?: 0,
    contentAlignment = runCatching {
        BannerContentAlignment.valueOf(this["contentAlignment"] as? String ?: "BOTTOM_START")
    }.getOrDefault(BannerContentAlignment.BOTTOM_START),
    buttonEnabled = this["buttonEnabled"] as? Boolean ?: false,
    buttonText = this["buttonText"] as? String ?: "Shop Now",
    revision = (this["revision"] as? Number)?.toLong() ?: 0,
    textPlacement = this["textPlacement"].placement(), buttonPlacement = this["buttonPlacement"].placement(),
    subtitlePlacement = this["subtitlePlacement"].placement(),
    buttonColor = this["buttonColor"] as? String ?: "RED",
    buttonTextColor = this["buttonTextColor"] as? String ?: "WHITE",
    buttonShape = this["buttonShape"] as? String ?: "ROUNDED",
    buttonArrow = this["buttonArrow"] as? Boolean ?: false,
)

private fun Map<*, *>.bannerList() = (this["banners"] as? List<*>).orEmpty()
    .mapNotNull { (it as? Map<*, *>)?.toBanner() }

private fun Throwable.bannerError(): AppError {
    val exception = this as? FirebaseFunctionsException
    return when (exception?.code) {
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        FirebaseFunctionsException.Code.INVALID_ARGUMENT,
        FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
            AppError.Validation(message = exception.message ?: "Check banner details")
        FirebaseFunctionsException.Code.ABORTED -> AppError.Validation(message = "Banner changed. Refresh and retry")
        FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

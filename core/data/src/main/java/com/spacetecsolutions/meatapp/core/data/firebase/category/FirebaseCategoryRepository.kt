package com.spacetecsolutions.meatapp.core.data.firebase.category

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.spacetecsolutions.meatapp.core.data.firebase.product.ProductImageCompressor
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.CategoryRepository
import com.spacetecsolutions.meatapp.core.model.CategoryImageUpload
import com.spacetecsolutions.meatapp.core.model.CategoryInput
import com.spacetecsolutions.meatapp.core.model.ProductCategory
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseCategoryRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
    private val compressor: ProductImageCompressor,
) : CategoryRepository {
    override suspend fun getAdminCategories(): AppResult<List<ProductCategory>> =
        categoryListCall("adminGetCategories")

    override suspend fun getActiveCategories(): AppResult<List<ProductCategory>> =
        categoryListCall("customerGetActiveCategories")

    override suspend fun uploadCategoryImage(localUri: String): AppResult<CategoryImageUpload> = try {
        val session = rawCall("adminBeginCategoryImageUpload", emptyMap())
        val path = session["storagePath"] as? String ?: error("Missing storagePath")
        val token = session["uploadToken"] as? String ?: error("Missing uploadToken")
        val bytes = compressor.compress(localUri)
        require(bytes.size <= MAX_UPLOAD_BYTES) { "Compressed image is too large" }
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        storage.reference.child(path).putBytes(bytes, metadata).await()
        AppResult.Success(CategoryImageUpload(token))
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    override suspend fun createCategory(input: CategoryInput): AppResult<ProductCategory> =
        mutationCall("adminCreateCategory", input.payload(includeId = false))

    override suspend fun updateCategory(input: CategoryInput): AppResult<ProductCategory> =
        mutationCall("adminUpdateCategory", input.payload(includeId = true))

    override suspend fun setCategoryActive(
        categoryId: String,
        active: Boolean,
        expectedRevision: Long,
    ): AppResult<ProductCategory> = mutationCall(
        "adminSetCategoryActive",
        mapOf("categoryId" to categoryId, "active" to active, "expectedRevision" to expectedRevision),
    )

    override suspend fun updateCategoryOrder(
        categoryIds: List<String>,
    ): AppResult<List<ProductCategory>> = try {
        val response = rawCall("adminUpdateCategoryOrder", mapOf("categoryIds" to categoryIds))
        AppResult.Success(response.categories())
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    override suspend fun deleteCategory(categoryId: String, expectedRevision: Long): AppResult<Unit> = try {
        rawCall("adminDeleteCategory", mapOf("categoryId" to categoryId, "expectedRevision" to expectedRevision))
        AppResult.Success(Unit)
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    private suspend fun categoryListCall(name: String): AppResult<List<ProductCategory>> = try {
        AppResult.Success(rawCall(name, emptyMap()).categories())
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    private suspend fun mutationCall(
        name: String,
        payload: Map<String, Any>,
    ): AppResult<ProductCategory> = try {
        val response = rawCall(name, payload)
        val category = response["category"] as? Map<*, *> ?: response
        AppResult.Success(category.toCategory())
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    private suspend fun rawCall(name: String, payload: Map<String, Any>): Map<*, *> =
        functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid category response")

    private companion object { const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024 }
}

private fun CategoryInput.payload(includeId: Boolean): Map<String, Any> = buildMap {
    if (includeId) put("categoryId", requireNotNull(categoryId))
    put("name", name)
    put("description", description)
    put("sortOrder", sortOrder)
    put("active", active)
    imageUploadToken?.let { put("imageUploadToken", it) }
    expectedRevision?.let { put("expectedRevision", it) }
}

private fun Map<*, *>.categories(): List<ProductCategory> =
    (this["categories"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.toCategory() }

private fun Map<*, *>.toCategory() = ProductCategory(
    id = this["id"] as? String ?: error("Missing category id"),
    name = this["name"] as? String ?: error("Missing category name"),
    description = this["description"] as? String ?: "",
    imageUrl = this["imageUrl"] as? String,
    active = this["active"] as? Boolean ?: false,
    sortOrder = (this["sortOrder"] as? Number)?.toInt() ?: 0,
    productCount = (this["productCount"] as? Number)?.toInt() ?: 0,
    activeProductCount = (this["activeProductCount"] as? Number)?.toInt() ?: 0,
    revision = (this["revision"] as? Number)?.toLong() ?: 0L,
)

private fun Throwable.toAppError(): AppError {
    val exception = this as? FirebaseFunctionsException
    val reason = (exception?.details as? Map<*, *>)?.get("reason") as? String
    return when {
        reason == "ACTIVE_PRODUCTS_EXIST" -> AppError.Validation(
            field = "active",
            message = "Move or deactivate this category's active products first",
        )
        reason == "DUPLICATE_CATEGORY" -> AppError.Validation(
            field = "name",
            message = "A category with this name already exists",
        )
        reason == "CATEGORY_HAS_PRODUCTS" -> AppError.Validation(
            field = "category",
            message = "Move or delete this category's products first",
        )
        exception?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        exception?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        exception?.code == FirebaseFunctionsException.Code.ALREADY_EXISTS ->
            AppError.Validation(field = "name", message = "A category with this name already exists")
        exception?.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
            AppError.Validation(message = exception.message ?: "Category requirements are not satisfied")
        exception?.code == FirebaseFunctionsException.Code.NOT_FOUND -> AppError.NotFound
        exception?.code == FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
            AppError.Validation(message = "Check the category details")
        exception?.code == FirebaseFunctionsException.Code.ABORTED ->
            AppError.Validation(message = "The category changed. Refresh and try again")
        exception?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        exception?.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> AppError.Timeout
        exception?.code == FirebaseFunctionsException.Code.INTERNAL ->
            AppError.Validation(message = "The category service failed. Try again shortly")
        else -> AppError.Unknown(this)
    }
}

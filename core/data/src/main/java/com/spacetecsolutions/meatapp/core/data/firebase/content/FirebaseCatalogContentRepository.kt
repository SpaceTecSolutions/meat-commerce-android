package com.spacetecsolutions.meatapp.core.data.firebase.content

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.data.firebase.product.ProductImageCompressor
import com.spacetecsolutions.meatapp.core.domain.repository.CatalogContentRepository
import com.spacetecsolutions.meatapp.core.model.*
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

internal class FirebaseCatalogContentRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
    private val compressor: ProductImageCompressor,
) : CatalogContentRepository {
    override suspend fun getSubcategories(categoryId: String, admin: Boolean) = result {
        call(if (admin) "adminGetSubcategories" else "customerGetActiveSubcategories",
            mapOf("categoryId" to categoryId)).list("subcategories") { it.subcategory() }
    }
    override suspend fun uploadSubcategoryImage(localUri: String): AppResult<SubcategoryImageUpload> = result {
        val session = call("adminBeginSubcategoryImageUpload", emptyMap())
        val path = session["storagePath"] as? String ?: error("Missing storagePath")
        val token = session["uploadToken"] as? String ?: error("Missing uploadToken")
        val bytes = compressor.compress(localUri)
        require(bytes.size <= MAX_UPLOAD_BYTES) { "Compressed image is too large" }
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        storage.reference.child(path).putBytes(bytes, metadata).await()
        SubcategoryImageUpload(token)
    }
    override suspend fun saveSubcategory(input: SubcategoryInput) = result {
        call("adminSaveSubcategory", buildMap {
            input.subcategoryId?.let { put("subcategoryId", it) }; put("categoryId", input.categoryId)
            put("name", input.name); put("description", input.description); put("sortOrder", input.sortOrder)
            put("active", input.active); input.expectedRevision?.let { put("expectedRevision", it) }
            input.imageUploadToken?.let { put("imageUploadToken", it) }
        }).item("subcategory") { it.subcategory() }
    }
    override suspend fun deleteSubcategory(id: String) = result<Unit> {
        call("adminDeleteSubcategory", mapOf("subcategoryId" to id)); Unit
    }
    override suspend fun getFaqs(admin: Boolean) = result {
        call(if (admin) "adminGetFaqs" else "customerGetFaqs", emptyMap()).list("faqs") { it.faq() }
    }
    override suspend fun saveFaq(input: FaqInput) = result {
        call("adminSaveFaq", buildMap {
            input.faqId?.let { put("faqId", it) }; put("question", input.question); put("answer", input.answer)
            put("active", input.active); put("sortOrder", input.sortOrder)
            input.expectedRevision?.let { put("expectedRevision", it) }
        }).item("faq") { it.faq() }
    }
    override suspend fun deleteFaq(id: String) = result<Unit> { call("adminDeleteFaq", mapOf("faqId" to id)); Unit }

    private suspend fun call(name: String, payload: Map<String, Any>) =
        functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *> ?: error("Invalid response")
    private suspend fun <T> result(block: suspend () -> T): AppResult<T> = try { AppResult.Success(block()) }
    catch (error: Exception) { AppResult.Failure(error.contentError()) }

    private companion object { const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024 }
}

private fun <T> Map<*, *>.list(key: String, mapper: (Map<*, *>) -> T) =
    (this[key] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.let(mapper) }
private fun <T> Map<*, *>.item(key: String, mapper: (Map<*, *>) -> T) = mapper(this[key] as? Map<*, *> ?: this)
private fun Map<*, *>.subcategory() = ProductSubcategory(
    id = this["id"] as? String ?: error("Missing id"), categoryId = this["categoryId"] as? String ?: "",
    name = this["name"] as? String ?: "", description = this["description"] as? String ?: "",
    active = this["active"] as? Boolean ?: false, sortOrder = (this["sortOrder"] as? Number)?.toInt() ?: 0,
    revision = (this["revision"] as? Number)?.toLong() ?: 0,
    imageUrl = this["imageUrl"] as? String,
)
private fun Map<*, *>.faq() = FaqEntry(
    id = this["id"] as? String ?: error("Missing id"), question = this["question"] as? String ?: "",
    answer = this["answer"] as? String ?: "", active = this["active"] as? Boolean ?: false,
    sortOrder = (this["sortOrder"] as? Number)?.toInt() ?: 0, revision = (this["revision"] as? Number)?.toLong() ?: 0,
    createdAtEpochMillis = (this["createdAtEpochMillis"] as? Number)?.toLong() ?: 0,
    updatedAtEpochMillis = (this["updatedAtEpochMillis"] as? Number)?.toLong() ?: 0,
    createdBy = this["createdBy"] as? String,
)
private fun Throwable.contentError(): AppError { val e = this as? FirebaseFunctionsException; return when (e?.code) {
    FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
    FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
    FirebaseFunctionsException.Code.NOT_FOUND -> AppError.NotFound
    FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
    FirebaseFunctionsException.Code.ABORTED -> AppError.Validation(message = "Content changed. Refresh and try again")
    else -> AppError.Validation(message = e?.message ?: "Unable to update content")
} }

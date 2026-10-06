package com.spacetecsolutions.meatapp.core.data.firebase.product

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.ProductRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.spacetecsolutions.meatapp.core.domain.service.CartBadgeStore
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerCartRepository
import com.google.firebase.auth.FirebaseAuth

internal class FirebaseProductRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
    private val compressor: ProductImageCompressor,
    private val cartBadgeStore: CartBadgeStore,
    private val customerCart: CustomerCartRepository,
    private val auth: FirebaseAuth,
) : ProductRepository {
    override suspend fun getCustomerProduct(productId: String): AppResult<Product> = try {
        val response = rawCall("customerGetProduct", mapOf("productId" to productId))
        val product = response["product"] as? Map<*, *> ?: response
        AppResult.Success(product.toProduct())
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    override suspend fun getCustomerProducts(
        query: CustomerProductQuery,
    ): AppResult<CustomerProductPage> = try {
        val payload = buildMap<String, Any> {
            put("search", query.search.trim())
            put("filter", query.filter.name)
            put("pageSize", query.pageSize.coerceIn(1, 40))
            query.categoryId?.let { put("categoryId", it) }
            query.subcategoryId?.let { put("subcategoryId", it) }
            query.cursor?.let { put("cursor", it) }
        }
        val response = rawCall("customerGetProducts", payload)
        val guestCart = if (auth.currentUser == null) (customerCart.getCart() as? AppResult.Success)?.value else null
        val page = CustomerProductPage(
                products = response.productList(),
                nextCursor = response["nextCursor"] as? String,
                cartQuantity = guestCart?.distinctItemCount ?: (response["cartQuantity"] as? Number)?.toInt() ?: 0,
                cartQuantities = guestCart?.lines?.associate { it.productId to it.quantity } ?: (response["cartQuantities"] as? Map<*, *>).orEmpty()
                    .mapNotNull { (key, value) ->
                        if (key is String && value is Number) key to value.toInt() else null
                    }.toMap(),
                offersEnabled = response["offersEnabled"] as? Boolean ?: false,
            )
        cartBadgeStore.update(page.cartQuantity)
        AppResult.Success(page)
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    override suspend fun addProductToCart(
        productId: String,
        quantity: Int,
    ): AppResult<CartMutation> = customerCart.addProduct(productId, quantity)

    override suspend fun getAdminProducts(query: ProductQuery): AppResult<ProductPage> = try {
        val payload = buildMap<String, Any> {
            put("search", query.search.trim())
            put("filter", query.filter.name)
            put("pageSize", query.pageSize.coerceIn(1, 50))
            query.categoryId?.let { put("categoryId", it) }
            query.subcategoryId?.let { put("subcategoryId", it) }
            query.cursor?.let { put("cursor", it) }
        }
        val response = rawCall("adminGetProducts", payload)
        AppResult.Success(
            ProductPage(
                products = response.productList(),
                nextCursor = response["nextCursor"] as? String,
                countedProducts = response.int("countedProducts"),
                productLimit = response.int("productLimit"),
            ),
        )
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    override suspend fun uploadProductImage(localUri: String): AppResult<ProductImageUpload> = try {
        val session = rawCall("adminBeginProductImageUpload", emptyMap())
        val path = session["storagePath"] as? String ?: error("Missing storage path")
        val token = session["uploadToken"] as? String ?: error("Missing upload token")
        val bytes = compressor.compress(localUri)
        require(bytes.size <= MAX_UPLOAD_BYTES) { "Compressed image is too large" }
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        storage.reference.child(path).putBytes(bytes, metadata).await()
        AppResult.Success(ProductImageUpload(token))
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    override suspend fun createProduct(input: ProductInput): AppResult<Product> =
        mutation("adminCreateProduct", input.payload(includeId = false))

    override suspend fun updateProduct(input: ProductInput): AppResult<Product> =
        mutation("adminUpdateProduct", input.payload(includeId = true))

    override suspend fun setProductActive(
        productId: String,
        active: Boolean,
        expectedRevision: Long,
    ): AppResult<Product> = mutation(
        "adminSetProductActive",
        mapOf("productId" to productId, "active" to active, "expectedRevision" to expectedRevision),
    )

    override suspend fun archiveProduct(productId: String, expectedRevision: Long): AppResult<Unit> = try {
        rawCall("adminArchiveProduct", mapOf("productId" to productId, "expectedRevision" to expectedRevision))
        AppResult.Success(Unit)
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    private suspend fun mutation(name: String, payload: Map<String, Any>): AppResult<Product> = try {
        val response = rawCall(name, payload)
        val product = response["product"] as? Map<*, *> ?: response
        AppResult.Success(product.toProduct())
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    private suspend fun rawCall(name: String, payload: Map<String, Any>): Map<*, *> =
        functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid product response")

    private companion object { const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024 }
}

private fun logCartFailure(error: Exception) {
    val callable = error as? FirebaseFunctionsException
    val reason = (callable?.details as? Map<*, *>)?.get("reason") as? String
    Log.e(
        "CustomerCart",
        "customerAddToCart failed: code=${callable?.code ?: "LOCAL"}, reason=${reason ?: "NONE"}, " +
            "message=${error.message}",
        error,
    )
}

private fun ProductInput.payload(includeId: Boolean): Map<String, Any> = buildMap {
    if (includeId) put("productId", requireNotNull(productId))
    put("categoryId", categoryId)
    subcategoryId?.let { put("subcategoryId", it) }
    put("removeSubcategory", subcategoryId == null)
    put("name", name)
    put("description", description)
    put("unit", unit.name)
    put("priceMinor", priceMinor)
    offerPriceMinor?.let { put("offerPriceMinor", it) }
    put("removeOfferPrice", offerPriceMinor == null)
    put("stockQuantity", stockQuantity)
    lowStockThreshold?.let { put("lowStockThreshold", it) }
    put("removeLowStockThreshold", lowStockThreshold == null)
    put("attributes", attributes)
    put("imageUploadTokens", imageUploadTokens)
    put("retainedImageUrls", retainedImageUrls)
    put("replaceImages", replaceImages)
    put("active", active)
    expectedRevision?.let { put("expectedRevision", it) }
}

private fun Map<*, *>.productList(): List<Product> =
    (this["products"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.toProduct() }

private fun Map<*, *>.toProduct() = Product(
    id = string("id"),
    categoryId = string("categoryId"),
    categoryName = string("categoryName"),
    subcategoryId = this["subcategoryId"] as? String,
    subcategoryName = this["subcategoryName"] as? String,
    name = string("name"),
    description = this["description"] as? String ?: "",
    unit = ProductUnit.valueOf(string("unit")),
    priceMinor = long("priceMinor"),
    offerPriceMinor = (this["offerPriceMinor"] as? Number)?.toLong(),
    stockQuantity = number("stockQuantity"),
    lowStockThreshold = (this["lowStockThreshold"] as? Number)?.toDouble(),
    imageUrls = (this["imageUrls"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
    attributes = (this["attributes"] as? Map<*, *>)?.entries?.mapNotNull { (key, value) ->
        if (key is String && value is String) key to value else null
    }?.toMap().orEmpty(),
    active = this["active"] as? Boolean ?: false,
    archived = this["archived"] as? Boolean ?: false,
    revision = (this["revision"] as? Number)?.toLong() ?: 0L,
    ratingAverage = (this["ratingAverage"] as? Number)?.toDouble()?.takeIf { it > 0.0 },
    ratingCount = (this["ratingCount"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
)

private fun Map<*, *>.string(key: String) = this[key] as? String ?: error("Missing product field: $key")
private fun Map<*, *>.long(key: String) = (this[key] as? Number)?.toLong()
    ?: error("Missing product field: $key")
private fun Map<*, *>.number(key: String) = (this[key] as? Number)?.toDouble()
    ?: error("Missing product field: $key")
private fun Map<*, *>.int(key: String) = (this[key] as? Number)?.toInt()
    ?: error("Missing product field: $key")

private fun Throwable.toAppError(): AppError {
    val exception = this as? FirebaseFunctionsException
    val reason = (exception?.details as? Map<*, *>)?.get("reason") as? String
    return when {
        reason == "PRODUCT_LIMIT_REACHED" -> AppError.Validation(
            field = "productLimit",
            message = "Product limit reached. Archive a product or ask the Super Admin to increase it",
        )
        reason == "CATEGORY_INACTIVE" -> AppError.Validation(
            field = "categoryId",
            message = "Choose an active category",
        )
        reason == "DUPLICATE_PRODUCT" -> AppError.Validation(
            field = "name",
            message = "A product with this name already exists in the category",
        )
        reason == "OUT_OF_STOCK" -> AppError.Validation(
            field = "stockQuantity",
            message = "This product is out of stock",
        )
        reason == "PRODUCT_UNAVAILABLE" -> AppError.NotFound
        exception?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        exception?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        exception?.code == FirebaseFunctionsException.Code.ALREADY_EXISTS ->
            AppError.Validation(field = "name", message = "A product with this name already exists")
        exception?.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
            AppError.Validation(field = "productLimit", message = "Product limit reached")
        exception?.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
            AppError.Validation(message = exception.message ?: "Product requirements are not satisfied")
        // A missing product has PRODUCT_UNAVAILABLE. Bare NOT_FOUND commonly means the callable endpoint is absent.
        exception?.code == FirebaseFunctionsException.Code.NOT_FOUND -> AppError.Unknown(this)
        exception?.code == FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
            AppError.Validation(message = "Check the product details")
        exception?.code == FirebaseFunctionsException.Code.ABORTED ->
            AppError.Validation(message = "The product changed. Refresh and try again")
        exception?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        exception?.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> AppError.Timeout
        exception?.code == FirebaseFunctionsException.Code.INTERNAL ->
            AppError.Validation(message = "The product service failed. Try again shortly")
        else -> AppError.Unknown(this)
    }
}

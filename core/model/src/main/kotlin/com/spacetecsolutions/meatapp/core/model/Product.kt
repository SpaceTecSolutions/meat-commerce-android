package com.spacetecsolutions.meatapp.core.model

enum class ProductUnit { KILOGRAM, GRAM, PIECE, PACK }
enum class ProductFilter { ALL, ACTIVE, INACTIVE, LOW_STOCK }
enum class CustomerProductFilter { ALL, IN_STOCK, OFFERS }

data class Product(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val subcategoryId: String? = null,
    val subcategoryName: String? = null,
    val name: String,
    val description: String,
    val unit: ProductUnit,
    val priceMinor: Long,
    val offerPriceMinor: Long? = null,
    val stockQuantity: Double,
    val lowStockThreshold: Double? = null,
    val imageUrls: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val active: Boolean = true,
    val archived: Boolean = false,
    val revision: Long = 0L,
    val ratingAverage: Double? = null,
    val ratingCount: Int = 0,
)

data class ProductQuery(
    val search: String = "",
    val filter: ProductFilter = ProductFilter.ALL,
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val cursor: String? = null,
    val pageSize: Int = 30,
)

data class ProductPage(
    val products: List<Product>,
    val nextCursor: String?,
    val countedProducts: Int,
    val productLimit: Int,
)

data class CustomerProductQuery(
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val search: String = "",
    val filter: CustomerProductFilter = CustomerProductFilter.ALL,
    val cursor: String? = null,
    val pageSize: Int = 24,
)

data class CustomerProductPage(
    val products: List<Product>,
    val nextCursor: String? = null,
    val cartQuantity: Int = 0,
    val cartQuantities: Map<String, Int> = emptyMap(),
    val offersEnabled: Boolean = false,
)

data class CartMutation(val cartQuantity: Int)

data class ProductInput(
    val productId: String? = null,
    val categoryId: String,
    val subcategoryId: String? = null,
    val name: String,
    val description: String,
    val unit: ProductUnit,
    val priceMinor: Long,
    val offerPriceMinor: Long?,
    val stockQuantity: Double,
    val lowStockThreshold: Double?,
    val attributes: Map<String, String>,
    val imageUploadTokens: List<String> = emptyList(),
    val retainedImageUrls: List<String> = emptyList(),
    val replaceImages: Boolean = false,
    val expectedRevision: Long? = null,
    val active: Boolean = true,
)

data class ProductImageUpload(val uploadToken: String)

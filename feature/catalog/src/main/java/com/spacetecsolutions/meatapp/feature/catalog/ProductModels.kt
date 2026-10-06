package com.spacetecsolutions.meatapp.feature.catalog

import com.spacetecsolutions.meatapp.core.model.*

data class ProductAttributeInput(val key: String = "", val value: String = "")

data class ProductFormState(
    val productId: String? = null,
    val categoryId: String = "",
    val subcategoryId: String? = null,
    val name: String = "",
    val description: String = "",
    val unit: ProductUnit = ProductUnit.KILOGRAM,
    val price: String = "",
    val offerPrice: String = "",
    val stock: String = "",
    val lowStockThreshold: String = "",
    val attributes: List<ProductAttributeInput> = emptyList(),
    val originalImages: List<String> = emptyList(),
    val existingImages: List<String> = emptyList(),
    val selectedImageUris: List<String> = emptyList(),
    val expectedRevision: Long? = null,
    val active: Boolean = true,
    val nameError: String? = null,
    val categoryError: String? = null,
    val priceError: String? = null,
    val offerPriceError: String? = null,
    val stockError: String? = null,
    val attributesError: String? = null,
) {
    val editing: Boolean get() = productId != null
}

data class ProductManagementUiState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val submitting: Boolean = false,
    val products: List<Product> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val subcategories: List<ProductSubcategory> = emptyList(),
    val search: String = "",
    val filter: ProductFilter = ProductFilter.ALL,
    val categoryFilter: String? = null,
    val nextCursor: String? = null,
    val countedProducts: Int = 0,
    val productLimit: Int = 0,
    val error: String? = null,
    val form: ProductFormState? = null,
    val confirmation: Product? = null,
    val deleteConfirmation: Product? = null,
    val busyProductIds: Set<String> = emptySet(),
    val message: ProductMessage? = null,
) {
    val limitReached: Boolean get() = productLimit > 0 && countedProducts >= productLimit
}

data class ProductMessage(val text: String, val success: Boolean)

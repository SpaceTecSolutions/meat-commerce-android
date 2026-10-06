package com.spacetecsolutions.meatapp.feature.catalog

import com.spacetecsolutions.meatapp.core.model.ProductCategory
import com.spacetecsolutions.meatapp.core.model.ProductSubcategory

data class CategoryFormState(
    val categoryId: String? = null,
    val name: String = "",
    val description: String = "",
    val selectedImageUri: String? = null,
    val existingImageUrl: String? = null,
    val expectedRevision: Long? = null,
    val sortOrder: String = "0",
    val active: Boolean = true,
    val nameError: String? = null,
    val sortOrderError: String? = null,
) {
    val editing: Boolean get() = categoryId != null
}

data class CategoryAdminUiState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val categories: List<ProductCategory> = emptyList(),
    val search: String = "",
    val error: String? = null,
    val form: CategoryFormState? = null,
    val confirmation: ProductCategory? = null,
    val deleteConfirmation: ProductCategory? = null,
    val message: CategoryMessage? = null,
)

data class CustomerCategoriesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val categories: List<ProductCategory> = emptyList(),
    val categorySearch: String = "",
    val selectedCategory: ProductCategory? = null,
    val subcategories: List<ProductSubcategory> = emptyList(),
    val selectedSubcategory: ProductSubcategory? = null,
    val loadingSubcategories: Boolean = false,
    val products: List<com.spacetecsolutions.meatapp.core.model.Product> = emptyList(),
    val search: String = "",
    val selectedSubtype: String? = null,
    val nextCursor: String? = null,
    val cartQuantity: Int = 0,
    val cartQuantities: Map<String, Int> = emptyMap(),
    val offersEnabled: Boolean = false,
    val addingProductIds: Set<String> = emptySet(),
    val selectedProduct: com.spacetecsolutions.meatapp.core.model.Product? = null,
    val detailProductId: String? = null,
    val detailLoading: Boolean = false,
    val quantity: Int = 1,
    val offline: Boolean = false,
    val error: String? = null,
    val message: CategoryMessage? = null,
) {
    val visibleCategories: List<ProductCategory>
        get() = categories.filter { it.matchesSearch(categorySearch) }

    val visibleProducts: List<com.spacetecsolutions.meatapp.core.model.Product>
        get() = products.filter { product ->
            product.matchesCustomerSearch(search) &&
                (selectedSubtype == null || product.subtypeLabel()?.equals(selectedSubtype, true) == true)
        }
}

data class CategoryMessage(
    val text: String,
    val success: Boolean,
    val openCart: Boolean = false,
)

object CategoryValidator {
    fun name(value: String): String? = when {
        value.isBlank() -> "Category name is required"
        value.trim().length < 2 -> "Enter a valid category name"
        value.trim().length > 60 -> "Category name is too long"
        else -> null
    }
}

internal fun ProductCategory.matchesSearch(query: String): Boolean =
    query.isBlank() || name.contains(query.trim(), ignoreCase = true)

internal fun List<ProductCategory>.forCustomerDisplay(): List<ProductCategory> =
    filter(ProductCategory::active).sortedWith(
        compareBy<ProductCategory>(ProductCategory::sortOrder)
            .thenBy(String.CASE_INSENSITIVE_ORDER, ProductCategory::name),
    )

internal fun categoryItemCount(count: Int): String =
    "$count ${if (count == 1) "item" else "items"}"

internal fun com.spacetecsolutions.meatapp.core.model.Product.matchesCustomerSearch(query: String): Boolean {
    val term = query.trim()
    return term.isBlank() || name.contains(term, ignoreCase = true) ||
        description.contains(term, ignoreCase = true)
}

internal fun com.spacetecsolutions.meatapp.core.model.Product.subtypeLabel(): String? {
    val entry = attributes.entries.firstOrNull { (key, value) ->
        (key.equals("subtype", true) || key.equals("type", true) || key.equals("cut", true)) &&
            value.isNotBlank()
    } ?: return null
    return entry.value.trim().takeIf(String::isNotEmpty)
}

internal fun List<com.spacetecsolutions.meatapp.core.model.Product>.validForCustomerCategory(
    categoryId: String?,
): List<com.spacetecsolutions.meatapp.core.model.Product> = filter { product ->
    product.active && !product.archived &&
        (categoryId == null || product.categoryId == categoryId)
}

object CustomerProductPolicy {
    fun canAddToCart(product: com.spacetecsolutions.meatapp.core.model.Product) =
        product.active && !product.archived && product.stockQuantity > 0

    fun maxQuantity(product: com.spacetecsolutions.meatapp.core.model.Product) =
        product.stockQuantity.toInt().coerceIn(1, 99)

    fun discountPercent(product: com.spacetecsolutions.meatapp.core.model.Product): Long {
        val offer = product.offerPriceMinor ?: return 0
        if (product.priceMinor <= 0) return 0
        return ((product.priceMinor - offer) * 100 / product.priceMinor).coerceIn(0, 100)
    }
}

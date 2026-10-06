package com.spacetecsolutions.meatapp.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.CategoryRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerCartRepository
import com.spacetecsolutions.meatapp.core.domain.repository.ProductRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CatalogContentRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class CustomerCategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val cartRepository: CustomerCartRepository,
    private val contentRepository: CatalogContentRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CustomerCategoriesUiState())
    val state = mutableState.asStateFlow()
    init { refreshCategories(initial = true) }

    fun refresh() {
        if (state.value.selectedCategory == null) refreshCategories(initial = false)
        else if (state.value.selectedSubcategory == null && state.value.subcategories.isEmpty()) {
            state.value.selectedCategory?.let { selectCategory(it.id, it.name) }
        } else loadProducts(reset = true, showRefresh = true)
    }

    fun updateCategorySearch(value: String) = mutableState.update { it.copy(categorySearch = value) }

    fun selectCategory(categoryId: String, categoryName: String) = viewModelScope.launch {
        val category = state.value.categories.firstOrNull { it.id == categoryId }
            ?: ProductCategory(id = categoryId, name = categoryName)
        mutableState.update {
            it.copy(
                selectedCategory = category,
                subcategories = emptyList(), selectedSubcategory = null,
                loadingSubcategories = true,
                categorySearch = "",
                search = "",
                selectedSubtype = null,
                products = emptyList(),
                nextCursor = null,
                offline = false,
                error = null,
            )
        }
        when (val result = contentRepository.getSubcategories(categoryId, admin = false)) {
            is AppResult.Success -> {
                if (state.value.selectedCategory?.id != categoryId) return@launch
                if (result.value.isNotEmpty()) mutableState.update {
                    it.copy(loadingSubcategories = false,
                        subcategories = result.value.sortedBy(ProductSubcategory::sortOrder))
                } else {
                    mutableState.update { it.copy(loadingSubcategories = false) }
                    loadProducts(true)
                }
            }
            is AppResult.Failure -> {
                if (state.value.selectedCategory?.id != categoryId) return@launch
                mutableState.update { it.copy(loadingSubcategories = false) }
                loadProducts(true)
            }
        }
    }

    fun selectSubcategory(value: ProductSubcategory) {
        mutableState.update { it.copy(selectedSubcategory = value, products = emptyList(), nextCursor = null) }
        loadProducts(true)
    }
    fun showSubcategories() = mutableState.update { it.copy(selectedSubcategory = null, products = emptyList(), nextCursor = null) }

    fun searchAllProducts(query: String) {
        val term = query.trim()
        if (term.isEmpty()) return
        mutableState.update {
            it.copy(
                selectedCategory = ProductCategory(id = "", name = "Search Results"),
                categorySearch = "",
                search = term,
                selectedSubtype = null,
                products = emptyList(),
                nextCursor = null,
                offline = false,
                error = null,
            )
        }
        loadProducts(true)
    }

    fun showCategories() {
        mutableState.update {
            it.copy(
                selectedCategory = null,
                subcategories = emptyList(), selectedSubcategory = null,
                products = emptyList(),
                search = "",
                selectedSubtype = null,
                nextCursor = null,
                error = null,
            )
        }
    }

    fun openProduct(productId: String) = viewModelScope.launch {
        mutableState.update {
            it.copy(
                detailProductId = productId,
                detailLoading = true,
                selectedProduct = null,
                quantity = 1,
                offline = false,
                error = null,
            )
        }
        when (val result = productRepository.getCustomerProduct(productId)) {
            is AppResult.Success -> mutableState.update {
                it.copy(detailLoading = false, selectedProduct = result.value)
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(
                    detailLoading = false,
                    offline = result.error == AppError.Network || result.error == AppError.Offline,
                    error = result.error.displayMessage(),
                )
            }
        }
    }

    fun retryProductDetails() = state.value.detailProductId?.let(::openProduct)

    fun closeProduct() = mutableState.update {
        it.copy(
            selectedProduct = null,
            detailProductId = null,
            detailLoading = false,
            quantity = 1,
            offline = false,
            error = null,
        )
    }

    fun changeQuantity(delta: Int) = mutableState.update { state ->
        val max = state.selectedProduct?.let(CustomerProductPolicy::maxQuantity) ?: 1
        state.copy(quantity = (state.quantity + delta).coerceIn(1, max))
    }

    fun updateSearch(value: String) {
        mutableState.update { it.copy(search = value) }
    }

    fun updateSubtype(value: String?) = mutableState.update { it.copy(selectedSubtype = value) }

    fun loadMore() {
        if (!state.value.loadingMore && state.value.nextCursor != null) loadProducts(false)
    }

    fun addToCart(product: Product) = viewModelScope.launch {
        if (!CustomerProductPolicy.canAddToCart(product) || product.id in state.value.addingProductIds) return@launch
        mutableState.update { it.copy(addingProductIds = it.addingProductIds + product.id) }
        val quantity = if (state.value.selectedProduct?.id == product.id) state.value.quantity else 1
        val previousQuantity = state.value.cartQuantities[product.id] ?: 0
        when (val result = productRepository.addProductToCart(product.id, quantity)) {
            is AppResult.Success -> mutableState.update {
                it.copy(
                    cartQuantity = result.value.cartQuantity,
                    cartQuantities = it.cartQuantities + (product.id to previousQuantity + quantity),
                    addingProductIds = it.addingProductIds - product.id,
                    message = CategoryMessage("Product added to cart", true),
                )
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(
                    addingProductIds = it.addingProductIds - product.id,
                    message = CategoryMessage(result.error.addToCartMessage(), false),
                )
            }
        }
    }

    fun changeProductCartQuantity(product: Product, delta: Int) = viewModelScope.launch {
        if (product.id in state.value.addingProductIds) return@launch
        val current = state.value.cartQuantities[product.id] ?: 0
        if (current == 0 && delta > 0) {
            addToCart(product)
            return@launch
        }
        val next = current + delta
        mutableState.update { it.copy(addingProductIds = it.addingProductIds + product.id) }
        val result = if (next <= 0) cartRepository.remove(product.id)
        else cartRepository.setQuantity(product.id, next)
        when (result) {
            is AppResult.Success -> mutableState.update {
                val quantities = result.value.lines.associate { line -> line.productId to line.quantity }
                it.copy(
                    cartQuantity = result.value.distinctItemCount,
                    cartQuantities = quantities,
                    addingProductIds = it.addingProductIds - product.id,
                    message = CategoryMessage(if (next <= 0) "Item removed from cart" else "Cart updated", true),
                )
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(addingProductIds = it.addingProductIds - product.id,
                    message = CategoryMessage(result.error.addToCartMessage(), false))
            }
        }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun refreshCategories(initial: Boolean) = viewModelScope.launch {
        mutableState.update {
            it.copy(
                loading = initial && it.categories.isEmpty(),
                refreshing = !initial || it.categories.isNotEmpty(),
                offline = false,
                error = null,
            )
        }
        when (val result = categoryRepository.getActiveCategories()) {
            is AppResult.Success -> mutableState.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    offline = false,
                    categories = result.value.forCustomerDisplay(),
                )
            }
            is AppResult.Failure -> mutableState.update {
                val message = result.error.displayMessage()
                val offline = result.error == AppError.Network || result.error == AppError.Offline
                if (it.categories.isEmpty()) {
                    it.copy(loading = false, refreshing = false, offline = offline, error = message)
                } else {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        offline = offline,
                        error = null,
                        message = CategoryMessage(message, false),
                    )
                }
            }
        }
    }

    private fun loadProducts(reset: Boolean, showRefresh: Boolean = false) = viewModelScope.launch {
        val snapshot = state.value
        if (reset) mutableState.update {
            it.copy(
                loading = !showRefresh && it.products.isEmpty(),
                refreshing = showRefresh || it.products.isNotEmpty(),
                offline = false,
                error = null,
            )
        }
        else mutableState.update { it.copy(loadingMore = true) }
        val query = CustomerProductQuery(
            categoryId = snapshot.selectedCategory?.id?.takeIf(String::isNotBlank),
            subcategoryId = snapshot.selectedSubcategory?.id,
            search = "",
            filter = CustomerProductFilter.ALL,
            cursor = if (reset) null else snapshot.nextCursor,
            pageSize = 40,
        )
        when (val result = productRepository.getCustomerProducts(query)) {
            is AppResult.Success -> mutableState.update {
                if (it.selectedCategory?.id != snapshot.selectedCategory?.id ||
                    it.selectedSubcategory?.id != snapshot.selectedSubcategory?.id) return@update it
                val categoryId = snapshot.selectedCategory?.id?.takeIf(String::isNotBlank)
                val validProducts = result.value.products.validForCustomerCategory(categoryId)
                it.copy(
                    loading = false, refreshing = false, loadingMore = false, offline = false,
                    products = if (reset) validProducts else it.products + validProducts,
                    nextCursor = result.value.nextCursor,
                    cartQuantity = result.value.cartQuantity,
                    cartQuantities = result.value.cartQuantities,
                    offersEnabled = result.value.offersEnabled,
                )
            }
            is AppResult.Failure -> mutableState.update {
                val message = result.error.displayMessage()
                val offline = result.error == AppError.Network || result.error == AppError.Offline
                if (it.products.isEmpty()) {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                        offline = offline,
                        error = message,
                    )
                } else {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                        offline = offline,
                        error = null,
                        message = CategoryMessage(message, false),
                    )
                }
            }
        }
    }
}

private fun AppError.displayMessage() = when (this) {
    AppError.Network, AppError.Offline -> "You're offline. Check your connection and try again."
    AppError.NotFound -> "This product is no longer available"
    is AppError.Validation -> message
    else -> "Something went wrong. Please try again."
}

private fun AppError.addToCartMessage() = when (this) {
    AppError.Network, AppError.Offline, AppError.Timeout ->
        "Unable to add item while offline. Check your connection and try again."
    AppError.Unauthorized -> "Please sign in again to add items to your cart."
    AppError.NotFound -> "This product is no longer available."
    is AppError.Validation -> message
    else -> "Unable to add item to cart. Please try again."
}

package com.spacetecsolutions.meatapp.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.CategoryRepository
import com.spacetecsolutions.meatapp.core.domain.repository.ProductRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CatalogContentRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductManagementViewModel @Inject constructor(
    private val productsRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val contentRepository: CatalogContentRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProductManagementUiState())
    val state = mutableState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadCategories()
        loadProducts(reset = true)
    }

    fun refresh() {
        loadCategories()
        loadProducts(reset = true)
    }

    fun updateSearch(value: String) {
        mutableState.update { it.copy(search = value.take(80)) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadProducts(reset = true)
        }
    }
    fun selectFilter(value: ProductFilter) {
        mutableState.update { it.copy(filter = value) }
        loadProducts(reset = true)
    }
    fun selectCategoryFilter(value: String?) {
        mutableState.update { it.copy(categoryFilter = value) }
        loadProducts(reset = true)
    }
    fun loadMore() {
        if (state.value.nextCursor != null && !state.value.loadingMore) loadProducts(reset = false)
    }

    fun openCreate() {
        if (state.value.limitReached) {
            mutableState.update {
                it.copy(message = ProductMessage("Product limit reached", false))
            }
        } else mutableState.update { it.copy(form = ProductFormState()) }
    }

    fun openEdit(product: Product) {
        mutableState.update {
        it.copy(form = ProductFormState(
            productId = product.id,
            categoryId = product.categoryId,
            subcategoryId = product.subcategoryId,
            name = product.name,
            description = product.description,
            unit = product.unit,
            price = product.priceMinor.moneyInput(),
            offerPrice = product.offerPriceMinor?.moneyInput().orEmpty(),
            stock = product.stockQuantity.cleanNumber(),
            lowStockThreshold = product.lowStockThreshold?.cleanNumber().orEmpty(),
            attributes = product.attributes.map { ProductAttributeInput(it.key, it.value) },
            originalImages = product.imageUrls,
            existingImages = product.imageUrls,
            expectedRevision = product.revision,
            active = product.active,
        ))
        }
        loadSubcategories(product.categoryId)
    }
    fun closeForm() = mutableState.update { if (it.submitting) it else it.copy(form = null) }
    fun updateName(value: String) = updateForm { copy(name = value.take(100), nameError = null) }
    fun updateDescription(value: String) = updateForm { copy(description = value.take(1000)) }
    fun updateCategory(value: String) {
        updateForm { copy(categoryId = value, subcategoryId = null, categoryError = null) }
        loadSubcategories(value)
    }
    fun updateSubcategory(value: String?) = updateForm { copy(subcategoryId = value) }
    fun updateUnit(value: ProductUnit) = updateForm { copy(unit = value) }
    fun updatePrice(value: String) = updateForm { copy(price = value.take(12), priceError = null) }
    fun updateOfferPrice(value: String) = updateForm {
        copy(offerPrice = value.take(12), offerPriceError = null)
    }
    fun updateStock(value: String) = updateForm { copy(stock = value.take(12), stockError = null) }
    fun updateLowStock(value: String) = updateForm { copy(lowStockThreshold = value.take(12)) }
    fun updateActive(value: Boolean) = updateForm { copy(active = value) }
    fun selectImages(uris: List<String>) = updateForm {
        copy(selectedImageUris = uris.distinct().take((MAX_IMAGES - existingImages.size).coerceAtLeast(0)))
    }
    fun removeImage(index: Int) = updateForm {
        when {
            index < 0 -> this
            index < existingImages.size -> copy(existingImages = existingImages.filterIndexed { i, _ -> i != index })
            index - existingImages.size < selectedImageUris.size -> copy(
                selectedImageUris = selectedImageUris.filterIndexed { i, _ -> i != index - existingImages.size },
            )
            else -> this
        }
    }
    fun addAttribute() = updateForm {
        if (attributes.size >= MAX_ATTRIBUTES) this else copy(attributes = attributes + ProductAttributeInput())
    }
    fun removeAttribute(index: Int) = updateForm {
        copy(attributes = attributes.filterIndexed { itemIndex, _ -> itemIndex != index })
    }
    fun updateAttribute(index: Int, key: String? = null, value: String? = null) = updateForm {
        copy(
            attributes = attributes.mapIndexed { itemIndex, attribute ->
                if (itemIndex == index) attribute.copy(
                    key = key?.take(30) ?: attribute.key,
                    value = value?.take(100) ?: attribute.value,
                ) else attribute
            },
            attributesError = null,
        )
    }

    fun save() {
        val form = state.value.form ?: return
        val validation = ProductValidator.validate(form)
        val threshold = form.lowStockThreshold.takeIf(String::isNotBlank)?.toDoubleOrNull()
        val thresholdInvalid = form.lowStockThreshold.isNotBlank() && (threshold == null || threshold < 0)
        if (!validation.valid || thresholdInvalid) {
            mutableState.update { it.copy(form = form.withErrors(validation).copy(
                stockError = if (thresholdInvalid) "Enter a valid low-stock threshold"
                else validation.stockError,
            )) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            val tokens = mutableListOf<String>()
            for (uri in form.selectedImageUris) {
                when (val upload = productsRepository.uploadProductImage(uri)) {
                    is AppResult.Success -> tokens += upload.value.uploadToken
                    is AppResult.Failure -> {
                        mutationFailed(upload.error)
                        return@launch
                    }
                }
            }
            val input = form.toInput(tokens)
            val result = if (form.editing) productsRepository.updateProduct(input)
            else productsRepository.createProduct(input)
            handleMutation(result, if (form.editing) "Product updated" else "Product created")
        }
    }

    fun requestStatusChange(product: Product) = mutableState.update { it.copy(confirmation = product) }
    fun dismissConfirmation() = mutableState.update {
        if (it.submitting) it else it.copy(confirmation = null)
    }
    fun confirmStatusChange() {
        val product = state.value.confirmation ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            handleMutation(
                productsRepository.setProductActive(product.id, !product.active, product.revision),
                if (product.active) "Product deactivated" else "Product activated",
            )
        }
    }
    fun toggleStatus(product: Product) {
        if (product.id in state.value.busyProductIds) return
        viewModelScope.launch {
            mutableState.update { it.copy(busyProductIds = it.busyProductIds + product.id) }
            when (val result = productsRepository.setProductActive(product.id, !product.active, product.revision)) {
                is AppResult.Success -> mutableState.update { state -> state.copy(
                    products = state.products.map { if (it.id == result.value.id) result.value else it },
                    busyProductIds = state.busyProductIds - product.id,
                    message = ProductMessage(if (result.value.active) "Product enabled" else "Product disabled", true),
                ) }
                is AppResult.Failure -> mutableState.update { it.copy(
                    busyProductIds = it.busyProductIds - product.id,
                    message = ProductMessage(result.error.productMessage(), false),
                ) }
            }
        }
    }
    fun requestDelete(product: Product) = mutableState.update { it.copy(deleteConfirmation = product) }
    fun dismissDelete() = mutableState.update { if (it.submitting) it else it.copy(deleteConfirmation = null) }
    fun confirmDelete() {
        val product = state.value.deleteConfirmation ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            when (val result = productsRepository.archiveProduct(product.id, product.revision)) {
                is AppResult.Success -> mutableState.update { current -> current.copy(
                    submitting = false,
                    products = current.products.filterNot { it.id == product.id },
                    countedProducts = (current.countedProducts - 1).coerceAtLeast(0),
                    deleteConfirmation = null,
                    message = ProductMessage("Product deleted", true),
                ) }
                is AppResult.Failure -> mutationFailed(result.error)
            }
        }
    }
    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun loadCategories() = viewModelScope.launch {
        val result = categoryRepository.getAdminCategories()
        if (result is AppResult.Success) {
            mutableState.update { it.copy(categories = result.value.sortedBy(ProductCategory::sortOrder)) }
        }
    }

    private fun loadSubcategories(categoryId: String) = viewModelScope.launch {
        when (val result = contentRepository.getSubcategories(categoryId, admin = true)) {
            is AppResult.Success -> mutableState.update { it.copy(subcategories = result.value) }
            is AppResult.Failure -> mutableState.update { it.copy(subcategories = emptyList()) }
        }
    }

    private fun loadProducts(reset: Boolean) = viewModelScope.launch {
        val snapshot = state.value
        mutableState.update {
            if (reset) it.copy(loading = true, error = null) else it.copy(loadingMore = true)
        }
        val query = ProductQuery(
            search = snapshot.search,
            filter = snapshot.filter,
            categoryId = snapshot.categoryFilter,
            cursor = if (reset) null else snapshot.nextCursor,
        )
        when (val result = productsRepository.getAdminProducts(query)) {
            is AppResult.Success -> mutableState.update {
                it.copy(
                    loading = false,
                    loadingMore = false,
                    products = if (reset) result.value.products else it.products + result.value.products,
                    nextCursor = result.value.nextCursor,
                    countedProducts = result.value.countedProducts,
                    productLimit = result.value.productLimit,
                )
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, loadingMore = false, error = result.error.productMessage())
            }
        }
    }

    private fun updateForm(transform: ProductFormState.() -> ProductFormState) =
        mutableState.update { state -> state.form?.let { state.copy(form = it.transform()) } ?: state }

    private fun handleMutation(result: AppResult<Product>, message: String) {
        when (result) {
            is AppResult.Success -> mutableState.update { state ->
                val existed = state.products.any { it.id == result.value.id }
                state.copy(
                    submitting = false,
                    products = listOf(result.value) + state.products.filterNot { it.id == result.value.id },
                    countedProducts = state.countedProducts + if (existed) 0 else 1,
                    form = null,
                    confirmation = null,
                    message = ProductMessage(message, true),
                )
            }
            is AppResult.Failure -> mutationFailed(result.error)
        }
    }

    private fun mutationFailed(error: AppError) = mutableState.update {
        it.copy(submitting = false, message = ProductMessage(error.productMessage(), false))
    }

    private companion object { const val MAX_IMAGES = 5; const val MAX_ATTRIBUTES = 20 }
}

private fun ProductFormState.withErrors(value: ProductValidation) = copy(
    nameError = value.nameError,
    categoryError = value.categoryError,
    priceError = value.priceError,
    offerPriceError = value.offerPriceError,
    stockError = value.stockError,
    attributesError = value.attributesError,
)

private fun ProductFormState.toInput(tokens: List<String>) = ProductInput(
    productId = productId, categoryId = categoryId, subcategoryId = subcategoryId,
    name = name.trim(), description = description.trim(), unit = unit,
    priceMinor = price.toMinorUnits()!!, offerPriceMinor = offerPrice.takeIf(String::isNotBlank)?.toMinorUnits(),
    stockQuantity = stock.toDouble(), lowStockThreshold = lowStockThreshold.takeIf(String::isNotBlank)?.toDouble(),
    attributes = attributes.filter { it.key.isNotBlank() }.associate { it.key.trim() to it.value.trim() },
    imageUploadTokens = tokens, retainedImageUrls = existingImages,
    replaceImages = selectedImageUris.isNotEmpty() || existingImages != originalImages,
    expectedRevision = expectedRevision, active = active,
)

private fun Long.moneyInput() = BigDecimal(this).movePointLeft(2).stripTrailingZeros().toPlainString()
private fun Double.cleanNumber() = BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

private fun AppError.productMessage(): String = when (this) {
    AppError.Forbidden -> "Only an authorized Admin can manage products"
    AppError.Unauthorized -> "Your session or app verification expired. Sign in again"
    AppError.NotFound -> "The product or category no longer exists. Refresh and try again"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    is AppError.Unknown -> "Unable to reach the product service. Install the latest app and try again"
    else -> "Unable to complete the product request"
}

package com.spacetecsolutions.meatapp.feature.admin.banner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.BannerRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BannerForm(
    val id: String? = null, val revision: Long? = null, val title: String = "",
    val subtitle: String = "", val existingImage: String? = null, val selectedImage: String? = null,
    val alignment: BannerContentAlignment = BannerContentAlignment.BOTTOM_START,
    val buttonEnabled: Boolean = false, val buttonText: String = "Shop Now",
    val productId: String = "", val active: Boolean = true, val sortOrder: String = "0",
    val categoryId: String = "",
    val textPlacement: BannerPlacement = BannerPlacement(0f, .15f),
    val buttonPlacement: BannerPlacement = BannerPlacement(0f, .95f),
    val subtitlePlacement: BannerPlacement = BannerPlacement(0f, .45f),
    val buttonColor: String = "RED", val buttonTextColor: String = "WHITE", val buttonShape: String = "ROUNDED",
    val buttonArrow: Boolean = false,
)

data class BannerUiState(
    val loading: Boolean = true, val banners: List<PromotionBanner> = emptyList(),
    val form: BannerForm? = null, val busy: Boolean = false, val deletingId: String? = null,
    val error: String? = null, val message: String? = null,
    val products: List<Product> = emptyList(),
)

@HiltViewModel
class BannerManagementViewModel @Inject constructor(
    private val repository: BannerRepository,
    private val products: com.spacetecsolutions.meatapp.core.domain.repository.ProductRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BannerUiState())
    val state: StateFlow<BannerUiState> = mutableState.asStateFlow()
    init {
        refresh()
        viewModelScope.launch {
            when (val result = products.getAdminProducts(ProductQuery(pageSize = 500))) {
                is AppResult.Success -> mutableState.update { it.copy(products = result.value.products) }
                is AppResult.Failure -> mutableState.update { it.copy(message = "Unable to load products for banner selection") }
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.getBanners()) {
            is AppResult.Success -> mutableState.update {
                it.copy(loading = false, banners = result.value.sortedBy(PromotionBanner::sortOrder))
            }
            is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error.text()) }
        }
    }

    fun edit(banner: PromotionBanner?) {
        if (banner == null && state.value.banners.size >= 5) {
            mutableState.update { it.copy(message = "Maximum 5 banners allowed") }; return
        }
        mutableState.update { it.copy(form = banner?.toForm() ?: BannerForm(sortOrder = it.banners.size.toString())) }
    }
    fun update(transform: (BannerForm) -> BannerForm) = mutableState.update { state ->
        state.form?.let { state.copy(form = transform(it), message = null) } ?: state
    }
    fun closeForm() = mutableState.update { it.copy(form = null) }
    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    fun save() = viewModelScope.launch {
        val form = state.value.form ?: return@launch
        if (form.title.isBlank() || (form.existingImage == null && form.selectedImage == null)) {
            mutableState.update { it.copy(message = "Add a banner image and title") }; return@launch
        }
        mutableState.update { it.copy(busy = true) }
        val token = form.selectedImage?.let { uri ->
            when (val upload = repository.uploadImage(uri)) {
                is AppResult.Success -> upload.value.token
                is AppResult.Failure -> { fail(upload.error.text()); return@launch }
            }
        }
        val input = BannerInput(
            bannerId = form.id, title = form.title.trim(), subtitle = form.subtitle.trim(),
            imageUploadToken = token, actionRoute = if (!form.buttonEnabled) null
            else form.productId.trim().takeIf(String::isNotBlank)?.let { "product:$it" }
                ?: form.categoryId.takeIf(String::isNotBlank)?.let { "category:$it" } ?: "products",
            active = form.active, sortOrder = form.sortOrder.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            contentAlignment = form.alignment, buttonEnabled = form.buttonEnabled,
            buttonText = form.buttonText.trim().ifBlank { "Shop Now" }, expectedRevision = form.revision,
            textPlacement = form.textPlacement, buttonPlacement = form.buttonPlacement,
            subtitlePlacement = form.subtitlePlacement, buttonColor = form.buttonColor,
            buttonTextColor = form.buttonTextColor,
            buttonShape = form.buttonShape, buttonArrow = form.buttonArrow,
        )
        when (val result = repository.save(input)) {
            is AppResult.Success -> mutableState.update { current -> current.copy(
                busy = false, form = null,
                banners = (current.banners.filterNot { it.id == result.value.id } + result.value)
                    .sortedBy(PromotionBanner::sortOrder), message = "Banner saved",
            ) }
            is AppResult.Failure -> fail(result.error.text())
        }
    }

    fun toggle(banner: PromotionBanner) = mutate(banner.id) {
        repository.setActive(banner.id, !banner.active, banner.revision)
    }
    fun delete(banner: PromotionBanner) = viewModelScope.launch {
        mutableState.update { it.copy(deletingId = banner.id) }
        when (val result = repository.delete(banner.id, banner.revision)) {
            is AppResult.Success -> mutableState.update {
                it.copy(deletingId = null, banners = it.banners.filterNot { value -> value.id == banner.id },
                    message = "Banner deleted")
            }
            is AppResult.Failure -> fail(result.error.text())
        }
    }
    private fun mutate(id: String, block: suspend () -> AppResult<PromotionBanner>) = viewModelScope.launch {
        mutableState.update { it.copy(deletingId = id) }
        when (val result = block()) {
            is AppResult.Success -> mutableState.update { current -> current.copy(deletingId = null,
                banners = current.banners.map { if (it.id == id) result.value else it }, message = "Banner updated") }
            is AppResult.Failure -> fail(result.error.text())
        }
    }
    private fun fail(message: String) = mutableState.update { it.copy(busy = false, deletingId = null, message = message) }
}

private fun PromotionBanner.toForm() = BannerForm(
    id, revision, title, subtitle, imageUrl, alignment = contentAlignment,
    buttonEnabled = buttonEnabled, buttonText = buttonText,
    productId = actionRoute?.takeIf { it.startsWith("product:") }?.removePrefix("product:").orEmpty(),
    categoryId = actionRoute?.takeIf { it.startsWith("category:") }?.removePrefix("category:").orEmpty(),
    active = active, sortOrder = sortOrder.toString(),
    textPlacement = textPlacement ?: BannerPlacement(0f, .15f),
    buttonPlacement = buttonPlacement ?: BannerPlacement(0f, .95f),
    subtitlePlacement = subtitlePlacement ?: BannerPlacement(0f, .45f),
    buttonColor = buttonColor, buttonTextColor = buttonTextColor,
    buttonShape = buttonShape, buttonArrow = buttonArrow,
)
private fun AppError.text() = when (this) {
    is AppError.Validation -> message
    AppError.Network, AppError.Offline -> "Unable to connect. Try again."
    AppError.Forbidden -> "Banner management is not permitted"
    else -> "Unable to update banners"
}

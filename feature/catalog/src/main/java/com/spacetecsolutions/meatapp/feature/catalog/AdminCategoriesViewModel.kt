package com.spacetecsolutions.meatapp.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.CategoryRepository
import com.spacetecsolutions.meatapp.core.model.CategoryInput
import com.spacetecsolutions.meatapp.core.model.ProductCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminCategoriesViewModel @Inject constructor(
    private val repository: CategoryRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CategoryAdminUiState())
    val state = mutableState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = it.categories.isEmpty(), error = null) }
        when (val result = repository.getAdminCategories()) {
            is AppResult.Success -> mutableState.update {
                it.copy(loading = false, categories = result.value.sortedBy(ProductCategory::sortOrder))
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, error = result.error.message())
            }
        }
    }

    fun openCreate() = mutableState.update { it.copy(form = CategoryFormState()) }
    fun updateSearch(value: String) = mutableState.update { it.copy(search = value.take(60)) }
    fun openEdit(category: ProductCategory) = mutableState.update {
        it.copy(form = CategoryFormState(
            categoryId = category.id,
            name = category.name,
            description = category.description,
            existingImageUrl = category.imageUrl,
            expectedRevision = category.revision,
            sortOrder = category.sortOrder.toString(),
            active = category.active,
        ))
    }
    fun closeForm() = mutableState.update { if (it.submitting) it else it.copy(form = null) }
    fun updateName(value: String) = updateForm { copy(name = value, nameError = null) }
    fun updateDescription(value: String) = updateForm { copy(description = value.take(200)) }
    fun selectImage(uri: String) = updateForm { copy(selectedImageUri = uri) }
    fun updateSortOrder(value: String) = updateForm { copy(sortOrder = value.take(5), sortOrderError = null) }
    fun updateActive(value: Boolean) = updateForm { copy(active = value) }

    fun save() {
        val form = state.value.form ?: return
        val validation = CategoryValidator.name(form.name)
        val sortOrder = form.sortOrder.toIntOrNull()
        if (validation != null || sortOrder == null || sortOrder < 0) {
            mutableState.update { it.copy(form = form.copy(
                nameError = validation,
                sortOrderError = if (sortOrder == null || sortOrder < 0) "Enter a valid display order" else null,
            )) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            val upload = form.selectedImageUri?.let { repository.uploadCategoryImage(it) }
            if (upload is AppResult.Failure) {
                mutationFailed(upload.error)
                return@launch
            }
            val input = CategoryInput(
                categoryId = form.categoryId,
                name = form.name.trim(),
                description = form.description.trim(),
                imageUploadToken = (upload as? AppResult.Success)?.value?.uploadToken,
                expectedRevision = form.expectedRevision,
                sortOrder = sortOrder,
                active = form.active,
            )
            val result = if (form.editing) repository.updateCategory(input)
            else repository.createCategory(input)
            handleCategoryMutation(result, if (form.editing) "Category updated" else "Category created")
        }
    }

    fun requestStatusChange(category: ProductCategory) = mutableState.update {
        it.copy(confirmation = category)
    }
    fun dismissConfirmation() = mutableState.update {
        if (it.submitting) it else it.copy(confirmation = null)
    }
    fun confirmStatusChange() {
        val category = state.value.confirmation ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            handleCategoryMutation(
                repository.setCategoryActive(category.id, !category.active, category.revision),
                if (category.active) "Category deactivated" else "Category activated",
            )
        }
    }

    fun requestDelete(category: ProductCategory) = mutableState.update { it.copy(deleteConfirmation = category) }
    fun dismissDelete() = mutableState.update { if (it.submitting) it else it.copy(deleteConfirmation = null) }
    fun confirmDelete() {
        val category = state.value.deleteConfirmation ?: return
        if (category.productCount > 0) return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            when (val result = repository.deleteCategory(category.id, category.revision)) {
                is AppResult.Success -> mutableState.update { it.copy(
                    submitting = false,
                    categories = it.categories.filterNot { item -> item.id == category.id },
                    deleteConfirmation = null,
                    message = CategoryMessage("Category deleted", true),
                ) }
                is AppResult.Failure -> mutationFailed(result.error)
            }
        }
    }

    fun move(categoryId: String, offset: Int) {
        val current = state.value.categories
        val from = current.indexOfFirst { it.id == categoryId }
        val to = from + offset
        if (from !in current.indices || to !in current.indices || state.value.submitting) return
        val reordered = current.toMutableList().apply { add(to, removeAt(from)) }
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            when (val result = repository.updateCategoryOrder(reordered.map(ProductCategory::id))) {
                is AppResult.Success -> mutableState.update {
                    it.copy(submitting = false, categories = result.value.sortedBy(ProductCategory::sortOrder))
                }
                is AppResult.Failure -> mutationFailed(result.error)
            }
        }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun updateForm(transform: CategoryFormState.() -> CategoryFormState) =
        mutableState.update { state -> state.form?.let { state.copy(form = it.transform()) } ?: state }

    private fun handleCategoryMutation(result: AppResult<ProductCategory>, message: String) {
        when (result) {
            is AppResult.Success -> mutableState.update { state ->
                val list = (state.categories.filterNot { it.id == result.value.id } + result.value)
                    .sortedBy(ProductCategory::sortOrder)
                state.copy(
                    submitting = false,
                    categories = list,
                    form = null,
                    confirmation = null,
                    message = CategoryMessage(message, true),
                )
            }
            is AppResult.Failure -> mutationFailed(result.error)
        }
    }

    private fun mutationFailed(error: AppError) = mutableState.update {
        it.copy(submitting = false, message = CategoryMessage(error.message(), false))
    }
}

internal fun AppError.message(): String = when (this) {
    AppError.Forbidden -> "Only an authorized Admin can manage categories"
    AppError.Unauthorized -> "Your session or app verification expired. Sign in again"
    AppError.NotFound -> "The category no longer exists. Refresh and try again"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    is AppError.Unknown -> "Unable to reach the category service. Install the latest app and try again"
    else -> "Unable to complete the category request"
}

package com.spacetecsolutions.meatapp.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerCartRepository
import com.spacetecsolutions.meatapp.core.model.CustomerCart
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repository: CustomerCartRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CartUiState())
    val state = mutableState.asStateFlow()

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        handle(repository.getCart(), null)
    }

    fun setQuantity(productId: String, quantity: Int) = mutate(productId) {
        repository.setQuantity(productId, quantity)
    }

    fun remove(productId: String) = mutate(productId, "Item removed from cart") { repository.remove(productId) }
    fun toggleEditing() = mutableState.update { it.copy(editing = !it.editing) }
    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun mutate(
        productId: String,
        successMessage: String? = "Quantity updated",
        block: suspend () -> AppResult<CustomerCart>,
    ) =
        viewModelScope.launch {
            if (productId in state.value.busyProductIds) return@launch
            mutableState.update { it.copy(busyProductIds = it.busyProductIds + productId) }
            handle(block(), productId, successMessage)
        }

    private fun handle(result: AppResult<CustomerCart>, productId: String?, successMessage: String? = null) {
        when (result) {
            is AppResult.Success -> mutableState.update {
                it.copy(
                    loading = false, cart = result.value,
                    busyProductIds = productId?.let(it.busyProductIds::minus) ?: emptySet(),
                    message = result.value.adjustments.firstOrNull()?.let { CartMessage(it.message, false) }
                        ?: successMessage?.let { CartMessage(it, true) },
                )
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(
                    loading = false,
                    busyProductIds = productId?.let(it.busyProductIds::minus) ?: emptySet(),
                    error = if (productId == null) result.error.displayMessage() else it.error,
                    message = if (productId != null) CartMessage(result.error.displayMessage(), false) else it.message,
                )
            }
        }
    }
}

private fun AppError.displayMessage() = when (this) {
    AppError.Network, AppError.Offline -> "You're offline. Your saved cart is safe."
    AppError.NotFound -> "A product is no longer available. Refresh your cart."
    is AppError.Validation -> message
    else -> "We couldn't update your cart. Please try again."
}

package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerHomeRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerCartRepository
import com.spacetecsolutions.meatapp.core.domain.repository.ProductRepository
import com.spacetecsolutions.meatapp.core.domain.repository.AuthenticationRepository
import com.spacetecsolutions.meatapp.core.domain.repository.FeatureManagementRepository
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.spacetecsolutions.meatapp.core.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CustomerHomeViewModel @Inject constructor(
    private val repository: CustomerHomeRepository,
    private val messaging: com.spacetecsolutions.meatapp.core.domain.repository.MessagingRepository,
    private val products: ProductRepository,
    private val cart: CustomerCartRepository,
    private val auth: AuthenticationRepository,
    private val features: FeatureManagementRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CustomerHomeUiState())
    val state = mutableState.asStateFlow()

    private var unreadCount: Int? = null
    init {
        load()
        viewModelScope.launch { auth.authenticatedUserId.flatMapLatest { uid ->
            if (uid == null) flowOf(null)
            else features.featureConfig.flatMapLatest { config ->
                if ((config as? AppResult.Success)?.value?.inAppNotificationsEnabled == true)
                    messaging.observeUnreadCount() else flowOf(null)
            }
        }.collect { result ->
            if (result is AppResult.Success) {
                unreadCount = result.value
                mutableState.update { it.copy(data = it.data?.copy(unreadNotifications = result.value)) }
            } else { unreadCount = 0; mutableState.update { it.copy(data = it.data?.copy(unreadNotifications = 0)) } }
        } }
    }

    fun refresh() = load(refreshing = true)

    /** One cart snapshot on Home entry, never one read per product/card. */
    fun refreshCart() = viewModelScope.launch {
        if (mutableState.value.updatingProductIds.isNotEmpty()) return@launch
        when (val result = cart.getCart()) {
            is AppResult.Success -> mutableState.update { current -> current.copy(
                cartQuantities = result.value.lines.associate { it.productId to it.quantity },
                cart = result.value,
                cartLoaded = true,
                data = current.data?.copy(cartQuantity = result.value.distinctItemCount),
            ) }
            is AppResult.Failure -> Unit // Home remains usable with server-provided badge state.
        }
    }

    fun addToCart(product: Product) = viewModelScope.launch {
        if (!product.active || product.archived || product.stockQuantity <= 0 ||
            product.id in state.value.updatingProductIds) return@launch
        mutableState.update { it.copy(updatingProductIds = it.updatingProductIds + product.id) }
        val result = products.addProductToCart(product.id, 1)
        when (result) {
            is AppResult.Success -> mutableState.update { current -> current.copy(
                updatingProductIds = current.updatingProductIds - product.id,
                cartQuantities = current.cartQuantities +
                    (product.id to (current.cartQuantities[product.id] ?: 0) + 1),
                cartLoaded = true,
                data = current.data?.copy(cartQuantity = result.value.cartQuantity),
            ) }
            is AppResult.Failure -> mutableState.update { it.copy(
                updatingProductIds = it.updatingProductIds - product.id,
                cartMessage = result.error.cartText(),
            ) }
        }
        if (result is AppResult.Success) refreshCart()
    }

    fun changeCartQuantity(product: Product, delta: Int) = viewModelScope.launch {
        val current = state.value.cartQuantities[product.id] ?: 0
        if (current == 0 && delta > 0) { addToCart(product); return@launch }
        if (product.id in state.value.updatingProductIds) return@launch
        val next = current + delta
        mutableState.update { it.copy(updatingProductIds = it.updatingProductIds + product.id) }
        val result = if (next <= 0) cart.remove(product.id) else cart.setQuantity(product.id, next)
        when (result) {
            is AppResult.Success -> mutableState.update { value -> value.copy(
                updatingProductIds = value.updatingProductIds - product.id,
                cartQuantities = result.value.lines.associate { it.productId to it.quantity },
                cart = result.value,
                cartLoaded = true,
                data = value.data?.copy(cartQuantity = result.value.distinctItemCount),
            ) }
            is AppResult.Failure -> mutableState.update { it.copy(
                updatingProductIds = it.updatingProductIds - product.id,
                cartMessage = result.error.cartText(),
            ) }
        }
    }

    fun consumeCartMessage() = mutableState.update { it.copy(cartMessage = null) }

    private fun load(refreshing: Boolean = false) = viewModelScope.launch {
        mutableState.update {
            if (refreshing && it.data != null) it.copy(refreshing = true, error = null)
            else it.copy(loading = true, error = null)
        }
        when (val result = repository.getHome()) {
            is AppResult.Success -> mutableState.update { current -> current.copy(
                loading = false, refreshing = false, error = null,
                data = result.value.copy(unreadNotifications = unreadCount ?: result.value.unreadNotifications,
                    cartQuantity = if (current.cartLoaded)
                        current.data?.cartQuantity ?: result.value.cartQuantity else result.value.cartQuantity),
            ) }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, refreshing = false, error = result.error.message())
            }
        }
    }
}

private fun AppError.cartText() = when (this) {
    AppError.Network, AppError.Offline, AppError.Timeout -> "Couldn't update cart. Check your connection."
    AppError.NotFound -> "This product is no longer available."
    is AppError.Validation -> message
    else -> "Couldn't update cart right now. Please try again."
}

private fun AppError.message() = when (this) {
    AppError.Network -> "You're offline. Check your connection and try again."
    AppError.Unauthorized -> "Please sign in again to continue."
    AppError.Forbidden -> "This home is not available for your account."
    else -> "We couldn't load the shop right now."
}

package com.spacetecsolutions.meatapp.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.CodOrderRepository
import com.spacetecsolutions.meatapp.core.domain.repository.UserRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeliveryRoleUiState(
    val loading: Boolean = true,
    val orders: List<CodOrder> = emptyList(),
    val user: User? = null,
    val error: String? = null,
)

@HiltViewModel
class DeliveryRoleViewModel @Inject constructor(
    private val ordersRepository: CodOrderRepository,
    userRepository: UserRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DeliveryRoleUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch { userRepository.observeAuthenticatedUser().collect { result ->
            if (result is AppResult.Success) mutableState.update { it.copy(user = result.value) }
        } }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = ordersRepository.getOrders(CodActor.DELIVERY)) {
            is AppResult.Success -> mutableState.update { it.copy(loading = false, orders = result.value) }
            is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error.text()) }
        }
    }
}

private fun AppError.text() = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "You can access only orders assigned to you"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    else -> "Unable to load assigned orders"
}

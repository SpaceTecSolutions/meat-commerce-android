package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.CodOrderRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerHomeRepository
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeleteAccountState(
    val loading: Boolean = true,
    val checked: Boolean = false,
    val activeOrder: CodOrder? = null,
    val deleting: Boolean = false,
    val completed: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CustomerDeleteAccountViewModel @Inject constructor(
    private val orders: CodOrderRepository,
    private val profile: CustomerHomeRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DeleteAccountState())
    val state = mutableState.asStateFlow()

    init { observeOrders() }

    private fun observeOrders() = viewModelScope.launch {
        orders.observeCustomerOrders().collect { result ->
            if (mutableState.value.completed) return@collect
            when (result) {
                is AppResult.Success -> mutableState.update {
                    it.copy(loading = false, checked = true,
                        activeOrder = result.value.firstOrNull(CodOrder::isActive),
                        error = null)
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(loading = false, error = "Unable to check current orders. Retry before deleting.")
                }
            }
        }
    }

    fun delete() = viewModelScope.launch {
        if (!mutableState.value.checked || mutableState.value.deleting ||
            mutableState.value.activeOrder != null) return@launch
        mutableState.update { it.copy(deleting = true, error = null) }
        when (val result = profile.deleteAccount()) {
            is AppResult.Success -> mutableState.update { it.copy(deleting = false, completed = true) }
            is AppResult.Failure -> mutableState.update { it.copy(deleting = false,
                error = when (result.error) {
                    is AppError.Validation -> "An active order is still in progress. Finish or cancel it first."
                    AppError.Network, AppError.Offline, AppError.Timeout ->
                        "Connection lost. Your account may still be processing deletion; retry safely."
                    else -> "Account deletion could not finish. Please retry or contact support."
                }) }
        }
    }

    fun retryCheck() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = orders.getCustomerOrders()) {
            is AppResult.Success -> mutableState.update { it.copy(loading = false, checked = true,
                activeOrder = result.value.firstOrNull(CodOrder::isActive)) }
            is AppResult.Failure -> mutableState.update { it.copy(loading = false,
                error = "Unable to check current orders. Retry before deleting.") }
        }
    }
}

private fun CodOrder.isActive() = orderStatus in setOf(OrderStatus.PENDING, OrderStatus.CONFIRMED,
    OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY)

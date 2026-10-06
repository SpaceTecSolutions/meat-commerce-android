package com.spacetecsolutions.meatapp.feature.admin.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.AdminCustomerRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

data class AdminCustomersUiState(
    val loading: Boolean = true,
    val customers: List<AdminCustomerSummary> = emptyList(),
    val query: String = "",
    val details: AdminCustomerDetails? = null,
    val loadingDetails: Boolean = false,
    val statusTarget: AdminCustomerSummary? = null,
    val busyCustomerId: String? = null,
    val error: String? = null,
    val message: AdminCustomerMessage? = null,
)
data class AdminCustomerMessage(val text: String, val success: Boolean)

@HiltViewModel
class AdminCustomersViewModel @Inject constructor(
    private val repository: AdminCustomerRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminCustomersUiState())
    val state = mutableState.asStateFlow()
    private var searchJob: Job? = null

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.getCustomers(state.value.query)) {
            is AppResult.Success -> mutableState.update {
                it.copy(loading = false, customers = result.value, error = null)
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, error = result.error.text())
            }
        }
    }

    fun search(value: String) {
        if (value.length > 80) return
        mutableState.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(350); refresh() }
    }

    fun openDetails(customer: AdminCustomerSummary?) {
        if (customer == null) {
            mutableState.update { it.copy(details = null, loadingDetails = false) }
            return
        }
        mutableState.update { it.copy(loadingDetails = true) }
        viewModelScope.launch {
            when (val result = repository.getCustomerDetails(customer.id)) {
                is AppResult.Success -> mutableState.update { it.copy(details = result.value, loadingDetails = false) }
                is AppResult.Failure -> mutableState.update { it.copy(
                    loadingDetails = false, message = AdminCustomerMessage(result.error.text(), false),
                ) }
            }
        }
    }

    fun requestStatus(customer: AdminCustomerSummary?) = mutableState.update { it.copy(statusTarget = customer) }

    fun confirmStatusChange() {
        val customer = state.value.statusTarget ?: return
        if (!customer.statusManagementAllowed || state.value.busyCustomerId != null) return
        mutableState.update { it.copy(statusTarget = null, busyCustomerId = customer.id) }
        viewModelScope.launch {
            when (val result = repository.setCustomerActive(customer.id, !customer.active, customer.revision)) {
                is AppResult.Success -> mutableState.update { current -> current.copy(
                    busyCustomerId = null,
                    customers = current.customers.map { if (it.id == customer.id) result.value else it },
                    details = current.details?.takeIf { it.customer.id != customer.id } ?: current.details?.copy(customer = result.value),
                    message = AdminCustomerMessage(
                        if (result.value.active) "Customer activated" else "Customer deactivated", true,
                    ),
                ) }
                is AppResult.Failure -> mutableState.update { it.copy(
                    busyCustomerId = null, message = AdminCustomerMessage(result.error.text(), false),
                ) }
            }
        }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }
}

internal fun AdminCustomersUiState.filteredCustomers(): List<AdminCustomerSummary> {
    val term = query.trim()
    return if (term.isEmpty()) customers else customers.filter {
        it.displayName.contains(term, true) || it.mobileNumber.contains(term)
    }
}

private fun AppError.text() = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "You are not authorized to manage customers"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    AppError.NotFound -> "Customer no longer exists"
    is AppError.Validation -> message
    else -> "Unable to load customer information"
}

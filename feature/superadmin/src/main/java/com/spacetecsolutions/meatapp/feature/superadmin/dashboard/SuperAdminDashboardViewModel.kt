package com.spacetecsolutions.meatapp.feature.superadmin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.SuperAdminDashboardRepository
import com.spacetecsolutions.meatapp.core.model.SuperAdminDashboard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuperAdminDashboardViewModel @Inject constructor(
    private val repository: SuperAdminDashboardRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val state = mutableState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableState.update { if (it is DashboardUiState.Content) it else DashboardUiState.Loading }
        when (val result = repository.getDashboard()) {
            is AppResult.Success -> mutableState.value = DashboardUiState.Content(result.value.toUiData())
            is AppResult.Failure -> mutableState.value = DashboardUiState.Error(result.error)
        }
    }
}

private fun SuperAdminDashboard.toUiData(): SuperAdminDashboardData {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    runCatching { formatter.currency = Currency.getInstance(currencyCode) }
    fun money(value: Long) = formatter.format(value / 100.0)
    return SuperAdminDashboardData(
        totalRevenue = money(totalRevenueMinor),
        monthlyRevenue = money(monthlyRevenueMinor),
        totalOrders = totalOrders.toString(),
        pendingOrders = pendingOrders.toString(),
        totalCustomers = totalCustomers.toString(),
        activeCustomers = activeCustomers.toString(),
        newCustomers = newCustomers.toString(),
        totalProducts = totalProducts.toString(),
        currentProductLimit = currentProductLimit.toString(),
        revenuePoints = revenuePointsMinor.map { (it / 100.0).toFloat() },
        revenueLabels = revenueLabels,
        paymentMethods = paymentMethods.map { PaymentMethodShare(it.label, it.percentage) },
    )
}

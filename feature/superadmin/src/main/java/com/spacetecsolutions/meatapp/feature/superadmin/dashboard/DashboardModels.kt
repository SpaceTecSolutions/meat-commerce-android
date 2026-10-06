package com.spacetecsolutions.meatapp.feature.superadmin.dashboard

import androidx.compose.runtime.Immutable

@Immutable
data class SuperAdminDashboardData(
    val totalRevenue: String = "₹0",
    val monthlyRevenue: String = "₹0",
    val totalOrders: String = "0",
    val pendingOrders: String = "0",
    val totalCustomers: String = "0",
    val activeCustomers: String = "0",
    val newCustomers: String = "0",
    val totalProducts: String = "0",
    val currentProductLimit: String = "0",
    val revenuePoints: List<Float> = List(7) { 0f },
    val revenueLabels: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val paymentMethods: List<PaymentMethodShare> = emptyList(),
)

@Immutable
data class PaymentMethodShare(
    val label: String,
    val percentage: Int,
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Content(val data: SuperAdminDashboardData) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data object Offline : DashboardUiState
}

enum class DashboardQuickAction {
    ADMINS, REPORTS, PRODUCT_LIMIT, FEATURE_MANAGEMENT,
}

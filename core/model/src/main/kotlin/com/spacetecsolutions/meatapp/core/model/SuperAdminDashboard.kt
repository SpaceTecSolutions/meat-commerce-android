package com.spacetecsolutions.meatapp.core.model

data class SuperAdminDashboard(
    val currencyCode: String,
    val totalRevenueMinor: Long,
    val monthlyRevenueMinor: Long,
    val totalOrders: Long,
    val pendingOrders: Long,
    val totalCustomers: Long,
    val activeCustomers: Long,
    val newCustomers: Long,
    val totalProducts: Long,
    val currentProductLimit: Long,
    val revenuePointsMinor: List<Long>,
    val revenueLabels: List<String>,
    val paymentMethods: List<SuperAdminPaymentShare>,
)

data class SuperAdminPaymentShare(val label: String, val percentage: Int)

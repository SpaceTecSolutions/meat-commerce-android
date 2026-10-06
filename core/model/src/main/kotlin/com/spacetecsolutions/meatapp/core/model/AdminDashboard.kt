package com.spacetecsolutions.meatapp.core.model

data class AdminDashboard(
    val adminName: String,
    val currencyCode: String,
    val todayRevenueMinor: Long,
    val todayOrders: Long,
    val pendingOrders: Long,
    val deliveredOrders: Long,
    val unreadNotificationCount: Int,
    val period: AdminDashboardPeriod,
    val periodLabel: String,
    val revenueSeries: List<ReportSeriesPoint> = emptyList(),
    val topSellingProduct: AdminDashboardProduct? = null,
    val topSellingProducts: List<AdminDashboardProduct> = topSellingProduct?.let(::listOf).orEmpty(),
)

enum class AdminDashboardPeriod { TODAY, THIS_WEEK, THIS_MONTH }

data class AdminDashboardProduct(
    val productId: String,
    val name: String,
    val imageUrl: String? = null,
    val unit: String,
    val quantitySold: Double,
    val revenueMinor: Long,
)

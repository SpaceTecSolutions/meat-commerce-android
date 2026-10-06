package com.spacetecsolutions.meatapp.core.model

data class AdminReport(
    val periodLabel: String,
    val currencyCode: String,
    val revenueMinor: Long,
    val orders: Long,
    val customers: Long,
    val products: Long,
    val activeProducts: Long,
    val revenueSeries: List<ReportSeriesPoint> = emptyList(),
    val completedOrders: Long = 0,
    val cancelledOrders: Long = 0,
    val quantitySold: Double = 0.0,
    val quantityUnit: String? = null,
    val averageOrderValueMinor: Long = 0,
    val averageSellingPriceMinor: Long = 0,
    val revenueChangePercent: Double? = null,
    val ordersChangePercent: Double? = null,
    val topProducts: List<AdminTopProductReport> = emptyList(),
)

data class AdminTopProductReport(
    val productId: String,
    val productName: String,
    val imageUrl: String? = null,
    val unit: String,
    val quantitySold: Double,
    val revenueMinor: Long,
    val orderCount: Long,
)

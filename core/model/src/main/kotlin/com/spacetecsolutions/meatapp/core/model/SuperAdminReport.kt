package com.spacetecsolutions.meatapp.core.model

enum class ReportPeriodType {
    WEEKLY, CURRENT_MONTH_BY_WEEK, MONTHLY, YEARLY, CUSTOM,
}

data class ReportRequest(
    val period: ReportPeriodType,
    val startDateIso: String? = null,
    val endDateIso: String? = null,
    val anchorDateIso: String? = null,
    val productId: String? = null,
)

data class ReportFilter(
    val period: ReportPeriodType,
    val anchorDateIso: String,
    val startDateIso: String? = null,
    val endDateIso: String? = null,
    val productId: String? = null,
)

data class ReportSeriesPoint(val label: String, val revenueMinor: Long)

data class BestSellingProduct(
    val productId: String,
    val name: String,
    val quantitySold: Long,
    val revenueMinor: Long,
)

data class SuperAdminReport(
    val periodLabel: String,
    val currencyCode: String,
    val revenueMinor: Long,
    val orders: Long,
    val delivered: Long,
    val cancelled: Long,
    val averageOrderValueMinor: Long,
    val customers: Long,
    val newCustomers: Long,
    val activeCustomers: Long,
    val revenueSeries: List<ReportSeriesPoint>,
    val bestSellingProducts: List<BestSellingProduct>,
)

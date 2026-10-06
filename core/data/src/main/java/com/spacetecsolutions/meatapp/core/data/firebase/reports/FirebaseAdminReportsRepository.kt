package com.spacetecsolutions.meatapp.core.data.firebase.reports

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.AdminReportsRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseAdminReportsRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : AdminReportsRepository {
    override suspend fun getReport(request: ReportRequest): AppResult<AdminReport> = try {
        val payload = buildMap<String, Any> {
            put("period", request.period.name)
            request.startDateIso?.let { put("startDate", it) }
            request.endDateIso?.let { put("endDate", it) }
            request.anchorDateIso?.let { put("anchorDate", it) }
            request.productId?.let { put("productId", it) }
        }
        val response = functions.getHttpsCallable("adminGetReport").call(payload).await().data as? Map<*, *>
            ?: return AppResult.Failure(AppError.Unknown())
        AppResult.Success((response["report"] as? Map<*, *> ?: response).adminReport())
    } catch (error: Exception) { AppResult.Failure(error.reportError()) }
}

private fun Map<*, *>.adminReport() = AdminReport(
    periodLabel = this["periodLabel"] as? String ?: "Selected period",
    currencyCode = this["currencyCode"] as? String ?: "INR",
    revenueMinor = number("revenueMinor"), orders = number("orders"),
    customers = number("customers"), products = number("products"),
    activeProducts = number("activeProducts"),
    revenueSeries = (this["revenueSeries"] as? List<*>).orEmpty().mapNotNull { item ->
        (item as? Map<*, *>)?.let { ReportSeriesPoint(it["label"] as? String ?: return@let null, it.number("revenueMinor")) }
    },
    completedOrders = number("completedOrders"), cancelledOrders = number("cancelledOrders"),
    quantitySold = decimal("quantitySold"), quantityUnit = this["quantityUnit"] as? String,
    averageOrderValueMinor = number("averageOrderValueMinor"),
    averageSellingPriceMinor = number("averageSellingPriceMinor"),
    revenueChangePercent = optionalDecimal("revenueChangePercent"),
    ordersChangePercent = optionalDecimal("ordersChangePercent"),
    topProducts = (this["topProducts"] as? List<*>).orEmpty().mapNotNull { item ->
        (item as? Map<*, *>)?.adminTopProduct()
    },
)
private fun Map<*, *>.adminTopProduct(): AdminTopProductReport? {
    val productId = this["productId"] as? String ?: return null
    return AdminTopProductReport(
        productId = productId,
        productName = this["productName"] as? String ?: "Product",
        imageUrl = this["imageUrl"] as? String, unit = this["unit"] as? String ?: "PIECE",
        quantitySold = decimal("quantitySold"), revenueMinor = number("revenueMinor"),
        orderCount = number("orderCount"),
    )
}
private fun Map<*, *>.number(key: String) = (this[key] as? Number)?.toLong() ?: 0L
private fun Map<*, *>.decimal(key: String) = (this[key] as? Number)?.toDouble() ?: 0.0
private fun Map<*, *>.optionalDecimal(key: String) = (this[key] as? Number)?.toDouble()
private fun Throwable.reportError(): AppError = when ((this as? FirebaseFunctionsException)?.code) {
    FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
    FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
    FirebaseFunctionsException.Code.INVALID_ARGUMENT -> AppError.Validation(message = "Select a valid date range")
    FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
    else -> AppError.Unknown(this)
}

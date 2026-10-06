package com.spacetecsolutions.meatapp.core.data.firebase.dashboard

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.AdminDashboardRepository
import com.spacetecsolutions.meatapp.core.model.AdminDashboard
import com.spacetecsolutions.meatapp.core.model.AdminDashboardPeriod
import com.spacetecsolutions.meatapp.core.model.AdminDashboardProduct
import com.spacetecsolutions.meatapp.core.model.ReportSeriesPoint
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseAdminDashboardRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : AdminDashboardRepository {
    override suspend fun getDashboard(period: AdminDashboardPeriod): AppResult<AdminDashboard> = try {
        val response = functions.getHttpsCallable("adminGetDashboard")
            .call(mapOf("period" to period.name))
            .await().data as? Map<*, *> ?: return AppResult.Failure(AppError.Unknown())
        val data = response["dashboard"] as? Map<*, *> ?: response
        AppResult.Success(data.toDashboard())
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }
}

private fun Map<*, *>.toDashboard() = AdminDashboard(
    adminName = string("adminName"),
    currencyCode = string("currencyCode"),
    todayRevenueMinor = long("todayRevenueMinor"),
    todayOrders = long("todayOrders"),
    pendingOrders = long("pendingOrders"),
    deliveredOrders = long("deliveredOrders"),
    unreadNotificationCount = long("unreadNotificationCount").toInt(),
    period = runCatching { AdminDashboardPeriod.valueOf(string("period")) }
        .getOrDefault(AdminDashboardPeriod.THIS_WEEK),
    periodLabel = string("periodLabel"),
    revenueSeries = (this["revenueSeries"] as? List<*>).orEmpty().mapNotNull {
        val point = it as? Map<*, *> ?: return@mapNotNull null
        ReportSeriesPoint(point.string("label"), point.long("revenueMinor"))
    },
    topSellingProduct = (this["topSellingProduct"] as? Map<*, *>)?.toDashboardProduct(),
    topSellingProducts = (this["topSellingProducts"] as? List<*>).orEmpty()
        .mapNotNull { (it as? Map<*, *>)?.toDashboardProduct() },
)

private fun Map<*, *>.toDashboardProduct() = AdminDashboardProduct(
    productId = string("productId"), name = string("name"), imageUrl = this["imageUrl"] as? String,
    unit = string("unit"), quantitySold = (this["quantitySold"] as? Number)?.toDouble() ?: 0.0,
    revenueMinor = long("revenueMinor"),
)

private fun Map<*, *>.string(key: String): String = this[key] as? String
    ?: error("Missing dashboard field: $key")
private fun Map<*, *>.long(key: String): Long = (this[key] as? Number)?.toLong()
    ?: error("Missing dashboard field: $key")

private fun Throwable.toAppError(): AppError {
    val exception = this as? FirebaseFunctionsException
    return when (exception?.code) {
        FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

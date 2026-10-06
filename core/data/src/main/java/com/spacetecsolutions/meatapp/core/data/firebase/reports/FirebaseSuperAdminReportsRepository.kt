package com.spacetecsolutions.meatapp.core.data.firebase.reports

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.SuperAdminReportsRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseSuperAdminReportsRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : SuperAdminReportsRepository {
    override suspend fun getReport(request: ReportRequest): AppResult<SuperAdminReport> = try {
        val payload = buildMap<String, Any> {
            put("period", request.period.name)
            request.startDateIso?.let { put("startDate", it) }
            request.endDateIso?.let { put("endDate", it) }
        }
        val response = functions.getHttpsCallable("superAdminGetReport").call(payload).await().data
            as? Map<*, *> ?: return AppResult.Failure(AppError.Unknown())
        val report = response["report"] as? Map<*, *> ?: response
        AppResult.Success(report.toReport())
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }
}

private fun Map<*, *>.toReport() = SuperAdminReport(
    periodLabel = string("periodLabel"),
    currencyCode = string("currencyCode"),
    revenueMinor = long("revenueMinor"),
    orders = long("orders"),
    delivered = long("delivered"),
    cancelled = long("cancelled"),
    averageOrderValueMinor = long("averageOrderValueMinor"),
    customers = long("customers"),
    newCustomers = long("newCustomers"),
    activeCustomers = long("activeCustomers"),
    revenueSeries = list("revenueSeries").mapNotNull { value ->
        (value as? Map<*, *>)?.let { ReportSeriesPoint(it.string("label"), it.long("revenueMinor")) }
    },
    bestSellingProducts = list("bestSellingProducts").mapNotNull { value ->
        (value as? Map<*, *>)?.let {
            BestSellingProduct(
                productId = it.string("productId"),
                name = it.string("name"),
                quantitySold = it.long("quantitySold"),
                revenueMinor = it.long("revenueMinor"),
            )
        }
    },
)

private fun Map<*, *>.string(key: String): String = this[key] as? String
    ?: error("Missing report field: $key")
private fun Map<*, *>.long(key: String): Long = (this[key] as? Number)?.toLong()
    ?: error("Missing report field: $key")
private fun Map<*, *>.list(key: String): List<*> = this[key] as? List<*> ?: emptyList<Any>()

private fun Throwable.toAppError(): AppError {
    val exception = this as? FirebaseFunctionsException
    return when (exception?.code) {
        FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
            AppError.Validation(message = "Select a valid report range")
        FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> AppError.TooManyRequests
        FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

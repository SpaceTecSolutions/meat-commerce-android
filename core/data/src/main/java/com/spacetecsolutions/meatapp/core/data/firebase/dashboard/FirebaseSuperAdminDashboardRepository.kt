package com.spacetecsolutions.meatapp.core.data.firebase.dashboard

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.SuperAdminDashboardRepository
import com.spacetecsolutions.meatapp.core.model.SuperAdminDashboard
import com.spacetecsolutions.meatapp.core.model.SuperAdminPaymentShare
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseSuperAdminDashboardRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : SuperAdminDashboardRepository {
    override suspend fun getDashboard(): AppResult<SuperAdminDashboard> = try {
        val response = functions.getHttpsCallable("superAdminGetDashboard").call().await().data as? Map<*, *>
            ?: error("Invalid Super Admin dashboard response")
        val data = response["dashboard"] as? Map<*, *> ?: error("Missing dashboard response")
        AppResult.Success(SuperAdminDashboard(
            currencyCode = data.text("currencyCode", "INR"),
            totalRevenueMinor = data.long("totalRevenueMinor"),
            monthlyRevenueMinor = data.long("monthlyRevenueMinor"),
            totalOrders = data.long("totalOrders"),
            pendingOrders = data.long("pendingOrders"),
            totalCustomers = data.long("totalCustomers"),
            activeCustomers = data.long("activeCustomers"),
            newCustomers = data.long("newCustomers"),
            totalProducts = data.long("totalProducts"),
            currentProductLimit = data.long("currentProductLimit"),
            revenuePointsMinor = (data["revenuePointsMinor"] as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toLong() },
            revenueLabels = (data["revenueLabels"] as? List<*>).orEmpty().filterIsInstance<String>(),
            paymentMethods = (data["paymentMethods"] as? List<*>).orEmpty().mapNotNull { raw ->
                val method = raw as? Map<*, *> ?: return@mapNotNull null
                SuperAdminPaymentShare(method.text("label", ""), method.long("percentage").toInt())
            },
        ))
    } catch (error: Exception) {
        val functionsError = error as? FirebaseFunctionsException
        AppResult.Failure(when (functionsError?.code) {
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
            FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
            else -> AppError.Unknown(error)
        })
    }
}

private fun Map<*, *>.text(key: String, fallback: String) = this[key] as? String ?: fallback
private fun Map<*, *>.long(key: String) = (this[key] as? Number)?.toLong() ?: 0L

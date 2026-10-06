package com.spacetecsolutions.meatapp.core.data.firebase.customer

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.AdminCustomerRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseAdminCustomerRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : AdminCustomerRepository {
    override suspend fun getCustomers(query: String): AppResult<List<AdminCustomerSummary>> = try {
        val data = call("adminGetCustomers", mapOf("query" to query.trim(), "limit" to 50))
        val customers = (data["customers"] as? List<*>).orEmpty().mapNotNull {
            (it as? Map<*, *>)?.toSummary()
        }
        AppResult.Success(customers)
    } catch (error: Exception) { AppResult.Failure(error.toCustomerError()) }

    override suspend fun getCustomerDetails(customerId: String): AppResult<AdminCustomerDetails> = try {
        val data = call("adminGetCustomerDetails", mapOf("customerId" to customerId))
        val raw = data["details"] as? Map<*, *> ?: data
        AppResult.Success(AdminCustomerDetails(
            customer = (raw["customer"] as? Map<*, *> ?: raw).toSummary(),
            joinedAtEpochMillis = raw.long("joinedAtEpochMillis"),
            highestOrderMinor = raw.long("highestOrderMinor"),
            orders = (raw["orders"] as? List<*>).orEmpty().mapNotNull {
                (it as? Map<*, *>)?.toOrder()
            },
        ))
    } catch (error: Exception) { AppResult.Failure(error.toCustomerError()) }

    override suspend fun setCustomerActive(
        customerId: String, active: Boolean, expectedRevision: Long,
    ): AppResult<AdminCustomerSummary> = try {
        val data = call("adminSetCustomerActive", mapOf(
            "customerId" to customerId, "active" to active, "expectedRevision" to expectedRevision,
        ))
        AppResult.Success((data["customer"] as? Map<*, *> ?: data).toSummary())
    } catch (error: Exception) { AppResult.Failure(error.toCustomerError()) }

    private suspend fun call(name: String, payload: Map<String, Any> = emptyMap()) =
        functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid customer response")
}

private fun Map<*, *>.toSummary() = AdminCustomerSummary(
    id = string("id"), displayName = this["displayName"] as? String ?: "Customer",
    mobileNumber = string("mobileNumber"), active = this["active"] as? Boolean ?: true,
    totalOrders = long("totalOrders"), totalSpendingMinor = long("totalSpendingMinor"),
    currencyCode = this["currencyCode"] as? String ?: "INR",
    statusManagementAllowed = this["statusManagementAllowed"] as? Boolean ?: false,
    revision = long("revision"),
)
private fun Map<*, *>.toOrder() = AdminCustomerOrder(
    id = string("id"), displayNumber = string("displayNumber"),
    createdAtEpochMillis = long("createdAtEpochMillis"),
    productSummary = this["productSummary"] as? String ?: "Products unavailable",
    totalMinor = long("totalMinor"),
    paymentMethod = enum("paymentMethod", CheckoutPaymentMethod.COD),
    paymentStatus = enum("paymentStatus", PaymentStatus.PENDING),
    orderStatus = enum("orderStatus", OrderStatus.PENDING),
)
private fun Map<*, *>.string(key: String) = this[key] as? String ?: error("Missing $key")
private fun Map<*, *>.long(key: String) = (this[key] as? Number)?.toLong() ?: 0
private inline fun <reified T : Enum<T>> Map<*, *>.enum(key: String, default: T) =
    runCatching { enumValueOf<T>(string(key)) }.getOrDefault(default)
private fun Throwable.toCustomerError(): AppError {
    val exception = this as? FirebaseFunctionsException
    return when (exception?.code) {
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        FirebaseFunctionsException.Code.NOT_FOUND -> AppError.NotFound
        FirebaseFunctionsException.Code.ABORTED -> AppError.Validation(message = "Customer changed. Refresh and retry")
        FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

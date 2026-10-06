package com.spacetecsolutions.meatapp.core.data.firebase.order

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.CodOrderRepository
import com.spacetecsolutions.meatapp.core.domain.service.CartBadgeStore
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseCodOrderRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val cartBadgeStore: CartBadgeStore,
    private val customerObserver: CustomerOrderRealtimeDataSource,
) : CodOrderRepository {
    override fun observeCustomerOrders() = customerObserver.observe()
    override suspend fun getCustomerDeliveryOtp(orderId: String): AppResult<DeliveryOtp> = try {
        val data = functions.getHttpsCallable("customerGetDeliveryOtp")
            .call(mapOf("orderId" to orderId)).await().data as? Map<*, *>
            ?: error("Invalid delivery verification response")
        val code = data["code"] as? String ?: error("Missing delivery code")
        val session = data["sessionId"] as? String ?: error("Missing delivery session")
        val expires = (data["expiresAtEpochMillis"] as? Number)?.toLong()
            ?: error("Missing delivery code expiry")
        AppResult.Success(DeliveryOtp(code, session, expires))
    } catch (error: Exception) { AppResult.Failure(error.toCodError()) }
    override suspend fun verifyDeliveryOtp(orderId: String, code: String): AppResult<Unit> = try {
        val data = functions.getHttpsCallable("verifyDeliveryOtp")
            .call(mapOf("orderId" to orderId, "code" to code)).await().data as? Map<*, *>
            ?: error("Invalid delivery verification response")
        if (data["verified"] == true) AppResult.Success(Unit)
        else AppResult.Failure(AppError.Validation(message = when (data["reason"]) {
            "INCORRECT_OTP" -> "Incorrect OTP. Check the customer's code and try again."
            "OTP_LOCKED" -> "Too many attempts. Please contact support."
            "OTP_EXPIRED" -> "Delivery OTP expired. Ask the customer to reopen tracking."
            else -> "Unable to verify OTP. Try again."
        }))
    } catch (error: Exception) { AppResult.Failure(error.toCodError()) }
    override suspend fun getCustomerOrders() = listCall("customerGetOrders")
    override suspend fun reorderCustomerOrder(orderId: String): AppResult<Int> = try {
        val data = functions.getHttpsCallable("customerReorder").call(mapOf("orderId" to orderId))
            .await().data as? Map<*, *> ?: error("Invalid reorder response")
        (data["cartQuantity"] as? Number)?.toInt()?.let { cartBadgeStore.update(it) }
        AppResult.Success((data["addedItemCount"] as? Number)?.toInt() ?: 0)
    } catch (error: Exception) { AppResult.Failure(error.toCodError()) }
    override suspend fun cancelCustomerOrder(
        orderId: String, expectedRevision: Long, reason: String,
    ) = orderCall("customerCancelOrder", payload(orderId, expectedRevision) + ("reason" to reason))
    override suspend fun getOrders(actor: CodActor) = listCall(
        if (actor == CodActor.ADMIN) "adminGetOrders" else "deliveryGetAssignedCodOrders",
    )
    override suspend fun confirm(orderId: String, expectedRevision: Long, adjustments: List<FinalWeightAdjustment>) = orderCall(
        "adminConfirmCodOrder", payload(orderId, expectedRevision) + ("adjustments" to adjustments.map {
            mapOf("productId" to it.productId, "finalQuantity" to it.finalQuantity)
        }),
    )
    override suspend fun startPreparing(orderId: String, expectedRevision: Long) = orderCall(
        "adminStartPreparingOrder", payload(orderId, expectedRevision),
    )
    override suspend fun markReadyForDelivery(orderId: String, expectedRevision: Long) = orderCall(
        "adminMarkOrderReadyForDelivery", payload(orderId, expectedRevision),
    )
    override suspend fun cancel(orderId: String, expectedRevision: Long, reason: String) = orderCall(
        "adminCancelCodOrder", payload(orderId, expectedRevision) + ("reason" to reason),
    )
    override suspend fun startPersonalDelivery(orderId: String, expectedRevision: Long) = orderCall(
        "adminAssignOrderToSelf", payload(orderId, expectedRevision),
    )
    override suspend fun assignDeliveryUser(
        orderId: String, deliveryUserId: String, expectedRevision: Long,
    ) = orderCall(
        "adminAssignDeliveryUser",
        payload(orderId, expectedRevision) + ("deliveryUserId" to deliveryUserId),
    )
    override suspend fun startDelivery(actor: CodActor, orderId: String, expectedRevision: Long) = orderCall(
        if (actor == CodActor.ADMIN) "adminStartAssignedDelivery" else "deliveryStartAssignedOrder",
        payload(orderId, expectedRevision),
    )
    override suspend fun completeDelivery(
        actor: CodActor, orderId: String, paymentMethod: CheckoutPaymentMethod,
        collectedMinor: Long?, expectedRevision: Long,
    ) = orderCall(
        if (actor == CodActor.ADMIN) "adminCompleteDelivery" else "deliveryCompleteAssignedOrder",
        payload(orderId, expectedRevision) + ("paymentMethod" to paymentMethod.name) +
            (collectedMinor?.let { mapOf("collectedMinor" to it) } ?: emptyMap()),
    )
    override suspend fun reportAmountMismatch(
        actor: CodActor, orderId: String, actualMinor: Long, expectedRevision: Long,
    ) = orderCall(
        if (actor == CodActor.ADMIN) "adminReportCodMismatch" else "deliveryReportCodMismatch",
        payload(orderId, expectedRevision) + ("actualMinor" to actualMinor),
    )

    private suspend fun listCall(name: String): AppResult<List<CodOrder>> = try {
        val data = functions.getHttpsCallable(name).call().await().data as? Map<*, *>
            ?: error("Invalid COD orders response")
        val orders = (data["orders"] as? List<*>).orEmpty().mapNotNull {
            (it as? Map<*, *>)?.toCodOrder()
        }
        AppResult.Success(orders)
    } catch (error: Exception) { AppResult.Failure(error.toCodError()) }

    private suspend fun orderCall(name: String, payload: Map<String, Any>): AppResult<CodOrder> = try {
        val data = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid COD order response")
        AppResult.Success((data["order"] as? Map<*, *> ?: data).toCodOrder())
    } catch (error: Exception) { AppResult.Failure(error.toCodError()) }
}

private fun payload(id: String, revision: Long): Map<String, Any> =
    mapOf("orderId" to id, "expectedRevision" to revision)
internal fun Map<*, *>.toCodOrder(): CodOrder {
    val address = this["deliveryAddressSnapshot"] as? Map<*, *>
    return CodOrder(
    id = string("id"), displayNumber = firstString("displayNumber", "orderNumber"),
    customerName = firstString("customerName").ifBlank { "Customer" },
    customerMobile = firstString("customerMobile", "customerPhone"),
    addressSummary = firstString("addressSummary").ifBlank {
        address?.firstString("formattedAddress", "address").orEmpty()
    },
    addressLabel = firstString("addressLabel").ifBlank { address?.firstString("type") ?: "Home" },
    shopName = firstString("shopName").ifBlank { "Meat Station" },
    shopAddress = firstString("shopAddress"),
    items = maps("items").map { it.toSnapshot() }, subtotalMinor = firstLong("subtotalMinor", "itemTotal"),
    discountMinor = long("discountMinor"), deliveryFeeMinor = long("deliveryFeeMinor"),
    taxMinor = long("taxMinor"),
    totalMinor = long("totalMinor"), paymentMethod = enum("paymentMethod", CheckoutPaymentMethod.COD),
    instructions = this["instructions"] as? String ?: "",
    deliverySlotDateLabel = firstString("deliverySlotDateLabel").ifBlank {
        (this["deliverySlotSnapshot"] as? Map<*, *>)?.firstString("dateLabel").orEmpty()
    }.ifBlank { null },
    deliverySlotTimeLabel = firstString("deliverySlotTimeLabel").ifBlank {
        (this["deliverySlotSnapshot"] as? Map<*, *>)?.firstString("timeLabel").orEmpty()
    }.ifBlank { null },
    deliveryDateIso = this["deliveryDateIso"] as? String
        ?: this["deliveryDate"] as? String
        ?: (this["deliverySlotSnapshot"] as? Map<*, *>)?.get("date") as? String,
    deliverySlotStartMinutes = (this["deliverySlotStartMinutes"] as? Number)?.toInt()
        ?: ((this["deliverySlotSnapshot"] as? Map<*, *>)?.get("startMinutes") as? Number)?.toInt(),
    deliverySlotEndMinutes = (this["deliverySlotEndMinutes"] as? Number)?.toInt()
        ?: ((this["deliverySlotSnapshot"] as? Map<*, *>)?.get("endMinutes") as? Number)?.toInt(),
    amountDueMinor = firstLong("amountDueMinor", "totalAmount", "totalMinor"),
    currencyCode = this["currencyCode"] as? String ?: "INR",
    orderStatus = enum("orderStatus", OrderStatus.PENDING),
    paymentStatus = enum("paymentStatus", PaymentStatus.PENDING),
    createdAtEpochMillis = optionalLong("createdAtEpochMillis") ?: 0,
    customerCancellationAllowed = this["customerCancellationAllowed"] as? Boolean ?: false,
    confirmedAtEpochMillis = optionalLong("confirmedAtEpochMillis"),
    confirmedByAdminId = this["confirmedByAdminId"] as? String,
    preparingAtEpochMillis = optionalLong("preparingAtEpochMillis"),
    preparingByAdminId = this["preparingByAdminId"] as? String,
    readyForDeliveryAtEpochMillis = optionalLong("readyForDeliveryAtEpochMillis"),
    readyForDeliveryByAdminId = this["readyForDeliveryByAdminId"] as? String,
    assignedDeliveryUserId = this["assignedDeliveryUserId"] as? String,
    assignedDeliveryUserName = this["assignedDeliveryUserName"] as? String,
    assignedAtEpochMillis = optionalLong("assignedAtEpochMillis"),
    assignedByAdminId = this["assignedByAdminId"] as? String,
    deliveryStartedAtEpochMillis = optionalLong("deliveryStartedAtEpochMillis"),
    deliveryStartedByUserId = this["deliveryStartedByUserId"] as? String,
    assignedDeliveryRole = optionalEnum<UserRole>("assignedDeliveryRole"),
    deliveredAtEpochMillis = optionalLong("deliveredAtEpochMillis"),
    deliveredByUserId = this["deliveredByUserId"] as? String,
    cancelledAtEpochMillis = optionalLong("cancelledAtEpochMillis"),
    cancelledByUserId = this["cancelledByUserId"] as? String,
    cancelledByRole = optionalEnum<UserRole>("cancelledByRole"),
    cancelReason = this["cancelReason"] as? String,
    trackingLifecycle = enum("trackingLifecycle", DeliveryTrackingLifecycle.DISABLED),
    trackingSessionId = this["trackingSessionId"] as? String,
    realtimeTrackingAvailable = this["realtimeTrackingAvailable"] as? Boolean ?: false,
    deliveryContactName = this["deliveryContactName"] as? String,
    deliveryContactMobile = this["deliveryContactMobile"] as? String,
    deliveryDestinationLatitude = numberOrNull("deliveryDestinationLatitude") ?: address?.numberOrNull("latitude"),
    deliveryDestinationLongitude = numberOrNull("deliveryDestinationLongitude") ?: address?.numberOrNull("longitude"),
    adminDeliveringPersonally = this["adminDeliveringPersonally"] as? Boolean ?: false,
    codMismatchReported = this["codMismatchReported"] as? Boolean ?: false,
    revision = long("revision"),
)}
private fun Map<*, *>.toSnapshot() = OrderItemSnapshot(
    productId = string("productId"), name = string("name"), imageUrl = this["imageUrl"] as? String,
    categoryId = this["categoryId"] as? String, categoryName = this["categoryName"] as? String,
    unit = enum("unit", ProductUnit.PIECE), quantity = long("quantity").toInt(),
    unitPriceMinor = long("unitPriceMinor"), regularPriceMinor = long("regularPriceMinor"),
    lineTotalMinor = long("lineTotalMinor"),
    attributes = (this["attributes"] as? Map<*, *>).orEmpty().entries.mapNotNull { (key, value) ->
        if (key is String && value is String) key to value else null
    }.toMap(),
)
private fun Map<*, *>.maps(key: String) = (this[key] as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
private fun Map<*, *>.string(key: String) = this[key] as? String ?: error("Missing $key")
private fun Map<*, *>.long(key: String) = (this[key] as? Number)?.toLong() ?: 0
private fun Map<*, *>.firstLong(vararg keys: String) = keys.firstNotNullOfOrNull {
    (this[it] as? Number)?.toLong()
} ?: 0L
private fun Map<*, *>.firstString(vararg keys: String) = keys.firstNotNullOfOrNull {
    (this[it] as? String)?.takeIf(String::isNotBlank)
}.orEmpty()
private fun Map<*, *>.numberOrNull(key: String) = (this[key] as? Number)?.toDouble()
private fun Map<*, *>.optionalLong(key: String) = when (val value = this[key]) {
    is Number -> value.toLong()
    is com.google.firebase.Timestamp -> value.toDate().time
    else -> null
}
private inline fun <reified T : Enum<T>> Map<*, *>.enum(key: String, default: T) =
    runCatching { enumValueOf<T>(string(key)) }.getOrDefault(default)
private inline fun <reified T : Enum<T>> Map<*, *>.optionalEnum(key: String): T? =
    (this[key] as? String)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

private fun Throwable.toCodError(): AppError {
    val exception = this as? FirebaseFunctionsException
    val reason = (exception?.details as? Map<*, *>)?.get("reason") as? String
    return when {
        reason == "COD_AMOUNT_MISMATCH" -> AppError.Validation(message = "Collected amount does not match COD due")
        reason == "ORDER_CANCELLED" -> AppError.Validation(message = "Cancelled orders cannot be updated")
        reason == "INVALID_TRANSITION" -> AppError.Validation(message = "This order transition is no longer valid")
        reason == "NOT_ASSIGNED" -> AppError.Forbidden
        reason == "DELIVERY_USER_INACTIVE" -> AppError.Validation(message = "This delivery user is inactive")
        reason == "ALREADY_ASSIGNED" -> AppError.Validation(message = "This order already has an active assignment")
        reason == "CANCELLATION_WINDOW_CLOSED" -> AppError.Validation(
            message = "This order can no longer be cancelled under the shop policy",
        )
        reason == "NO_REORDERABLE_ITEMS" -> AppError.Validation(
            message = "No items from this order are currently available",
        )
        reason == "DELIVERY_OTP_REQUIRED" -> AppError.Validation(
            message = "Verify the customer's delivery OTP before completing this order",
        )
        exception?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        exception?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        exception?.code == FirebaseFunctionsException.Code.NOT_FOUND -> AppError.NotFound
        exception?.code == FirebaseFunctionsException.Code.ABORTED ->
            AppError.Validation(message = "Order changed. Refresh and try again")
        exception?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        else -> AppError.Unknown(this)
    }
}

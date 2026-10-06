package com.spacetecsolutions.meatapp.core.data.firebase.checkout

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.CheckoutRepository
import com.spacetecsolutions.meatapp.core.domain.service.CartBadgeStore
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseCheckoutRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val badgeStore: CartBadgeStore,
) : CheckoutRepository {
    override suspend fun prepare(): AppResult<CheckoutQuote> = try {
        val data = functions.getHttpsCallable("customerPrepareCheckout").call().await().data.asMap()
        AppResult.Success(data.toQuote())
    } catch (error: Exception) { AppResult.Failure(error.toCheckoutError()) }

    override suspend fun placeOrder(request: PlaceOrderRequest): AppResult<PlacedOrder> = try {
        val payload = buildMap<String, Any> {
            put("quoteToken", request.quoteToken)
            put("addressId", request.addressId)
            put("deliveryOption", request.deliveryOption.name)
            put("deliveryDateId", request.deliveryDateId)
            request.deliverySlotId?.let { put("deliverySlotId", it) }
            put("instructions", request.instructions)
            put("paymentMethod", request.paymentMethod.name)
            put("idempotencyKey", request.idempotencyKey)
        }
        val data = functions.getHttpsCallable("customerPlaceOrder").call(payload).await().data.asMap()
        val placedOrder = (data["order"] as? Map<*, *> ?: data).toPlacedOrder()
        badgeStore.update(0)
        AppResult.Success(placedOrder)
    } catch (error: Exception) {
        val code = (error as? FirebaseFunctionsException)?.code
        if (code == FirebaseFunctionsException.Code.UNAVAILABLE || code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED)
            AppResult.Failure(AppError.Timeout)
        else AppResult.Failure(error.toCheckoutError())
    }

    override suspend fun reconcileOrder(idempotencyKey: String): AppResult<PlacedOrder?> = try {
        val data = functions.getHttpsCallable("customerReconcileOrderCreation")
            .call(mapOf("idempotencyKey" to idempotencyKey)).await().data.asMap()
        val raw = data["order"] as? Map<*, *> ?: return AppResult.Success(null)
        val order = raw.toPlacedOrder()
        badgeStore.update(0)
        AppResult.Success(order)
    } catch (error: Exception) { AppResult.Failure(error.toCheckoutError()) }
}

private fun Map<*, *>.toPlacedOrder() = PlacedOrder(
    orderId = string("orderId"), orderNumber = string("orderNumber"),
    orderStatus = enum("orderStatus", OrderStatus.PENDING),
    paymentStatus = enum("paymentStatus", PaymentStatus.PENDING), totalMinor = long("totalMinor"),
    currencyCode = this["currencyCode"] as? String ?: "INR", estimatedDelivery = string("estimatedDelivery"),
    items = maps("items").map { it.toOrderItemSnapshot() },
)

private fun Any?.asMap() = this as? Map<*, *> ?: error("Invalid checkout response")
private fun Map<*, *>.toQuote() = CheckoutQuote(
    quoteToken = string("quoteToken"),
    cart = (this["cart"] as? Map<*, *> ?: error("Missing cart")).toCart(),
    addresses = maps("addresses").map { it.toAddress() },
    normalDeliveryAvailable = this["normalDeliveryAvailable"] as? Boolean ?: false,
    scheduledDeliveryAvailable = this["scheduledDeliveryAvailable"] as? Boolean ?: false,
    deliveryDates = maps("deliveryDates").map { date ->
        CheckoutDeliveryDate(
            id = date.string("id"), label = date.string("label"),
            availableSlotIds = date.strings("availableSlotIds").toSet(),
        )
    },
    deliverySlots = maps("deliverySlots").mapNotNull { it.toSlot() },
    paymentMethods = strings("paymentMethods").mapNotNull {
        runCatching { CheckoutPaymentMethod.valueOf(it) }.getOrNull()
    }.toSet(),
    currencyCode = this["currencyCode"] as? String ?: "INR",
    expiresAtEpochMillis = long("expiresAtEpochMillis"),
)

private fun Map<*, *>.toCart() = CustomerCart(
    lines = maps("lines").map { it.toLine() }, subtotalMinor = long("subtotalMinor"),
    discountMinor = long("discountMinor"), deliveryFeeMinor = long("deliveryFeeMinor"),
    totalMinor = long("totalMinor"), deliveryEstimate = this["deliveryEstimate"] as? String,
    adjustments = emptyList(),
)
private fun Map<*, *>.toLine() = CartLine(
    productId = string("productId"), name = string("name"), imageUrl = this["imageUrl"] as? String,
    unit = enum("unit", ProductUnit.PIECE), unitPriceMinor = long("unitPriceMinor"),
    regularPriceMinor = long("regularPriceMinor"), quantity = int("quantity"),
    maxQuantity = int("maxQuantity"), available = this["available"] as? Boolean ?: false,
)
private fun Map<*, *>.toOrderItemSnapshot() = OrderItemSnapshot(
    productId = string("productId"), name = string("name"), imageUrl = this["imageUrl"] as? String,
    categoryId = this["categoryId"] as? String, categoryName = this["categoryName"] as? String,
    unit = enum("unit", ProductUnit.PIECE), quantity = int("quantity"),
    unitPriceMinor = long("unitPriceMinor"), regularPriceMinor = long("regularPriceMinor"),
    lineTotalMinor = long("lineTotalMinor"),
    attributes = (this["attributes"] as? Map<*, *>).orEmpty().entries.mapNotNull { (key, value) ->
        if (key is String && value is String) key to value else null
    }.toMap(),
)
private fun Map<*, *>.toAddress() = CustomerAddress(
    id = string("id"), type = enum("type", AddressType.OTHER), name = string("name"),
    mobile = string("mobile"), address = string("address"), landmark = optional("landmark"),
    city = string("city"), state = string("state"), postalCode = string("postalCode"),
    isDefault = this["isDefault"] as? Boolean ?: false, revision = long("revision"),
)
private fun Map<*, *>.toSlot() = runCatching { DeliverySlot(
    id = string("id"), label = string("label"), startMinutes = int("startMinutes"),
    endMinutes = int("endMinutes"), active = this["active"] as? Boolean ?: true,
) }.getOrNull()
private fun Map<*, *>.maps(key: String) = (this[key] as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
private fun Map<*, *>.strings(key: String) = (this[key] as? List<*>).orEmpty().mapNotNull { it as? String }
private fun Map<*, *>.string(key: String) = this[key] as? String ?: error("Missing $key")
private fun Map<*, *>.optional(key: String) = this[key] as? String ?: ""
private fun Map<*, *>.long(key: String) = (this[key] as? Number)?.toLong() ?: 0
private fun Map<*, *>.int(key: String) = (this[key] as? Number)?.toInt() ?: 0
private inline fun <reified T : Enum<T>> Map<*, *>.enum(key: String, default: T) =
    runCatching { enumValueOf<T>(string(key)) }.getOrDefault(default)

private fun Throwable.toCheckoutError(): AppError {
    val exception = this as? FirebaseFunctionsException
    val reason = (exception?.details as? Map<*, *>)?.get("reason") as? String
    return when {
        reason == "PRICE_CHANGED" -> AppError.Validation(
            message = "Some prices have changed. Please review your updated order.",
        )
        reason == "STOCK_CHANGED" -> AppError.Validation(
            message = "Some items are no longer available in the requested quantity.",
        )
        reason == "CART_CHANGED" -> AppError.Validation(
            message = "Your cart changed. Please review your updated order.",
        )
        reason in setOf("DELIVERY_CHANGED", "FEATURE_CHANGED", "QUOTE_EXPIRED") ->
            AppError.Validation(message = exception?.message ?: "Checkout details changed. Review again")
        reason == "EMPTY_CART" -> AppError.Validation(message = "Your cart is empty")
        reason == "PAYMENT_UNAVAILABLE" -> AppError.Validation(message = "Payment method is unavailable")
        exception?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        exception?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        exception?.code == FirebaseFunctionsException.Code.NOT_FOUND -> AppError.NotFound
        exception?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        exception?.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> AppError.Timeout
        else -> AppError.Unknown(this)
    }
}

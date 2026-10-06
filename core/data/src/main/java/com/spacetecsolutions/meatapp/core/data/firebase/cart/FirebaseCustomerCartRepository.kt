package com.spacetecsolutions.meatapp.core.data.firebase.cart

import android.util.Log
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerCartRepository
import com.spacetecsolutions.meatapp.core.domain.service.CartBadgeStore
import com.spacetecsolutions.meatapp.core.model.*
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

internal class FirebaseCustomerCartRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val badgeStore: CartBadgeStore,
    private val auth: FirebaseAuth,
    @ApplicationContext context: Context,
) : CustomerCartRepository {
    private val guestPreferences = context.getSharedPreferences("guest_customer_cart", Context.MODE_PRIVATE)
    private val isGuest get() = auth.currentUser == null
    private fun guestLines(): MutableMap<String, Int> = runCatching {
        val array = JSONArray(guestPreferences.getString("lines", "[]"))
        (0 until array.length()).map { array.getJSONObject(it) }
            .associate { it.getString("productId") to it.getInt("quantity") }.toMutableMap()
    }.getOrDefault(mutableMapOf())
    private fun saveGuest(lines: Map<String, Int>) {
        val array = JSONArray()
        lines.forEach { (id, quantity) -> array.put(JSONObject().put("productId", id).put("quantity", quantity)) }
        guestPreferences.edit().putString("lines", array.toString()).apply()
        badgeStore.update(lines.size)
    }
    private fun payload(lines: Map<String, Int>) = mapOf("lines" to lines.map { (id, quantity) ->
        mapOf("productId" to id, "quantity" to quantity) })

    override suspend fun getCart() = if (isGuest) call("customerQuoteGuestCart", payload(guestLines()))
        else call("customerGetCart", emptyMap())

    override suspend fun setQuantity(productId: String, quantity: Int): AppResult<CustomerCart> = if (isGuest) {
        val lines = guestLines()
        if (productId !in lines) return AppResult.Failure(AppError.NotFound)
        lines[productId] = quantity.coerceIn(1, 99)
        val result = call("customerQuoteGuestCart", payload(lines))
        if (result is AppResult.Success) saveGuest(lines + result.value.lines.associate { it.productId to it.quantity })
        result
    } else call(
        "customerSetCartQuantity",
        mapOf("productId" to productId, "quantity" to quantity.coerceIn(1, 99)),
    )

    override suspend fun remove(productId: String): AppResult<CustomerCart> = if (isGuest) {
        val lines = guestLines(); lines.remove(productId)
        saveGuest(lines)
        call("customerQuoteGuestCart", payload(lines))
    } else call(
        "customerRemoveCartItem",
        mapOf("productId" to productId),
    )

    override suspend fun addProduct(productId: String, quantity: Int): AppResult<CartMutation> {
        if (!isGuest) return try {
            val response = functions.getHttpsCallable("customerAddToCart")
                .call(mapOf("productId" to productId, "quantity" to quantity.coerceIn(1, 99)))
                .await().data as? Map<*, *> ?: error("Invalid cart response")
            val count = (response["cartQuantity"] as? Number)?.toInt() ?: 0
            badgeStore.update(count)
            AppResult.Success(CartMutation(count))
        } catch (error: Exception) { AppResult.Failure(error.toCartError()) }
        val lines = guestLines()
        lines[productId] = ((lines[productId] ?: 0) + quantity).coerceIn(1, 99)
        return when (val quote = call("customerQuoteGuestCart", payload(lines))) {
            is AppResult.Failure -> quote
            is AppResult.Success -> {
                val line = quote.value.lines.firstOrNull { it.productId == productId }
                if (line?.available != true) AppResult.Failure(AppError.NotFound)
                else { saveGuest(lines + (productId to line.quantity)); AppResult.Success(CartMutation(lines.size)) }
            }
        }
    }

    override suspend fun mergeGuestCart(): AppResult<CustomerCart> {
        val lines = guestLines()
        if (lines.isEmpty()) return getCart()
        if (isGuest) return AppResult.Failure(AppError.Unauthorized)
        val result = call("customerMergeGuestCart", payload(lines))
        if (result is AppResult.Success) clearGuestCart()
        return result
    }

    override fun clearGuestCart() { guestPreferences.edit().remove("lines").apply() }

    private suspend fun call(name: String, payload: Map<String, Any>): AppResult<CustomerCart> = try {
        val response = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: error("Invalid cart response")
        val cartMap = response["cart"] as? Map<*, *> ?: response
        val cart = cartMap.toCart()
        badgeStore.update(cart.distinctItemCount)
        AppResult.Success(cart)
    } catch (error: Exception) {
        Log.e("CustomerCart", "$name failed", error)
        AppResult.Failure(error.toCartError())
    }
}

private fun Map<*, *>.toCart() = CustomerCart(
    lines = maps("lines").map { it.toLine() },
    subtotalMinor = long("subtotalMinor"),
    discountMinor = long("discountMinor"),
    deliveryFeeMinor = long("deliveryFeeMinor"),
    totalMinor = long("totalMinor"),
    deliveryEstimate = this["deliveryEstimate"] as? String,
    deliveryAddress = (this["deliveryAddress"] as? Map<*, *>)?.toAddress(),
    minimumOrderMinor = long("minimumOrderMinor"),
    adjustments = maps("adjustments").mapNotNull { it.toAdjustment() },
)

private fun Map<*, *>.toLine() = CartLine(
    productId = string("productId"), name = string("name"),
    imageUrl = this["imageUrl"] as? String,
    unit = runCatching { ProductUnit.valueOf(string("unit")) }.getOrDefault(ProductUnit.PIECE),
    unitPriceMinor = long("unitPriceMinor"), regularPriceMinor = long("regularPriceMinor"),
    quantity = int("quantity"), maxQuantity = int("maxQuantity"),
    available = this["available"] as? Boolean ?: false,
)

private fun Map<*, *>.toAdjustment(): CartAdjustment? {
    val type = runCatching { CartAdjustmentType.valueOf(string("type")) }.getOrNull() ?: return null
    return CartAdjustment(string("productId"), type, string("message"))
}

private fun Map<*, *>.toAddress() = CustomerAddress(
    id = string("id"),
    type = runCatching { AddressType.valueOf(string("type")) }.getOrDefault(AddressType.OTHER),
    name = string("name"), mobile = string("mobile"), address = string("address"),
    landmark = this["landmark"] as? String ?: "", city = string("city"),
    state = string("state"), postalCode = string("postalCode"),
    isDefault = this["isDefault"] as? Boolean ?: false, revision = long("revision"),
)

private fun Map<*, *>.maps(key: String) = (this[key] as? List<*>).orEmpty()
    .mapNotNull { it as? Map<*, *> }
private fun Map<*, *>.string(key: String) = this[key] as? String ?: error("Missing $key")
private fun Map<*, *>.long(key: String) = (this[key] as? Number)?.toLong() ?: 0L
private fun Map<*, *>.int(key: String) = (this[key] as? Number)?.toInt() ?: 0

private fun Throwable.toCartError(): AppError {
    val exception = this as? FirebaseFunctionsException
    val reason = (exception?.details as? Map<*, *>)?.get("reason") as? String
    return when {
        reason == "OUT_OF_STOCK" -> AppError.Validation(message = "Stock changed. Your cart was refreshed")
        reason == "PRODUCT_UNAVAILABLE" -> AppError.NotFound
        exception?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        exception?.code == FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        exception?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        exception?.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> AppError.Timeout
        else -> AppError.Unknown(this)
    }
}

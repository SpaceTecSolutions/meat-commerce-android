package com.spacetecsolutions.meatapp.core.data.firebase.home

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerHomeRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.spacetecsolutions.meatapp.core.domain.service.CartBadgeStore
import com.spacetecsolutions.meatapp.core.data.firebase.promotion.toPromotion

internal class FirebaseCustomerHomeRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val cartBadgeStore: CartBadgeStore,
) : CustomerHomeRepository {
    override suspend fun updateProfile(firstName: String, lastName: String): AppResult<Unit> = try {
        functions.getHttpsCallable("customerUpdateProfile")
            .call(mapOf("firstName" to firstName, "lastName" to lastName)).await()
        AppResult.Success(Unit)
    } catch (error: Exception) { AppResult.Failure(error.toHomeError()) }
    override suspend fun deleteAccount(): AppResult<Unit> = try {
        functions.getHttpsCallable("customerDeleteAccount").call().await()
        cartBadgeStore.update(0)
        AppResult.Success(Unit)
    } catch (error: Exception) {
        val callable = error as? FirebaseFunctionsException
        AppResult.Failure(if (callable?.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION)
            AppError.Validation(message = "Complete or cancel your active order first")
        else error.toHomeError())
    }
    // Public catalog-only fallback. Never caches location, cart, or notification data.
    private var cachedCatalog: CustomerHomeData? = null
    override suspend fun getHome(): AppResult<CustomerHomeData> = try {
        val data = functions.getHttpsCallable("customerGetHome").call().await().data as? Map<*, *>
            ?: error("Invalid home response")
        val home = data.toHome()
        cartBadgeStore.update(home.cartQuantity)
        cachedCatalog = home.publicCatalogSnapshot()
        AppResult.Success(home)
    } catch (error: Exception) {
        val appError = error.toHomeError()
        val cached = cachedCatalog
        if (cached != null && appError in setOf(AppError.Network, AppError.Timeout, AppError.Offline)) {
            AppResult.Success(cached.copy(catalogStale = true))
        } else AppResult.Failure(appError)
    }
}

private fun CustomerHomeData.publicCatalogSnapshot() = CustomerHomeData(
    banners = banners, categories = categories, bestSellers = bestSellers, offers = offers,
    recommended = recommended, newProducts = newProducts, offersEnabled = offersEnabled,
    popularThisWeek = popularThisWeek, quickPicks = quickPicks,
    promotions = promotions, catalogCachedAtEpochMillis = System.currentTimeMillis(),
)

private fun Map<*, *>.toHome() = CustomerHomeData(
    deliveryLocationLabel = this["deliveryLocationLabel"] as? String ?: "Select delivery location",
    deliveryAddressLabel = this["deliveryAddressLabel"] as? String ?: "Select location",
    unreadNotifications = number("unreadNotifications").toInt().coerceAtLeast(0),
    cartQuantity = number("cartQuantity").toInt().coerceAtLeast(0),
    banners = maps("banners").map { it.toBanner() },
    categories = maps("categories").map { it.toCategory() },
    bestSellers = maps("bestSellers").map { it.toProduct() },
    offers = maps("offers").map { it.toProduct() },
    recommended = maps("recommended").map { it.toProduct() },
    popularThisWeek = maps("popularThisWeek").map { it.toProduct() },
    quickPicks = maps("quickPicks").map { it.toProduct() },
    newProducts = maps("newProducts").map { it.toProduct() },
    offersEnabled = this["offersEnabled"] as? Boolean ?: false,
    promotions = maps("promotions").map { it.toPromotion() }
)

private fun Map<*, *>.toCategory() = ProductCategory(
    id = string("id"), name = string("name"),
    description = this["description"] as? String ?: "",
    imageUrl = this["imageUrl"] as? String, active = true,
    sortOrder = number("sortOrder").toInt(), productCount = number("productCount").toInt(),
)

private fun Map<*, *>.toProduct() = Product(
    id = string("id"), categoryId = string("categoryId"),
    categoryName = this["categoryName"] as? String ?: "",
    name = string("name"), description = this["description"] as? String ?: "",
    unit = runCatching { ProductUnit.valueOf(string("unit")) }.getOrDefault(ProductUnit.PIECE),
    priceMinor = number("priceMinor").toLong(),
    offerPriceMinor = (this["offerPriceMinor"] as? Number)?.toLong(),
    stockQuantity = number("stockQuantity").toDouble(),
    imageUrls = (this["imageUrls"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
    attributes = (this["attributes"] as? Map<*, *>).orEmpty().entries.mapNotNull { (key, value) ->
        if (key is String && value is String) key to value else null
    }.toMap(),
    ratingAverage = (this["ratingAverage"] as? Number)?.toDouble()?.takeIf { it > 0.0 },
    ratingCount = number("ratingCount").toInt().coerceAtLeast(0),
)

private fun Map<*, *>.maps(key: String) = (this[key] as? List<*>).orEmpty()
    .mapNotNull { it as? Map<*, *> }
private fun Map<*, *>.string(key: String) = this[key] as? String ?: error("Missing $key")
private fun Map<*, *>.number(key: String) = this[key] as? Number ?: 0

private fun Throwable.toHomeError(): AppError {
    val exception = this as? FirebaseFunctionsException
    return when (exception?.code) {
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
        FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
        FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> AppError.Timeout
        else -> AppError.Unknown(this)
    }
}

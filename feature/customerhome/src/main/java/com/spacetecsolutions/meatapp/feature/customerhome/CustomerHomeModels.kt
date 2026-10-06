package com.spacetecsolutions.meatapp.feature.customerhome

import com.spacetecsolutions.meatapp.core.model.CustomerHomeData
import com.spacetecsolutions.meatapp.core.model.CustomerCart

data class CustomerHomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val data: CustomerHomeData? = null,
    val error: String? = null,
    val cartQuantities: Map<String, Int> = emptyMap(),
    val cart: CustomerCart? = null,
    val cartLoaded: Boolean = false,
    val updatingProductIds: Set<String> = emptySet(),
    val cartMessage: String? = null,
)

object CustomerHomeSectionPolicy {
    fun showOffers(data: CustomerHomeData) = data.offersEnabled && data.offers.isNotEmpty()
    fun validPromotions(data: CustomerHomeData, now: Long = System.currentTimeMillis()) =
        data.promotions.filter { it.isValidAt(now) }
}

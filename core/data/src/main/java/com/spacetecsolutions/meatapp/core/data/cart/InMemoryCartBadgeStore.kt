package com.spacetecsolutions.meatapp.core.data.cart

import com.spacetecsolutions.meatapp.core.domain.service.CartBadgeStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class InMemoryCartBadgeStore @Inject constructor() : CartBadgeStore {
    private val mutableQuantity = MutableStateFlow(0)
    override val quantity = mutableQuantity.asStateFlow()
    override fun update(quantity: Int) { mutableQuantity.value = quantity.coerceAtLeast(0) }
}

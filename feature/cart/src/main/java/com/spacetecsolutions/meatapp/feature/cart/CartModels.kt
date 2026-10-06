package com.spacetecsolutions.meatapp.feature.cart

import com.spacetecsolutions.meatapp.core.model.CustomerCart

data class CartUiState(
    val loading: Boolean = true,
    val cart: CustomerCart = CustomerCart(),
    val busyProductIds: Set<String> = emptySet(),
    val editing: Boolean = false,
    val error: String? = null,
    val message: CartMessage? = null,
)

data class CartMessage(val text: String, val success: Boolean)

package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CustomerCart
import com.spacetecsolutions.meatapp.core.model.CartMutation

interface CustomerCartRepository {
    suspend fun getCart(): AppResult<CustomerCart>
    suspend fun setQuantity(productId: String, quantity: Int): AppResult<CustomerCart>
    suspend fun remove(productId: String): AppResult<CustomerCart>
    suspend fun addProduct(productId: String, quantity: Int): AppResult<CartMutation>
    suspend fun mergeGuestCart(): AppResult<CustomerCart>
    fun clearGuestCart()
}

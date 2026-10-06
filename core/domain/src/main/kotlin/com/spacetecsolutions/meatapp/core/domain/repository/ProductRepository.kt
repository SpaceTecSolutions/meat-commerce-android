package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.Product
import com.spacetecsolutions.meatapp.core.model.ProductImageUpload
import com.spacetecsolutions.meatapp.core.model.ProductInput
import com.spacetecsolutions.meatapp.core.model.ProductPage
import com.spacetecsolutions.meatapp.core.model.ProductQuery
import com.spacetecsolutions.meatapp.core.model.CartMutation
import com.spacetecsolutions.meatapp.core.model.CustomerProductPage
import com.spacetecsolutions.meatapp.core.model.CustomerProductQuery

interface ProductRepository {
    suspend fun getCustomerProduct(productId: String): AppResult<Product>
    suspend fun getCustomerProducts(query: CustomerProductQuery): AppResult<CustomerProductPage>
    suspend fun addProductToCart(productId: String, quantity: Int = 1): AppResult<CartMutation>
    suspend fun getAdminProducts(query: ProductQuery): AppResult<ProductPage>
    suspend fun uploadProductImage(localUri: String): AppResult<ProductImageUpload>
    suspend fun createProduct(input: ProductInput): AppResult<Product>
    suspend fun updateProduct(input: ProductInput): AppResult<Product>
    suspend fun setProductActive(
        productId: String,
        active: Boolean,
        expectedRevision: Long,
    ): AppResult<Product>
    suspend fun archiveProduct(productId: String, expectedRevision: Long): AppResult<Unit>
}

package com.spacetecsolutions.meatapp.feature.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductValidatorTest {
    private fun validForm() = ProductFormState(
        name = "Chicken Curry Cut",
        categoryId = "chicken",
        price = "249.50",
        offerPrice = "229.00",
        stock = "12.5",
    )

    @Test
    fun `valid product and exact currency conversion are accepted`() {
        assertTrue(ProductValidator.validate(validForm()).valid)
        assertEquals(24950L, "249.50".toMinorUnits())
    }

    @Test
    fun `offer must be below regular price`() {
        val result = ProductValidator.validate(validForm().copy(offerPrice = "249.50"))
        assertFalse(result.valid)
        assertTrue(result.offerPriceError != null)
    }

    @Test
    fun `negative stock and duplicate attribute names are rejected`() {
        val result = ProductValidator.validate(
            validForm().copy(
                stock = "-1",
                attributes = listOf(
                    ProductAttributeInput("Cut", "Small"),
                    ProductAttributeInput("cut", "Large"),
                ),
            ),
        )
        assertTrue(result.stockError != null)
        assertTrue(result.attributesError != null)
    }

    @Test
    fun `unknown product limit does not block product creation`() {
        assertFalse(ProductManagementUiState(productLimit = 0).limitReached)
        assertTrue(ProductManagementUiState(countedProducts = 10, productLimit = 10).limitReached)
    }
}

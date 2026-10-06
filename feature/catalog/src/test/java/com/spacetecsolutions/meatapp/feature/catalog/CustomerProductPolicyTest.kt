package com.spacetecsolutions.meatapp.feature.catalog

import com.spacetecsolutions.meatapp.core.model.Product
import com.spacetecsolutions.meatapp.core.model.ProductUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerProductPolicyTest {
    private fun product(active: Boolean = true, archived: Boolean = false, stock: Double = 1.0) =
        Product("p", "c", "Chicken", "sc","Curry Cut", "", "",ProductUnit.KILOGRAM,
            25000, stockQuantity = stock, active = active, archived = archived)

    @Test
    fun `only active non archived stocked products can be added`() {
        assertTrue(CustomerProductPolicy.canAddToCart(product()))
        assertFalse(CustomerProductPolicy.canAddToCart(product(active = false)))
        assertFalse(CustomerProductPolicy.canAddToCart(product(archived = true)))
        assertFalse(CustomerProductPolicy.canAddToCart(product(stock = 0.0)))
    }

    @Test
    fun `quantity is stock bounded and discount uses minor units`() {
        val discounted = product(stock = 150.0).copy(priceMinor = 25000, offerPriceMinor = 20000)
        assertTrue(CustomerProductPolicy.maxQuantity(discounted) == 99)
        assertTrue(CustomerProductPolicy.discountPercent(discounted) == 20L)
    }

    @Test
    fun `product search is local and matches name or description`() {
        val item = product().copy(name = "Mutton Curry Cut", description = "Bone-in pieces")
        assertTrue(item.matchesCustomerSearch(" curry "))
        assertTrue(item.matchesCustomerSearch("BONE-IN"))
        assertFalse(item.matchesCustomerSearch("fish"))
    }

    @Test
    fun `subtype filters are data driven from product attributes`() {
        val boneless = product().copy(attributes = mapOf("Cut" to "Boneless"))
        val state = CustomerCategoriesUiState(
            products = listOf(boneless, product().copy(id = "other")),
            selectedSubtype = "boneless",
        )
//        assertEquals(listOf("Boneless"), state.productSubtypes)
        assertEquals(listOf(boneless), state.visibleProducts)
    }

    @Test
    fun `customer category list excludes unrelated and inactive products`() {
        val expected = product()
        val unrelated = product().copy(id = "other", categoryId = "mutton")
        val inactive = product(active = false).copy(id = "inactive")
        assertEquals(
            listOf(expected),
            listOf(expected, unrelated, inactive).validForCustomerCategory("c"),
        )
    }

    @Test
    fun `effective price accepts only a valid lower offer`() {
        assertEquals(20000L, product().copy(priceMinor = 25000, offerPriceMinor = 20000).effectivePriceMinor)
        assertEquals(25000L, product().copy(priceMinor = 25000, offerPriceMinor = 26000).effectivePriceMinor)
    }
}

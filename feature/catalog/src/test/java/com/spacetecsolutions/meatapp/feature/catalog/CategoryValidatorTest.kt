package com.spacetecsolutions.meatapp.feature.catalog

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.spacetecsolutions.meatapp.core.model.ProductCategory

class CategoryValidatorTest {
    @Test
    fun `valid category name is accepted`() {
        assertNull(CategoryValidator.name("Fresh Chicken"))
    }

    @Test
    fun `blank short and oversized names are rejected`() {
        assertTrue(CategoryValidator.name(" ") != null)
        assertTrue(CategoryValidator.name("A") != null)
        assertTrue(CategoryValidator.name("A".repeat(61)) != null)
    }

    @Test
    fun `category search is trimmed and case insensitive`() {
        val category = ProductCategory(id = "chicken", name = "Fresh Chicken")
        assertTrue(category.matchesSearch("  CHICK  "))
        assertTrue(!category.matchesSearch("mutton"))
    }

    @Test
    fun `customer categories exclude inactive and use stable display order`() {
        val categories = listOf(
            ProductCategory(id = "z", name = "Zebra", sortOrder = 1),
            ProductCategory(id = "b", name = "beef", sortOrder = 0),
            ProductCategory(id = "a", name = "Apple", sortOrder = 0),
            ProductCategory(id = "hidden", name = "Hidden", active = false, sortOrder = -1),
        )

        assertEquals(listOf("Apple", "beef", "Zebra"), categories.forCustomerDisplay().map { it.name })
    }

    @Test
    fun `item count uses singular and plural labels`() {
        assertEquals("1 item", categoryItemCount(1))
        assertEquals("0 items", categoryItemCount(0))
        assertEquals("24 items", categoryItemCount(24))
    }
}

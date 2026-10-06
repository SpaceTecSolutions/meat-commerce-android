package com.spacetecsolutions.meatapp.feature.customerhome

import com.spacetecsolutions.meatapp.core.model.CustomerHomeData
import com.spacetecsolutions.meatapp.core.model.Product
import com.spacetecsolutions.meatapp.core.model.ProductUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerHomeSectionPolicyTest {
    private val offer = Product(
        id = "p1", categoryId = "c1", categoryName = "Chicken", name = "Curry Cut",
        description = "", unit = ProductUnit.KILOGRAM, priceMinor = 25000,
        offerPriceMinor = 22500, stockQuantity = 10.0,
    )

    @Test
    fun `offers require both feature authorization and content`() {
        assertFalse(CustomerHomeSectionPolicy.showOffers(CustomerHomeData(offers = listOf(offer))))
        assertFalse(CustomerHomeSectionPolicy.showOffers(CustomerHomeData(offersEnabled = true)))
        assertTrue(CustomerHomeSectionPolicy.showOffers(
            CustomerHomeData(offersEnabled = true, offers = listOf(offer)),
        ))
    }
}

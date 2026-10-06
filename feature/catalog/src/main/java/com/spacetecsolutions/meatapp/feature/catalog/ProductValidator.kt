package com.spacetecsolutions.meatapp.feature.catalog

import java.math.BigDecimal
import java.math.RoundingMode

data class ProductValidation(
    val nameError: String? = null,
    val categoryError: String? = null,
    val priceError: String? = null,
    val offerPriceError: String? = null,
    val stockError: String? = null,
    val attributesError: String? = null,
) {
    val valid: Boolean get() = listOf(
        nameError, categoryError, priceError, offerPriceError, stockError, attributesError,
    ).all { it == null }
}

object ProductValidator {
    fun validate(form: ProductFormState): ProductValidation {
        val price = form.price.toMinorUnits()
        val offer = form.offerPrice.takeIf(String::isNotBlank)?.toMinorUnits()
        val stock = form.stock.toDoubleOrNull()
        val keys = form.attributes.map { it.key.trim().lowercase() }.filter(String::isNotEmpty)
        return ProductValidation(
            nameError = when {
                form.name.isBlank() -> "Product name is required"
                form.name.trim().length !in 2..100 -> "Enter a valid product name"
                else -> null
            },
            categoryError = if (form.categoryId.isBlank()) "Choose a category" else null,
            priceError = if (price == null || price <= 0) "Enter a valid price" else null,
            offerPriceError = when {
                form.offerPrice.isBlank() -> null
                offer == null || offer < 0 -> "Enter a valid offer price"
                price != null && offer >= price -> "Offer price must be lower than price"
                else -> null
            },
            stockError = if (stock == null || stock < 0) "Enter valid stock" else null,
            attributesError = when {
                form.attributes.size > 20 -> "Use at most 20 attributes"
                keys.size != keys.distinct().size -> "Attribute names must be unique"
                form.attributes.any { it.key.isBlank() != it.value.isBlank() } ->
                    "Complete both attribute name and value"
                form.attributes.any { it.key.length > 30 || it.value.length > 100 } ->
                    "An attribute is too long"
                else -> null
            },
        )
    }
}

fun String.toMinorUnits(): Long? = runCatching {
    BigDecimal(trim()).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
}.getOrNull()

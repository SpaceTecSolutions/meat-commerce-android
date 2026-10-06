package com.spacetecsolutions.meatapp.feature.catalog

import com.spacetecsolutions.meatapp.core.model.Product
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal val Product.effectivePriceMinor: Long
    get() = validOfferPriceMinor ?: priceMinor

internal val Product.validOfferPriceMinor: Long?
    get() = offerPriceMinor?.takeIf { it in 1 until priceMinor }

internal fun formatProductMoney(minor: Long): String = NumberFormat.getCurrencyInstance(
    Locale.forLanguageTag("en-IN"),
).apply {
    currency = Currency.getInstance("INR")
    maximumFractionDigits = 0
}.format(minor / 100.0)

internal fun Double.cleanProductQuantity(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

internal fun Map.Entry<String, String>.productAttributeLabel(): String =
    if (value.isBlank() || value.equals("true", true) || value.equals("yes", true)) key
    else "$key: $value"

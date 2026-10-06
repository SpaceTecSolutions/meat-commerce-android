package com.spacetecsolutions.meatapp.feature.checkout

import com.spacetecsolutions.meatapp.core.model.*

data class CheckoutUiState(
    val loading: Boolean = true,
    val placing: Boolean = false,
    val reviewing: Boolean = false,
    val deliveryStep: Boolean = false,
    val quote: CheckoutQuote? = null,
    val addressId: String? = null,
    val deliveryOption: CheckoutDeliveryOption? = null,
    val deliveryDateId: String? = null,
    val deliverySlotId: String? = null,
    val instructions: String = "",
    val paymentMethod: CheckoutPaymentMethod? = null,
    val placedOrder: PlacedOrder? = null,
    val razorpaySession: RazorpayPaymentSession? = null,
    val razorpayLaunchPending: Boolean = false,
    val paymentAttempt: PaymentAttempt? = null,
    val recoveringPayment: Boolean = false,
    val error: String? = null,
    val message: CheckoutMessage? = null,
) {
    val selectionComplete: Boolean get() = addressId != null &&
        deliveryOption == CheckoutDeliveryOption.SCHEDULED && deliveryDateId != null &&
        deliverySlotId != null && paymentMethod != null &&
        (quote == null || paymentMethod in quote.paymentMethods)
}

data class CheckoutMessage(val text: String, val success: Boolean)

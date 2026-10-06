package com.spacetecsolutions.meatapp.feature.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.PaymentAttemptStatus

@Composable
internal fun RazorpayPaymentContent(
    state: CheckoutUiState,
    padding: PaddingValues,
    viewOrders: () -> Unit,
    continueShopping: () -> Unit,
    retry: () -> Unit,
    reconcile: () -> Unit,
) {
    val attempt = state.paymentAttempt
    val status = attempt?.status ?: PaymentAttemptStatus.VERIFICATION_PENDING
    if (status == PaymentAttemptStatus.PAID && state.placedOrder != null) {
        OrderPlacedContent(state.placedOrder, padding, viewOrders, continueShopping)
        return
    }
    val failed = status in setOf(PaymentAttemptStatus.FAILED, PaymentAttemptStatus.CANCELLED)
    val accent = if (failed) CustomerOrdersTokens.red else MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize().padding(padding).background(CustomerOrdersTokens.canvas),
        contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth)
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = CustomerOrdersTokens.surface,
                border = BorderStroke(1.dp, CustomerOrdersTokens.border), shadowElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = if (failed) CustomerOrdersTokens.rose else CustomerOrdersTokens.soft,
                        shape = RoundedCornerShape(50)) {
                        Icon(if (status == PaymentAttemptStatus.PAID) AppIcons.PaymentSuccess
                            else if (failed) AppIcons.PaymentFailed else AppIcons.Pending,
                            null, Modifier.padding(17.dp).size(34.dp), tint = accent)
                    }
                    Text(status.title(), style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = CustomerOrdersTokens.ink)
                    Text(attempt?.failureMessage ?: status.description(),
                        color = CustomerOrdersTokens.muted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center)
                }
            }
            if (failed) Surface(shape = RoundedCornerShape(16.dp),
                color = CustomerOrdersTokens.surface,
                border = BorderStroke(1.dp, CustomerOrdersTokens.border)) {
                Text("If money was deducted, check your payment status before retrying. " +
                    "Your bank or payment provider can confirm the transaction.",
                    Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall,
                    color = CustomerOrdersTokens.muted)
            }
            when {
                status == PaymentAttemptStatus.PAID -> Button(viewOrders,
                    Modifier.fillMaxWidth().height(50.dp)) { Text("View order") }
                attempt?.retryAllowed == true -> {
                    Button(retry, Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CustomerOrdersTokens.red)) {
                        Text("Retry payment")
                    }
                    OutlinedButton(viewOrders, Modifier.fillMaxWidth().height(50.dp)) {
                        Text("View my order")
                    }
                }
                status !in setOf(PaymentAttemptStatus.REFUNDED, PaymentAttemptStatus.PARTIALLY_REFUNDED) ->
                    OutlinedButton(reconcile, Modifier.fillMaxWidth().height(50.dp)) {
                        Text("Check payment status")
                    }
            }
        }
    }
}

private fun PaymentAttemptStatus.title() = when (this) {
    PaymentAttemptStatus.PAID -> "Payment verified"
    PaymentAttemptStatus.FAILED -> "Payment failed"
    PaymentAttemptStatus.CANCELLED -> "Payment cancelled"
    PaymentAttemptStatus.REFUND_PENDING -> "Refund pending"
    PaymentAttemptStatus.REFUNDED -> "Payment refunded"
    PaymentAttemptStatus.PARTIALLY_REFUNDED -> "Partially refunded"
    else -> "Verification pending"
}
private fun PaymentAttemptStatus.description() = when (this) {
    PaymentAttemptStatus.PAID -> "Your payment was securely verified by the backend."
    PaymentAttemptStatus.FAILED -> "No verified payment was found. You can retry safely."
    PaymentAttemptStatus.CANCELLED -> "You cancelled Razorpay Checkout. No payment was confirmed."
    PaymentAttemptStatus.REFUND_PENDING -> "The refund has been initiated."
    PaymentAttemptStatus.REFUNDED -> "The payment has been refunded."
    PaymentAttemptStatus.PARTIALLY_REFUNDED -> "Part of this payment has been refunded."
    else -> "We are checking the callback and webhook with Razorpay. Do not pay again yet."
}

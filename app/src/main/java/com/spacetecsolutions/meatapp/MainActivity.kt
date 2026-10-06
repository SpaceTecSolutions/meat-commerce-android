package com.spacetecsolutions.meatapp

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.AndroidEntryPoint
import com.spacetecsolutions.meatapp.core.data.firebase.auth.PhoneAuthActivityProvider
import com.spacetecsolutions.meatapp.core.model.RazorpayPaymentSession
import com.spacetecsolutions.meatapp.core.model.RazorpaySdkResult
import com.spacetecsolutions.meatapp.core.domain.repository.MessagingRepository
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    @Inject lateinit var phoneAuthActivityProvider: PhoneAuthActivityProvider
    @Inject lateinit var messagingRepository: MessagingRepository
    private val pendingDeepLink = mutableStateOf<String?>(null)
    private val pendingNotificationOrderId = mutableStateOf<String?>(null)
    private val razorpayResult = mutableStateOf<RazorpaySdkResult?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Checkout.preload(applicationContext)
        enableEdgeToEdge()
        pendingDeepLink.value = notificationDeepLink(intent)
        pendingNotificationOrderId.value = intent?.getStringExtra("notificationOrderId")
        logNotificationOpen(intent)
        setContent {
            MeatBushApp(
                deepLink = pendingDeepLink.value,
                onDeepLinkConsumed = {
                    pendingDeepLink.value = null
                    intent?.data = null
                    intent?.removeExtra("deepLinkRoute")
                },
                notificationOrderId = pendingNotificationOrderId.value,
                onNotificationOrderConsumed = {
                    pendingNotificationOrderId.value = null
                    intent?.removeExtra("notificationOrderId")
                },
                razorpaySdkResult = razorpayResult.value,
                onRazorpayResultConsumed = { razorpayResult.value = null },
                launchRazorpay = ::launchRazorpay,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        phoneAuthActivityProvider.attach(this)
    }

    override fun onStop() {
        phoneAuthActivityProvider.detach(this)
        super.onStop()
    }

    private fun launchRazorpay(session: RazorpayPaymentSession) {
        val options = JSONObject().apply {
            put("key", session.publicKeyId)
            put("order_id", session.razorpayOrderId)
            put("amount", session.amountMinor)
            put("currency", session.currencyCode)
            put("name", session.merchantName)
            put("description", session.description)
            put("retry", JSONObject().put("enabled", true).put("max_count", 4))
            put("theme", JSONObject().put("color", "#D71920"))
            put("prefill", JSONObject().apply {
                session.customerName?.let { put("name", it) }
                session.customerMobile?.let { put("contact", it) }
                session.customerEmail?.let { put("email", it) }
            })
        }
        Checkout().apply {
            setKeyID(session.publicKeyId)
            open(this@MainActivity, options)
        }
    }

    override fun onPaymentSuccess(paymentId: String?, data: PaymentData?) {
        val orderId = data?.orderId
        val signature = data?.signature
        if (!paymentId.isNullOrBlank() && !orderId.isNullOrBlank() && !signature.isNullOrBlank()) {
            razorpayResult.value = RazorpaySdkResult.Success(
                UUID.randomUUID().toString(), paymentId, orderId, signature,
            )
        } else {
            razorpayResult.value = RazorpaySdkResult.Failure(
                UUID.randomUUID().toString(), -1, "Incomplete Razorpay callback", false,
            )
        }
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        val error = customerPaymentError(response)
        razorpayResult.value = RazorpaySdkResult.Failure(
            callbackId = UUID.randomUUID().toString(),
            code = code,
            description = error.message,
            cancelled = error.cancelled,
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.value = notificationDeepLink(intent)
        pendingNotificationOrderId.value = intent.getStringExtra("notificationOrderId")
        logNotificationOpen(intent)
    }

    private fun notificationDeepLink(intent: Intent?): String? = intent?.dataString ?: intent
        ?.getStringExtra("deepLinkRoute")
        ?.takeIf { it.matches(Regex("[a-z_]+/[a-z0-9_-]+")) }
        ?.let { "${getString(R.string.deep_link_scheme)}://app/$it" }

    private fun logNotificationOpen(intent: Intent?) {
        val notificationId = intent?.getStringExtra("notificationId")
        if (notificationId.isNullOrBlank()) return
        lifecycleScope.launch { messagingRepository.markNotificationRead(notificationId) }
    }
}

private data class CustomerPaymentError(val message: String, val cancelled: Boolean)

private fun customerPaymentError(response: String?): CustomerPaymentError {
    val error = runCatching { JSONObject(response.orEmpty()).optJSONObject("error") }.getOrNull()
    val code = error?.optString("code").orEmpty()
    val reason = error?.optString("reason").orEmpty()
    val description = error?.optString("description").orEmpty()
    val cancelled = sequenceOf(code, reason, description).any { it.contains("cancel", ignoreCase = true) }
    if (cancelled) return CustomerPaymentError("Payment was cancelled. You can retry when ready.", true)
    val usefulDescription = description.takeIf {
        it.isNotBlank() && !it.equals("undefined", true) && !it.equals("null", true)
    }
    val message = usefulDescription ?: when {
        reason.contains("payment", true) -> "Payment could not be completed. Try another UPI app or payment method."
        code == "BAD_REQUEST_ERROR" -> "This payment option could not be started. Choose another payment method and retry."
        else -> "Payment could not be completed. Check your connection or choose another method."
    }
    return CustomerPaymentError(message, false)
}

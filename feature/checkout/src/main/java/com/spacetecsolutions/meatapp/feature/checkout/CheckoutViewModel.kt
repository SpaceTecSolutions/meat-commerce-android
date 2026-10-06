package com.spacetecsolutions.meatapp.feature.checkout

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.CheckoutRepository
import com.spacetecsolutions.meatapp.core.domain.repository.RazorpayPaymentRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val repository: CheckoutRepository,
    private val razorpayRepository: RazorpayPaymentRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CheckoutUiState())
    val state = mutableState.asStateFlow()
    private var idempotencyKey: String
        get() = savedStateHandle[ORDER_KEY]
            ?: UUID.randomUUID().toString().also { savedStateHandle[ORDER_KEY] = it }
        set(value) { savedStateHandle[ORDER_KEY] = value }

    init {
        if (savedStateHandle.get<Boolean>(ORDER_PENDING) == true ||
            savedStateHandle.get<Boolean>(PAYMENT_PENDING) == true) reconcileOrderCreation(true)
        else prepare()
    }

    fun beginVisit() {
        if (state.value.placedOrder == null || savedStateHandle.get<Boolean>(PAYMENT_PENDING) == true) return
        mutableState.value = CheckoutUiState()
        savedStateHandle[ORDER_PENDING] = false
        prepare()
    }

    fun prepare() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, reviewing = false, deliveryStep = false, error = null) }
        when (val result = repository.prepare()) {
            is AppResult.Success -> applyQuote(result.value)
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, error = result.error.checkoutText())
            }
        }
    }

    fun selectAddress(id: String) = mutableState.update { it.copy(addressId = id) }

    fun selectDate(id: String) = mutableState.update { state ->
        val date = state.quote?.deliveryDates?.firstOrNull { it.id == id }
        state.copy(
            deliveryDateId = id,
            deliverySlotId = state.deliverySlotId?.takeIf { it in date?.availableSlotIds.orEmpty() }
                ?: date?.availableSlotIds?.firstOrNull(),
        )
    }

    fun selectSlot(id: String) = mutableState.update { it.copy(deliverySlotId = id) }

    fun selectPaymentMethod(method: CheckoutPaymentMethod) {
        savedStateHandle[PAYMENT_METHOD] = method.name
        mutableState.update { it.copy(paymentMethod = method) }
    }

    fun setInstructions(value: String) {
        if (value.length <= 300) mutableState.update { it.copy(instructions = value) }
    }

    fun continueFromAddress() = mutableState.update {
        if (it.addressId != null) it.copy(deliveryStep = true)
        else it.copy(message = CheckoutMessage("Select a delivery address", false))
    }

    fun backToAddress() = mutableState.update { it.copy(deliveryStep = false) }

    fun review() = mutableState.update {
        if (it.deliveryStep && it.selectionComplete) it.copy(reviewing = true)
        else it.copy(message = CheckoutMessage("Select an address and delivery time", false))
    }

    fun edit() = mutableState.update { it.copy(reviewing = false, deliveryStep = true) }

    fun placeOrder() = viewModelScope.launch {
        val current = state.value; val quote = current.quote ?: return@launch
        if (current.placing || !current.reviewing || !current.selectionComplete) return@launch
        val request = PlaceOrderRequest(
            quoteToken = quote.quoteToken,
            addressId = current.addressId!!,
            deliveryOption = CheckoutDeliveryOption.SCHEDULED,
            deliveryDateId = current.deliveryDateId!!,
            deliverySlotId = current.deliverySlotId,
            instructions = current.instructions.trim(),
            paymentMethod = current.paymentMethod!!,
            idempotencyKey = idempotencyKey,
        )
        savedStateHandle[PAYMENT_METHOD] = current.paymentMethod.name
        mutableState.update { it.copy(placing = true) }
        savedStateHandle[ORDER_PENDING] = true
        when (val result = repository.placeOrder(request)) {
            is AppResult.Success -> handleCreatedOrder(result.value, current.paymentMethod)
            is AppResult.Failure -> {
                Log.e(TAG, "Place order failed", (result.error as? AppError.Unknown)?.cause)
                if (result.error in setOf(AppError.Network, AppError.Timeout, AppError.Offline)) {
                    reconcileOrderCreation(false)
                } else {
                    savedStateHandle[ORDER_PENDING] = false
                    handleRejectedOrder(result.error.checkoutText())
                }
            }
        }
    }

    private fun handleRejectedOrder(message: String) {
        if (message.contains("changed", true) || message.contains("available", true) ||
            message.contains("expired", true)) refreshReview(message)
        else mutableState.update { it.copy(placing = false, reviewing = true,
            message = CheckoutMessage(message, false)) }
    }

    private fun refreshReview(message: String) = viewModelScope.launch {
        when (val result = repository.prepare()) {
            is AppResult.Success -> {
                val previous = state.value
                applyQuote(result.value, reviewing = true, previous = previous)
                mutableState.update { it.copy(message = CheckoutMessage(message, false)) }
            }
            is AppResult.Failure -> mutableState.update { it.copy(
                placing = false, reviewing = true, message = CheckoutMessage(message, false),
            ) }
        }
    }

    private fun reconcileOrderCreation(restoring: Boolean) = viewModelScope.launch {
        mutableState.update { it.copy(placing = true, loading = restoring) }
        when (val result = repository.reconcileOrder(idempotencyKey)) {
            is AppResult.Success -> if (result.value != null) handleCreatedOrder(
                result.value!!, savedPaymentMethod(), restoring,
            ) else {
                savedStateHandle[ORDER_PENDING] = false
                if (restoring) prepare() else mutableState.update { it.copy(
                    placing = false, loading = false, reviewing = true,
                    message = CheckoutMessage("No order was created. You can retry safely.", false),
                ) }
            }
            is AppResult.Failure -> mutableState.update { it.copy(
                placing = false, loading = false, reviewing = true,
                message = CheckoutMessage("Order status is uncertain. Retry is safe and cannot duplicate it.", false),
            ) }
        }
    }

    private fun complete(order: PlacedOrder) {
        savedStateHandle[ORDER_PENDING] = false
        savedStateHandle[PAYMENT_PENDING] = false
        mutableState.update { it.copy(placing = false, loading = false, reviewing = false,
            placedOrder = order, message = CheckoutMessage("Order placed successfully", true)) }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun handleCreatedOrder(
        order: PlacedOrder,
        method: CheckoutPaymentMethod,
        restoring: Boolean = false,
    ) {
        savedStateHandle[ORDER_PENDING] = false
        if (method == CheckoutPaymentMethod.COD) { complete(order); return }
        savedStateHandle[PAYMENT_PENDING] = true
        mutableState.update { it.copy(placing = false, loading = false, reviewing = false,
            placedOrder = order, paymentMethod = method, recoveringPayment = true) }
        if (restoring) reconcileRazorpay() else createRazorpaySession(order.orderId)
    }

    private fun createRazorpaySession(orderId: String) = viewModelScope.launch {
        mutableState.update { it.copy(recoveringPayment = true) }
        when (val result = razorpayRepository.createSession(orderId, idempotencyKey)) {
            is AppResult.Success -> mutableState.update { it.copy(
                razorpaySession = result.value, razorpayLaunchPending = true,
                paymentAttempt = PaymentAttempt(result.value.attemptId, result.value.appOrderId,
                    PaymentAttemptStatus.CREATED, PaymentStatus.PROCESSING), recoveringPayment = false,
            ) }
            is AppResult.Failure -> paymentFailure(result.error.checkoutText())
        }
    }

    fun markRazorpayOpened() = viewModelScope.launch {
        val session = state.value.razorpaySession ?: return@launch
        mutableState.update { it.copy(razorpayLaunchPending = false) }
        when (val result = razorpayRepository.markSdkOpened(session.attemptId)) {
            is AppResult.Success -> mutableState.update { it.copy(paymentAttempt = result.value) }
            is AppResult.Failure -> Log.w(TAG, "Unable to record Razorpay SDK open")
        }
    }

    fun handleRazorpayResult(result: RazorpaySdkResult) = viewModelScope.launch {
        val attemptId = state.value.paymentAttempt?.attemptId
            ?: state.value.razorpaySession?.attemptId ?: return@launch
        mutableState.update { it.copy(recoveringPayment = true) }
        val verified = when (result) {
            is RazorpaySdkResult.Success -> razorpayRepository.verify(attemptId, result)
            is RazorpaySdkResult.Failure -> razorpayRepository.recordFailure(attemptId, result)
        }
        when (verified) {
            is AppResult.Success -> applyPaymentAttempt(verified.value)
            is AppResult.Failure -> paymentFailure(verified.error.checkoutText())
        }
    }

    fun reconcileRazorpay() = viewModelScope.launch {
        mutableState.update { it.copy(recoveringPayment = true) }
        when (val result = razorpayRepository.reconcilePending()) {
            is AppResult.Success -> result.value?.let(::applyPaymentAttempt) ?: paymentFailure(
                "Payment session was not found. Please return to your orders.")
            is AppResult.Failure -> paymentFailure(result.error.checkoutText())
        }
    }

    fun retryRazorpay() = viewModelScope.launch {
        val attempt = state.value.paymentAttempt ?: return@launch
        mutableState.update { it.copy(recoveringPayment = true) }
        when (val result = razorpayRepository.retry(attempt.attemptId, UUID.randomUUID().toString())) {
            is AppResult.Success -> mutableState.update { it.copy(
                razorpaySession = result.value, razorpayLaunchPending = true,
                paymentAttempt = PaymentAttempt(result.value.attemptId, result.value.appOrderId,
                    PaymentAttemptStatus.CREATED, PaymentStatus.PROCESSING), recoveringPayment = false,
            ) }
            is AppResult.Failure -> paymentFailure(result.error.checkoutText())
        }
    }

    private fun applyPaymentAttempt(attempt: PaymentAttempt) {
        if (attempt.status == PaymentAttemptStatus.PAID) savedStateHandle[PAYMENT_PENDING] = false
        mutableState.update { it.copy(paymentAttempt = attempt, recoveringPayment = false,
            razorpayLaunchPending = false, message = if (attempt.status == PaymentAttemptStatus.PAID)
                CheckoutMessage("Payment verified successfully", true) else null) }
    }

    private fun paymentFailure(message: String) = mutableState.update { it.copy(
        recoveringPayment = false, razorpayLaunchPending = false,
        paymentAttempt = it.paymentAttempt ?: PaymentAttempt("unknown", it.placedOrder?.orderId.orEmpty(),
            PaymentAttemptStatus.VERIFICATION_PENDING, PaymentStatus.PROCESSING, message),
        message = CheckoutMessage(message, false),
    ) }

    private fun savedPaymentMethod() = runCatching {
        CheckoutPaymentMethod.valueOf(savedStateHandle.get<String>(PAYMENT_METHOD).orEmpty())
    }.getOrDefault(CheckoutPaymentMethod.COD)

    private fun applyQuote(
        quote: CheckoutQuote,
        reviewing: Boolean = false,
        previous: CheckoutUiState = state.value,
    ) {
        if (!reviewing) idempotencyKey = UUID.randomUUID().toString()
        savedStateHandle[ORDER_PENDING] = false
        val address = previous.addressId?.takeIf { id -> quote.addresses.any { it.id == id } }
            ?: quote.defaultAddressId
        val date = previous.deliveryDateId?.let { id -> quote.deliveryDates.firstOrNull { it.id == id } }
            ?: quote.deliveryDates.firstOrNull()
        val slot = previous.deliverySlotId?.takeIf { it in date?.availableSlotIds.orEmpty() }
            ?: date?.availableSlotIds?.firstOrNull()
        mutableState.update { previous.copy(
            loading = false, placing = false, reviewing = reviewing,
            deliveryStep = reviewing || previous.deliveryStep, quote = quote,
            addressId = address, deliveryOption = CheckoutDeliveryOption.SCHEDULED,
            deliveryDateId = date?.id, deliverySlotId = slot,
            paymentMethod = previous.paymentMethod?.takeIf { it in quote.paymentMethods }
                ?: quote.paymentMethods.firstOrNull(), error = null,
        ) }
    }

    private companion object {
        const val TAG = "CheckoutViewModel"
        const val ORDER_KEY = "checkout_order_idempotency_key"
        const val ORDER_PENDING = "checkout_order_pending"
        const val PAYMENT_PENDING = "checkout_payment_pending"
        const val PAYMENT_METHOD = "checkout_payment_method"
    }
}

private fun AppError.checkoutText() = when (this) {
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    AppError.NotFound -> "An item or address is no longer available. Review checkout again"
    is AppError.Validation -> message
    AppError.Unauthorized -> "Please sign in again to continue"
    AppError.Forbidden -> "Your account cannot place an order right now"
    else -> "Unable to place your order. Please try again."
}

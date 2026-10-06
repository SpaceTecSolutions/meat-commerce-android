package com.spacetecsolutions.meatapp.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.CodOrderRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CategoryRepository
import com.spacetecsolutions.meatapp.core.domain.repository.DeliveryStaffRepository
import com.spacetecsolutions.meatapp.core.domain.repository.DeliveryConfigurationRepository
import com.spacetecsolutions.meatapp.core.domain.repository.LiveTrackingRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CodOrdersViewModel @Inject constructor(
    private val repository: CodOrderRepository,
    private val categoryRepository: CategoryRepository,
    private val staffRepository: DeliveryStaffRepository,
    private val deliveryConfigRepository: DeliveryConfigurationRepository,
    private val trackingRepository: LiveTrackingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CodOrdersUiState())
    val state = mutableState.asStateFlow()

    fun load(actor: CodActor, deliveryStaffAllowed: Boolean = false, trackingAllowed: Boolean = false) {
        if (state.value.actor == actor) {
            mutableState.update { it.copy(
                deliveryStaffAllowed = deliveryStaffAllowed,
                realtimeTrackingAllowed = trackingAllowed,
            ) }
            if (trackingAllowed) loadTrackingAdminConfig()
            return
        }
        mutableState.update { CodOrdersUiState(
            actor = actor,
            deliveryStaffAllowed = deliveryStaffAllowed,
            realtimeTrackingAllowed = trackingAllowed,
        ) }
        refresh()
        if (trackingAllowed) loadTrackingAdminConfig()
    }

    fun refresh() = viewModelScope.launch {
        val actor = state.value.actor ?: return@launch
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.getOrders(actor)) {
            is AppResult.Success -> mutableState.update {
                it.copy(loading = false, orders = result.value, error = null)
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, error = result.error.text())
            }
        }
        if (actor == CodActor.ADMIN) loadCategories()
    }

    private suspend fun loadCategories() {
        when (val result = categoryRepository.getAdminCategories()) {
            is AppResult.Success -> mutableState.update { current -> current.copy(
                categories = result.value.sortedWith(compareBy<ProductCategory> { it.sortOrder }.thenBy { it.name }),
            ) }
            is AppResult.Failure -> Unit // Orders remain usable; the category filter is simply unavailable.
        }
    }

    fun confirm(order: CodOrder, adjustments: List<FinalWeightAdjustment> = emptyList()) = mutate(order.id, "Final bill saved and order confirmed") {
        repository.confirm(order.id, order.revision, adjustments)
    }
    fun startPreparing(order: CodOrder) = mutate(order.id, "Order is now preparing") {
        repository.startPreparing(order.id, order.revision)
    }
    fun markReadyForDelivery(order: CodOrder) = mutate(order.id, "Ready for delivery assignment") {
        repository.markReadyForDelivery(order.id, order.revision)
    }
    fun selectStatus(status: OrderStatus) = mutableState.update { it.copy(selectedStatus = status) }
    fun selectOrder(order: CodOrder?) = mutableState.update { it.copy(selectedOrder = order) }
    fun startPersonalDelivery(order: CodOrder) = mutate(order.id, "Order assigned to you for delivery") {
        repository.startPersonalDelivery(order.id, order.revision)
    }
    fun requestAssignment(order: CodOrder?) {
        if (order == null) {
            mutableState.update { it.copy(assignmentOrder = null, eligibleStaff = emptyList(), loadingStaff = false) }
            return
        }
        if (!state.value.deliveryStaffAllowed || !order.canAssignDelivery) return
        mutableState.update { it.copy(assignmentOrder = order, loadingStaff = true) }
        viewModelScope.launch {
            when (val result = staffRepository.getStaff()) {
                is AppResult.Success -> mutableState.update {
                    it.copy(eligibleStaff = result.value.filter(User::active).sortedBy(User::displayName),
                        loadingStaff = false)
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(loadingStaff = false, message = CodOrderMessage(result.error.text(), false))
                }
            }
        }
    }
    fun assignDeliveryUser(user: User) {
        val order = state.value.assignmentOrder ?: return
        if (!state.value.deliveryStaffAllowed || !user.active) return
        mutableState.update { it.copy(assignmentOrder = null, eligibleStaff = emptyList()) }
        mutate(order.id, "Delivery user assigned") {
            repository.assignDeliveryUser(order.id, user.id, order.revision)
        }
    }
    fun startDelivery(order: CodOrder) {
        val actor = state.value.actor ?: return
        if (!order.canStartDelivery) return
        selectOrder(null)
        mutate(order.id, "Delivery started", onSuccess = { started ->
            if (started.canActivateTracking(
                    state.value.realtimeTrackingAllowed,
                    state.value.realtimeTrackingAdminEnabled,
                )) {
                mutableState.update { it.copy(trackingPermissionOrder = started) }
            }
        }) {
            repository.startDelivery(actor, order.id, order.revision)
        }
    }

    fun onTrackingPermission(granted: Boolean) {
        val order = state.value.trackingPermissionOrder ?: return
        mutableState.update { it.copy(trackingPermissionOrder = null) }
        if (!granted) {
            mutableState.update { it.copy(message = CodOrderMessage("Location permission denied; delivery remains status-based", false)) }
            return
        }
        val actor = state.value.actor ?: return
        val session = order.trackingSessionId ?: return
        viewModelScope.launch {
            when (trackingRepository.activate(actor, order.id, session)) {
                is AppResult.Success -> mutableState.update { it.copy(
                    trackingCommand = TrackingCommand.Start(order.id, session, actor),
                    message = CodOrderMessage("Live tracking started", true),
                ) }
                is AppResult.Failure -> mutableState.update { it.copy(
                    message = CodOrderMessage("Live tracking is no longer available", false),
                ) }
            }
        }
    }

    fun resumeLiveTracking(order: CodOrder) {
        val current = state.value
        if (!current.realtimeTrackingAllowed || !current.realtimeTrackingAdminEnabled ||
            !order.canResumeLiveTracking(current.actor)) return
        mutableState.update { it.copy(trackingPermissionOrder = order) }
    }

    fun consumeTrackingCommand() = mutableState.update { it.copy(trackingCommand = null) }

    private fun loadTrackingAdminConfig() = viewModelScope.launch {
        val enabled = (deliveryConfigRepository.get() as? AppResult.Success)?.value
            ?.realtimeTrackingEnabled == true
        mutableState.update { it.copy(realtimeTrackingAdminEnabled = enabled) }
    }
    fun showCancel(order: CodOrder?) = mutableState.update {
        it.copy(cancelOrder = order, cancelReason = "")
    }
    fun setCancelReason(value: String) {
        if (value.length <= 200) mutableState.update { it.copy(cancelReason = value) }
    }
    fun cancel() {
        val state = state.value
        val order = state.cancelOrder ?: return
        if (state.cancelReason.isBlank()) return
        mutate(order.id, "Order cancelled") {
            repository.cancel(order.id, order.revision, state.cancelReason.trim())
        }
        showCancel(null)
    }

    fun showCollection(order: CodOrder?) = mutableState.update { current ->
        val otpRequired = order != null && order.isRealtimeTrackingAvailable(
            current.realtimeTrackingAllowed, current.realtimeTrackingAdminEnabled)
        if (otpRequired && current.otpVerifiedSessionId != order.trackingSessionId)
            current.copy(otpOrder = order, otpInput = "", otpError = null,
                collectionOrder = null)
        else current.copy(collectionOrder = order,
            collectedAmount = order?.amountDueMinor?.asInput().orEmpty())
    }
    fun dismissOtp() = mutableState.update {
        if (it.otpBusy) it else it.copy(otpOrder = null, otpInput = "", otpError = null)
    }
    fun setOtpInput(value: String) {
        if (value.length <= 4 && value.all(Char::isDigit)) mutableState.update {
            it.copy(otpInput = value, otpError = null)
        }
    }
    fun verifyOtp() {
        val current = state.value
        val order = current.otpOrder ?: return
        if (current.otpBusy || current.otpInput.length != 4) return
        mutableState.update { it.copy(otpBusy = true, otpError = null) }
        viewModelScope.launch {
            when (val result = repository.verifyDeliveryOtp(order.id, current.otpInput)) {
                is AppResult.Success -> mutableState.update {
                    it.copy(otpBusy = false, otpOrder = null, otpInput = "",
                        otpVerifiedSessionId = order.trackingSessionId,
                        collectionOrder = order,
                        collectedAmount = order.amountDueMinor.asInput())
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(otpBusy = false, otpError = when (val error = result.error) {
                        is AppError.Validation -> error.message
                        else -> "Unable to verify OTP. Check your connection and try again."
                    })
                }
            }
        }
    }
    fun setCollectedAmount(value: String) {
        if (value.length <= 12 && value.matches(Regex("\\d*(\\.\\d{0,2})?"))) {
            mutableState.update { it.copy(collectedAmount = value) }
        }
    }
    fun completeDelivery() {
        val state = state.value
        val actor = state.actor ?: return
        val order = state.collectionOrder ?: return
        if (!order.canOpenDeliveryCompletion || !order.paymentReadyForDeliveryCompletion) return
        val actual = if (order.paymentMethod == CheckoutPaymentMethod.COD) {
            state.collectedAmount.toMinorUnitsOrNull() ?: return
        } else null
        if (order.paymentMethod == CheckoutPaymentMethod.COD && (actual == null || actual < order.amountDueMinor)) return
        val message = if (order.paymentMethod == CheckoutPaymentMethod.COD)
            "Delivery completed and COD collected" else "Delivery completed"
        showCollection(null)
        mutate(order.id, message, onSuccess = { delivered ->
            val session = delivered.trackingSessionId ?: order.trackingSessionId
            if (session != null) viewModelScope.launch { trackingRepository.stop(order.id, session) }
            mutableState.update { it.copy(trackingCommand = TrackingCommand.Stop) }
        }) {
            repository.completeDelivery(actor, order.id, order.paymentMethod, actual, order.revision)
        }
    }
    fun reportMismatch() {
        val state = state.value
        val actor = state.actor ?: return
        val order = state.collectionOrder ?: return
        if (order.paymentMethod != CheckoutPaymentMethod.COD || !order.canOpenDeliveryCompletion) return
        val actual = state.collectedAmount.toMinorUnitsOrNull() ?: return
        if (actual >= order.amountDueMinor) return
        mutate(order.id, "COD amount mismatch reported") {
            repository.reportAmountMismatch(actor, order.id, actual, order.revision)
        }
        showCollection(null)
    }
    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun mutate(
        id: String, success: String, onSuccess: (CodOrder) -> Unit = {},
        call: suspend () -> AppResult<CodOrder>,
    ) =
        viewModelScope.launch {
            if (state.value.busyOrderId != null) return@launch
            mutableState.update { it.copy(busyOrderId = id) }
            when (val result = call()) {
                is AppResult.Success -> {
                    mutableState.update { current -> current.copy(
                        busyOrderId = null,
                        orders = current.orders.map { if (it.id == id) result.value else it },
                        selectedOrder = current.selectedOrder?.let { if (it.id == id) result.value else it },
                        message = CodOrderMessage(success, true),
                    ) }
                    onSuccess(result.value)
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(busyOrderId = null, message = CodOrderMessage(result.error.text(), false))
                }
            }
        }
}

private fun Long.asInput() = if (this % 100 == 0L) (this / 100).toString() else "%.2f".format(this / 100.0)
private fun AppError.text() = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "You are not authorized for this order"
    AppError.NotFound -> "Order no longer exists"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Unable to update the COD order"
}

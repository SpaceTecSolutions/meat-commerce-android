package com.spacetecsolutions.meatapp.feature.orders

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.CodOrderRepository
import com.spacetecsolutions.meatapp.core.domain.repository.LiveTrackingRepository
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.DeliveryLocation
import com.spacetecsolutions.meatapp.core.model.DeliveryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class CustomerOrdersUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val orders: List<CodOrder> = emptyList(),
    val selectedOrder: CodOrder? = null,
    val destination: CustomerOrderDestination = CustomerOrderDestination.LIST,
    val selectedTab: CustomerOrderTab = CustomerOrderTab.ONGOING,
    val cancelOrder: CodOrder? = null,
    val cancelReason: String = "",
    val busyOrderId: String? = null,
    val message: CustomerOrderMessage? = null,
    val error: String? = null,
    val liveLocation: DeliveryLocation? = null,
    val liveRoute: DeliveryRoute? = null,
    val liveTrackingError: Boolean = false,
    val liveRouteError: Boolean = false,
    val liveRouteLoading: Boolean = false,
    val deliveryOtp: com.spacetecsolutions.meatapp.core.model.DeliveryOtp? = null,
    val deliveryOtpError: Boolean = false,
    val nowEpochMillis: Long = System.currentTimeMillis(),
)

enum class CustomerOrderDestination { LIST, DETAILS, TRACKING }
enum class CustomerOrderTab { ALL, ONGOING, COMPLETED, CANCELLED }
data class CustomerOrderMessage(val text: String, val success: Boolean)

@HiltViewModel
class CustomerOrdersViewModel @Inject constructor(
    private val repository: CodOrderRepository,
    private val liveTrackingRepository: LiveTrackingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CustomerOrdersUiState())
    val state = mutableState.asStateFlow()
    private var ordersJob: Job? = null
    private var trackingJob: Job? = null
    private var trackingSessionId: String? = null
    private var routeJob: Job? = null
    private var lastRouteRequestAt = 0L
    private var lastRouteOrigin: DeliveryLocation? = null
    private var pendingOrderId: String? = null
    private var trackingForeground = true
    private var otpJob: Job? = null
    private var otpLastRequestedAt = 0L

    init {
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                mutableState.update { it.copy(nowEpochMillis = System.currentTimeMillis()) }
                if (state.value.deliveryOtp?.expiresAtEpochMillis?.let {
                        it <= System.currentTimeMillis()
                    } == true) {
                    mutableState.update { it.copy(deliveryOtp = null) }
                    otpLastRequestedAt = 0
                    reconcileOtp()
                }
            }
        }
    }

    fun load() {
        if (ordersJob == null) observeOrders(showFullLoader = state.value.orders.isEmpty())
    }

    fun refresh(showFullLoader: Boolean = false) {
        ordersJob?.cancel()
        ordersJob = null
        observeOrders(showFullLoader)
    }

    private fun observeOrders(showFullLoader: Boolean) {
        mutableState.update {
            it.copy(loading = showFullLoader && it.orders.isEmpty(), refreshing = !showFullLoader, error = null)
        }
        ordersJob = viewModelScope.launch {
            repository.observeCustomerOrders().collect { result ->
                when (result) {
                    is AppResult.Success -> mutableState.update { current ->
                        val sorted = result.value.sortedByDescending(CodOrder::createdAtEpochMillis)
                        val pending = pendingOrderId?.let { id -> sorted.firstOrNull { it.id == id } }
                        if (pending != null) pendingOrderId = null
                        val selected = pending ?: current.selectedOrder?.id?.let { id -> sorted.firstOrNull { it.id == id } }
                        current.copy(
                            loading = false, refreshing = false, orders = sorted, selectedOrder = selected,
                            destination = when {
                                pending != null -> CustomerOrderDestination.DETAILS
                                current.selectedOrder != null && selected == null -> CustomerOrderDestination.LIST
                                else -> current.destination
                            },
                            error = null,
                        )
                    }
                    is AppResult.Failure -> {
                        Log.e(TAG, "Customer order listener failed", (result.error as? AppError.Unknown)?.cause)
                        mutableState.update {
                            it.copy(
                                loading = false, refreshing = false,
                                error = if (it.orders.isEmpty()) result.error.ordersText() else null,
                                message = if (it.orders.isNotEmpty()) CustomerOrderMessage(
                                    "Unable to refresh order updates.", false,
                                ) else it.message,
                            )
                        }
                    }
                }
                reconcileTracking()
                reconcileOtp()
            }
        }
    }

    fun openDetails(order: CodOrder) {
        mutableState.update { it.copy(selectedOrder = order, destination = CustomerOrderDestination.DETAILS) }
        reconcileOtp()
    }

    fun openDetailsById(orderId: String) {
        val order = state.value.orders.firstOrNull { it.id == orderId }
        if (order != null) openDetails(order) else pendingOrderId = orderId
    }

    fun openTracking(order: CodOrder? = state.value.selectedOrder) {
        val requested = order ?: return
        val selected = state.value.orders.firstOrNull { it.id == requested.id } ?: requested
        if (selected.orderStatus == com.spacetecsolutions.meatapp.core.model.OrderStatus.OUT_FOR_DELIVERY &&
            (!selected.realtimeTrackingAvailable || !selected.hasTrackingDestination)) return
        mutableState.update { it.copy(
            selectedOrder = selected, destination = CustomerOrderDestination.TRACKING,
            liveLocation = null, liveRoute = null, liveTrackingError = false,
            liveRouteError = false, liveRouteLoading = false,
        ) }
        reconcileTracking()
        reconcileOtp()
    }

    private fun reconcileOtp() {
        val current = state.value
        val order = current.selectedOrder
        val eligible = current.destination != CustomerOrderDestination.LIST &&
            order?.realtimeTrackingAvailable == true && !order.trackingSessionId.isNullOrBlank()
        if (!eligible) {
            otpJob?.cancel(); otpJob = null; otpLastRequestedAt = 0
            if (current.deliveryOtp != null || current.deliveryOtpError) mutableState.update {
                it.copy(deliveryOtp = null, deliveryOtpError = false)
            }
            return
        }
        val activeOrder = requireNotNull(order)
        val cachedOtp = current.deliveryOtp
        if (cachedOtp != null && cachedOtp.sessionId == activeOrder.trackingSessionId &&
            cachedOtp.expiresAtEpochMillis > System.currentTimeMillis()) return
        if (otpJob?.isActive == true || System.currentTimeMillis() - otpLastRequestedAt < 60_000) return
        otpLastRequestedAt = System.currentTimeMillis()
        otpJob = viewModelScope.launch {
            when (val result = repository.getCustomerDeliveryOtp(activeOrder.id)) {
                is AppResult.Success -> mutableState.update { state ->
                    if (state.selectedOrder?.id == activeOrder.id &&
                        state.selectedOrder.trackingSessionId == result.value.sessionId &&
                        state.selectedOrder.realtimeTrackingAvailable)
                        state.copy(deliveryOtp = result.value, deliveryOtpError = false)
                    else state
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(deliveryOtp = null, deliveryOtpError = true)
                }
            }
        }
    }

    fun retryDeliveryOtp() {
        otpLastRequestedAt = 0
        reconcileOtp()
    }

    private fun reconcileTracking() {
        val current = state.value
        val selected = current.selectedOrder
        val session = selected?.trackingSessionId?.takeIf(String::isNotBlank)
        val shouldListen = current.destination == CustomerOrderDestination.TRACKING &&
            trackingForeground && selected?.realtimeTrackingAvailable == true &&
            selected.hasTrackingDestination && session != null
        if (!shouldListen) {
            trackingJob?.cancel(); trackingJob = null; routeJob?.cancel(); routeJob = null
            trackingSessionId = null; lastRouteRequestAt = 0; lastRouteOrigin = null
            mutableState.update {
                it.copy(liveLocation = null, liveRoute = null, liveTrackingError = false,
                    liveRouteError = false, liveRouteLoading = false)
            }
            return
        }
        val activeOrder = requireNotNull(selected)
        val activeSession = requireNotNull(session)
        if (trackingJob?.isActive == true && trackingSessionId == activeSession) return
        trackingJob?.cancel()
        trackingSessionId = activeSession
        lastRouteRequestAt = 0; lastRouteOrigin = null
        trackingJob = viewModelScope.launch {
            liveTrackingRepository.observeCustomerLocation(activeOrder.id, activeSession).collect { result ->
                mutableState.update { current -> when (result) {
                    is AppResult.Success -> current.copy(liveLocation = result.value, liveTrackingError = false)
                    is AppResult.Failure -> current.copy(liveTrackingError = true)
                } }
                if (result is AppResult.Success) requestRoute(activeOrder.id)
            }
        }
    }

    private fun requestRoute(orderId: String) {
        val now = System.currentTimeMillis()
        val origin = state.value.liveLocation ?: return
        if (state.value.selectedOrder?.hasTrackingDestination != true) return
        val elapsed = now - lastRouteRequestAt
        val moved = lastRouteOrigin.distanceTo(origin)
        val hasRoute = state.value.liveRoute != null
        if (routeJob?.isActive == true || (!hasRoute && elapsed < ROUTE_RETRY_MILLIS) ||
            (hasRoute && elapsed < ROUTE_REFRESH_MILLIS && moved < ROUTE_MOVEMENT_METERS)) return
        lastRouteRequestAt = now
        lastRouteOrigin = origin
        mutableState.update { it.copy(liveRouteLoading = true, liveRouteError = false) }
        routeJob = viewModelScope.launch {
            when (val result = liveTrackingRepository.getCustomerRoute(orderId)) {
                is AppResult.Success -> mutableState.update {
                    it.copy(liveRoute = result.value, liveRouteError = false, liveRouteLoading = false)
                }
                is AppResult.Failure -> {
                    Log.w(TAG, "Delivery route unavailable: ${result.error}")
                    mutableState.update { it.copy(liveRouteError = true, liveRouteLoading = false) }
                }
            }
        }
    }

    fun back() {
        mutableState.update {
        when (it.destination) {
            CustomerOrderDestination.TRACKING -> {
                trackingJob?.cancel(); trackingJob = null; routeJob?.cancel(); routeJob = null
                trackingSessionId = null; lastRouteRequestAt = 0; lastRouteOrigin = null
                it.copy(destination = CustomerOrderDestination.DETAILS, liveLocation = null,
                    liveRoute = null, liveTrackingError = false, liveRouteError = false,
                    liveRouteLoading = false)
            }
            CustomerOrderDestination.DETAILS -> it.copy(
                selectedOrder = null, destination = CustomerOrderDestination.LIST,
            )
            CustomerOrderDestination.LIST -> it
        }
        }
        reconcileOtp()
    }

    fun selectTab(tab: CustomerOrderTab) = mutableState.update { it.copy(selectedTab = tab) }

    fun setTrackingForeground(foreground: Boolean) {
        if (trackingForeground == foreground) return
        trackingForeground = foreground
        reconcileTracking()
    }

    fun showCancel(order: CodOrder?) = mutableState.update {
        it.copy(cancelOrder = order, cancelReason = "")
    }

    fun setCancelReason(value: String) {
        if (value.length <= 200) mutableState.update { it.copy(cancelReason = value) }
    }

    fun cancelOrder() {
        val current = state.value
        val order = current.cancelOrder ?: return
        if (!order.canCustomerCancel || current.cancelReason.isBlank()) return
        showCancel(null)
        mutate(order.id) {
            when (val result = repository.cancelCustomerOrder(
                order.id, order.revision, current.cancelReason.trim(),
            )) {
                is AppResult.Success -> mutableState.update { state -> state.copy(
                    busyOrderId = null,
                    orders = state.orders.map { if (it.id == order.id) result.value else it },
                    selectedOrder = result.value,
                    message = CustomerOrderMessage("Order cancelled", true),
                ) }
                is AppResult.Failure -> fail(result.error.ordersText())
            }
        }
    }

    fun reorder(order: CodOrder) {
        if (!order.canReorder) return
        mutate(order.id) {
            when (val result = repository.reorderCustomerOrder(order.id)) {
                is AppResult.Success -> mutableState.update { it.copy(
                    busyOrderId = null,
                    message = CustomerOrderMessage(
                        if (result.value > 0) "${result.value} product(s) added to cart"
                        else "No items are currently available to reorder",
                        result.value > 0,
                    ),
                ) }
                is AppResult.Failure -> fail(result.error.ordersText())
            }
        }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun mutate(orderId: String, block: suspend () -> Unit) = viewModelScope.launch {
        if (state.value.busyOrderId != null) return@launch
        mutableState.update { it.copy(busyOrderId = orderId) }
        block()
    }

    private fun fail(message: String) = mutableState.update {
        it.copy(busyOrderId = null, message = CustomerOrderMessage(message, false))
    }

    private companion object {
        const val TAG = "CustomerOrders"
        const val ROUTE_REFRESH_MILLIS = 45_000L
        const val ROUTE_RETRY_MILLIS = 15_000L
        const val ROUTE_MOVEMENT_METERS = 80f
    }
}

private fun DeliveryLocation?.distanceTo(other: DeliveryLocation): Float {
    if (this == null) return Float.MAX_VALUE
    val result = FloatArray(1)
    Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, result)
    return result.firstOrNull()?.takeIf(Float::isFinite) ?: Float.MAX_VALUE
}

private fun AppError.ordersText() = when (this) {
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again."
    AppError.Forbidden, AppError.Unauthorized -> "You can only view your own orders."
    AppError.NotFound -> "Order details are unavailable."
    is AppError.Validation -> message
    else -> "Unable to load your orders."
}

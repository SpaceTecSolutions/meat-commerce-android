package com.spacetecsolutions.meatapp.feature.orders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimatedListItem
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.component.HideAppBottomBar
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import java.time.Instant.ofEpochMilli
import java.time.ZoneOffset
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T

@Composable
fun CustomerOrdersRoute(
    onStartShopping: () -> Unit,
    onHelp: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onCall: (String) -> Unit,
    onMessage: (CustomerOrderMessage) -> Unit = {},
    initialOrderId: String? = null,
    onInitialOrderConsumed: () -> Unit = {},
    viewModel: CustomerOrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.setTrackingForeground(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.setTrackingForeground(false) }
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(initialOrderId) {
        initialOrderId?.let(viewModel::openDetailsById)
        if (initialOrderId != null) onInitialOrderConsumed()
    }
    LaunchedEffect(state.message) {
        state.message?.let { onMessage(it); viewModel.consumeMessage() }
    }
    if (state.destination != CustomerOrderDestination.LIST) {
        HideAppBottomBar()
        BackHandler(onBack = viewModel::back)
    }
    AnimatedContent(
        targetState = state.destination,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            (slideInHorizontally(tween(AppMotion.SCREEN_MILLIS)) { if (forward) it / 4 else -it / 4 } +
                fadeIn(tween(AppMotion.STANDARD_MILLIS))) togetherWith
                (slideOutHorizontally(tween(AppMotion.SCREEN_MILLIS)) { if (forward) -it / 5 else it / 5 } +
                    fadeOut(tween(AppMotion.QUICK_MILLIS)))
        },
        label = "customer-order-flow",
    ) { destination ->
        val order = state.selectedOrder
        val shopDetails = state.shopSupport
        when {
            destination == CustomerOrderDestination.TRACKING && order != null -> CustomerOrderTrackingScreen(
                order = order,
                liveLocation = state.liveLocation,
                liveRoute = state.liveRoute,
                liveRouteError = state.liveRouteError,
                liveRouteLoading = state.liveRouteLoading,
                liveTrackingError = state.liveTrackingError,
                deliveryOtp = state.deliveryOtp,
                deliveryOtpError = state.deliveryOtpError,
                nowEpochMillis = state.nowEpochMillis,
                onBack = viewModel::back,
                onContact = { order.deliveryContactMobile?.takeIf(String::isNotBlank)?.let(onCall) ?: onHelp() },
                onRetryOtp = viewModel::retryDeliveryOtp,
            )
            destination == CustomerOrderDestination.DETAILS && order != null -> CustomerOrderDetailsScreen(
                order = order,
                shopDetails = shopDetails,
                nowEpochMillis = state.nowEpochMillis,
                busy = state.busyOrderId == order.id,
                onBack = viewModel::back,
                onHelp = onHelp,
                onTrack = { viewModel.openTracking() },
                onNavigate = { onNavigate(order.addressSummary) },
                onCancel = { viewModel.showCancel(order) },
                onReorder = { viewModel.reorder(order) },
                deliveryOtp = state.deliveryOtp,
                deliveryOtpError = state.deliveryOtpError,
                onRetryOtp = viewModel::retryDeliveryOtp,
            )
            else -> CustomerOrdersListScreen(state, onBack,viewModel, onStartShopping)
        }
    }
    state.cancelOrder?.let { order ->
        CustomerCancelSheet(
            order = order,
            reason = state.cancelReason,
            change = viewModel::setCancelReason,
            confirm = viewModel::cancelOrder,
            dismiss = { viewModel.showCancel(null) },
        )
    }
}

private object DatesThroughToday : SelectableDates {
    private fun todayUtcMillis() = java.time.LocalDate.now().toEpochDay() * 86_400_000L
    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMillis()
    override fun isSelectableYear(year: Int) = year <= java.time.LocalDate.now().year
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomerOrdersListScreen(
    state: CustomerOrdersUiState,
    onBack: () -> Unit,
    actions: CustomerOrdersViewModel,
    onStartShopping: () -> Unit,
) {
    var datePickerOpen by remember { mutableStateOf(false) }
    var selectedDate by rememberSaveable { mutableStateOf<Long?>(null) }
    val visibleOrders = state.filteredOrderModels().filter { model ->
        selectedDate?.let { selected ->
            java.time.Instant.ofEpochMilli(model.order.createdAtEpochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate() ==
                java.time.Instant.ofEpochMilli(selected).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        } ?: true
    }
    if (datePickerOpen) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate,
            selectableDates = DatesThroughToday,
        )
        DatePickerDialog(onDismissRequest = { datePickerOpen = false },
            confirmButton = { TextButton({ selectedDate = picker.selectedDateMillis; datePickerOpen = false }) { Text(stringResource(R.string.ui_apply_cfea419)) } },
            dismissButton = { TextButton({ selectedDate = null; datePickerOpen = false }) { Text(stringResource(R.string.ui_all_dates_69feaaf)) } }) {
            DatePicker(state = picker)
        }
    }
    Column(Modifier.fillMaxSize().background(T.canvas)) {
        TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_my_orders_db2821a), style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = T.ink)
                if (state.orders.isNotEmpty()) Surface(
                    Modifier.padding(start = 10.dp), color = T.rose,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50)) {
                    Text(state.orders.size.toString(), Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium, color = T.red)
                }
            }
        },
            navigationIcon = { IconButton(onBack) { Icon(AppIcons.Back, stringResource(R.string.ui_back_b52b36b)) } },
            actions = { IconButton({ datePickerOpen = true }) {
            Icon(AppIcons.Calendar, stringResource(R.string.ui_filter_orders_by_date_542122e), tint = T.ink)
        } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = T.surface))
        if (state.refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
        CustomerOrderStatusTabs(state.selectedTab, actions::selectTab)
        selectedDate?.let { selected -> TextButton({ selectedDate = null }) {
            Text(stringResource(R.string.ui_java_time_instant_ofepochmilli_selected_276847f, ofEpochMilli(selected).atZone(
                ZoneOffset.UTC).toLocalDate()))
        } }
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = { actions.refresh() }, modifier = Modifier.fillMaxSize()) {
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null -> ContentStateView(
                ContentState.Error(
                    title = stringResource(R.string.ui_unable_to_load_your_orders_e5e152e),
                    description = state.error,
                    retryLabel = "Retry",
                ),
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                onAction = { actions.refresh(showFullLoader = true) },
            )
            visibleOrders.isEmpty() -> {
                val copy = if (selectedDate != null) "No orders on this date" to "Choose another date or clear the date filter."
                    else state.selectedTab.emptyCopy()
                ContentStateView(
                    ContentState.Empty(
                        title = copy.first,
                        description = copy.second,
                        actionLabel = if (state.orders.isEmpty()) "Start Shopping" else null,
                    ),
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    onAction = onStartShopping,
                )
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().widthIn(max = AppDimensions.contentMaxWidth),
                    contentPadding = PaddingValues(
                        start = AppSpacing.medium,
                        end = AppSpacing.medium,
                        top = AppSpacing.medium,
                        bottom = AppSpacing.large,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                ) {
                    itemsIndexed(visibleOrders, key = { _, model -> model.order.id }) { index, model ->
                        val order = model.order
                        AppAnimatedListItem(index = index, modifier = Modifier.animateItem()) {
                            CustomerOrderCard(
                                model = model,
                                onClick = actions::openDetails,
                                onTrack = if (order.realtimeTrackingAvailable && order.hasTrackingDestination) {
                                    { actions.openTracking(order) }
                                } else null,
                                onReorder = if (order.orderStatus == com.spacetecsolutions.meatapp.core.model.OrderStatus.DELIVERED &&
                                    order.canReorder) ({ actions.reorder(order) }) else null,
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

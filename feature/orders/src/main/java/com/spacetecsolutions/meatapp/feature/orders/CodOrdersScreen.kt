package com.spacetecsolutions.meatapp.feature.orders

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.*

@Composable
fun CodOrdersRoute(
    actor: CodActor,
    deliveryStaffAllowed: Boolean = false,
    realtimeTrackingAllowed: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onCall: (String) -> Unit = {},
    onTrackingCommand: (TrackingCommand) -> Unit = {},
    onMessage: (CodOrderMessage) -> Unit,
    initialOrderId: String? = null,
    onInitialOrderConsumed: () -> Unit = {},
    operatorPermissions: Set<StaffPermission>? = null,
    staffPresentation: Boolean = false,
    viewModel: CodOrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(actor, deliveryStaffAllowed, realtimeTrackingAllowed) {
        viewModel.load(actor, deliveryStaffAllowed, realtimeTrackingAllowed)
    }
    LaunchedEffect(state.message) { state.message?.let { onMessage(it); viewModel.consumeMessage() } }
    LaunchedEffect(initialOrderId, state.orders) {
        val order = initialOrderId?.let { id -> state.orders.firstOrNull { it.id == id } }
        if (order != null) {
            viewModel.selectOrder(order)
            onInitialOrderConsumed()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.onTrackingPermission(
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
        )
    }
    LaunchedEffect(state.trackingPermissionOrder?.id) {
        if (state.trackingPermissionOrder != null) {
            val permissions = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
    LaunchedEffect(state.trackingCommand) {
        state.trackingCommand?.let { onTrackingCommand(it); viewModel.consumeTrackingCommand() }
    }
    CodOrdersScreen(state, onNavigate, onCall, operatorPermissions, staffPresentation, viewModel)
}

@Composable
private fun CodOrdersScreen(
    state: CodOrdersUiState,
    onNavigate: (String) -> Unit,
    onCall: (String) -> Unit,
    operatorPermissions: Set<StaffPermission>?,
    staffPresentation: Boolean,
    actions: CodOrdersViewModel,
) {
    if (state.selectedOrder != null) HideAppBottomBar()
    if (state.actor == CodActor.ADMIN) {
        AdminOrdersFlow(state, onNavigate, onCall, operatorPermissions, staffPresentation, actions)
        return
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(if (state.actor == CodActor.ADMIN) "Orders" else "COD Orders", actionIcon = AppIcons.Refresh,
            actionDescription = "Refresh orders", onAction = actions::refresh)
        if (state.actor == CodActor.ADMIN) OrderStatusTabs(state.selectedStatus, actions::selectStatus)
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null -> ContentStateView(
                ContentState.Error(description = state.error), onAction = actions::refresh)
            state.ordersForDisplay().isEmpty() -> ContentStateView(ContentState.Empty(
                title = "No ${state.selectedStatus.label().lowercase()} orders",
                description = if (state.actor == CodActor.DELIVERY) "Assigned COD orders will appear here."
                else "Orders in this state will appear here.",
            ))
            else -> LazyColumn(
                Modifier.fillMaxSize().wrapContentWidth().widthIn(max = AppDimensions.dashboardMaxWidth),
                contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
            ) {
                items(state.ordersForDisplay(), key = CodOrder::id) { order ->
                    CodOrderCard(order, state.actor ?: return@items, state.busyOrderId == order.id,
                        actions::confirm, actions::showCancel, actions::startPersonalDelivery,
                        actions::showCollection, actions::selectOrder,
                        actions::startPreparing, actions::markReadyForDelivery,
                        state.deliveryStaffAllowed, actions::requestAssignment,
                        onNavigate, actions::startDelivery)
                }
            }
        }
    }
    state.collectionOrder?.let { CollectionDialog(it, state.collectedAmount,
        actions::setCollectedAmount, actions::completeDelivery, actions::reportMismatch,
        { actions.showCollection(null) }) }
    state.otpOrder?.let { DeliveryOtpVerificationSheet(it, state.otpInput,
        state.otpError, state.otpBusy, actions::setOtpInput, actions::verifyOtp,
        actions::dismissOtp) }
    state.cancelOrder?.let { CancelDialog(state.cancelReason, actions::setCancelReason,
        actions::cancel, { actions.showCancel(null) }) }
    state.selectedOrder?.let { order ->
        if (state.actor == CodActor.ADMIN) AdminOrderDetailsDialog(
            order = order,
            busy = state.busyOrderId == order.id,
            onDismiss = { actions.selectOrder(null) },
            onConfirm = { actions.selectOrder(null); actions.confirm(order) },
            onReject = { actions.selectOrder(null); actions.showCancel(order) },
            onStartPreparing = { actions.selectOrder(null); actions.startPreparing(order) },
            onMarkReady = { actions.selectOrder(null); actions.markReadyForDelivery(order) },
        ) else DeliveryOrderDetailsDialog(
            order = order,
            trackingAllowed = state.realtimeTrackingAllowed,
            dismiss = { actions.selectOrder(null) },
            navigate = { onNavigate(order.addressSummary) },
            startDelivery = { actions.startDelivery(order) },
            canResumeTracking = state.realtimeTrackingAllowed && state.realtimeTrackingAdminEnabled &&
                order.canResumeLiveTracking(state.actor),
            resumeTracking = { actions.resumeLiveTracking(order) },
        )
    }
    state.assignmentOrder?.let { order -> DeliveryAssignmentDialog(
        order = order,
        staff = state.eligibleStaff,
        loading = state.loadingStaff,
        onAssign = actions::assignDeliveryUser,
        onDismiss = { actions.requestAssignment(null) },
    ) }
}

@Composable
private fun OrderStatusTabs(selected: OrderStatus, select: (OrderStatus) -> Unit) {
    val statuses = OrderStatus.entries
    PrimaryScrollableTabRow(selectedTabIndex = statuses.indexOf(selected), edgePadding = AppSpacing.small) {
        statuses.forEach { status ->
            Tab(selected = status == selected, onClick = { select(status) }, text = { Text(status.label()) })
        }
    }
}

private fun CodOrdersUiState.ordersForDisplay() =
    if (actor == CodActor.ADMIN) orders.filter { it.orderStatus == selectedStatus }
    else orders.filter { it.orderStatus !in setOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED) }

@Composable
private fun CodOrderCard(
    order: CodOrder,
    actor: CodActor,
    busy: Boolean,
    confirm: (CodOrder) -> Unit,
    cancel: (CodOrder) -> Unit,
    personal: (CodOrder) -> Unit,
    collect: (CodOrder) -> Unit,
    details: (CodOrder) -> Unit,
    startPreparing: (CodOrder) -> Unit,
    markReady: (CodOrder) -> Unit,
    staffAllowed: Boolean,
    assign: (CodOrder) -> Unit,
    navigate: (String) -> Unit,
    startDelivery: (CodOrder) -> Unit,
) {
    ElevatedCard(onClick = { details(order) }, modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large) {
        Column(Modifier.fillMaxWidth().padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Order ${order.displayNumber}", style = MaterialTheme.typography.titleMedium)
                SuggestionChip(onClick = {}, enabled = false,
                    label = { Text(order.orderStatus.label()) })
            }
            Text("${order.customerName} · ${order.customerMobile}")
            Text(order.addressSummary, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
            Text("Total: ${order.totalMinor.money(order.currencyCode)}",
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text("Payment: ${order.paymentMethod.name} · ${order.paymentStatus.name}",
                style = MaterialTheme.typography.labelMedium)
            if (order.adminDeliveringPersonally) Text("Admin delivering personally",
                color = MaterialTheme.colorScheme.secondary)
            if (order.codMismatchReported) Text("COD amount mismatch reported — collection pending",
                color = MaterialTheme.colorScheme.error)
            if (order.readyForDeliveryAssignment) Text("Ready for delivery assignment",
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            order.assignedDeliveryUserName?.let { Text("Assigned to $it") }
            if (order.adminDeliveringPersonally) Text("Assigned to you")
            if (order.orderStatus == OrderStatus.CANCELLED) Text("Cancelled — no further action allowed",
                color = MaterialTheme.colorScheme.error)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            ActionButtons(order, actor, busy, confirm, cancel, personal, collect, startPreparing,
                markReady, staffAllowed, assign, navigate, startDelivery)
        }
    }
}

@Composable
private fun AdminOrdersFlow(
    state: CodOrdersUiState,
    onNavigate: (String) -> Unit,
    onCall: (String) -> Unit,
    permissions: Set<StaffPermission>?,
    staffPresentation: Boolean,
    actions: CodOrdersViewModel,
) {
    var confirmationOrder by remember { mutableStateOf<CodOrder?>(null) }
    androidx.compose.animation.AnimatedContent(
        targetState = state.selectedOrder,
        label = "admin order navigation",
    ) { selected ->
        if (selected == null) {
            AdminOrdersListScreen(state, actions::selectOrder, actions::refresh,
                { confirmationOrder = it }, actions::showCancel,
                permissions == null || StaffPermission.ADJUST_FINAL_BILL in permissions,
                permissions == null || StaffPermission.UPDATE_ORDER_STATUS in permissions,
                staffPresentation)
        } else {
            AdminOrderDetailsScreen(
                order = selected,
                busy = state.busyOrderId == selected.id,
                deliveryStaffAllowed = state.deliveryStaffAllowed,
                onBack = { actions.selectOrder(null) },
                onCall = onCall,
                onNavigate = onNavigate,
                onConfirm = { confirmationOrder = selected },
                onCancel = { actions.showCancel(selected) },
                onStartPreparing = { actions.startPreparing(selected) },
                onMarkReady = { actions.markReadyForDelivery(selected) },
                onDeliverMyself = { actions.startPersonalDelivery(selected) },
                onAssignDelivery = { actions.requestAssignment(selected) },
                onStartDelivery = { actions.startDelivery(selected) },
                onCompleteDelivery = { actions.showCollection(selected) },
                canResumeTracking = state.realtimeTrackingAllowed && state.realtimeTrackingAdminEnabled &&
                    selected.canResumeLiveTracking(state.actor),
                onResumeTracking = { actions.resumeLiveTracking(selected) },
                canAdjustFinalBill = permissions == null || StaffPermission.ADJUST_FINAL_BILL in permissions,
                canUpdateStatus = permissions == null || StaffPermission.UPDATE_ORDER_STATUS in permissions,
                staffPresentation = staffPresentation,
            )
        }
    }
    confirmationOrder?.let { order -> FinalBillConfirmationDialog(
        order = order,
        busy = state.busyOrderId == order.id,
        onDismiss = { confirmationOrder = null },
        onConfirm = { adjustments ->
            confirmationOrder = null
            actions.selectOrder(null)
            actions.confirm(order, adjustments)
        },
    ) }
    state.collectionOrder?.let { CollectionDialog(it, state.collectedAmount,
        actions::setCollectedAmount, actions::completeDelivery, actions::reportMismatch,
        { actions.showCollection(null) }) }
    state.otpOrder?.let { DeliveryOtpVerificationSheet(it, state.otpInput,
        state.otpError, state.otpBusy, actions::setOtpInput, actions::verifyOtp,
        actions::dismissOtp) }
    state.cancelOrder?.let { CancelDialog(state.cancelReason, actions::setCancelReason,
        actions::cancel, { actions.showCancel(null) }) }
    state.assignmentOrder?.let { order -> DeliveryAssignmentDialog(
        order = order,
        staff = state.eligibleStaff,
        loading = state.loadingStaff,
        onAssign = actions::assignDeliveryUser,
        onDismiss = { actions.requestAssignment(null) },
    ) }
}

@Composable
private fun ActionButtons(
    order: CodOrder, actor: CodActor, busy: Boolean,
    confirm: (CodOrder) -> Unit, cancel: (CodOrder) -> Unit,
    personal: (CodOrder) -> Unit, collect: (CodOrder) -> Unit,
    startPreparing: (CodOrder) -> Unit, markReady: (CodOrder) -> Unit,
    staffAllowed: Boolean, assign: (CodOrder) -> Unit,
    navigate: (String) -> Unit, startDelivery: (CodOrder) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        if (actor == CodActor.ADMIN && order.canConfirm) {
            Button({ confirm(order) }, Modifier.fillMaxWidth(), enabled = !busy) { Text("Confirm") }
        }
        if (actor == CodActor.ADMIN && order.canStartPreparing) {
            Button({ startPreparing(order) }, Modifier.fillMaxWidth(), enabled = !busy) {
                Text("Start Preparing")
            }
        }
        if (actor == CodActor.ADMIN && order.canMarkReadyForDelivery) {
            Button({ markReady(order) }, Modifier.fillMaxWidth(), enabled = !busy) {
                Text("Mark Ready for Delivery")
            }
        }
        if (actor == CodActor.ADMIN && order.canStartPersonalDelivery) {
            OutlinedButton({ personal(order) }, Modifier.fillMaxWidth(), enabled = !busy) {
                Icon(AppIcons.Delivery, null); Text("Deliver Myself")
            }
        }
        if (actor == CodActor.ADMIN && staffAllowed && order.canAssignDelivery) {
            OutlinedButton({ assign(order) }, Modifier.fillMaxWidth(), enabled = !busy) {
                Icon(AppIcons.Admin, null); Text("Assign Delivery User")
            }
        }
        if (actor == CodActor.ADMIN && order.adminDeliveringPersonally && order.canStartDelivery) {
            OutlinedButton({ navigate(order.addressSummary) }, Modifier.fillMaxWidth(), enabled = !busy) {
                Text("Navigate")
            }
            Button({ startDelivery(order) }, Modifier.fillMaxWidth(), enabled = !busy) {
                Text("Start Delivery")
            }
        }
        if (order.canOpenDeliveryCompletion && (actor == CodActor.DELIVERY || order.adminDeliveringPersonally)) {
            Button({ collect(order) }, Modifier.fillMaxWidth(), enabled = !busy) {
                Icon(AppIcons.Delivery, null); Text("Reached Customer")
            }
        }
        if (actor == CodActor.ADMIN && order.canCancel) {
            TextButton({ cancel(order) }, Modifier.fillMaxWidth(), enabled = !busy) {
                Text(if (order.canReject) "Reject" else "Cancel order", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

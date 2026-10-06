package com.spacetecsolutions.meatapp.feature.orders

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminOrderDetailsScreen(
    order: CodOrder,
    busy: Boolean,
    deliveryStaffAllowed: Boolean,
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onStartPreparing: () -> Unit,
    onMarkReady: () -> Unit,
    onDeliverMyself: () -> Unit,
    onAssignDelivery: () -> Unit,
    onStartDelivery: () -> Unit,
    onCompleteDelivery: () -> Unit,
    canResumeTracking: Boolean,
    onResumeTracking: () -> Unit,
    canAdjustFinalBill: Boolean = true,
    canUpdateStatus: Boolean = true,
    staffPresentation: Boolean = false,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(order.displayNumber.withHash(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold)
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons.Back, "Back to orders") } },
                actions = { Box(Modifier.padding(end = AppSpacing.small)) { OrderStatusChip(order.orderStatus) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            AdminOrderActionBar(
                order, busy, deliveryStaffAllowed, onConfirm, onCancel, onStartPreparing,
                onMarkReady, onDeliverMyself, onAssignDelivery, onStartDelivery, onCompleteDelivery,
                canResumeTracking, onResumeTracking,
                canAdjustFinalBill, canUpdateStatus,
            )
        },
    ) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            val tablet = maxWidth >= AppDimensions.expandedContentBreakpoint
            LazyColumn(
                modifier = Modifier.fillMaxWidth().widthIn(max = AppDimensions.dashboardMaxWidth),
                contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            ) {
                if (staffPresentation && !canAdjustFinalBill && !canUpdateStatus) item {
                    DetailSurface("Read-Only View", AppIcons.Security) {
                        Text("View Orders permission only", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (order.orderStatus == OrderStatus.CANCELLED) item { AdminCancelledHero(order) }
                else {
                    if (order.orderStatus == OrderStatus.DELIVERED) item { AdminDeliveredHero(order) }
                    item { AdminOrderProgress(order) }
                }
                item {
                    if (tablet) Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                        CustomerInfoSection(order, onCall, Modifier.weight(1f))
                        DeliveryAddressSection(order, onNavigate, Modifier.weight(1f))
                    } else Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                        CustomerInfoSection(order, onCall)
                        DeliveryAddressSection(order, onNavigate)
                    }
                }
                item { AdminOrderItemsSection(order) }
                item { OrderBillSummary(order) }
                item { PaymentDetailsSection(order) }
                if (!order.deliverySlotDateLabel.isNullOrBlank() || !order.deliverySlotTimeLabel.isNullOrBlank()) {
                    item { DeliverySlotSection(order) }
                }
                if (order.instructions.isNotBlank()) item { CustomerNoteSection(order.instructions) }
                order.assignedDeliveryUserName?.let { name ->
                    item { DetailSurface("Delivery Assignment", AppIcons.DeliveryPerson) {
                        Text(name, fontWeight = FontWeight.SemiBold)
                        Text(order.assignedDeliveryRole?.name?.pretty() ?: "Delivery user",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                }
                if (order.orderStatus == OrderStatus.CANCELLED && !order.cancelReason.isNullOrBlank()) {
                    item { DetailSurface("Cancellation", AppIcons.Cancelled) {
                        Text(order.cancelReason.orEmpty(), color = MaterialTheme.colorScheme.error)
                    } }
                }
                item { Spacer(Modifier.height(AppSpacing.small)) }
            }
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
private fun AdminOrderActionBar(
    order: CodOrder,
    busy: Boolean,
    staffAllowed: Boolean,
    confirm: () -> Unit,
    cancel: () -> Unit,
    prepare: () -> Unit,
    ready: () -> Unit,
    personal: () -> Unit,
    assign: () -> Unit,
    startDelivery: () -> Unit,
    complete: () -> Unit,
    canResumeTracking: Boolean,
    resumeTracking: () -> Unit,
    canAdjustFinalBill: Boolean,
    canUpdateStatus: Boolean,
) {
    val actions = order.actions(staffAllowed, confirm, cancel, prepare, ready, personal, assign,
        startDelivery, complete, canResumeTracking, resumeTracking, canAdjustFinalBill, canUpdateStatus)
    if (actions.isEmpty() && !busy) return
    Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(AppSpacing.medium)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = AppSpacing.small))
            AnimatedContent(order.orderStatus, label = "order status actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                    actions.forEachIndexed { index, action ->
                        if (index == actions.lastIndex) Button(action.click, Modifier.weight(1f), enabled = !busy) {
                            Text(action.label, maxLines = 1)
                        } else OutlinedButton(action.click, Modifier.weight(1f), enabled = !busy) {
                            Text(action.label, maxLines = 1, color = if (action.destructive)
                                MaterialTheme.colorScheme.error else LocalContentColor.current)
                        }
                    }
                }
            }
        }
    }
}

private data class DetailAction(val label: String, val destructive: Boolean = false, val click: () -> Unit)

private fun CodOrder.actions(
    staffAllowed: Boolean, confirm: () -> Unit, cancel: () -> Unit, prepare: () -> Unit,
    ready: () -> Unit, personal: () -> Unit, assign: () -> Unit, start: () -> Unit, complete: () -> Unit,
    canResumeTracking: Boolean, resumeTracking: () -> Unit,
    canAdjustFinalBill: Boolean, canUpdateStatus: Boolean,
): List<DetailAction> = when {
    canConfirm -> buildList { if (canUpdateStatus) add(DetailAction("Reject Order", true, cancel));
        if (canAdjustFinalBill) add(DetailAction("Confirm Order", click = confirm)) }
    canStartPreparing && canUpdateStatus -> listOf(DetailAction("Cancel Order", true, cancel), DetailAction("Mark as Preparing", click = prepare))
    canMarkReadyForDelivery && canUpdateStatus -> listOf(DetailAction("Cancel Order", true, cancel), DetailAction("Mark Ready", click = ready))
    canAssignDelivery && canUpdateStatus -> buildList {
        add(DetailAction("Deliver Myself", click = personal))
        if (staffAllowed) add(DetailAction("Assign Delivery", click = assign))
    }
    canStartDelivery && adminDeliveringPersonally && canUpdateStatus ->
        listOf(DetailAction("Mark Out for Delivery", click = start))
    canOpenDeliveryCompletion && adminDeliveringPersonally && canUpdateStatus -> buildList {
        if (canResumeTracking) add(DetailAction("Resume Tracking", click = resumeTracking))
        add(DetailAction("Mark Delivered", click = complete))
    }
    else -> emptyList()
}

private fun String.pretty() = lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)

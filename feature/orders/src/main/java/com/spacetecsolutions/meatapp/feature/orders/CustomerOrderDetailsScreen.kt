package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.DeliveryOtp
import com.spacetecsolutions.meatapp.core.model.OrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerOrderDetailsScreen(order: CodOrder, nowEpochMillis: Long, busy: Boolean,
    onBack: () -> Unit, onHelp: () -> Unit, onTrack: () -> Unit, onNavigate: () -> Unit,
    onCancel: () -> Unit, onReorder: () -> Unit,
    deliveryOtp: DeliveryOtp?, deliveryOtpError: Boolean, onRetryOtp: () -> Unit) {
    val model = order.toCustomerOrderUi(nowEpochMillis)
    var showInvoice by remember { mutableStateOf(false) }
    var requestedOption by remember { mutableStateOf<String?>(null) }
    if (showInvoice) CustomerInvoiceDialog(order = order, onDismiss = { showInvoice = false })
    requestedOption?.let { option -> AlertDialog(onDismissRequest = { requestedOption = null },
        title = { Text(option) },
        text = { Text("Tell us what went wrong and our support team will help.") },
        confirmButton = { TextButton({ requestedOption = null; onHelp() }) { Text("Contact support") } },
        dismissButton = { TextButton({ requestedOption = null }) { Text("Close") } }) }
    Scaffold(containerColor = T.canvas, topBar = {
        TopAppBar(title = { Text("Order Details", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onBack) { Icon(AppIcons.Back, "Back") } },
            actions = { TextButton(onHelp) { Text("Help", color = T.red) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = T.surface))
    }, bottomBar = {
        if (order.canCustomerCancel || order.canReorder) Surface(color = T.surface,
            shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order.canCustomerCancel) OutlinedButton(onCancel,
                    Modifier.weight(1f).height(48.dp), enabled = !busy) {
                    Text("Cancel Order", color = T.red)
                }
                if (order.canReorder) Button(onReorder, Modifier.weight(1f).height(48.dp),
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = T.red)) {
                    Text("Reorder")
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = T.red)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item { CustomerDetailHeader(model) }
                    if (order.realtimeTrackingAvailable) item {
                        CustomerDeliveryOtpCard(deliveryOtp, deliveryOtpError, onRetryOtp)
                    }
                    item {
                        when (order.orderStatus) {
                            OrderStatus.DELIVERED -> CustomerDeliveredState(order, model)
                            OrderStatus.CANCELLED -> CustomerCancelledState(order, model)
                            else -> CustomerActiveState(order, model, onHelp, onTrack)
                        }
                    }
                    item { CustomerAddressDetail(order, model, onNavigate) }
                    item { CustomerPaymentDetail(order) }
                    item { CustomerOrderItemsDetail(order) }
                    item { CustomerBillDetail(order) }
                    if (order.instructions.isNotBlank()) item {
                        CustomerDetailSurface {
                            CustomerDetailLabel("DELIVERY INSTRUCTIONS")
                            Text(order.instructions, style = MaterialTheme.typography.bodySmall,
                                color = T.muted)
                        }
                    }
                    if (order.orderStatus == OrderStatus.DELIVERED) item {
                        /*Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ showInvoice = true },
                                Modifier.weight(1f)) { Text("Invoice") }
                            OutlinedButton({ requestedOption = "Report an issue" },
                                Modifier.weight(1f)) { Text("Report issue") }
                        }*/
                        Row(Modifier.fillMaxWidth().clickable(onClick = { showInvoice = true }),
                            horizontalArrangement = Arrangement.Center) {
                            CustomerDownloadInvoice()
                        }
                    }
                    item { CustomerDetailHelp(onHelp) }
                }
            }
        }
    }
}

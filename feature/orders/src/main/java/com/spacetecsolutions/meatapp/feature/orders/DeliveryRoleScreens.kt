package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.*

@Composable
fun DeliveryHomeRoute(
    openOrders: () -> Unit, openHistory: () -> Unit, openNotifications: () -> Unit,
    viewModel: DeliveryRoleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val active = state.orders.count { it.orderStatus !in terminalStatuses }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("Delivery Home", actionIcon = AppIcons.Notifications,
            actionDescription = "Notifications", onAction = openNotifications)
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null -> ContentStateView(ContentState.Error(description = state.error), onAction = viewModel::refresh)
            else -> Column(
                Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = AppDimensions.contentMaxWidth).padding(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            ) {
                Text("Hello, ${state.user?.displayName.orEmpty()}", style = MaterialTheme.typography.headlineSmall)
                ElevatedCard(onClick = openOrders, modifier = Modifier.fillMaxWidth()) {
                    Metric("Assigned Orders", active.toString(), "View today's active deliveries")
                }
                ElevatedCard(onClick = openHistory, modifier = Modifier.fillMaxWidth()) {
                    Metric("Delivery History", state.orders.count { it.orderStatus in terminalStatuses }.toString(),
                        "Delivered and cancelled assignments")
                }
            }
        }
    }
}

@Composable
private fun Metric(title: String, value: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(AppSpacing.large)) {
        Text(value, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DeliveryHistoryRoute(viewModel: DeliveryRoleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<CodOrder?>(null) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("Delivery History", actionIcon = AppIcons.Refresh,
            actionDescription = "Refresh", onAction = viewModel::refresh)
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null -> ContentStateView(ContentState.Error(description = state.error), onAction = viewModel::refresh)
            state.orders.none { it.orderStatus in terminalStatuses } -> ContentStateView(ContentState.Empty(
                title = "No delivery history", description = "Completed assignments will appear here."))
            else -> LazyColumn(contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                items(state.orders.filter { it.orderStatus in terminalStatuses }, key = CodOrder::id) { order ->
                    ElevatedCard(onClick = { selected = order }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(AppSpacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("Order ${order.displayNumber}", style = MaterialTheme.typography.titleMedium)
                                Text(order.customerName) }
                            Text(order.orderStatus.label(), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
    selected?.let { DeliveryOrderDetailsDialog(order = it, dismiss = { selected = null }) }
}

@Composable
fun DeliveryProfileRoute(
    notifications: () -> Unit, help: () -> Unit, logout: () -> Unit,
    viewModel: DeliveryRoleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }
    var profileInfo by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("Profile & Settings")
        Column(
            Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = AppDimensions.formMaxWidth).verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
        ) {
            SettingsProfileHeader(state.user?.displayName ?: "Delivery User", state.user?.mobileNumber.orEmpty())
            SettingsMenuCard {
                SettingsMenuRow(AppIcons.Profile, "Profile Information", { profileInfo = true })
                SettingsMenuRow(AppIcons.Notifications, "Notifications", notifications)
                SettingsMenuRow(AppIcons.Help, "Help & Support", help)
                SettingsMenuRow(
                    AppIcons.Logout, "Logout", { confirmLogout = true },
                    destructive = true, showDivider = false,
                )
            }
        }
    }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("Logout?") },
        text = { Text("You will need to sign in again to access delivery assignments.") },
        confirmButton = { Button({ confirmLogout = false; logout() }) { Text("Logout") } },
        dismissButton = { TextButton({ confirmLogout = false }) { Text("Cancel") } },
    )
    if (profileInfo) AlertDialog(
        onDismissRequest = { profileInfo = false },
        title = { Text("Profile Information") },
        text = { Column {
            Text(state.user?.displayName ?: "Delivery User", style = MaterialTheme.typography.titleMedium)
            Text(state.user?.mobileNumber.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } },
        confirmButton = { TextButton({ profileInfo = false }) { Text("Close") } },
    )
}

@Composable
fun DeliveryNotificationsScreen(back: () -> Unit, onOpen: (com.spacetecsolutions.meatapp.core.model.AppNotification) -> Unit = {}) {
    NotificationHistoryRoute(back, onOpen)
}

@Composable
fun DeliveryHelpScreen(back: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppBackTopBar("Help", back)
        Column(Modifier.padding(AppSpacing.large), verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
            Text("Delivery Help", style = MaterialTheme.typography.headlineSmall)
            Text("For assignment, customer, collection, or account issues, contact your shop Admin.")
            Text("Only deliver orders visible in Assigned Orders. Never collect a different COD amount without reporting it.")
        }
    }
}

private val terminalStatuses = setOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED)

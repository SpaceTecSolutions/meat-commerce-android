package com.spacetecsolutions.meatapp.feature.admin.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.*
import java.text.*
import java.util.*

@Composable
fun AdminCustomersRoute(
    onBack: () -> Unit,
    onMessage: (AdminCustomerMessage) -> Unit,
    viewModel: AdminCustomersViewModel = hiltViewModel(),
) {
    HideAppBottomBar()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) { state.message?.let { onMessage(it); viewModel.consumeMessage() } }
    AdminCustomersScreen(state, onBack, viewModel)
}

private enum class CustomerFilter(val label: String) { ALL("All customers"), ACTIVE("Active"), INACTIVE("Inactive") }

@Composable
private fun AdminCustomersScreen(state: AdminCustomersUiState, back: () -> Unit, actions: AdminCustomersViewModel) {
    var filter by remember { mutableStateOf(CustomerFilter.ALL) }
    var filterOpen by remember { mutableStateOf(false) }
    val visible = state.filteredCustomers().filter {
        filter == CustomerFilter.ALL || (filter == CustomerFilter.ACTIVE) == it.active
    }
    Column(Modifier.fillMaxSize()) {
        AppBackTopBar("Customers", back)
        when {
            state.loading && state.customers.isEmpty() -> ContentStateView(ContentState.Loading)
            state.error != null && state.customers.isEmpty() -> ContentStateView(
                ContentState.Error(description = state.error), onAction = actions::refresh)
            state.customers.isEmpty() && state.query.isBlank() -> ContentStateView(ContentState.Empty(
                title = "No customers", description = "Registered customers will appear here.",
            ))
            else -> Column(Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = AppDimensions.contentMaxWidth)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        state.query, actions::search, Modifier.weight(1f), singleLine = true,
                        placeholder = { Text("Search customers…") },
                        leadingIcon = { Icon(AppIcons.Search, null) },
                        trailingIcon = { if (state.query.isNotEmpty()) IconButton({ actions.search("") }) {
                            Icon(AppIcons.Close, "Clear search")
                        } },
                        shape = AppShapes.medium,
                    )
                    Box {
                        IconButton({ filterOpen = true }) { BadgedBox(
                            badge = { if (filter != CustomerFilter.ALL) Badge() },
                        ) { Icon(AppIcons.Filter, "Filter customers") } }
                        DropdownMenu(filterOpen, { filterOpen = false }) {
                            CustomerFilter.entries.forEach { option -> DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { filter = option; filterOpen = false },
                                leadingIcon = { if (filter == option) Icon(AppIcons.Check, null) },
                            ) }
                        }
                    }
                }
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (visible.isEmpty()) ContentStateView(ContentState.Empty(
                    title = "No matching customers",
                    description = "Try another name, mobile number or customer filter.",
                )) else LazyColumn(
                    contentPadding = PaddingValues(horizontal = AppSpacing.medium, vertical = AppSpacing.extraSmall),
                ) {
                    items(visible, key = AdminCustomerSummary::id) { customer ->
                        CustomerRow(customer, state.busyCustomerId == customer.id) { actions.openDetails(customer) }
                        HorizontalDivider(Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
                    }
                }
            }
        }
    }
    if (state.loadingDetails) AlertDialog(onDismissRequest = {}, confirmButton = {},
        text = { Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
            CircularProgressIndicator(); Text("Loading customer details…")
        } })
    state.details?.let { CustomerDetailsDialog(it, { actions.openDetails(null) }, actions::requestStatus) }
    state.statusTarget?.let { customer -> AlertDialog(onDismissRequest = { actions.requestStatus(null) },
        title = { Text(if (customer.active) "Deactivate customer?" else "Activate customer?") },
        text = { Text(if (customer.active) "The customer will be unable to sign in or place orders. Existing order history is preserved."
            else "The customer will regain account access.") },
        confirmButton = { Button(actions::confirmStatusChange) { Text(if (customer.active) "Deactivate" else "Activate") } },
        dismissButton = { TextButton({ actions.requestStatus(null) }) { Text("Cancel") } }) }
}

@Composable
private fun CustomerRow(customer: AdminCustomerSummary, busy: Boolean, click: () -> Unit) {
    Surface(onClick = click, enabled = !busy, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.medium), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text(customer.displayName.trim().firstOrNull()?.uppercase() ?: "C",
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.medium)) {
                Text(customer.displayName, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(customer.mobileNumber, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (busy) CircularProgressIndicator(Modifier.size(20.dp)) else Text(
                "${customer.totalOrders} ${if (customer.totalOrders == 1L) "Order" else "Orders"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerDetailsDialog(
    details: AdminCustomerDetails, dismiss: () -> Unit, status: (AdminCustomerSummary) -> Unit,
) = ModalBottomSheet(onDismissRequest = dismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(horizontal = AppSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        item { Text(details.customer.displayName, style = MaterialTheme.typography.titleLarge) }
        item { Text(details.customer.mobileNumber); Text("Status: ${if (details.customer.active) "Active" else "Inactive"}")
            Text("Joined: ${details.joinedAtEpochMillis.date()}") }
        item { HorizontalDivider(); Text("${details.customer.totalOrders} total orders",
            style = MaterialTheme.typography.titleMedium); Text("Total spending: ${details.customer.totalSpendingMinor.money(details.customer.currencyCode)}") }
        item { Text("Highest order: ${details.highestOrderMinor.money(details.customer.currencyCode)}") }
        item { Text("Recent orders", style = MaterialTheme.typography.titleMedium) }
        if (details.orders.isEmpty()) item { Text("No orders") }
        items(details.orders.take(3), key = AdminCustomerOrder::id) { order ->
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppSpacing.small)) {
                Text("Order ${order.displayNumber}", style = MaterialTheme.typography.titleSmall)
                Text("${order.createdAtEpochMillis.date()} · ${order.orderStatus.name}")
                Text(order.productSummary, maxLines = 2)
                Text("${order.totalMinor.money(details.customer.currencyCode)} · ${order.paymentMethod.name} · ${order.paymentStatus.name}")
            } }
        }
        item { TextButton(dismiss) { Text("Close") } }
        item {
        if (details.customer.statusManagementAllowed) TextButton({ dismiss(); status(details.customer) }) {
            Text(if (details.customer.active) "Deactivate" else "Activate")
        }
        }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

private fun Long.money(code: String) = NumberFormat.getCurrencyInstance().apply {
    currency = Currency.getInstance(code)
}.format(this / 100.0)
private fun Long.date() = if (this <= 0) "Unavailable" else
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(this))

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
        AppBackTopBar(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_customers_035eae6), back)
        when {
            state.loading && state.customers.isEmpty() -> ContentStateView(ContentState.Loading)
            state.error != null && state.customers.isEmpty() -> ContentStateView(
                ContentState.Error(description = state.error), onAction = actions::refresh)
            state.customers.isEmpty() && state.query.isBlank() -> ContentStateView(ContentState.Empty(
                title = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_no_customers_1560099), description = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_registered_customers_will_appear_here_c203354),
            ))
            else -> Column(Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = AppDimensions.contentMaxWidth)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        state.query, actions::search, Modifier.weight(1f), singleLine = true,
                        placeholder = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_search_customers_1aac222)) },
                        leadingIcon = { Icon(AppIcons.Search, null) },
                        trailingIcon = { if (state.query.isNotEmpty()) IconButton({ actions.search("") }) {
                            Icon(AppIcons.Close, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_clear_search_67300d0))
                        } },
                        shape = AppShapes.medium,
                    )
                    Box {
                        IconButton({ filterOpen = true }) { BadgedBox(
                            badge = { if (filter != CustomerFilter.ALL) Badge() },
                        ) { Icon(AppIcons.Filter, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_filter_customers_735d3da)) } }
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
                    title = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_no_matching_customers_91a9a74),
                    description = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_try_another_name_mobile_number_e1ac3f3),
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
            CircularProgressIndicator(); Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_loading_customer_details_85ecd43))
        } })
    state.details?.let { CustomerDetailsDialog(it, { actions.openDetails(null) }, actions::requestStatus) }
    state.statusTarget?.let { customer -> AlertDialog(onDismissRequest = { actions.requestStatus(null) },
        title = { Text(if (customer.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_deactivate_customer_b3433e8) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_activate_customer_7ae6288)) },
        text = { Text(if (customer.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_the_customer_will_be_unable_5bbb196) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_the_customer_will_regain_account_7c633ff)) },
        confirmButton = { Button(actions::confirmStatusChange) { Text(if (customer.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_deactivate_d65ded9) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_activate_92ef083)) } },
        dismissButton = { TextButton({ actions.requestStatus(null) }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_cancel_77dfd21)) } }) }
}

@Composable
private fun CustomerRow(customer: AdminCustomerSummary, busy: Boolean, click: () -> Unit) {
    Surface(onClick = click, enabled = !busy, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.medium), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text(customer.displayName.trim().firstOrNull()?.uppercase() ?: androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_c_32096c2),
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
        item { Text(details.customer.mobileNumber); Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_customer_status,
            androidx.compose.ui.res.stringResource(if (details.customer.active) com.spacetecsolutions.meatapp.feature.admin.R.string.ui_active else com.spacetecsolutions.meatapp.feature.admin.R.string.ui_inactive)))
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_joined_details_joinedatepochmillis_date_ca70131, details.joinedAtEpochMillis.date())) }
        item { HorizontalDivider(); Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_details_customer_totalorders_total_orders_03d5267, details.customer.totalOrders),
            style = MaterialTheme.typography.titleMedium); Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_total_spending_details_customer_totalspending_a490f84, details.customer.totalSpendingMinor.money(details.customer.currencyCode))) }
        item { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_highest_order_details_highestorderminor_money_1604626, details.highestOrderMinor.money(details.customer.currencyCode))) }
        item { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_recent_orders_cfc7dec), style = MaterialTheme.typography.titleMedium) }
        if (details.orders.isEmpty()) item { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_no_orders_62e70d7)) }
        items(details.orders.take(3), key = AdminCustomerOrder::id) { order ->
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppSpacing.small)) {
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_order_order_displaynumber_5a620eb, order.displayNumber), style = MaterialTheme.typography.titleSmall)
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_order_createdatepochmillis_date_order_orderst_91c900a, order.createdAtEpochMillis.date(), order.orderStatus.name))
                Text(order.productSummary, maxLines = 2)
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_order_totalminor_money_details_customer_e0a149e, order.totalMinor.money(details.customer.currencyCode), order.paymentMethod.name, order.paymentStatus.name))
            } }
        }
        item { TextButton(dismiss) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_close_bbfa773)) } }
        item {
        if (details.customer.statusManagementAllowed) TextButton({ dismiss(); status(details.customer) }) {
            Text(if (details.customer.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_deactivate_d65ded9) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_activate_92ef083))
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

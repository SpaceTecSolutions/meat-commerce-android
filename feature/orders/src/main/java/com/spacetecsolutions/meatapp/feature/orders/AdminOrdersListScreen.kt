package com.spacetecsolutions.meatapp.feature.orders

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.*
import java.text.DateFormat
import java.util.Date

internal enum class AdminOrderFilter(val label: String) {
    ALL("All"), PENDING("Pending"), CONFIRMED("Confirmed"), PREPARING("Preparing"),
    OUT_FOR_DELIVERY("Out for Delivery"), DELIVERED("Delivered"), CANCELLED("Cancelled");

    fun matches(order: CodOrder) = this == ALL || order.orderStatus.name == name
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminOrdersListScreen(
    state: CodOrdersUiState,
    selectOrder: (CodOrder) -> Unit,
    refresh: () -> Unit,
    confirm: (CodOrder) -> Unit,
    reject: (CodOrder) -> Unit,
    canAdjustFinalBill: Boolean = true,
    canUpdateStatus: Boolean = true,
    staffPresentation: Boolean = false,
) {
    var selected by rememberSaveable { mutableStateOf(AdminOrderFilter.ALL) }
    var searchVisible by rememberSaveable { mutableStateOf(staffPresentation) }
    var query by rememberSaveable { mutableStateOf("") }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    var paymentMethod by rememberSaveable { mutableStateOf<CheckoutPaymentMethod?>(null) }
    var paymentStatus by rememberSaveable { mutableStateOf<PaymentStatus?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategory = state.categories.firstOrNull { it.id == categoryId }
    val filtered = remember(state.orders, selected, query, paymentMethod, paymentStatus, selectedCategory) {
        filterAdminOrders(
            state.orders, selected, query, paymentMethod, paymentStatus,
            selectedCategory?.id, selectedCategory?.name,
        )
    }
    Column(Modifier.fillMaxSize().background(T.canvas)) {
        TopAppBar(
            title = { Column {
                if (staffPresentation) Text("MEAT STATION STAFF", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, color = T.red)
                Text(if (staffPresentation && !canAdjustFinalBill && !canUpdateStatus) "Orders Feed" else "Orders",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = T.ink)
            } },
            actions = {
                IconButton(onClick = { searchVisible = !searchVisible }) { Icon(AppIcons.Search, "Search orders") }
                BadgedBox(badge = { if (paymentMethod != null || paymentStatus != null || categoryId != null) Badge() }) {
                    IconButton(onClick = { filterOpen = true }) { Icon(AppIcons.Filter, "Filter orders") }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
        AnimatedVisibility(searchVisible) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(60) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                placeholder = { Text("Order ID, customer or mobile") },
                leadingIcon = { Icon(AppIcons.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(AppIcons.Close, "Clear") } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
        }
        OrderStatusFilterRow(state.orders, selected) { selected = it }
        if (staffPresentation && !canAdjustFinalBill && !canUpdateStatus) {
            Surface(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                shape = RoundedCornerShape(14.dp), color = T.border.copy(alpha = .7f)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Security, null, tint = T.muted)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Read-Only View", fontWeight = FontWeight.SemiBold, color = T.ink)
                        Text("You can inspect orders; operational actions are hidden.",
                            style = MaterialTheme.typography.bodySmall, color = T.muted)
                    }
                }
            }
        }
        PullToRefreshBox(
            isRefreshing = state.loading && state.orders.isNotEmpty(),
            onRefresh = refresh,
            modifier = Modifier.weight(1f),
        ) {
            when {
                state.loading && state.orders.isEmpty() -> ContentStateView(ContentState.Loading)
                state.error != null && state.orders.isEmpty() -> ContentStateView(
                    if (state.error.contains("connection", true)) ContentState.Offline(description = state.error)
                    else ContentState.Error(description = state.error),
                    onAction = refresh,
                )
                filtered.isEmpty() -> ContentStateView(ContentState.Empty(
                    title = if (query.isNotBlank()) "No matching orders found"
                    else "No ${selected.label.lowercase()} orders",
                    description = if (query.isNotBlank()) "Try another order ID, customer name or mobile number."
                    else "Orders in this status will appear here.",
                ))
                else -> BoxWithConstraints(Modifier.fillMaxSize().background(T.canvas), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                            .widthIn(max = AppDimensions.dashboardMaxWidth),
                        contentPadding = PaddingValues(AppSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    ) {
                        items(filtered, key = CodOrder::id) { order ->
                            AdminOrderCard(order, state.busyOrderId == order.id,
                                { selectOrder(order) }, { confirm(order) }, { reject(order) },
                                canAdjustFinalBill, canUpdateStatus)
                        }
                    }
                }
            }
        }
    }
    if (filterOpen) OrderFilterSheet(
        selectedMethod = paymentMethod,
        selectedPaymentStatus = paymentStatus,
        categories = state.categories,
        selectedCategoryId = categoryId,
        onApply = { method, status, selectedCategory ->
            paymentMethod = method; paymentStatus = status; categoryId = selectedCategory; filterOpen = false
        },
        onDismiss = { filterOpen = false },
    )
}

@Composable
internal fun OrderStatusFilterRow(
    orders: List<CodOrder>, selected: AdminOrderFilter, select: (AdminOrderFilter) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(color = T.surface).horizontalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
    ) {
        AdminOrderFilter.entries.forEach { filter ->
            val active = filter == selected
            val background by animateColorAsState(
                if (active) T.red else T.surface,
                label = "order filter",
            )
            val count = orders.count(filter::matches)
            Surface(
                color = background,
                contentColor = if (active) T.surface else T.ink,
                shape = CircleShape,
                border = if (active) null else BorderStroke(1.dp, T.border),
                modifier = Modifier.heightIn(min = 40.dp).clickable(role = Role.Tab) { select(filter) },
            ) {
                Text("${filter.label} ($count)", Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun AdminOrderCard(order: CodOrder, busy: Boolean, onClick: () -> Unit,
    onConfirm: () -> Unit, onReject: () -> Unit, canAdjustFinalBill: Boolean = true,
    canUpdateStatus: Boolean = true) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "Order ${order.displayNumber}, ${order.customerName}, ${order.orderStatus.label()}"
        },
        color = T.surface, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (order.orderStatus == OrderStatus.PENDING)
            T.amber.copy(alpha = .5f) else T.border), shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = order.orderStatus.softColor()) {
                    Icon(order.orderStatus.icon(), null, Modifier.padding(12.dp).size(20.dp),
                        tint = order.orderStatus.strongColor())
                }
                Column(Modifier.weight(1f)) {
                    Text(if (order.orderStatus == OrderStatus.PENDING) "REQUIRES CONFIRMATION"
                        else "ORDER ID", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (order.orderStatus == OrderStatus.PENDING) T.amber else T.muted)
                    Text(order.displayNumber.withHash(), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = T.ink)
                    Text(order.customerName, style = MaterialTheme.typography.bodyMedium,
                        color = T.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(order.totalMinor.money(order.currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = T.ink)
                    OrderStatusChip(order.orderStatus)
                }
            }
            HorizontalDivider(color = T.border)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val summary = order.items.firstOrNull()?.name.orEmpty()
                Text(if (order.orderStatus == OrderStatus.PENDING &&
                    !order.deliverySlotTimeLabel.isNullOrBlank())
                    "Window: ${order.deliverySlotTimeLabel}" else
                    "${order.items.size} item${if (order.items.size == 1) "" else "s"}" +
                        if (summary.isNotBlank()) " ($summary) · ${order.paymentMethod.name}" else
                            " · ${order.paymentMethod.name}",
                    Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                    color = T.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(order.createdAtEpochMillis.orderDateTime(),
                    style = MaterialTheme.typography.labelSmall, color = T.muted)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = T.red)
            if (order.canConfirm && (canAdjustFinalBill || canUpdateStatus)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canUpdateStatus) OutlinedButton(onReject, Modifier.weight(1f), enabled = !busy) { Text("Reject") }
                if (canAdjustFinalBill) Button(onConfirm, Modifier.weight(1f), enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = T.red)) {
                    Text("Accept Order")
                }
            }
        }
    }
}

internal fun String.withHash() = if (startsWith("#")) this else "#$this"
internal fun Long.orderDateTime(): String = if (this <= 0) "Date unavailable" else
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(this))

internal fun filterAdminOrders(
    orders: List<CodOrder>,
    selected: AdminOrderFilter,
    query: String,
    paymentMethod: CheckoutPaymentMethod?,
    paymentStatus: PaymentStatus?,
    categoryId: String? = null,
    categoryName: String? = null,
) = orders.filter { order ->
    selected.matches(order) &&
        (paymentMethod == null || order.paymentMethod == paymentMethod) &&
        (paymentStatus == null || order.paymentStatus == paymentStatus) &&
        (categoryId == null || order.items.any { it.matchesCategory(categoryId, categoryName) }) &&
        (query.isBlank() || listOf(order.displayNumber, order.customerName, order.customerMobile)
            .any { it.contains(query.trim(), ignoreCase = true) })
}

private fun OrderItemSnapshot.matchesCategory(categoryId: String, categoryName: String?): Boolean {
    val expectedId = categoryId.trim()
    val itemId = this.categoryId?.trim().orEmpty()
    if (itemId.isNotEmpty()) return itemId.equals(expectedId, ignoreCase = true)
    return normalizedCategory(this.categoryName) == normalizedCategory(categoryName) &&
        normalizedCategory(categoryName).isNotEmpty()
}

private fun normalizedCategory(value: String?): String = value.orEmpty().trim()
    .replace(Regex("\\s+"), " ").lowercase()

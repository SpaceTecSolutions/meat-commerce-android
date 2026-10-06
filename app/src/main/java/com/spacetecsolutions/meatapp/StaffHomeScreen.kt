package com.spacetecsolutions.meatapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.model.*
import com.spacetecsolutions.meatapp.core.navigation.StaffRoutes
import com.spacetecsolutions.meatapp.feature.orders.CodOrdersViewModel

@Composable
internal fun StaffHomeRoute(user: User, navigate: (String) -> Unit,
    viewModel: CodOrdersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load(CodActor.ADMIN, false, false) }
    StaffHomeScreen(user, state.orders, state.loading, navigate)
}

@Composable
private fun StaffHomeScreen(user: User, orders: List<CodOrder>, loading: Boolean,
    navigate: (String) -> Unit) {
    val capabilities = remember(user.permissions) { StaffCapabilities.from(user.permissions) }
    Scaffold(containerColor = StaffDesign.canvas,
        topBar = { StaffTopBar("Staff Home", { navigate(StaffRoutes.PROFILE) }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { StaffIdentityCard(user) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Operational Hub", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text("Permission based", style = MaterialTheme.typography.labelMedium,
                        color = StaffDesign.muted)
                }
            }
            if (capabilities.canViewOrders) item {
                OperationalCard(AppIcons.Orders, "Active Orders Queue", "Fulfilment and order tracking",
                    if (loading) "Loading…" else "${orders.count { it.orderStatus !in setOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED) }} active",
                    "Open Orders Console") { navigate(StaffRoutes.ORDERS) }
            }
            if (capabilities.canOpenProducts) item {
                OperationalCard(AppIcons.Products, "Product Catalog & Stock",
                    when {
                        capabilities.canManageProducts && capabilities.canManageStock -> "Catalog, pricing and availability"
                        capabilities.canManageProducts -> "Catalog products and pricing"
                        else -> "Stock availability controls"
                    }, "Authorized", if (capabilities.canManageProducts) "Manage Product Catalog" else "Update Stock") {
                    navigate(StaffRoutes.PRODUCTS)
                }
            }
            if (capabilities.canManageFaq) item {
                StaffCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Help, null, Modifier.size(26.dp), tint = StaffDesign.red)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("FAQ Management", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text("Manage customer questions and answers",
                                style = MaterialTheme.typography.bodySmall, color = StaffDesign.muted)
                        }
                        TextButton({ navigate(StaffRoutes.FAQ) }) { Text("Manage") }
                    }
                }
            }
            if (capabilities.canViewOrders && orders.isNotEmpty()) {
                item { StaffSectionLabel("Recent Order Queue", "Live orders") }
                items(orders.filter { it.orderStatus !in setOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED) }
                    .take(2), key = CodOrder::id) { order ->
                    RecentOrderCard(order) { navigate(StaffRoutes.ORDERS) }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = StaffDesign.inset) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Security, null, tint = StaffDesign.muted)
                        Text("Your workspace shows only administrator-approved tools.",
                            Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodySmall,
                            color = StaffDesign.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationalCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String,
    description: String, metric: String, action: String, click: () -> Unit) = StaffCard(Modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(StaffDesign.paleRed, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center) { Icon(icon, null, tint = StaffDesign.red) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = StaffDesign.muted)
        }
        Text(metric, style = MaterialTheme.typography.labelMedium, color = StaffDesign.red)
    }
    Button(click, Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StaffDesign.red)) {
        Text(action); Spacer(Modifier.width(8.dp)); Icon(AppIcons.ArrowRight, null, Modifier.size(18.dp))
    }
}

@Composable
private fun RecentOrderCard(order: CodOrder, click: () -> Unit) = StaffCard(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(order.displayNumber.let { if (it.startsWith("#")) it else "#$it" }, fontWeight = FontWeight.Bold)
            Text(order.customerName, color = StaffDesign.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AssistChip(onClick = click, label = {
            Text(order.orderStatus.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase))
        })
    }
}

@Composable
internal fun StaffAccessDenied() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text("You do not have permission to access this section.", modifier = Modifier.padding(24.dp))
}

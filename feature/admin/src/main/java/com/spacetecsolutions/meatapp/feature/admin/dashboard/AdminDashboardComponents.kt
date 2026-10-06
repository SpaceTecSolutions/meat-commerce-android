package com.spacetecsolutions.meatapp.feature.admin.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetecsolutions.meatapp.core.designsystem.component.RoundedSquareImage
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.model.*
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.roundToLong

@Composable
internal fun AdminDashboardHeader(unread: Int, notifications: () -> Unit, showNotifications: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("Hello, Meat 👋", fontSize = 24.sp, lineHeight = 30.sp,
                fontWeight = FontWeight.Bold, color = DashboardStyle.ink)
            Text("Here's what's happening today", fontSize = 14.sp,
                color = DashboardStyle.muted)
        }
        if (showNotifications) Surface(onClick = notifications, modifier = Modifier.size(44.dp), shape = CircleShape,
            color = DashboardStyle.card, shadowElevation = 2.dp) {
            BadgedBox(badge = { if (unread > 0) Badge(containerColor = DashboardStyle.red) },
                modifier = Modifier.padding(11.dp)) {
                Icon(AppIcons.NotificationsOutline, "Notifications, $unread unread", tint = DashboardStyle.ink)
            }
        }
    }
}

private data class MetricSpec(val value: String, val label: String, val icon: ImageVector,
    val accent: Boolean, val pending: Boolean, val click: () -> Unit)

@Composable
internal fun DashboardMetricGrid(dashboard: AdminDashboard, onSales: () -> Unit, onOrders: () -> Unit) {
    val specs = listOf(
        MetricSpec(money(dashboard.todayRevenueMinor, dashboard.currencyCode), "Today's Sales", AppIcons.Revenue, true, false, onSales),
        MetricSpec(dashboard.todayOrders.toString(), "Total Orders", AppIcons.Orders, false, false, onOrders),
        MetricSpec(dashboard.deliveredOrders.toString(), "Delivered", AppIcons.Confirmed, false, false, onOrders),
        MetricSpec(dashboard.pendingOrders.toString(), "Pending", AppIcons.Hourglass, true, true, onOrders),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        specs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { DashboardMetricCard(it, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(spec: MetricSpec, modifier: Modifier) {
    Surface(onClick = spec.click, modifier = modifier.heightIn(min = 98.dp),
        shape = DashboardStyle.metricShape, color = DashboardStyle.card,
        border = BorderStroke(.5.dp, DashboardStyle.border), shadowElevation = 1.dp) {
        Box(Modifier.fillMaxWidth()) {
        if (spec.pending) Box(Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp)
            .size(46.dp).background(DashboardStyle.paleRed.copy(alpha = .25f), CircleShape))
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (spec.accent) DashboardStyle.paleRed else DashboardStyle.inset,
                    shape = DashboardStyle.metricShape) {
                    Icon(spec.icon, null, Modifier.padding(5.dp).size(18.dp),
                        tint = if (spec.accent) DashboardStyle.red else DashboardStyle.muted)
                }
                Surface(Modifier.size(if (spec.pending) 7.dp else 5.dp), CircleShape,
                    color = if (spec.pending) DashboardStyle.red else DashboardStyle.paleRed) {}
            }
            Spacer(Modifier.height(1.dp))
            Text(spec.value, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontSize = 26.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, color = DashboardStyle.ink)
            Text(spec.label.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold,
                color = if (spec.pending) DashboardStyle.red else DashboardStyle.muted)
        }
        }
    }
}

@Composable
internal fun RevenueOverviewCard(dashboard: AdminDashboard, period: AdminDashboardPeriod,
    onPeriod: (AdminDashboardPeriod) -> Unit) {
    Surface(shape = DashboardStyle.analyticsShape, color = DashboardStyle.card,
        border = BorderStroke(.5.dp, DashboardStyle.border), shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Revenue Overview", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        color = DashboardStyle.ink)
                    Text("Revenue (₹)", fontSize = 12.sp, color = DashboardStyle.muted)
                }
                DashboardPeriodSelector(period, onPeriod)
            }
            AnimatedContent(targetState = dashboard.revenueSeries, label = "revenue contents") { series ->
                if (series.none { it.revenueMinor > 0 }) RevenueEmptyState()
                else DashboardRevenueChart(series, dashboard.currencyCode, period)
            }
        }
    }
}

@Composable
private fun DashboardPeriodSelector(period: AdminDashboardPeriod, onPeriod: (AdminDashboardPeriod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(Modifier.height(48.dp).clickable { expanded = true }, contentAlignment = Alignment.Center) {
            Surface(shape = CircleShape, color = DashboardStyle.inset) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(period.label(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = DashboardStyle.ink)
                    Spacer(Modifier.width(5.dp))
                    Icon(AppIcons.ArrowDown, null, Modifier.size(16.dp), tint = DashboardStyle.muted)
                }
            }
        }
        DropdownMenu(expanded, { expanded = false }) {
            AdminDashboardPeriod.entries.forEach { option -> DropdownMenuItem(
                text = { Text(option.label()) }, onClick = { expanded = false; onPeriod(option) }) }
        }
    }
}

@Composable
private fun RevenueEmptyState() {
    Surface(shape = DashboardStyle.metricShape, color = DashboardStyle.inset,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = DashboardStyle.paleRed) {
                Icon(AppIcons.Analytics, null, Modifier.padding(12.dp).size(23.dp), tint = DashboardStyle.red)
            }
            Spacer(Modifier.height(10.dp))
            Text("No finalized revenue", fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, color = DashboardStyle.ink)
            Text("Sales for this period will appear here.", fontSize = 12.sp,
                color = DashboardStyle.muted)
        }
    }
}

@Composable
internal fun TopSellingProductSection(products: List<AdminDashboardProduct>, currencyCode: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Top Selling Products", Modifier.weight(1f), fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold, color = DashboardStyle.ink)
            if (products.isNotEmpty()) Text("TOP 3", fontSize = 11.sp,
                fontWeight = FontWeight.Bold, color = DashboardStyle.red)
        }
        if (products.isEmpty()) Surface(shape = DashboardStyle.metricShape, color = DashboardStyle.card,
            border = BorderStroke(.5.dp, DashboardStyle.border), shadowElevation = 1.dp) {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = DashboardStyle.inset) {
                    Icon(AppIcons.ProductsOutline, null, Modifier.padding(10.dp).size(18.dp), tint = DashboardStyle.muted)
                }
                Spacer(Modifier.height(6.dp))
                Text("No sales data yet", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = DashboardStyle.ink)
                Text("Products with sales will appear here", fontSize = 12.sp,
                    color = DashboardStyle.muted)
            }
        } else products.take(3).forEachIndexed { index, product -> Surface(onClick = onClick, shape = DashboardStyle.metricShape,
            color = DashboardStyle.card, border = BorderStroke(.5.dp, DashboardStyle.border),
            shadowElevation = 1.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    RoundedSquareImage(product.imageUrl, product.name, 58.dp, cornerRadius = 10.dp)
                    /*Surface(Modifier.align(Alignment.TopStart).size(20.dp), CircleShape, color = DashboardStyle.red) {
                        Box(contentAlignment = Alignment.Center) { Text("${index + 1}", color = Color.White,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }*/
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(product.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DashboardStyle.ink)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(5.dp), CircleShape, color = DashboardStyle.red) {}
                        Spacer(Modifier.width(5.dp))
                        Text("${quantity(product.quantitySold)} ${product.unit.lowercase()}",
                            fontSize = 12.sp, color = DashboardStyle.muted, maxLines = 1)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(money(product.revenueMinor, currencyCode), fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, color = DashboardStyle.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Total Revenue", fontSize = 10.sp, color = DashboardStyle.muted)
                }
            }
        } }
    }
}

@Composable
internal fun DashboardLoadingState(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AdminDashboardHeader(0, {}, false)
        repeat(2) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) { Surface(Modifier.weight(1f).height(98.dp), DashboardStyle.metricShape,
                color = DashboardStyle.inset) {} }
        } }
        Surface(Modifier.fillMaxWidth().height(190.dp), DashboardStyle.analyticsShape,
            color = DashboardStyle.inset) {}
    }
}

internal fun AdminDashboardPeriod.label() = when (this) {
    AdminDashboardPeriod.TODAY -> "Today"
    AdminDashboardPeriod.THIS_WEEK -> "This Week"
    AdminDashboardPeriod.THIS_MONTH -> "This Month"
}
private fun quantity(value: Double) = if (value == value.roundToLong().toDouble()) value.roundToLong().toString() else "%.1f".format(value)
private fun money(minor: Long, code: String) = runCatching {
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(code) }.format(minor / 100.0)
}.getOrElse { "$code ${minor / 100}" }

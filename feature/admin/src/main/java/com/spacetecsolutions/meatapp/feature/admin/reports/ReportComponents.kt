package com.spacetecsolutions.meatapp.feature.admin.reports

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.*
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportsTopBar(back: () -> Unit, periodLabel: String, openFilters: () -> Unit,
    export: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text("Reports", fontWeight = FontWeight.Bold, color = ReportsStyle.ink) },
        navigationIcon = { IconButton(back) { Icon(AppIcons.Back, "Back") } },
        actions = {
            export?.let { IconButton(it) { Icon(AppIcons.Download, "Export Excel report") } }
            Row(Modifier.padding(end = 16.dp).clip(ReportsStyle.pillShape)
                .background(ReportsStyle.surface).border(1.dp, ReportsStyle.rose, ReportsStyle.pillShape)
                .clickable(onClick = openFilters).padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(periodLabel, color = ReportsStyle.red, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(AppIcons.ArrowDown, "Open report filters", Modifier.size(20.dp), tint = ReportsStyle.red)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ReportsStyle.canvas),
    )
}

@Composable
internal fun ProductSelector(name: String, open: () -> Unit) {
    ReportsSurface(Modifier.clickable(onClick = open).heightIn(min = 56.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).background(ReportsStyle.inset, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center) {
                Icon(AppIcons.ProductsOutline, null, Modifier.size(18.dp), tint = ReportsStyle.muted)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("PRODUCT", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, color = ReportsStyle.muted)
                Text(name, fontWeight = FontWeight.SemiBold, color = ReportsStyle.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(AppIcons.ArrowDown, null, tint = ReportsStyle.muted)
        }
    }
}

@Composable
internal fun PeriodNavigation(label: String, period: ReportPeriodType, move: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        PeriodArrow(AppIcons.Back, "Previous period", period != ReportPeriodType.CUSTOM) { move(-1) }
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = ReportsStyle.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        PeriodArrow(AppIcons.ArrowRight, "Next period", period != ReportPeriodType.CUSTOM) { move(1) }
    }
}

@Composable
private fun PeriodArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
    enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(ReportsStyle.surface)
        .border(1.dp, ReportsStyle.border, CircleShape)
        .clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, Modifier.size(18.dp), tint = if (enabled) ReportsStyle.ink else ReportsStyle.muted)
    }
}

@Composable
internal fun ReportMetricGrid(report: AdminReport, productSelected: Boolean) = BoxWithConstraints {
    val metrics = listOf(
        ReportMetric("Total Sales", report.revenueMinor.money(report.currencyCode), report.revenueChangePercent, AppIcons.Revenue),
        ReportMetric("Total Orders", report.orders.toString(), report.ordersChangePercent, AppIcons.Orders),
        ReportMetric("Quantity Sold", report.quantitySold.quantity(report.quantityUnit), null, AppIcons.ProductsOutline),
        ReportMetric("Average Order Value", report.averageOrderValueMinor.money(report.currencyCode), null, AppIcons.Cash),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { ReportMetricCard(it, Modifier.weight(1f)) }
            }
        }
    }
}

private data class ReportMetric(val label: String, val value: String, val change: Double?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun ReportMetricCard(metric: ReportMetric, modifier: Modifier = Modifier) = ReportsSurface(modifier.heightIn(min = 112.dp)) {
    Column(Modifier.fillMaxWidth().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(metric.label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                color = ReportsStyle.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.size(25.dp).background(if (metric.label == "Total Sales") ReportsStyle.rose
                else ReportsStyle.canvas, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(metric.icon, null, Modifier.size(15.dp),
                    tint = if (metric.label == "Total Sales") ReportsStyle.red else ReportsStyle.ink)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(metric.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
            color = ReportsStyle.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = ReportsStyle.border, thickness = .5.dp)
        Spacer(Modifier.height(6.dp))
        ReportComparisonText(metric.change)
    }
}

@Composable
internal fun ReportComparisonText(change: Double?) {
    if (change == null) {
        Text("No comparison data", style = MaterialTheme.typography.labelSmall,
            color = ReportsStyle.muted)
    } else {
        val positive = change >= 0
        Text("${if (positive) "↑" else "↓"} ${kotlin.math.abs(change)}% vs previous period",
            style = MaterialTheme.typography.labelSmall,
            color = if (positive) Success else MaterialTheme.colorScheme.error,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun SalesBarChart(points: List<ReportSeriesPoint>, currency: String, period: ReportPeriodType,
    axisLabel: String = when (period) {
        ReportPeriodType.WEEKLY -> "Day of week"
        ReportPeriodType.MONTHLY -> "Day of month"
        ReportPeriodType.CURRENT_MONTH_BY_WEEK -> "Week of month"
        ReportPeriodType.YEARLY -> "Month"
        ReportPeriodType.CUSTOM -> "Date / period"
    }) {
    AdminRevenueChart(points, currency, axisLabel, line = false)
}

@Composable
internal fun TopProductsSection(products: List<AdminTopProductReport>, currency: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Top Products", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = ReportsStyle.ink)
            Text("Ranked by revenue", style = MaterialTheme.typography.labelMedium, color = ReportsStyle.red)
        }
        if (products.isEmpty()) ReportsSurface(Modifier.heightIn(min = 130.dp)) {
            Column(Modifier.align(Alignment.Center).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(AppIcons.ProductsOutline, null, tint = ReportsStyle.muted)
                Spacer(Modifier.height(8.dp))
                Text("No product sales", fontWeight = FontWeight.SemiBold, color = ReportsStyle.ink)
                Text("No completed product transactions in this period.",
                    style = MaterialTheme.typography.bodySmall, color = ReportsStyle.muted)
            }
        }
        products.forEach { product -> TopProductRow(product, currency) }
    }
}

@Composable
private fun TopProductRow(product: AdminTopProductReport, currency: String) {
    ReportsSurface(Modifier.heightIn(min = 72.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundedSquareImage(product.imageUrl, product.productName, 46.dp, cornerRadius = 10.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(product.productName, fontWeight = FontWeight.Bold, color = ReportsStyle.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(product.quantitySold.quantity(product.unit), style = MaterialTheme.typography.bodySmall,
                    color = ReportsStyle.muted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(product.revenueMinor.money(currency), fontWeight = FontWeight.Bold, color = ReportsStyle.ink)
                Text("REVENUE", style = MaterialTheme.typography.labelSmall, color = ReportsStyle.muted)
            }
        }
    }
}

@Composable
internal fun ProductPerformanceSection(report: AdminReport, product: Product?) {
    val performance = report.topProducts.firstOrNull()
    AppCard(Modifier.fillMaxWidth()) {
        SectionHeader("Product Performance")
        Spacer(Modifier.height(AppSpacing.small))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundedSquareImage(performance?.imageUrl ?: product?.imageUrls?.firstOrNull(), product?.name ?: performance?.productName, 56.dp)
            Column(Modifier.padding(start = AppSpacing.medium)) {
                Text(product?.name ?: performance?.productName ?: "Selected product", fontWeight = FontWeight.Bold)
                Text("${report.completedOrders} completed orders · ${report.quantitySold.quantity(report.quantityUnit)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun ReportEmptyState(productSelected: Boolean, changeFilters: () -> Unit) = ContentStateView(
    ContentState.Empty(
        title = if (productSelected) "No sales for this product" else "No report data",
        description = "No completed sales were found for the selected period.",
        actionLabel = "Change Filters",
    ),
    onAction = changeFilters,
)

internal fun Long.money(currency: String): String = runCatching {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { this.currency = Currency.getInstance(currency) }
        .format(this / 100.0)
}.getOrElse { "$currency ${this / 100.0}" }

private fun Long.compactMoney(currency: String): String {
    val symbol = if (currency == "INR") "₹" else "$currency "
    return when {
        this >= 100_000_00 -> "$symbol${this / 100_000_00}L"
        this >= 1_000_00 -> "$symbol${this / 1_000_00}K"
        else -> "$symbol${this / 100}"
    }
}
private fun niceAxisMaximum(value: Long): Long {
    val major = max(1.0, value / 100.0)
    val step = when {
        major > 100_000 -> 50_000.0
        major > 10_000 -> 5_000.0
        major > 1_000 -> 500.0
        else -> 100.0
    }
    return (ceil(major / step) * step * 100).toLong().coerceAtLeast(100)
}
private fun Double.quantity(unit: String?) = "${if (this % 1.0 == 0.0) toLong() else this} ${unit.unitLabel()}"
private fun String?.unitLabel() = when (this) {
    "KILOGRAM" -> "kg"; "GRAM" -> "g"; "PACK" -> "pack"; "PIECE" -> "piece"
    null -> "mixed units"; else -> lowercase().replace('_', ' ')
}

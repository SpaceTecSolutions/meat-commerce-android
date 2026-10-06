package com.spacetecsolutions.meatapp.feature.superadmin.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.model.BestSellingProduct
import com.spacetecsolutions.meatapp.core.model.ReportSeriesPoint
import java.text.NumberFormat
import java.util.Currency

data class ReportMetric(val label: String, val value: String)

@Composable
fun ReportMetricGrid(metrics: List<ReportMetric>, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        metrics.chunked(columns).forEach { rowMetrics ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                rowMetrics.forEach { metric ->
                    Column(Modifier.weight(1f)) {
                        Text(metric.label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(metric.value, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }
                repeat(columns - rowMetrics.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun ReportRevenueChart(series: List<ReportSeriesPoint>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Column(modifier) {
        if (series.isEmpty()) {
            Text("No finalized revenue in this period", modifier = Modifier.padding(vertical = 72.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val maximum = series.maxOf { it.revenueMinor }.coerceAtLeast(1L)
            Row(Modifier.fillMaxWidth().height(190.dp)) {
                Column(Modifier.width(34.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    listOf(maximum, maximum * 2 / 3, maximum / 3, 0).forEach {
                        Text(compactMoney(it), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Canvas(Modifier.weight(1f).fillMaxHeight()) {
                    repeat(4) { index ->
                        val y = size.height * index / 3
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    }
                    val slot = size.width / series.size
                    val barWidth = (slot * 0.42f).coerceAtMost(10.dp.toPx())
                    series.forEachIndexed { index, point ->
                        val height = size.height * point.revenueMinor.toFloat() / maximum
                        val left = slot * index + (slot - barWidth) / 2
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, size.height - height),
                            size = Size(barWidth, height.coerceAtLeast(2.dp.toPx())),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 34.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                series.filterIndexed { index, _ -> index == 0 || index == series.lastIndex || index % 5 == 0 }
                    .take(7).forEach {
                        Text(it.label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
            }
        }
    }
}

@Composable
fun BestSellingProductsCard(products: List<BestSellingProduct>, currencyCode: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier, shape = AppShapes.large) {
        Column(Modifier.padding(AppSpacing.medium)) {
            Row(Modifier.fillMaxWidth()) {
                Icon(AppIcons.Products, null, tint = MaterialTheme.colorScheme.primary)
                Text("Best selling products", Modifier.padding(start = AppSpacing.small),
                    style = MaterialTheme.typography.titleMedium)
            }
            if (products.isEmpty()) Text("No delivered product sales in this period",
                Modifier.padding(vertical = AppSpacing.large), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else products.take(5).forEachIndexed { index, product ->
                Row(Modifier.fillMaxWidth().padding(top = AppSpacing.medium)) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelLarge)
                    Column(Modifier.padding(start = AppSpacing.medium).weight(1f)) {
                        Text(product.name)
                        Text("${product.quantitySold} sold", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatMoney(product.revenueMinor, currencyCode))
                }
            }
        }
    }
}

fun formatMoney(minor: Long, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(currencyCode)
        minimumFractionDigits = if (minor % 100 == 0L) 0 else 2
        maximumFractionDigits = 2
    }.format(minor / 100.0)
}.getOrElse { "$currencyCode ${minor / 100.0}" }

private fun compactMoney(minor: Long): String {
    val major = minor / 100
    return when {
        major >= 100_000 -> "${major / 100_000}L"
        major >= 1_000 -> "${major / 1_000}K"
        else -> major.toString()
    }
}

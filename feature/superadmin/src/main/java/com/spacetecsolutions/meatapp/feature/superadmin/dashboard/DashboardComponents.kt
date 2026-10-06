package com.spacetecsolutions.meatapp.feature.superadmin.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

data class DashboardMetric(
    val title: String,
    val value: String,
    val icon: ImageVector,
)

@Composable
fun MetricGrid(metrics: List<DashboardMetric>, columns: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        metrics.chunked(columns).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            ) {
                rowMetrics.forEach { metric -> MetricCard(metric, Modifier.weight(1f)) }
                repeat(columns - rowMetrics.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MetricCard(metric: DashboardMetric, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.heightIn(min = 92.dp),
        shape = AppShapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppSpacing.compact),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(metric.title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(metric.icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(metric.value, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}

@Composable
fun RevenueChart(points: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    OutlinedCard(
        modifier, shape = AppShapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(AppSpacing.medium)) {
            Text("Revenue trend", style = MaterialTheme.typography.titleMedium)
            Text(
                "Last 7 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Canvas(
                Modifier.fillMaxWidth().height(132.dp).padding(top = AppSpacing.medium)
                    .semantics { contentDescription = "Seven day revenue trend chart" },
            ) {
                repeat(4) { index ->
                    val y = size.height * index / 3
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                if (points.size > 1) {
                    val maximum = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                    val path = Path()
                    points.forEachIndexed { index, value ->
                        val x = size.width * index / (points.lastIndex)
                        val y = size.height - (value / maximum * size.height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.take(7).forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
fun PaymentDistribution(methods: List<PaymentMethodShare>, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier, shape = AppShapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(AppSpacing.medium)) {
            Text("Payment methods", style = MaterialTheme.typography.titleMedium)
            if (methods.isEmpty()) {
                Text(
                    "No payment data yet",
                    modifier = Modifier.padding(top = AppSpacing.medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                methods.forEachIndexed { index, method ->
                    PaymentRow(method, paymentColor(index), Modifier.padding(top = AppSpacing.medium))
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(method: PaymentMethodShare, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(10.dp), shape = CircleShape, color = color) {}
        Text(method.label, Modifier.padding(start = AppSpacing.small).weight(1f))
        Text("${method.percentage}%", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun paymentColor(index: Int): Color = when (index % 3) {
    0 -> MaterialTheme.colorScheme.primary
    1 -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.tertiary
}

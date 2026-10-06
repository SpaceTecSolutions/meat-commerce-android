package com.spacetecsolutions.meatapp.feature.admin.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.spacetecsolutions.meatapp.core.model.ReportSeriesPoint
import kotlin.math.ceil

@Composable
internal fun AdminRevenueChart(points: List<ReportSeriesPoint>, currency: String, axisLabel: String, line: Boolean) {
    val primary = ReportsStyle.red
    val grid = ReportsStyle.border
    val maximum = points.maxOfOrNull { it.revenueMinor } ?: 0L
    val top = (ceil(maximum.coerceAtLeast(100L) / 10000.0) * 10000).toLong().coerceAtLeast(10000)
    val symbol = if (currency == "INR") "₹" else currency
    Column(Modifier.fillMaxWidth().background(ReportsStyle.surface, ReportsStyle.chartShape)
        .border(1.dp, ReportsStyle.border, ReportsStyle.chartShape).padding(16.dp).semantics {
        contentDescription = points.joinToString { "${it.label}: ${it.revenueMinor.money(currency)}" }
    }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sales Overview", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = ReportsStyle.ink)
                Text("Revenue ($symbol)", style = MaterialTheme.typography.bodySmall,
                    color = ReportsStyle.muted)
            }
            Text("Peak: ${maximum.money(currency)}", style = MaterialTheme.typography.labelSmall,
                color = ReportsStyle.red, modifier = Modifier.background(ReportsStyle.rose, ReportsStyle.pillShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(20.dp))
        if (points.isEmpty() || points.all { it.revenueMinor == 0L }) {
            Box(Modifier.fillMaxWidth().height(175.dp), contentAlignment = Alignment.Center) {
                Text("No revenue recorded in this period", color = ReportsStyle.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Row(Modifier.fillMaxWidth().height(165.dp)) {
                Column(Modifier.width(48.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    listOf(top, top * 2 / 3, top / 3, 0L).forEach { value ->
                        Text(when {
                            value >= 10_000_000 -> "$symbol${value / 10_000_000}L"
                            value >= 100_000 -> "$symbol${value / 100_000}K"
                            else -> "$symbol${value / 100}"
                        }, style = MaterialTheme.typography.labelSmall, color = ReportsStyle.muted)
                    }
                }
                Canvas(Modifier.weight(1f).fillMaxHeight()) {
                    repeat(4) { index ->
                        val y = size.height * index / 3f
                        val segments = 18
                        repeat(segments) { segment ->
                            val x = size.width * segment / segments
                            drawLine(grid, Offset(x, y), Offset(x + size.width / segments * .5f, y), 1.dp.toPx())
                        }
                    }
                    val slot = size.width / points.size
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val height = size.height * point.revenueMinor.toFloat() / top
                        val x = if (line && points.size > 1) size.width * index / (points.size - 1) else slot * (index + .5f)
                        val y = size.height - height
                        if (line) {
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(primary, 3.dp.toPx(), Offset(x, y))
                        } else if (height > 0) {
                            val width = (slot * .42f).coerceAtMost(9.dp.toPx())
                            drawRoundRect(primary, Offset(x - width / 2, y), Size(width, height), CornerRadius(5.dp.toPx()))
                        }
                    }
                    if (line) drawPath(path, primary, style = Stroke(2.dp.toPx()))
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 48.dp, top = 8.dp)) {
                val step = ceil(points.size / 6.0).toInt().coerceAtLeast(1)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    points.filterIndexed { index, _ -> index == 0 || index == points.lastIndex || index % step == 0 }
                        .forEach { Text(it.label, style = MaterialTheme.typography.labelSmall,
                            color = ReportsStyle.muted) }
                }
            }
        }
        Text(axisLabel, Modifier.fillMaxWidth().padding(top = 6.dp),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
            color = ReportsStyle.muted)
    }
}

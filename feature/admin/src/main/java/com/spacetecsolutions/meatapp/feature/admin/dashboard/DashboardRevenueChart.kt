package com.spacetecsolutions.meatapp.feature.admin.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetecsolutions.meatapp.core.model.AdminDashboardPeriod
import com.spacetecsolutions.meatapp.core.model.ReportSeriesPoint
import kotlin.math.ceil

/** Dashboard-only presentation of the existing period-grouped revenue series. */
@Composable
internal fun DashboardRevenueChart(points: List<ReportSeriesPoint>, currency: String,
    period: AdminDashboardPeriod) {
    val maximum = points.maxOf { it.revenueMinor }
    val top = (ceil(maximum / 100_000.0) * 100_000).toLong().coerceAtLeast(100_000)
    val selected = points.indexOfFirst { it.revenueMinor == maximum }
    val symbol = if (currency == "INR") "₹" else currency
    Column(Modifier.fillMaxWidth().semantics {
        contentDescription = points.joinToString { "${it.label}, ${moneyLabel(it.revenueMinor, symbol)}" }
    }) {
        Row(Modifier.fillMaxWidth().height(160.dp)) {
            Column(Modifier.width(42.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End) {
                listOf(top, top * 2 / 3, top / 3, 0L).forEach { amount ->
                    Text(moneyLabel(amount, symbol), fontSize = 10.sp, color = DashboardStyle.muted,
                        maxLines = 1)
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxSize().padding(start = 7.dp, end = 4.dp, top = 17.dp, bottom = 2.dp)) {
                    val width = size.width
                    val height = size.height
                    val line = Path()
                    val area = Path()
                    val locations = points.mapIndexed { index, point ->
                        Offset(if (points.size == 1) width / 2 else width * index / (points.size - 1),
                            height * (1f - point.revenueMinor.toFloat() / top))
                    }
                    repeat(4) { index ->
                        val y = height * index / 3f
                        drawLine(DashboardStyle.border, Offset(0f, y), Offset(width, y),
                            1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
                    }
                    locations.forEachIndexed { index, here ->
                        if (index == 0) { line.moveTo(here.x, here.y); area.moveTo(here.x, here.y) }
                        else {
                            val before = locations[index - 1]
                            val middle = (before.x + here.x) / 2
                            line.cubicTo(middle, before.y, middle, here.y, here.x, here.y)
                            area.cubicTo(middle, before.y, middle, here.y, here.x, here.y)
                        }
                    }
                    area.lineTo(locations.last().x, height)
                    area.lineTo(locations.first().x, height)
                    area.close()
                    drawPath(area, Brush.verticalGradient(listOf(DashboardStyle.red.copy(alpha = .19f),
                        Color.Transparent), endY = height))
                    drawPath(line, DashboardStyle.red, style = Stroke(2.dp.toPx()))
                    locations.forEachIndexed { index, location ->
                        drawCircle(if (index == selected) DashboardStyle.card else DashboardStyle.red,
                            if (index == selected) 7.dp.toPx() else 3.dp.toPx(), location)
                        if (index == selected) drawCircle(DashboardStyle.red, 4.dp.toPx(), location)
                    }
                }
                if (points.size > 1 && selected >= 0) {
                    val usable = maxWidth - 11.dp
                    val x = (usable * selected / (points.size - 1)).coerceIn(0.dp, maxWidth - 47.dp)
                    val y = ((maxHeight - 19.dp) * (1f - maximum.toFloat() / top) - 9.dp)
                        .coerceIn(0.dp, maxHeight - 32.dp)
                    Surface(Modifier.offset(x = x, y = y), CircleShape, color = DashboardStyle.ink,
                        shadowElevation = 2.dp) {
                        Text(moneyLabel(maximum, symbol), Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 42.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            val step = ceil(points.size / 6.0).toInt().coerceAtLeast(1)
            points.forEachIndexed { index, point ->
                if (index == 0 || index == points.lastIndex || index % step == 0)
                    Text(point.label, Modifier.weight(1f), fontSize = 10.sp, textAlign = TextAlign.Center,
                        color = if (index == selected) DashboardStyle.red else DashboardStyle.muted,
                        fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(when (period) {
            AdminDashboardPeriod.TODAY -> "Time of day (4-hour periods)"
            AdminDashboardPeriod.THIS_WEEK -> "Day of week"
            AdminDashboardPeriod.THIS_MONTH -> "Days of month (weekly periods)"
        }, Modifier.fillMaxWidth().padding(top = 5.dp), textAlign = TextAlign.Center,
            fontSize = 11.sp, color = DashboardStyle.muted)
    }
}

private fun moneyLabel(minor: Long, symbol: String): String {
    val amount = minor / 100
    return when {
        amount >= 100_000 -> "$symbol${amount / 100_000}L"
        amount >= 1_000 -> "$symbol${amount / 1_000}K"
        else -> "$symbol$amount"
    }
}

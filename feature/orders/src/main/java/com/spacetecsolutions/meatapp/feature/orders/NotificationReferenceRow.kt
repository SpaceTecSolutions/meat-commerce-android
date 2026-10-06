package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun NotificationReferenceRow(item: AppNotification, click: () -> Unit) {
    val tint = when {
        item.event.name == "ORDER_OUT_FOR_DELIVERY" || item.event.name == "OUT_FOR_DELIVERY" -> MaterialTheme.colorScheme.primary
        item.event.name.contains("CONFIRMED") || item.event.name.contains("DELIVERED") -> Success
        item.category == NotificationCategory.PROMOTION -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    val icon = when {
        item.category == NotificationCategory.PROMOTION -> AppIcons.Offer
        item.event.name.contains("CONFIRMED") -> AppIcons.Delivered
        item.event.name.contains("DELIVERY") -> AppIcons.DeliveryPerson
        item.category == NotificationCategory.PAYMENT -> AppIcons.Card
        else -> AppIcons.Orders
    }
    ElevatedCard(onClick = click, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = AppShapes.medium, elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
    Row(Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.Top) {
        Surface(color = tint.copy(alpha = .1f), shape = AppShapes.small,
            modifier = Modifier.padding(top = 2.dp).size(30.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(16.dp), tint = tint) }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(item.title, Modifier.weight(1f).padding(end = 8.dp), style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold)
                Text(notificationTime(item.createdAtEpochMillis), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    }
}

internal fun notificationTime(epoch: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epoch).atZone(zone)
    val today = LocalDate.now(zone)
    return when (date.toLocalDate()) {
        today -> date.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "MMM d" else "MMM d, yyyy", Locale.getDefault()))
    }
}

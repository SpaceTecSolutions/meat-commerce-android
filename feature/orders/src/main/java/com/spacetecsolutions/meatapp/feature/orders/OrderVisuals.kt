package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.spacetecsolutions.meatapp.core.designsystem.component.StatusChip
import com.spacetecsolutions.meatapp.core.designsystem.component.StatusTone
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.OrderStatus

@Composable
internal fun OrderStatusChip(status: OrderStatus) = StatusChip(status.label(), tone = status.tone())

internal fun OrderStatus.label() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)

internal fun OrderStatus.tone() = when (this) {
    OrderStatus.PENDING, OrderStatus.PREPARING -> StatusTone.WARNING
    OrderStatus.CONFIRMED, OrderStatus.OUT_FOR_DELIVERY -> StatusTone.INFO
    OrderStatus.DELIVERED -> StatusTone.SUCCESS
    OrderStatus.CANCELLED -> StatusTone.ERROR
}

internal fun OrderStatus.icon(): ImageVector = when (this) {
    OrderStatus.PENDING -> AppIcons.Pending
    OrderStatus.CONFIRMED -> AppIcons.Confirmed
    OrderStatus.PREPARING -> AppIcons.Preparing
    OrderStatus.OUT_FOR_DELIVERY -> AppIcons.Delivery
    OrderStatus.DELIVERED -> AppIcons.Delivered
    OrderStatus.CANCELLED -> AppIcons.Cancelled
}

internal fun OrderStatus.softColor(): Color = when (this) {
    OrderStatus.PENDING, OrderStatus.PREPARING -> SoftAmber
    OrderStatus.CONFIRMED, OrderStatus.OUT_FOR_DELIVERY -> Color(0xFFE7F1FA)
    OrderStatus.DELIVERED -> SoftGreen
    OrderStatus.CANCELLED -> SoftRed
}

@Composable
internal fun OrderStatus.strongColor(): Color = when (this) {
    OrderStatus.PENDING, OrderStatus.PREPARING -> Warning
    OrderStatus.CONFIRMED, OrderStatus.OUT_FOR_DELIVERY -> Info
    OrderStatus.DELIVERED -> Success
    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
}

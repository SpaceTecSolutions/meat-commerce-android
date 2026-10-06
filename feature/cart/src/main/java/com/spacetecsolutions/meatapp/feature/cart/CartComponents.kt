package com.spacetecsolutions.meatapp.feature.cart

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImage
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerCommerceTokens as T
import com.spacetecsolutions.meatapp.core.model.*
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CartTopBar(hasItems: Boolean, editing: Boolean, back: () -> Unit, edit: () -> Unit) {
    TopAppBar(
        title = { Text("My Cart", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold) },
        navigationIcon = { IconButton(onClick = back) { Icon(AppIcons.Back, "Back") } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        expandedHeight = 64.dp,
    )
}

@Composable
internal fun DeliveryAddressSection(address: CustomerAddress?, change: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(vertical = AppSpacing.small),
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp) {
        Row(Modifier.padding(AppSpacing.medium), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(AppIcons.Location, null, Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.small),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("DELIVERING TO", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(address?.type?.name?.lowercase()?.replaceFirstChar(Char::titlecase)
                    ?: "Choose an address", fontWeight = FontWeight.Bold)
                Text(address?.compactAddress() ?: "No delivery address selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = change) { Text(if (address == null) "Add" else "Change") }
        }
    }
}

@Composable
internal fun CartItemRow(
    line: CartLine,
    busy: Boolean,
    editing: Boolean,
    onProduct: () -> Unit,
    onQuantity: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)),
        shadowElevation = 8.dp,
    ) {
      Row(Modifier.fillMaxWidth().padding(AppSpacing.small), verticalAlignment = Alignment.CenterVertically) {
        AppImage(
            line.imageUrl, line.name,
            Modifier.size(68.dp).clickable(onClick = onProduct),
            cornerRadius = 10.dp,
        )
        Column(
            Modifier.weight(1f).padding(start = AppSpacing.compact),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(line.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).clickable(onClick = onProduct))
            }
            /*Text(line.unit.displayName(), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)*/
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${money(line.lineTotalMinor)} / ${line.unit.homeUnit()}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    if (line.regularPriceMinor > line.unitPriceMinor) Text(
                        money(line.regularPriceMinor * line.quantity),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough)
                }
//                CartQuantitySelector(line, busy, onQuantity)
            }
            if (!line.available) Text("Currently unavailable", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error)
        }
          CartQuantitySelector(line, busy, onQuantity)
      }
    }
}

@Composable
private fun CartQuantitySelector(line: CartLine, busy: Boolean, quantity: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier) {
        QuantityAction(AppIcons.Remove, "Decrease ${line.name}", !busy, false) { quantity(line.quantity - 1) }
        AnimatedContent(
            targetState = line.quantity,
            modifier = Modifier.width(34.dp),
            contentAlignment = Alignment.Center,
            transitionSpec = { fadeIn(tween(AppMotion.MICRO_MILLIS)) togetherWith fadeOut(tween(AppMotion.QUICK_MILLIS)) },
            label = "cart quantity",
        ) { value -> Text("$value", textAlign = TextAlign.Center) }
        QuantityAction(AppIcons.Add, "Increase ${line.name}",
            !busy && line.available && line.quantity < line.maxQuantity, true) { quantity(line.quantity + 1) }
    }
}

@Composable
private fun QuantityAction(icon: ImageVector, description: String, enabled: Boolean, primary: Boolean, click: () -> Unit) {
    IconButton(onClick = click, shape = RoundedCornerShape(8.dp), enabled = enabled, modifier = Modifier.size(34.dp)) {
        Surface(
            modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (primary) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant),
            color = if (primary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .22f)
                else MaterialTheme.colorScheme.surface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    modifier = Modifier.size(20.dp),
                    tint = (if (primary) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        .copy(alpha = if (enabled) 1f else 0.38f),
                )
            }
        }
    }
}

@Composable
internal fun AddMoreItems(click: () -> Unit) {
    TextButton(onClick = click, modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.small)) {
        Icon(AppIcons.Add, null, Modifier.size(AppDimensions.iconSmall))
        Spacer(Modifier.width(AppSpacing.extraSmall))
        Text("Add more items")
    }
}

@Composable
internal fun CartBillSummary(cart: CustomerCart) {
    Column(Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
        Text("Bill Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SummaryRow("Item Total", money(cart.subtotalMinor))
        if (cart.discountMinor > 0) SummaryRow("Discount", "−${money(cart.discountMinor)}")
        if (cart.deliveryFeeMinor == 0L) FreeDeliveryRow()
        else SummaryRow("Delivery Charge", money(cart.deliveryFeeMinor))
        Surface(shape = RoundedCornerShape(12.dp), color = T.soft) {
            Column(Modifier.fillMaxWidth().padding(AppSpacing.small)) {
                SummaryRow("TO PAY", money(cart.totalMinor), emphasize = true)
                if (cart.discountMinor > 0) Text(
                    "Saved ${money(cart.discountMinor)} on this order",
                    style = MaterialTheme.typography.labelSmall, color = T.red)
            }
        }
        if (cart.minimumOrderMinor > cart.subtotalMinor) Text(
            "Add ${money(cart.minimumOrderMinor - cart.subtotalMinor)} more to reach the minimum order.",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun CartAdjustmentNotice(adjustments: List<CartAdjustment>) {
    if (adjustments.isEmpty()) return
    Text(adjustments.distinctBy(CartAdjustment::message).joinToString("\n") { it.message },
        Modifier.fillMaxWidth().padding(vertical = AppSpacing.small),
        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun EmptyCartContent(browse: () -> Unit) = ContentStateView(
    ContentState.Empty(
        title = "Your cart is empty",
        description = "Looks like you haven't added anything yet.",
        actionLabel = "Browse Products",
    ),
    onAction = browse,
)

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasize) T.green else MaterialTheme.colorScheme.onSurface)
        Text(value, style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (emphasize) T.green else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FreeDeliveryRow() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Delivery Charge", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = T.muted,
            textDecoration = TextDecoration.LineThrough)
        Text("FREE", style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold, color = T.green)
    }
}

private fun CustomerAddress.compactAddress() = listOf(address, city).filter(String::isNotBlank).joinToString(", ") +
        postalCode.takeIf(String::isNotBlank)?.let { " - $it" }.orEmpty()
private fun ProductUnit.displayName() = name.lowercase().replaceFirstChar(Char::titlecase)
internal fun money(minor: Long) = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
    currency = Currency.getInstance("INR"); maximumFractionDigits = if (minor % 100 == 0L) 0 else 2
}.format(minor / 100.0)

private fun ProductUnit.homeUnit() = when (this) {
    ProductUnit.KILOGRAM -> "kg"; ProductUnit.GRAM -> "g"
    ProductUnit.PIECE -> "piece"; ProductUnit.PACK -> "pack"
}
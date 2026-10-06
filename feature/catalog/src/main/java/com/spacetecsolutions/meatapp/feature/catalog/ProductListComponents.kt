package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.Product
import com.spacetecsolutions.meatapp.core.model.ProductCategory
import java.text.NumberFormat
import java.util.Currency

@Composable
internal fun ProductSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search products",
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        leadingIcon = { Icon(AppIcons.Search, null) },
        trailingIcon = if (value.isNotBlank()) {
            { IconButton({ onValueChange("") }) { Icon(AppIcons.Close, "Clear search") } }
        } else null,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = AppShapes.medium,
    )
}

@Composable
internal fun ProductCategoryFilter(
    categories: List<ProductCategory>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
    ) {
        ProductCategoryChip("All", selectedId == null) { onSelected(null) }
        categories.forEach { category ->
            ProductCategoryChip(category.name, selectedId == category.id) { onSelected(category.id) }
        }
    }
}

@Composable
private fun ProductCategoryChip(label: String, selected: Boolean, click: () -> Unit) {
    val background = animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "category chip",
    )
    Surface(
        onClick = click,
        shape = AppShapes.large,
        color = background.value,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) { Text(label, Modifier.padding(horizontal = AppSpacing.compact, vertical = AppSpacing.small),
        style = MaterialTheme.typography.labelMedium, maxLines = 1) }
}

@Composable
internal fun AdminProductRow(
    product: Product,
    busy: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    canEdit: Boolean = true,
    canToggle: Boolean = true,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy && canEdit, onClick = onClick)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp)),
        shape = AppShapes.small,
//        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(AppSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryImage(product.imageUrls.firstOrNull(), product.name, Modifier.size(60.dp))
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.compact)) {
                Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val selling = product.offerPriceMinor ?: product.priceMinor
                    Text("${money(selling)} / ${product.unit.shortLabel()}", style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium)
                    if (product.offerPriceMinor != null) Text(
                        money(product.priceMinor), Modifier.padding(start = AppSpacing.small),
                        style = MaterialTheme.typography.labelSmall,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (product.stockQuantity <= 0) "Out of Stock" else "In Stock · ${product.stockQuantity.cleanDisplay()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.stockQuantity <= 0) MaterialTheme.colorScheme.error else Color(0xFF16863C),
                )
            }
            Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Switch(
                    checked = product.active,
                    onCheckedChange = if (canToggle) ({ onToggle() }) else null,
                    modifier = Modifier.size(width = 48.dp, height = 32.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF22A652),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFB7BBC1),
                        uncheckedBorderColor = Color.Transparent,
                        disabledCheckedThumbColor = Color.White.copy(alpha = .9f),
                        disabledCheckedTrackColor = Color(0xFF22A652).copy(alpha = .45f),
                        disabledUncheckedThumbColor = Color.White.copy(alpha = .9f),
                        disabledUncheckedTrackColor = Color(0xFFB7BBC1).copy(alpha = .55f),
                        disabledUncheckedBorderColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

internal fun com.spacetecsolutions.meatapp.core.model.ProductUnit.shortLabel(): String = when (this) {
    com.spacetecsolutions.meatapp.core.model.ProductUnit.KILOGRAM -> "kg"
    com.spacetecsolutions.meatapp.core.model.ProductUnit.GRAM -> "g"
    com.spacetecsolutions.meatapp.core.model.ProductUnit.PIECE -> "piece"
    com.spacetecsolutions.meatapp.core.model.ProductUnit.PACK -> "pack"
}

private fun money(minor: Long): String = NumberFormat.getCurrencyInstance().apply {
    currency = Currency.getInstance("INR"); maximumFractionDigits = 0
}.format(minor / 100.0)
private fun Double.cleanDisplay() = if (this % 1.0 == 0.0) toLong().toString() else toString()

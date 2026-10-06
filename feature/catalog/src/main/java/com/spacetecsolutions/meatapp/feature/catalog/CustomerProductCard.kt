package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImage
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.AppLottieAnimation
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.Product

@Composable
fun CustomerProductListItem(
    product: Product,
    adding: Boolean,
    quantity: Int,
    onClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = CustomerProductPolicy.canAddToCart(product)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics {
            contentDescription = product.accessibilityLabel()
        },
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(AppSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppImage(
                model = product.imageUrls.firstOrNull(),
                contentDescription = null,
                modifier = Modifier.size(76.dp),
                cornerRadius = 10.dp,
            )
            Column(
                Modifier.weight(1f).padding(horizontal = AppSpacing.compact),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
            ) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "1 ${product.unit.shortLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ProductListPrice(product)
            }
            if (available) {
                ProductListQuantityControl(
                    productName = product.name,
                    quantity = quantity,
                    adding = adding,
                    canIncrease = quantity < CustomerProductPolicy.maxQuantity(product),
                    onIncrease = onIncrease,
                    onDecrease = onDecrease,
                )
            } else Text(
                "Out of Stock",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProductListQuantityControl(
    productName: String,
    quantity: Int,
    adding: Boolean,
    canIncrease: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
) {
    if (quantity <= 0) {
        OutlinedIconButton(
            onClick = onIncrease, enabled = !adding,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(40.dp),
        ) {
            if (adding) AppLottieAnimation(
                animation = AppAnimation.AddToCart,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            else Icon(AppIcons.Add, "Add $productName", Modifier.size(20.dp))
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SmallQuantityButton(AppIcons.Remove, "Remove one $productName", !adding, onDecrease)
        Text("$quantity", Modifier.widthIn(min = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        SmallQuantityButton(AppIcons.Add, "Add one $productName", !adding && canIncrease, onIncrease)
    }
}

@Composable
private fun SmallQuantityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    click: () -> Unit,
) {
    IconButton(onClick = click, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Surface(shape = AppShapes.extraSmall, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.surface) {
            Icon(icon, description, Modifier.size(22.dp).padding(3.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ProductListPrice(product: Product) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
    ) {
        Text(
            "${formatProductMoney(product.effectivePriceMinor)} / ${product.unit.shortLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        if (product.validOfferPriceMinor != null) Text(
            formatProductMoney(product.priceMinor),
            style = MaterialTheme.typography.labelSmall,
            textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Product.accessibilityLabel(): String = buildString {
    append(name)
    append(", 1 ")
    append(unit.shortLabel())
    append(", ")
    append(formatProductMoney(effectivePriceMinor))
    if (!CustomerProductPolicy.canAddToCart(this@accessibilityLabel)) append(", out of stock")
}

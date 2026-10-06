package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.AppLottieAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.bounceOnPress
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerCommerceTokens as T
import com.spacetecsolutions.meatapp.core.model.Product

@Composable
internal fun ProductRatingAndUnit(product: Product) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
    ) {
        Icon(AppIcons.Rating, null, Modifier.size(16.dp), tint = Warning)
        val rating = product.ratingAverage
        Text(
            if (rating != null) "${"%.1f".format(rating)} (${product.ratingCount})" else "No ratings yet",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ProductPriceBlock(product: Product) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            formatProductMoney(product.effectivePriceMinor),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
            color = T.ink, fontWeight = FontWeight.Black,
        )
        Text("/ ${product.unit.shortLabel()}", style = MaterialTheme.typography.bodyMedium,
            color = T.muted, fontWeight = FontWeight.SemiBold)
        if (product.validOfferPriceMinor != null) {
            Text(
                formatProductMoney(product.priceMinor),
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = TextDecoration.LineThrough,
                color = T.muted,
            )
            Text(
                "${CustomerProductPolicy.discountPercent(product)}% OFF",
                style = MaterialTheme.typography.labelMedium,
                color = T.red,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun ProductAttributeChip(label: String) {
    Surface(color = T.canvas, shape = AppShapes.large,
        border = BorderStroke(1.dp, T.border)) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
        ) {
            Icon(AppIcons.Check, null, Modifier.size(15.dp), tint = T.red)
            Text(label, style = MaterialTheme.typography.labelMedium, color = T.ink)
        }
    }
}

@Composable
internal fun ProductQuantitySelector(
    product: Product,
    quantity: Int,
    enabled: Boolean,
    onQuantity: (Int) -> Unit,
) {
    Surface(
        color = T.canvas,
        shape = AppShapes.large,
        border = BorderStroke(1.dp, T.border),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("SELECT QUANTITY", Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = T.ink)
                Text("Unit: 1 ${product.unit.shortLabel()}",
                    style = MaterialTheme.typography.labelSmall, color = T.muted)
            }
            Surface(shape = AppShapes.medium, color = T.surface,
                border = BorderStroke(1.dp, T.border), shadowElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().height(62.dp)
                    .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedIconButton(onClick = { onQuantity(-1) },
                        enabled = enabled && quantity > 1,
                        modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, T.border),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = T.red)) {
                        Icon(AppIcons.Remove, "Decrease quantity", Modifier.size(18.dp))
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        AnimatedContent(targetState = quantity,
                            transitionSpec = {
                                fadeIn(tween(AppMotion.MICRO_MILLIS)) togetherWith
                                    fadeOut(tween(AppMotion.QUICK_MILLIS))
                            }, contentAlignment = Alignment.Center,
                            label = "product quantity") { value ->
                            Text("$value ${product.unit.shortLabel()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = T.ink)
                        }
                    }
                    OutlinedIconButton(onClick = { onQuantity(1) },
                        enabled = enabled && quantity < CustomerProductPolicy.maxQuantity(product),
                        modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, T.border),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = T.red)) {
                        Icon(AppIcons.Add, "Increase quantity", Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddToCartBar(product: Product, quantity: Int, adding: Boolean, onAdd: () -> Unit, inCart: Boolean = false) {
    val available = CustomerProductPolicy.canAddToCart(product)
    val interactionSource = remember { MutableInteractionSource() }
        Button(
            onClick = onAdd,
            enabled = (inCart || available) && !adding,
            modifier = Modifier.bounceOnPress(interactionSource).fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = AppSpacing.medium)
                .heightIn(min = 60.dp),
            shape = AppShapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = T.red),
            interactionSource = interactionSource,
        ) {
            if (adding) AppLottieAnimation(
                animation = AppAnimation.AddToCart,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
            ) else {
                Surface(shape = AppShapes.large, color = T.surface.copy(alpha = .2f)) {
                    Icon(AppIcons.Cart, null, Modifier.padding(7.dp).size(22.dp), tint = T.surface)
                }
                Spacer(Modifier.width(AppSpacing.small))
                Text(if (inCart) "View Cart" else if (available) "Add to Cart" else "Out of Stock", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (!inCart) Text(formatProductMoney(product.effectivePriceMinor * quantity), fontWeight = FontWeight.Bold)
                Icon(AppIcons.ArrowRight, null, Modifier.padding(start = 5.dp).size(18.dp))
            }
        }
}

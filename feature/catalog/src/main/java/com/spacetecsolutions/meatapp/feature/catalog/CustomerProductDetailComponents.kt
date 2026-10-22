package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.AppLottieAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.bounceOnPress
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.designsystem.theme.Warning
import com.spacetecsolutions.meatapp.core.model.Product
import kotlin.math.roundToInt
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerCommerceTokens as T

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
        Text(stringResource(R.string.ui_product_unit_shortlabel_e6406af, product.unit.shortLabel()), style = MaterialTheme.typography.bodyMedium,
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
                Text(stringResource(R.string.ui_select_quantity_45d0342), Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = T.ink)
                Text(stringResource(R.string.ui_unit_1_product_unit_shortlabel_acd28e1, product.unit.shortLabel()),
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
                        Icon(AppIcons.Remove, stringResource(R.string.ui_decrease_quantity_6c02abf), Modifier.size(18.dp))
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
                        Icon(AppIcons.Add, stringResource(R.string.ui_increase_quantity_062e79b), Modifier.size(18.dp))
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
            enabled = (inCart || available),
            modifier = Modifier.bounceOnPress(interactionSource).fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = AppSpacing.medium)
                .heightIn(min = 60.dp),
            shape = AppShapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = T.red),
            interactionSource = interactionSource,
        ) {
            if (adding && !inCart) CartIconFlight()
            else {
                Surface(shape = AppShapes.large, color = T.surface.copy(alpha = .2f)) {
                    Icon(AppIcons.Cart, null, Modifier.padding(7.dp).size(22.dp), tint = T.surface)
                }
                Spacer(Modifier.width(AppSpacing.small))
                Text(stringResource(when {
                    inCart -> R.string.ui_view_cart
                    available -> R.string.ui_add_to_cart
                    else -> R.string.ui_out_of_stock
                }), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (!inCart) Text(formatProductMoney(product.effectivePriceMinor * quantity), fontWeight = FontWeight.Bold)
                Icon(AppIcons.ArrowRight, null, Modifier.padding(start = 5.dp).size(18.dp))
            }
        }
}

@Composable
private fun CartIconFlight() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(720, easing = LinearOutSlowInEasing))
    }

    BoxWithConstraints(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.TopStart) {
        val density = LocalDensity.current
        val iconSize = 22.dp
        val travelX = with(density) { (maxWidth - iconSize).toPx().coerceAtLeast(0f) }
        val travelY = with(density) { (32.dp - iconSize).toPx().coerceAtLeast(0f) }
        Icon(
            AppIcons.Cart,
            stringResource(R.string.ui_add_to_cart),
            Modifier.offset {
                IntOffset((travelX * progress.value).roundToInt(), (travelY * progress.value).roundToInt())
            }.size(iconSize),
            tint = T.surface,
        )
    }
}

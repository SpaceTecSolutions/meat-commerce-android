package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImage
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerHomeTokens
import com.spacetecsolutions.meatapp.core.model.Product
import com.spacetecsolutions.meatapp.core.model.ProductUnit
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun HomeBestSellerCard(product: Product, quantity: Int, cartLoaded: Boolean, updating: Boolean,
    onClick: () -> Unit, onAdd: () -> Unit, onQuantityChange: (Int) -> Unit) {
    Card(onClick = onClick,
        modifier = Modifier.width(146.dp).semantics {
            contentDescription = "${product.name}, ${product.effectivePriceMinor.homeMoney()} per ${product.unit.homeUnit()}"
        }, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CustomerHomeTokens.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
        border = BorderStroke(1.dp, CustomerHomeTokens.border)) {
        Column {
            AppImage(product.imageUrls.firstOrNull(), product.name,
                Modifier.fillMaxWidth().height(98.dp), cornerRadius = 0.dp)
            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                Text(product.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, color = CustomerHomeTokens.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(product.effectivePriceMinor.homeMoney(),
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                            color = CustomerHomeTokens.ink, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text("/ ${product.unit.homeUnit()}", style = MaterialTheme.typography.labelSmall,
                            color = CustomerHomeTokens.muted)
                    }
                    if (cartLoaded) HomeProductAddControl(product, quantity, updating, onAdd, onQuantityChange)
                }
            }
        }
    }
}

@Composable
private fun HomeProductAddControl(product: Product, quantity: Int, updating: Boolean,
    onAdd: () -> Unit, onQuantityChange: (Int) -> Unit) {
    val available = product.active && !product.archived && product.stockQuantity > 0
    if (!available) return
    if (quantity <= 0) OutlinedButton(onAdd, enabled = !updating,
        modifier = Modifier.height(34.dp), shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
        border = BorderStroke(1.dp, CustomerHomeTokens.red)) {
        Text("ADD +", style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, color = CustomerHomeTokens.red, maxLines = 1)
    } else Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton({ onQuantityChange(-1) }, enabled = !updating,
            modifier = Modifier.size(26.dp)) {
            Icon(AppIcons.Remove, "Remove one", Modifier.size(16.dp), tint = CustomerHomeTokens.red)
        }
        Text(quantity.toString(), style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, color = CustomerHomeTokens.red)
        IconButton({ onQuantityChange(1) }, enabled = !updating,
            modifier = Modifier.size(26.dp)) {
            Icon(AppIcons.Add, "Add one", Modifier.size(16.dp), tint = CustomerHomeTokens.red)
        }
    }
}

private val Product.effectivePriceMinor: Long
    get() = offerPriceMinor?.takeIf { it in 1 until priceMinor } ?: priceMinor

private fun Long.homeMoney(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
    maximumFractionDigits = if (this@homeMoney % 100L == 0L) 0 else 2
}.format(this / 100.0)

private fun ProductUnit.homeUnit() = when (this) {
    ProductUnit.KILOGRAM -> "kg"; ProductUnit.GRAM -> "g"
    ProductUnit.PIECE -> "piece"; ProductUnit.PACK -> "pack"
}

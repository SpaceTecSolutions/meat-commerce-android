package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerCommerceTokens as T
import com.spacetecsolutions.meatapp.core.model.Product

@Composable
fun CustomerProductDetails(
    product: Product?, loading: Boolean, offline: Boolean, error: String?,
    quantity: Int, adding: Boolean, onBack: () -> Unit, onRetry: () -> Unit,
    onQuantity: (Int) -> Unit, onAdd: () -> Unit,
    inCart: Boolean = false, onCart: () -> Unit = {},
) {
    when {
        loading -> ProductDetailState(onBack, ContentState.Loading)
        product == null -> ProductDetailState(onBack,
            if (offline) ContentState.Offline(description = error)
            else ContentState.Error(description = error ?: "This product is no longer available"), onRetry)
        else -> ProductDetailContent(product, quantity, adding, onBack, onQuantity, onAdd, inCart, onCart)
    }
}

@Composable
private fun ProductDetailState(onBack: () -> Unit, state: ContentState, onRetry: () -> Unit = {}) {
    Column(Modifier.fillMaxSize()) {
        AppBackTopBar("Product details", onBack)
        ContentStateView(state, onAction = onRetry)
    }
}

@Composable
private fun ProductDetailContent(
    product: Product, quantity: Int, adding: Boolean, onBack: () -> Unit,
    onQuantity: (Int) -> Unit, onAdd: () -> Unit, inCart: Boolean, onCart: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(color = T.surface), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
                item(key = "product-details") {
                    Column {
                        ProductImageCarousel(product.imageUrls, product.name, onBack)
                        Surface(Modifier.fillMaxWidth().offset(y = (-20).dp),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            color = T.surface) {
                            ProductDetailInformation(product, quantity, onQuantity)
                        }
                    }
                }
            }
        Box(Modifier.align(Alignment.BottomCenter)) {
            AddToCartBar(product, quantity, adding, if (inCart) onCart else onAdd, inCart)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetailInformation(product: Product, quantity: Int,
    onQuantity: (Int) -> Unit) {
    val available = CustomerProductPolicy.canAddToCart(product)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Surface(Modifier.align(Alignment.CenterHorizontally).padding(top = 15.dp, bottom = 14.dp)
            .size(width = 39.dp, height = 4.dp), shape = RoundedCornerShape(50), color = T.border) {}
        ProductAvailabilityRow(product, available)
        Spacer(Modifier.height(10.dp))
        Text(product.name, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold, color = T.ink)
        Spacer(Modifier.height(4.dp))
        ProductRatingAndUnit(product)
        Spacer(Modifier.height(17.dp))
        ProductPriceBlock(product)
        Spacer(Modifier.height(17.dp))
        HorizontalDivider(color = T.border)
        if (product.attributes.isNotEmpty()) {
            FlowRow(Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                product.attributes.entries.forEach { ProductAttributeChip(it.productAttributeLabel()) }
            }
            HorizontalDivider(color = T.border)
        }
        if (product.description.isNotBlank()) {
            Spacer(Modifier.height(17.dp))
            Text("ABOUT", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = T.ink)
            Spacer(Modifier.height(7.dp))
            Text(product.description, style = MaterialTheme.typography.bodyMedium, color = T.muted)
            Spacer(Modifier.height(18.dp))
        } else Spacer(Modifier.height(18.dp))
        ProductQuantitySelector(product, quantity, available, onQuantity)
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun ProductAvailabilityRow(product: Product, available: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(50),
            color = if (available) T.green.copy(alpha = .08f) else T.rose) {
            Text(if (available) "● In stock · ${product.stockQuantity.cleanProductQuantity()} available"
                else "Out of Stock", Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                color = if (available) T.green else T.red)
        }
        Spacer(Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(6.dp), color = T.soft,
            border = BorderStroke(1.dp, T.border)) {
            Text("PER ${product.unit.shortLabel().uppercase()}",
                Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = T.muted)
        }
    }
}

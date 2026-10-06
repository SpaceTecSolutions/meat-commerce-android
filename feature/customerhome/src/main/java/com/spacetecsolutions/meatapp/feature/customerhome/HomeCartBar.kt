package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImage
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImageCrop
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerHomeTokens
import com.spacetecsolutions.meatapp.core.model.CustomerCart
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun HomeCartBar(cart: CustomerCart, onViewCart: () -> Unit) {
    if (cart.lines.isEmpty()) return
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp), color = Color(0xFF1D2025), shadowElevation = 10.dp,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 70.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(54.dp).height(42.dp)) {
                cart.lines.take(2).forEachIndexed { index, line ->
                    AppImage(line.imageUrl, null,
                        Modifier.offset(x = (index * 15).dp).size(40.dp).clip(CircleShape)
                            .background(CustomerHomeTokens.surface), AppImageCrop.CIRCLE)
                }
            }
            Column(Modifier.weight(1f).padding(start = 5.dp)) {
                Text("${cart.quantity} ${if (cart.quantity == 1) "ITEM" else "ITEMS"} · ${cart.totalMinor.cartMoney()}",
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Ready in your cart", style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = .7f), maxLines = 1)
            }
            Button(onClick = onViewCart, shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CustomerHomeTokens.red),
                contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("View Cart", maxLines = 1)
                Spacer(Modifier.width(3.dp))
                Icon(AppIcons.ArrowRight, null, Modifier.size(15.dp))
            }
        }
    }
}

private fun Long.cartMoney(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    .apply { maximumFractionDigits = if (this@cartMoney % 100L == 0L) 0 else 2 }
    .format(this / 100.0)

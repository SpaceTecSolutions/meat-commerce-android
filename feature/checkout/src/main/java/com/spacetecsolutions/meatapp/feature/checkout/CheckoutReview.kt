package com.spacetecsolutions.meatapp.feature.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImage
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.AppLottieAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.PrimaryButton
import com.spacetecsolutions.meatapp.core.designsystem.component.SecondaryButton
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerCommerceTokens as T
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.model.CartLine
import com.spacetecsolutions.meatapp.core.model.PlacedOrder
import com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod

@Composable
internal fun CheckoutReview(
    state: CheckoutUiState,
    padding: PaddingValues,
    selectPaymentMethod: (CheckoutPaymentMethod) -> Unit,
    edit: () -> Unit,
) {
    val quote = state.quote ?: return
    val address = quote.addresses.firstOrNull { it.id == state.addressId }
    val date = quote.deliveryDates.firstOrNull { it.id == state.deliveryDateId }
    val slot = quote.deliverySlots.firstOrNull { it.id == state.deliverySlotId }
    Box(Modifier.fillMaxSize().background(T.canvas).padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxHeight().fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.compact),
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = T.surface,
                border = BorderStroke(1.dp, T.border)) { CheckoutStepIndicator(3) }
            Surface(shape = RoundedCornerShape(14.dp), color = T.surface,
                border = BorderStroke(1.dp, T.border)) {
                Column(Modifier.fillMaxWidth().padding(AppSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Location, null, Modifier.size(20.dp), tint = T.red)
                        Text("DELIVERING TO ${address?.type?.display()?.uppercase() ?: "ADDRESS"}",
                            Modifier.weight(1f).padding(start = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = T.ink)
                        TextButton(onClick = edit, contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text("Edit", color = T.red)
                        }
                    }
                    address?.let { Text(it.formatted(), style = MaterialTheme.typography.bodySmall,
                        color = T.ink) }
                    HorizontalDivider(color = T.border)
                    Text(listOfNotNull(date?.label, slot?.label).joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = T.ink)
                }
            }
            ReviewSurface {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Order Summary", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${quote.cart.quantity} items", style = MaterialTheme.typography.labelMedium,
                        color = T.muted)
                }
                quote.cart.lines.forEachIndexed { index, line ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    CheckoutOrderItemRow(line, quote.currencyCode)
                }
            }
            state.instructions.takeIf(String::isNotBlank)?.let { note -> ReviewSurface {
                Text("Delivery Instructions", fontWeight = FontWeight.Bold)
                Text(note, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            ReviewSurface {
                Text("Bill Details", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                BillRow("Item Total", quote.cart.subtotalMinor.money(quote.currencyCode))
                if (quote.cart.discountMinor > 0)
                    BillRow("Discount", "−${quote.cart.discountMinor.money(quote.currencyCode)}")
                if (quote.cart.deliveryFeeMinor == 0L)
                    FreeDeliveryRow()
                else BillRow("Delivery Charge", quote.cart.deliveryFeeMinor.money(quote.currencyCode))
                HorizontalDivider(color = T.border)
                Surface(shape = RoundedCornerShape(10.dp), color = T.green.copy(alpha = .08f)) {
                    Column(Modifier.fillMaxWidth().padding(AppSpacing.small)) {
                        BillRow(if (state.paymentMethod == CheckoutPaymentMethod.COD) "To Pay (COD)"
                            else "Pay Online", quote.cart.totalMinor.money(quote.currencyCode), true)
                    }
                }
            }
            ReviewSurface {
                Text("Payment Method", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                quote.paymentMethods.forEach { method ->
                    Surface(onClick = { selectPaymentMethod(method) },
                        shape = RoundedCornerShape(12.dp),
                        color = T.surface,
                        border = BorderStroke(if (state.paymentMethod == method) 2.dp else 1.dp,
                            if (state.paymentMethod == method) T.red else T.border)) {
                        Row(Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(when (method) {
                                CheckoutPaymentMethod.COD -> AppIcons.Cash
                                CheckoutPaymentMethod.UPI -> AppIcons.Upi
                                CheckoutPaymentMethod.RAZORPAY -> AppIcons.Card
                            }, null, tint = T.red)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(method.label(), style = MaterialTheme.typography.bodyMedium)
                                if (method == CheckoutPaymentMethod.RAZORPAY) Surface(
                                    shape = RoundedCornerShape(6.dp), color = T.rose) {
                                    Text("RECOMMENDED", Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold, color = T.red)
                                }
                            }
                            RadioButton(selected = state.paymentMethod == method,
                                onClick = { selectPaymentMethod(method) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(AppSpacing.small))
        }
    }
}

@Composable
private fun ReviewSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = T.surface,
        border = BorderStroke(1.dp, T.border)) {
        Column(Modifier.fillMaxWidth().padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small), content = content)
    }
}

@Composable
private fun CheckoutOrderItemRow(line: CartLine, currencyCode: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AppImage(
            model = line.imageUrl, contentDescription = line.name,
            modifier = Modifier.size(56.dp), cornerRadius = 8.dp,
        )
        Column(Modifier.weight(1f).padding(horizontal = AppSpacing.small)) {
            Text(line.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${line.quantity} ${line.unit.display()}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(line.lineTotalMinor.money(currencyCode), style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BillRow(label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f),
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (emphasized) T.green else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text(value,
            style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            color = if (emphasized) T.green else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium)
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

@Composable
internal fun OrderPlacedContent(
    order: PlacedOrder,
    padding: PaddingValues,
    viewOrders: () -> Unit,
    continueShopping: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("Order Tracking", Modifier.align(Alignment.TopStart)
            .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.medium),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(
            Modifier.fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth)
                .verticalScroll(rememberScrollState()).padding(AppSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(AppSpacing.extraLarge))
            AppLottieAnimation(
                animation = AppAnimation.OrderSuccess,
                modifier = Modifier.size(112.dp),
            )
            Spacer(Modifier.height(AppSpacing.extraLarge))
            Text("Order Placed Successfully!", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(AppSpacing.small))
            Text(
                "Your order has been received and will\nbe confirmed by the shop.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AppSpacing.large))
            OutlinedCard(
                Modifier.fillMaxWidth(), shape = AppShapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.fillMaxWidth().padding(AppSpacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    SuccessValue("Order ID", order.orderNumber)
                    HorizontalDivider(Modifier.padding(vertical = AppSpacing.compact),
                        color = MaterialTheme.colorScheme.outlineVariant)
                    SuccessValue("Estimated Delivery", order.estimatedDelivery)
                    HorizontalDivider(Modifier.padding(vertical = AppSpacing.compact),
                        color = MaterialTheme.colorScheme.outlineVariant)
                    SuccessValue("${order.items.size} items · Total",
                        order.totalMinor.money(order.currencyCode))
                }
            }
            Spacer(Modifier.height(AppSpacing.large))
            PrimaryButton("View My Orders", viewOrders)
            Spacer(Modifier.height(AppSpacing.small))
            SecondaryButton("Continue Shopping", continueShopping)
            Spacer(Modifier.height(AppSpacing.extraLarge))
        }
    }
}

@Composable
private fun SuccessValue(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(2.dp))
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center)
}

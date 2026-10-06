package com.spacetecsolutions.meatapp.feature.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimatedListItem
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerCommerceTokens as T
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.model.CustomerAddress

@Composable
fun CartRoute(
    onBack: () -> Unit,
    onStartShopping: () -> Unit,
    onChangeAddress: () -> Unit,
    onProduct: (String) -> Unit,
    onCheckout: () -> Unit,
    onMessage: (CartMessage) -> Unit,
    selectedAddress: CustomerAddress? = null,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    state.message?.let { message ->
        LaunchedEffect(message) { onMessage(message); viewModel.consumeMessage() }
    }
    CartScreen(
        state, onBack, viewModel::toggleEditing, viewModel::refresh, onStartShopping,
        onProduct, viewModel::setQuantity, viewModel::remove, onCheckout,
    )
}

@Composable
private fun CartScreen(
    state: CartUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRefresh: () -> Unit,
    onStartShopping: () -> Unit,
    onProduct: (String) -> Unit,
    onQuantity: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit,
) {
    var pendingRemoval by remember { mutableStateOf<String?>(null) }
    val pendingLine = state.cart.lines.firstOrNull { it.productId == pendingRemoval }
    Column(Modifier.fillMaxSize().background(T.canvas)) {
        CartTopBar(state.cart.lines.isNotEmpty(), state.editing, onBack, onEdit)
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null -> ContentStateView(
                ContentState.Error(description = state.error), onAction = onRefresh,
            )
            state.cart.lines.isEmpty() -> EmptyCartContent(onStartShopping)
            else -> Box(
                Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = AppDimensions.formMaxWidth),
            ) {
                Column(Modifier.fillMaxSize()) {
                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = AppSpacing.medium, end = AppSpacing.medium,
                            bottom = AppSpacing.large,
                        ),
                    ) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.medium),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("Total Items: ${state.cart.distinctItemCount}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text("Swipe to remove", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            CartAdjustmentNotice(state.cart.adjustments)
                        }
                        itemsIndexed(state.cart.lines, key = { _, line -> line.productId }) { index, line ->
                            AppAnimatedListItem(index = index, modifier = Modifier.animateItem().padding(
                                vertical = AppSpacing.extraSmall)) {
                                com.spacetecsolutions.meatapp.core.designsystem.component.SwipeDeleteContainer(
                                    enabled = line.productId !in state.busyProductIds,
                                    onDelete = { pendingRemoval = line.productId }) {
                                CartItemRow(
                                    line = line,
                                    busy = line.productId in state.busyProductIds,
                                    editing = false,
                                    onProduct = { onProduct(line.productId) },
                                    onQuantity = { quantity ->
                                        if (quantity < 1) pendingRemoval = line.productId
                                        else onQuantity(line.productId, quantity)
                                    },
                                    onRemove = { pendingRemoval = line.productId },
                                )
                                }
                            }
                        }
                        item {
                            AddMoreItems(onStartShopping)
                            Surface(shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp) {
                                Box(Modifier.padding(AppSpacing.medium)) {
                                    CartBillSummary(state.cart)
                                }
                            }
                        }
                        state.cart.deliveryEstimate?.takeIf(String::isNotBlank)?.let { estimate ->
                            item { Surface(Modifier.fillMaxWidth().padding(top = AppSpacing.small),
                                shape = RoundedCornerShape(14.dp), color = T.soft) {
                                Row(Modifier.padding(AppSpacing.small),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(AppIcons.Delivery, null, tint = T.red)
                                    Column(Modifier.padding(start = AppSpacing.small)) {
                                        Text("Delivery Estimate", fontWeight = FontWeight.SemiBold)
                                        Text(estimate, style = MaterialTheme.typography.bodySmall,
                                            color = T.muted)
                                    }
                                }
                            } }
                        }
                        item { SwipeToCheckout(state.cart, onCheckout) }
                    }
                }
            }
        }
    }
    pendingLine?.let { line ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove item?") },
            text = { Text("Remove ${line.name} from your cart?") },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { pendingRemoval = null; onRemove(line.productId) }) { Text("Remove") }
            },
        )
    }
}

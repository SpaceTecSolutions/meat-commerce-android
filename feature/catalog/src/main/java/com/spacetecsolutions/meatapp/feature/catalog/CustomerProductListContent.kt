package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimatedListItem
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerProductListContent(
    state: CustomerCategoriesUiState,
    onProduct: (String) -> Unit,
    onQuantity: (Product, Int) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
    ) {
        when {
            state.loading && state.products.isEmpty() -> ProductListSkeleton()
            state.error != null && state.products.isEmpty() -> ContentStateView(
                state = if (state.offline) ContentState.Offline(
                    title = "You're offline", description = state.error,
                ) else ContentState.Error(
                    title = "Unable to load products", description = state.error,
                ),
                onAction = onRefresh,
            )
            else -> ProductList(
                state = state,
                onProduct = onProduct,
                onQuantity = onQuantity,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun ProductList(
    state: CustomerCategoriesUiState,
    onProduct: (String) -> Unit,
    onQuantity: (Product, Int) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = AppDimensions.contentMaxWidth),
        contentPadding = PaddingValues(
            start = AppSpacing.medium,
            top = AppSpacing.small,
            end = AppSpacing.medium,
            bottom = AppSpacing.medium,
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
    ) {
        if (state.visibleProducts.isEmpty()) item {
            ContentStateView(
                state = ContentState.Empty(
                    title = if (state.search.isBlank()) {
                        "No products available in this category"
                    } else "No matching products found",
                    description = if (state.search.isBlank()) {
                        "Products will appear here when they become available."
                    } else "Try a different search.",
                ),
                modifier = Modifier.fillParentMaxHeight(.82f),
            )
        }
        itemsIndexed(state.visibleProducts, key = { _, product -> product.id }) { index, product ->
            AppAnimatedListItem(index = index, modifier = Modifier.animateItem()) {
                CustomerProductListItem(
                    product = product,
                    adding = product.id in state.addingProductIds,
                    quantity = state.cartQuantities[product.id] ?: 0,
                    onClick = { onProduct(product.id) },
                    onIncrease = { onQuantity(product, 1) },
                    onDecrease = { onQuantity(product, -1) },
                )
            }
        }
        if (state.nextCursor != null) item {
            OutlinedButton(
                onClick = onLoadMore,
                enabled = !state.loadingMore,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loadingMore) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Load more products")
            }
        }
    }
}

@Composable
private fun ProductListSkeleton() {
    Column(
        Modifier.fillMaxSize().widthIn(max = AppDimensions.contentMaxWidth)
            .wrapContentWidth(Alignment.CenterHorizontally).padding(AppSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
    ) {
        repeat(6) {
            Row(
                Modifier.fillMaxWidth().height(92.dp).clearAndSetSemantics { }
                    .background(MaterialTheme.colorScheme.surface, AppShapes.medium)
                    .padding(AppSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(72.dp, 72.dp)
                Column(Modifier.weight(1f).padding(horizontal = AppSpacing.compact)) {
                    SkeletonBox(128.dp, 12.dp)
                    Spacer(Modifier.height(AppSpacing.small))
                    SkeletonBox(72.dp, 10.dp)
                    Spacer(Modifier.height(AppSpacing.small))
                    SkeletonBox(88.dp, 12.dp)
                }
                SkeletonBox(40.dp, 40.dp)
            }
        }
    }
}

@Composable
private fun SkeletonBox(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(width, height).background(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            AppShapes.small,
        ),
    )
}

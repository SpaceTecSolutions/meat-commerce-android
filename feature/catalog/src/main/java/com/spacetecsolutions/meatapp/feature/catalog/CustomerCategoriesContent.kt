package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.ProductCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerCategoriesContent(
    state: CustomerCategoriesUiState,
    onCategory: (String, String) -> Unit,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.loading && state.categories.isEmpty() -> CategorySkeletonGrid()
            state.error != null && state.categories.isEmpty() -> ContentStateView(
                state = if (state.offline) ContentState.Offline(
                    title = "You're offline",
                    description = state.error,
                ) else ContentState.Error(
                    title = "Unable to load categories",
                    description = state.error,
                ),
                onAction = onRefresh,
            )
            state.categories.isEmpty() -> ContentStateView(ContentState.Empty(
                title = "No categories available",
                description = "Categories will appear here when products become available.",
            ))
            state.visibleCategories.isEmpty() -> ContentStateView(ContentState.Empty(
                title = "No categories found",
                description = "Try a different search.",
            ))
            else -> CategoryGrid(state.visibleCategories, onCategory)
        }
    }
}

@Composable
private fun CategoryGrid(categories: List<ProductCategory>, onCategory: (String, String) -> Unit) {
    CenteredCategoryGrid {
        items(categories, key = ProductCategory::id) { category ->
            CustomerCategoryCard(
                category = category,
                onClick = { onCategory(category.id, category.name) },
                modifier = Modifier.fillMaxWidth().animateItem(),
            )
        }
    }
}

@Composable
private fun CategorySkeletonGrid() {
    CenteredCategoryGrid {
        itemsIndexed(List(6) { it }) { index, _ ->
            Card(
                modifier = Modifier.fillMaxWidth().height(164.dp)
                    .clearAndSetSemantics { },
                shape = AppShapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(Modifier.fillMaxSize().padding(AppSpacing.small)) {
                    Box(
                        Modifier.fillMaxWidth().weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, AppShapes.small),
                    )
                    Spacer(Modifier.height(AppSpacing.small))
                    SkeletonLine(if (index % 2 == 0) 0.58f else 0.72f)
                    Spacer(Modifier.height(AppSpacing.extraSmall))
                    SkeletonLine(0.38f)
                }
            }
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float) {
    Box(
        Modifier.fillMaxWidth(widthFraction).height(10.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(5.dp)),
    )
}

@Composable
private fun CenteredCategoryGrid(content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val horizontalPadding = if (maxWidth < 360.dp) AppSpacing.small else AppSpacing.medium
        LazyVerticalGrid(
            columns = GridCells.Adaptive(AppDimensions.categoryCardMinWidth),
            modifier = Modifier.fillMaxSize().widthIn(max = AppDimensions.categoryGridMaxWidth),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                top = AppSpacing.small,
                end = horizontalPadding,
                bottom = AppSpacing.medium,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
            content = content,
        )
    }
}

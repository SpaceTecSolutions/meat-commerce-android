package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.ProductSubcategory

@Composable
internal fun CustomerSubcategoryBrowser(category: String, values: List<ProductSubcategory>,
    select: (ProductSubcategory) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val padding = if (maxWidth < 360.dp) AppSpacing.small else AppSpacing.medium
        LazyVerticalGrid(
            columns = GridCells.Adaptive(AppDimensions.categoryCardMinWidth),
            modifier = Modifier.fillMaxSize().widthIn(max = AppDimensions.categoryGridMaxWidth),
            contentPadding = PaddingValues(padding),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(bottom = AppSpacing.extraSmall)) {
                    Text("Choose from $category", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("Select a cut to view available products",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(values, key = ProductSubcategory::id) { item ->
                Card(
                    onClick = { select(item) },
                    modifier = Modifier.fillMaxWidth().shadow(8.dp, AppShapes.medium).animateItem(),
                    shape = AppShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        CategoryImage(item.imageUrl, item.name,
                            Modifier.fillMaxWidth().aspectRatio(1.42f)
                                .padding(start = AppSpacing.small, top = AppSpacing.small, end = AppSpacing.small))
                        Column(Modifier.fillMaxWidth().padding(start = AppSpacing.compact,
                            top = AppSpacing.small, end = AppSpacing.compact, bottom = AppSpacing.compact)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            Text(item.description.ifBlank { "View available products" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

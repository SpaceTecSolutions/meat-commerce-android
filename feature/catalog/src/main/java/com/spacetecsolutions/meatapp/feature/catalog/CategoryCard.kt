package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImage
import com.spacetecsolutions.meatapp.core.designsystem.component.appSwitchColors
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.ProductCategory

@Composable
fun CategoryImage(model: Any?, description: String?, modifier: Modifier = Modifier) {
    AppImage(model, description, modifier, cornerRadius = 10.dp)
}

@Composable
fun CustomerCategoryCard(
    category: ProductCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = category.activeProductCount.coerceAtLeast(0)
    val categoryDescription = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_category_name_categoryitemcount_count_54d7751, category.name, categoryItemCount(count))
    Card(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = categoryDescription
        }
            .shadow(elevation = 8.dp, AppShapes.medium),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth()) {
            CategoryImage(
                model = category.imageUrl,
                description = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.42f)
                    .padding(start = AppSpacing.small, top = AppSpacing.small, end = AppSpacing.small),
            )
            Column(
                Modifier.fillMaxWidth().padding(
                    start = AppSpacing.compact,
                    top = AppSpacing.small,
                    end = AppSpacing.compact,
                    bottom = AppSpacing.compact,
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.hairline),
            ) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                )
                Text(
                    categoryItemCount(count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun AdminCategoryRow(
    category: ProductCategory,
    busy: Boolean,
    onEdit: () -> Unit,
    onStatus: () -> Unit,
    onDelete: () -> Unit,
    onSubcategories: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth(), shape = AppShapes.medium) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppSpacing.small, vertical = AppSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).clickable(enabled = !busy, onClick = onSubcategories ?: onEdit),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryImage(category.imageUrl, category.name, Modifier.size(54.dp))
                Column(Modifier.weight(1f).padding(horizontal = AppSpacing.small)) {
                    Text(category.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${category.productCount} ${if (category.productCount == 1) "Product" else "Products"}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!category.active) Text(
                        "Inactive", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Switch(
                checked = category.active,
                onCheckedChange = { onStatus() },
                enabled = !busy,
                colors = appSwitchColors(),
            )
            Box {
                IconButton({ menu = true }, enabled = !busy) { Icon(AppIcons.MoreVertical, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_category_actions_e7c0183)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_edit_5301648)) }, { menu = false; onEdit() },
                        leadingIcon = { Icon(AppIcons.Edit, null) })
                    DropdownMenuItem({ Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_delete_f6fdbe4)) }, { menu = false; onDelete() },
                        leadingIcon = { Icon(AppIcons.Delete, null) })
                }
            }
        }
    }
}

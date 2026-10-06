package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.Product
import com.spacetecsolutions.meatapp.core.model.StaffPermission

@Composable
fun ProductManagementRoute(
    onCategories: () -> Unit,
    onMessage: (ProductMessage) -> Unit,
    operatorPermissions: Set<StaffPermission>? = null,
    viewModel: ProductManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCacheEviction by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(state.message) { state.message?.let {
        if (it.success && it.text == "Product deleted") {
            evictCachedImages(context, pendingCacheEviction); pendingCacheEviction = emptyList()
        }
        onMessage(it); viewModel.consumeMessage()
    } }
    val form = state.form
    val canManageProducts = operatorPermissions == null || StaffPermission.MANAGE_PRODUCTS in operatorPermissions
    val canManageStock = operatorPermissions == null || StaffPermission.MANAGE_STOCK in operatorPermissions
    if (form == null) ProductManagementScreen(
        state, viewModel::updateSearch, viewModel::selectCategoryFilter, viewModel::openCreate,
        viewModel::openEdit, viewModel::toggleStatus, viewModel::loadMore, viewModel::refresh,
        viewModel::requestDelete, canManageProducts, canManageStock, operatorPermissions != null,
    ) else {
        HideAppBottomBar()
        ProductFormScreen(form, state.categories, state.subcategories, state.submitting, onCategories, ProductFormActions(
            viewModel::updateName, viewModel::updateDescription, viewModel::updateCategory,
            viewModel::updateSubcategory,
            viewModel::updateUnit, viewModel::updatePrice, viewModel::updateOfferPrice,
            viewModel::updateStock, viewModel::updateLowStock, viewModel::selectImages,
            viewModel::removeImage,
            viewModel::updateActive, viewModel::addAttribute, viewModel::removeAttribute,
            { index, value -> viewModel.updateAttribute(index, key = value) },
            { index, value -> viewModel.updateAttribute(index, value = value) },
            viewModel::closeForm, viewModel::save,
        ))
    }
    state.deleteConfirmation?.let { product ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete Product?") },
            text = { Text("${product.name} will be removed from the catalog. Historical orders remain unchanged.") },
            confirmButton = { Button({ pendingCacheEviction = product.imageUrls; viewModel.confirmDelete() },
                enabled = !state.submitting) { Text("Delete") } },
            dismissButton = { TextButton(viewModel::dismissDelete, enabled = !state.submitting) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProductManagementScreen(
    state: ProductManagementUiState,
    onSearch: (String) -> Unit,
    onCategory: (String?) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Product) -> Unit,
    onToggle: (Product) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onDelete: (Product) -> Unit,
    canManageProducts: Boolean,
    canManageStock: Boolean,
    staffPresentation: Boolean,
) {
    var searchOpen by rememberSaveable { mutableStateOf(staffPresentation) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AppTopBar(if (staffPresentation) "Product Catalog" else "Products",
                actionIcon = AppIcons.Search, actionDescription = "Search products") {
                searchOpen = !searchOpen
                if (!searchOpen) onSearch("")
            }
        },
        bottomBar = if (canManageProducts) {{
//            Surface(color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.large, vertical = AppSpacing.small),
                    shape = AppShapes.large,
                ) { Icon(AppIcons.Add, null); Spacer(Modifier.width(AppSpacing.small)); Text("Add Product") }
//            }
        }} else ({}),
    ) { padding ->
        when {
            state.loading && state.products.isEmpty() -> ContentStateView(ContentState.Loading, contentPadding = padding)
            state.error != null && state.products.isEmpty() -> ContentStateView(
                ContentState.Error(description = state.error), contentPadding = padding, onAction = onRetry,
            )
            else -> ProductList(
                state, padding, searchOpen, onSearch, onCategory, onEdit, onToggle,
                onLoadMore, onAdd, onDelete, canManageProducts, canManageStock,
            )
        }
    }
}

@Composable
private fun ProductList(
    state: ProductManagementUiState,
    padding: PaddingValues,
    searchOpen: Boolean,
    onSearch: (String) -> Unit,
    onCategory: (String?) -> Unit,
    onEdit: (Product) -> Unit,
    onToggle: (Product) -> Unit,
    onLoadMore: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (Product) -> Unit,
    canManageProducts: Boolean,
    canManageStock: Boolean,
) {
    Column(Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = AppDimensions.contentMaxWidth)) {
        if (searchOpen) ProductSearchField(state.search, onSearch,
            modifier = Modifier.padding(horizontal = AppSpacing.medium))
        ProductCategoryFilter(
            state.categories, state.categoryFilter, onCategory,
            Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
        )
        Box(Modifier.padding(horizontal = AppSpacing.medium)) {
            Text(
                "${state.countedProducts} of ${state.productLimit} products",
                style = MaterialTheme.typography.labelMedium,
                color = if (state.limitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AppSpacing.extraSmall),
            )
        }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
        ) {
        if (state.products.isEmpty()) item {
            val searching = state.search.isNotBlank()
            ContentStateView(
                ContentState.Empty(
                    title = when {
                        searching -> "No matching products"
                        state.categoryFilter != null -> "No products in this category"
                        else -> "No products added"
                    },
                    description = if (searching) "Try another product name."
                    else "Add your first product to start selling.",
                    actionLabel = if (canManageProducts && !searching && state.categoryFilter == null) "Add Product" else null,
                ),
                onAction = onAdd,
            )
        } else items(state.products, key = Product::id) { product ->
            SwipeDeleteContainer(canManageProducts && product.id !in state.busyProductIds,
                product.name, { onDelete(product) }) {
                AdminProductRow(product, product.id in state.busyProductIds, { onEdit(product) }, { onToggle(product) },
                    canEdit = canManageProducts, canToggle = canManageStock)
            }
        }
        if (state.nextCursor != null) item {
            TextButton(onClick = onLoadMore, enabled = !state.loadingMore, modifier = Modifier.fillMaxWidth()) {
                if (state.loadingMore) CircularProgressIndicator(Modifier.size(AppSpacing.large)) else Text("Load more")
            }
        }
        }
    }
}

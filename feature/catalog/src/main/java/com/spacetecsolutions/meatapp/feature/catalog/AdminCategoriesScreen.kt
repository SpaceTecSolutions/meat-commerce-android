package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.ProductCategory

@Composable
fun AdminCategoriesRoute(
    onBack: () -> Unit,
    onMessage: (CategoryMessage) -> Unit,
    subcategoriesAllowed: Boolean = false,
    viewModel: AdminCategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCacheEviction by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(state.message) { state.message?.let {
        if (it.success && it.text == "Category deleted") {
            evictCachedImages(context, pendingCacheEviction); pendingCacheEviction = emptyList()
        }
        onMessage(it); viewModel.consumeMessage()
    } }
    var subcategoryParent by remember { mutableStateOf<ProductCategory?>(null) }
    subcategoryParent?.let { parent ->
        AdminSubcategoriesRoute(parent, { subcategoryParent = null }, onMessage)
        return
    }
    val form = state.form
    if (form == null) AdminCategoriesScreen(
        state, onBack, viewModel::openCreate, viewModel::updateSearch, viewModel::openEdit,
        viewModel::requestStatusChange, viewModel::requestDelete, viewModel::refresh,
        if (subcategoriesAllowed) ({ subcategoryParent = it }) else null,
    ) else {
        HideAppBottomBar()
        CategoryFormScreen(form, state.submitting, CategoryFormActions(
            viewModel::updateName, viewModel::updateDescription, viewModel::updateSortOrder,
            viewModel::updateActive, viewModel::selectImage, viewModel::closeForm, viewModel::save,
        ))
    }
    state.confirmation?.let {
        CategoryStatusDialog(it, state.submitting, viewModel::dismissConfirmation, viewModel::confirmStatusChange)
    }
    state.deleteConfirmation?.let {
        CategoryDeleteDialog(it, state.submitting, viewModel::dismissDelete, {
            pendingCacheEviction = listOfNotNull(it.imageUrl); viewModel.confirmDelete()
        })
    }
}

@Composable
private fun AdminCategoriesScreen(
    state: CategoryAdminUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onSearch: (String) -> Unit,
    onEdit: (ProductCategory) -> Unit,
    onStatus: (ProductCategory) -> Unit,
    onDelete: (ProductCategory) -> Unit,
    onRetry: () -> Unit,
    onSubcategories: ((ProductCategory) -> Unit)?,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CategoriesTopBar(onBack, onAdd) },
    ) { padding ->
        when {
            state.loading -> ContentStateView(ContentState.Loading, contentPadding = padding)
            state.error != null && state.categories.isEmpty() -> ContentStateView(
                ContentState.Error(description = state.error), contentPadding = padding, onAction = onRetry,
            )
            state.categories.isEmpty() -> ContentStateView(
                ContentState.Empty(
                    title = "No categories added",
                    description = "Add the first category to organize products.",
                    actionLabel = "Add Category",
                ),
                contentPadding = padding, onAction = onAdd,
            )
            else -> Column(
                Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = AppDimensions.contentMaxWidth),
            ) {
                ProductSearchField(
                    state.search, onSearch, "Search categories",
                    Modifier.padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                )
                LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
            ) {
                if (state.error != null) item { Text(state.error, color = MaterialTheme.colorScheme.error) }
                val visible = state.categories.filter { it.matchesSearch(state.search) }
                if (visible.isEmpty()) item {
                    ContentStateView(ContentState.Empty(
                        title = "No matching categories",
                        description = "Try another category name.",
                    ))
                }
                items(visible, key = ProductCategory::id) { category ->
                    SwipeDeleteContainer(!state.submitting, category.name, { onDelete(category) }) {
                        AdminCategoryRow(
                            category, state.submitting, { onEdit(category) },
                            { onStatus(category) }, { onDelete(category) },
                            onSubcategories?.let { action -> { action(category) } },
                        )
                    }
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesTopBar(onBack: () -> Unit, onAdd: () -> Unit) {
    TopAppBar(
        title = { Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        navigationIcon = { IconButton(onBack) { Icon(AppIcons.Back, "Back") } },
        actions = { TextButton(onAdd) { Text("+ Add Category") } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        expandedHeight = 56.dp,
    )
}

package com.spacetecsolutions.meatapp.feature.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.HideAppBottomBar

@Composable
fun CustomerCategoriesRoute(
    onBack: () -> Unit,
    onCart: () -> Unit,
    onMessage: (CategoryMessage) -> Unit,
    initialProductId: String? = null,
    onInitialProductConsumed: () -> Unit = {},
    initialCategoryId: String? = null,
    initialCategoryName: String? = null,
    onInitialCategoryConsumed: () -> Unit = {},
    initialSearchQuery: String? = null,
    onInitialSearchConsumed: () -> Unit = {},
    viewModel: CustomerCategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var subcategorySearch by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(initialProductId) {
        initialProductId?.let {
            viewModel.openProduct(it)
            onInitialProductConsumed()
        }
    }
    LaunchedEffect(initialCategoryId, initialCategoryName) {
        initialCategoryId?.let {
            viewModel.selectCategory(it, initialCategoryName.orEmpty())
            onInitialCategoryConsumed()
        }
    }
    LaunchedEffect(initialSearchQuery) {
        initialSearchQuery?.takeIf(String::isNotBlank)?.let {
            searchVisible = true
            viewModel.searchAllProducts(it)
            onInitialSearchConsumed()
        }
    }
    state.message?.let { message ->
        LaunchedEffect(message) {
            onMessage(message)
            if (message.openCart) onCart()
            viewModel.consumeMessage()
        }
    }

    val detailOpen = state.detailProductId != null
    if (state.selectedCategory != null || detailOpen) HideAppBottomBar()
    if (detailOpen) {
        BackHandler(onBack = viewModel::closeProduct)
        CustomerProductDetails(
            product = state.selectedProduct,
            loading = state.detailLoading,
            offline = state.offline,
            error = state.error,
            quantity = state.selectedProduct?.let { state.cartQuantities[it.id] }
                ?.takeIf { it > 0 } ?: state.quantity,
            adding = state.selectedProduct?.id in state.addingProductIds,
            inCart = (state.cartQuantities[state.selectedProduct?.id] ?: 0) > 0,
            onCart = onCart,
            onBack = viewModel::closeProduct,
            onRetry = viewModel::retryProductDetails,
            onQuantity = { delta -> state.selectedProduct?.let { product ->
                if ((state.cartQuantities[product.id] ?: 0) > 0)
                    viewModel.changeProductCartQuantity(product, delta)
                else viewModel.changeQuantity(delta)
            } },
            onAdd = { state.selectedProduct?.let { viewModel.addToCart(it) } },
        )
        return
    }

    BackHandler(state.selectedCategory != null && !searchVisible) {
        subcategorySearch = ""
        if (state.selectedSubcategory != null) viewModel.showSubcategories() else viewModel.showCategories()
    }
    BackHandler(searchVisible) {
        searchVisible = false
        when {
            state.selectedCategory == null -> viewModel.updateCategorySearch("")
            state.subcategories.isNotEmpty() && state.selectedSubcategory == null -> subcategorySearch = ""
            else -> viewModel.updateSearch("")
        }
    }
    Column(Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.surface)) {
        val selectedCategory = state.selectedCategory
        if (selectedCategory == null) {
            CustomerSearchTopBar(
                title = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_categories_6ccb600),
                placeholder = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_search_categories_1bf5c01),
                searchVisible = searchVisible,
                searchQuery = state.categorySearch,
                onBack = onBack,
                onSearch = { searchVisible = true },
                onSearchChange = viewModel::updateCategorySearch,
                onCloseSearch = {
                    searchVisible = false
                    viewModel.updateCategorySearch("")
                },
            )
            CustomerCategoriesContent(
                state = state,
                onCategory = { id, name ->
                    searchVisible = false
                    subcategorySearch = ""
                    viewModel.selectCategory(id, name)
                },
                onRefresh = viewModel::refresh,
            )
        } else if (state.loadingSubcategories) {
            CustomerSearchTopBar(
                title = selectedCategory.name,
                placeholder = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_search_products_b73f756),
                searchVisible = false,
                searchQuery = "",
                onBack = {
                    searchVisible = false
                    subcategorySearch = ""
                    viewModel.showCategories()
                },
                onSearch = {}, onSearchChange = {}, onCloseSearch = {},
                cartCount = state.cartQuantity, onCart = onCart,
            )
            com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView(
                com.spacetecsolutions.meatapp.core.designsystem.component.ContentState.Loading,
            )
        } else if (state.subcategories.isNotEmpty() && state.selectedSubcategory == null) {
            CustomerSearchTopBar(
                title = selectedCategory.name,
                placeholder = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_search_subcategories_e87dd4a),
                searchVisible = searchVisible,
                searchQuery = subcategorySearch,
                onBack = viewModel::showCategories,
                onSearch = { searchVisible = true },
                onSearchChange = { subcategorySearch = it },
                onCloseSearch = {
                    searchVisible = false
                    subcategorySearch = ""
                },
                cartCount = state.cartQuantity, onCart = onCart,
            )
            val visibleSubcategories = remember(state.subcategories, subcategorySearch) {
                val query = subcategorySearch.trim()
                if (query.isEmpty()) state.subcategories else state.subcategories.filter {
                    it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
                }
            }
            if (visibleSubcategories.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    androidx.compose.material3.Text(
                        androidx.compose.ui.res.stringResource(
                            com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_no_matching_subcategories,
                        ),
                    )
                }
            } else {
                CustomerSubcategoryBrowser(selectedCategory.name, visibleSubcategories) { subcategory ->
                    searchVisible = false
                    subcategorySearch = ""
                    viewModel.selectSubcategory(subcategory)
                }
            }
        } else {
            CustomerSearchTopBar(
                title = state.selectedSubcategory?.name ?: selectedCategory.name,
                placeholder = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_search_products_b73f756),
                searchVisible = searchVisible,
                searchQuery = state.search,
                onBack = {
                    searchVisible = false
                    if (state.selectedSubcategory != null) viewModel.showSubcategories() else viewModel.showCategories()
                },
                onSearch = { searchVisible = true },
                onSearchChange = viewModel::updateSearch,
                onCloseSearch = {
                    searchVisible = false
                    viewModel.updateSearch("")
                },
                cartCount = state.cartQuantity,
                onCart = onCart,
            )
            CustomerProductListContent(
                state = state,
                onProduct = viewModel::openProduct,
                onQuantity = viewModel::changeProductCartQuantity,
                onLoadMore = viewModel::loadMore,
                onRefresh = viewModel::refresh,
            )
        }
    }
}

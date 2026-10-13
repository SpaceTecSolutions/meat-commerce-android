package com.spacetecsolutions.meatapp.feature.customerhome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.AppLottieAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.CustomerHomeData
import com.spacetecsolutions.meatapp.core.model.ProductCategory

@Composable
fun CustomerHomeRoute(
    onLocation: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onCart: () -> Unit,
    profileName: String,
    showNotifications: Boolean = true,
    isGuest: Boolean = false,
    onSearch: (String) -> Unit,
    onViewAllCategories: () -> Unit,
    onCategory: (String, String) -> Unit,
    onProduct: (String) -> Unit,
    onAbout: () -> Unit = {},
    onFaq: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onTerms: () -> Unit = {},
    onUseCurrentLocation: () -> Unit = {},
    viewModel: CustomerHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.refreshCart() }
    CustomerHomeScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onLocation = onLocation,
        onNotifications = onNotifications,
        onProfile = onProfile,
        onCart = onCart,
        profileName = profileName,
        showNotifications = showNotifications,
        isGuest = isGuest,
        onSearch = onSearch,
        onViewAllCategories = onViewAllCategories,
        onCategory = onCategory,
        onProduct = onProduct,
        onAbout = onAbout,
        onFaq = onFaq,
        onPrivacy = onPrivacy,
        onTerms = onTerms,
        onUseCurrentLocation = onUseCurrentLocation,
        onAddToCart = viewModel::addToCart,
        onChangeCartQuantity = viewModel::changeCartQuantity,
        onConsumeCartMessage = viewModel::consumeCartMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerHomeScreen(
    state: CustomerHomeUiState,
    onRefresh: () -> Unit,
    onLocation: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onCart: () -> Unit,
    profileName: String,
    showNotifications: Boolean,
    isGuest: Boolean,
    onSearch: (String) -> Unit,
    onViewAllCategories: () -> Unit,
    onCategory: (String, String) -> Unit,
    onProduct: (String) -> Unit,
    onAbout: () -> Unit,
    onFaq: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onAddToCart: (com.spacetecsolutions.meatapp.core.model.Product) -> Unit,
    onChangeCartQuantity: (com.spacetecsolutions.meatapp.core.model.Product, Int) -> Unit,
    onConsumeCartMessage: () -> Unit,
) {
    val data = state.data
    val context = LocalContext.current
    val promptPreferences = remember { context.getSharedPreferences("customer_location_prompt", 0) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.cartMessage) {
        state.cartMessage?.let { snackbar.showSnackbar(it); onConsumeCartMessage() }
    }
    var showLocationRationale by remember { mutableStateOf(false) }
    var showLocationSettings by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onUseCurrentLocation() else showLocationSettings = true
    }
    LaunchedEffect(data?.deliveryLocationLabel) {
        if (!isGuest && data?.deliveryLocationLabel?.startsWith("Select", ignoreCase = true) == true &&
            !promptPreferences.getBoolean("asked", false)) showLocationRationale = true
    }
    if (showLocationRationale) AlertDialog(
        onDismissRequest = { showLocationRationale = false },
        title = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_use_your_current_location_c46c559)) },
        text = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_allow_location_access_to_quickly_6b283e5)) },
        confirmButton = { TextButton(onClick = {
            promptPreferences.edit().putBoolean("asked", true).apply()
            showLocationRationale = false
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) onUseCurrentLocation()
            else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_allow_3ad0e36)) } },
        dismissButton = { TextButton(onClick = {
            promptPreferences.edit().putBoolean("asked", true).apply(); showLocationRationale = false
        }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_not_now_e457149)) } },
    )
    if (showLocationSettings) AlertDialog(
        onDismissRequest = { showLocationSettings = false },
        title = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_location_permission_is_off_3f1770f)) },
        text = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_you_can_continue_shopping_and_986da27)) },
        confirmButton = { TextButton(onClick = {
            showLocationSettings = false
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")))
        }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_open_settings_134635e)) } },
        dismissButton = { TextButton(onClick = { showLocationSettings = false }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_continue_2e02623)) } },
    )
    Box(Modifier.fillMaxSize().background(CustomerHomeTokens.canvas).statusBarsPadding()) {
      Column(Modifier.fillMaxSize()) {
        HomeLocationHeader(
            addressLabel = data?.deliveryAddressLabel ?: "Select location",
            notifications = data?.unreadNotifications ?: 0,
            onLocation = onLocation,
            onNotifications = onNotifications,
            onProfile = onProfile,
            profileName = profileName,
            showNotifications = showNotifications,
        )
        Box(Modifier.fillMaxWidth().background(CustomerHomeTokens.surface)
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
            CustomerHomeSearch(onSearch)
        }
        Box(Modifier.weight(1f)) {
            when {
            state.loading && data == null -> CustomerHomeSkeleton()
            state.error != null && data == null -> ContentStateView(
                ContentState.Error(title = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_unable_to_load_home_017c9f6), description = state.error),
                onAction = onRefresh,
            )
            data == null -> ContentStateView(
                ContentState.Empty(
                    title = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_the_shop_is_getting_ready_f03739c),
                    description = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_fresh_products_and_categories_will_907f103),
                    actionLabel = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_refresh_56e3bad),
                ),
                onAction = onRefresh,
            )
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                HomeContent(
                    data = data,
                    error = state.error,
                    onRefresh = onRefresh,
                    onViewAllCategories = onViewAllCategories,
                    onCategory = onCategory,
                    onProduct = onProduct,
                    onAbout = onAbout,
                    onFaq = onFaq,
                    onPrivacy = onPrivacy,
                    onTerms = onTerms,
                    cartQuantities = state.cartQuantities,
                    cartLoaded = state.cartLoaded,
                    updatingProductIds = state.updatingProductIds,
                    onAddToCart = onAddToCart,
                    onChangeCartQuantity = onChangeCartQuantity,
                    hasCartBar = state.cart?.lines?.isNotEmpty() == true,
                )
            }
            }
            state.cart?.takeIf { it.lines.isNotEmpty() }?.let {
                Box(Modifier.align(Alignment.BottomCenter)) { HomeCartBar(it, onCart) }
            }
        }
      }
      SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun HomeContent(
    data: CustomerHomeData,
    error: String?,
    onRefresh: () -> Unit,
    onViewAllCategories: () -> Unit,
    onCategory: (String, String) -> Unit,
    onProduct: (String) -> Unit,
    onAbout: () -> Unit,
    onFaq: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    cartQuantities: Map<String, Int>,
    cartLoaded: Boolean,
    updatingProductIds: Set<String>,
    onAddToCart: (com.spacetecsolutions.meatapp.core.model.Product) -> Unit,
    onChangeCartQuantity: (com.spacetecsolutions.meatapp.core.model.Product, Int) -> Unit,
    hasCartBar: Boolean,
) {
    val fallbackProduct = data.bestSellers.firstOrNull() ?: data.recommended.firstOrNull()
    if (data.hasNoProducts()) {
        HomeWithoutProducts(
            data = data,
            error = error,
            onRefresh = onRefresh,
            onViewAllCategories = onViewAllCategories,
            onCategory = onCategory,
            onProduct = onProduct,
            onAbout = onAbout,
            onFaq = onFaq,
            onPrivacy = onPrivacy,
            onTerms = onTerms,
            hasCartBar = hasCartBar,
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = AppDimensions.contentMaxWidth),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp,
            bottom = if (hasCartBar) 110.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (error != null || data.catalogStale) item { HomeSavedDataNotice(error, onRefresh) }
        if (data.banners.isNotEmpty()) item {
            HomeBannerCarousel(data.banners) { banner ->
                val action = banner.actionRoute.orEmpty()
                val productId = action.productId()
                when {
                    productId != null -> onProduct(productId)
                    action.startsWith("category:") -> {
                        val id = action.removePrefix("category:")
                        onCategory(id, data.categories.firstOrNull { it.id == id }?.name ?: "Products")
                    }
                    else -> onViewAllCategories()
                }
            }
        } else if (fallbackProduct != null) item {
            HomePromoBanner(null, fallbackProduct.imageUrls.firstOrNull()) { onProduct(fallbackProduct.id) }
        }
        if (data.categories.isNotEmpty()) item {
            HomeCategories(data.categories, onViewAllCategories, onCategory)
        }
        if (data.bestSellers.isNotEmpty()) item {
            HomeProductSection("Best Sellers", data.bestSellers, onViewAllCategories, onProduct,
                cartQuantities, cartLoaded, updatingProductIds, onAddToCart, onChangeCartQuantity)
        }
        if (data.recommended.isNotEmpty()) item {
            HomeProductSection("Recommended for You", data.recommended, onViewAllCategories, onProduct,
                cartQuantities, cartLoaded, updatingProductIds, onAddToCart, onChangeCartQuantity)
        }
        if (data.popularThisWeek.isNotEmpty()) item {
            HomeProductSection("Popular This Week", data.popularThisWeek, onViewAllCategories, onProduct,
                cartQuantities, cartLoaded, updatingProductIds, onAddToCart, onChangeCartQuantity)
        }
        if (data.quickPicks.isNotEmpty()) item {
            HomeProductSpotlight("Picked for You", data.quickPicks, onProduct)
        }
        item { HomeTrustFooter(onAbout, onFaq, onPrivacy, onTerms) }
    }
}

@Composable
private fun HomeWithoutProducts(
    data: CustomerHomeData,
    error: String?,
    onRefresh: () -> Unit,
    onViewAllCategories: () -> Unit,
    onCategory: (String, String) -> Unit,
    onProduct: (String) -> Unit,
    onAbout: () -> Unit,
    onFaq: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    hasCartBar: Boolean,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val tablet = maxWidth >= AppDimensions.expandedContentBreakpoint
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = AppDimensions.contentMaxWidth)
                .then(if (tablet) Modifier.navigationBarsPadding() else Modifier),
            contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp,
                bottom = if (hasCartBar) 110.dp else if (tablet) 24.dp else 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (error != null || data.catalogStale) HomeSavedDataNotice(error, onRefresh)
                    if (data.banners.isNotEmpty()) {
                        HomeBannerCarousel(data.banners) { banner ->
                            val action = banner.actionRoute.orEmpty()
                            val productId = action.productId()
                            when {
                                productId != null -> onProduct(productId)
                                action.startsWith("category:") -> {
                                    val id = action.removePrefix("category:")
                                    onCategory(id,
                                        data.categories.firstOrNull { it.id == id }?.name ?: "Products")
                                }
                                else -> onViewAllCategories()
                            }
                        }
                    }
                    if (data.categories.isNotEmpty()) {
                        HomeCategories(data.categories, onViewAllCategories, onCategory)
                    }
                }
            }
            item { HomeEmptyAnimalScene(onRefresh) }
            item { HomeTrustFooter(onAbout, onFaq, onPrivacy, onTerms) }
        }
    }
}

@Composable
private fun HomeEmptyAnimalScene(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppLottieAnimation(
            animation = AppAnimation.EmptyData,
            modifier = Modifier.size(156.dp),
            loop = true,
        )
        Text(
            "Fresh choices are on the way",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = onRefresh) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_refresh_56e3bad)) }
    }
}

private data class TrustBadge(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun HomeTrustFooter(onAbout: () -> Unit, onFaq: () -> Unit,
    onPrivacy: () -> Unit, onTerms: () -> Unit) {
    val badges = remember { listOf(
        TrustBadge("Quality checked", com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons.Verification),
        TrustBadge("Fresh delivery", com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons.Delivery),
        TrustBadge("Secure ordering", com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons.Security),
    ) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = T.surface,
            border = BorderStroke(1.dp, T.border), shadowElevation = 1.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                badges.forEach { badge -> Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)) {
                    Icon(badge.icon, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                    Text(badge.title, style = MaterialTheme.typography.labelSmall)
                } }
            }
        }

        Row(horizontalArrangement = Arrangement.Center) {
            listOf("About" to onAbout, "FAQ" to onFaq, "Privacy" to onPrivacy, "Terms" to onTerms)
                .forEach { (label, action) -> TextButton(action) { Text(label,
                    style = MaterialTheme.typography.labelSmall) } }
        }
    }
}

@Composable
private fun HomeProductSection(
    title: String,
    products: List<com.spacetecsolutions.meatapp.core.model.Product>,
    onViewAll: () -> Unit,
    onProduct: (String) -> Unit,
    cartQuantities: Map<String, Int>,
    cartLoaded: Boolean,
    updatingProductIds: Set<String>,
    onAddToCart: (com.spacetecsolutions.meatapp.core.model.Product) -> Unit,
    onChangeCartQuantity: (com.spacetecsolutions.meatapp.core.model.Product, Int) -> Unit,
) {
    Column {
        HomeSectionHeader(title, onViewAll)
        LazyRow(
            contentPadding = PaddingValues(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(products, key = { it.id }) { product ->
                HomeBestSellerCard(product, cartQuantities[product.id] ?: 0, cartLoaded,
                    product.id in updatingProductIds, { onProduct(product.id) },
                    { onAddToCart(product) }, { delta -> onChangeCartQuantity(product, delta) })
            }
        }
    }
}

@Composable
private fun HomeCategories(
    categories: List<ProductCategory>,
    onViewAll: () -> Unit,
    onCategory: (String, String) -> Unit,
) {
    BoxWithConstraints {
        val limit = if (maxWidth >= AppDimensions.expandedContentBreakpoint) 8 else 4
        val shortcuts = categories.take(limit)
        Column {
            HomeSectionHeader("Categories", onViewAll)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                items(shortcuts, key = { it.id }) { category ->
                    HomeCategoryShortcut(category) { onCategory(category.id, category.name) }
                }
                if (categories.size > shortcuts.size) item { HomeMoreShortcut(onViewAll) }
            }
        }
    }
}

@Composable
private fun HomeSavedDataNotice(error: String?, onRefresh: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = AppShapes.small) {
        Row(
            Modifier.fillMaxWidth().padding(start = AppSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                error ?: "Showing saved shop data", Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onRefresh) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_retry_9f5cd8a)) }
        }
    }
}

private fun String.productId(): String? = when {
    startsWith("product:", ignoreCase = true) -> substringAfter(':').takeIf(String::isNotBlank)
    else -> null
}

private fun CustomerHomeData.hasNoProducts() = bestSellers.isEmpty() && recommended.isEmpty() &&
    popularThisWeek.isEmpty() && quickPicks.isEmpty()

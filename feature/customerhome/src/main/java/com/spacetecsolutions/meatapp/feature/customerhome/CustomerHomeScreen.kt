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
        title = { Text("Use your current location?") },
        text = { Text("Allow location access to quickly detect your delivery area.") },
        confirmButton = { TextButton(onClick = {
            promptPreferences.edit().putBoolean("asked", true).apply()
            showLocationRationale = false
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) onUseCurrentLocation()
            else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }) { Text("Allow") } },
        dismissButton = { TextButton(onClick = {
            promptPreferences.edit().putBoolean("asked", true).apply(); showLocationRationale = false
        }) { Text("Not now") } },
    )
    if (showLocationSettings) AlertDialog(
        onDismissRequest = { showLocationSettings = false },
        title = { Text("Location permission is off") },
        text = { Text("You can continue shopping and enter an address manually, or enable location in Settings.") },
        confirmButton = { TextButton(onClick = {
            showLocationSettings = false
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")))
        }) { Text("Open Settings") } },
        dismissButton = { TextButton(onClick = { showLocationSettings = false }) { Text("Continue") } },
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
                ContentState.Error(title = "Unable to load Home", description = state.error),
                onAction = onRefresh,
            )
            data == null || data.isEmpty() -> ContentStateView(
                ContentState.Empty(
                    title = "The shop is getting ready",
                    description = "Fresh products and categories will appear here soon.",
                    actionLabel = "Refresh",
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
            TextButton(onClick = onRefresh) { Text("Retry") }
        }
    }
}

private fun String.productId(): String? = when {
    startsWith("product:", ignoreCase = true) -> substringAfter(':').takeIf(String::isNotBlank)
    else -> null
}

private fun CustomerHomeData.isEmpty() = banners.isEmpty() && categories.isEmpty() &&
    bestSellers.isEmpty() && recommended.isEmpty() && popularThisWeek.isEmpty() && quickPicks.isEmpty()

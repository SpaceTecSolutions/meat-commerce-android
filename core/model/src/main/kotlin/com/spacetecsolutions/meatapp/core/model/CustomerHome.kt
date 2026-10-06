package com.spacetecsolutions.meatapp.core.model

data class PromotionBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null,
    val actionRoute: String? = null,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val contentAlignment: BannerContentAlignment = BannerContentAlignment.BOTTOM_START,
    val buttonEnabled: Boolean = false,
    val buttonText: String = "Shop Now",
    val revision: Long = 0,
    val textPlacement: BannerPlacement? = null,
    val buttonPlacement: BannerPlacement? = null,
    val subtitlePlacement: BannerPlacement? = null,
    val buttonColor: String = "RED",
    val buttonTextColor: String = "WHITE",
    val buttonShape: String = "ROUNDED",
    val buttonArrow: Boolean = false,
)

data class BannerPlacement(val x: Float = 0f, val y: Float = 0f, val alignment: String = "LEFT")

enum class BannerContentAlignment {
    TOP_START, TOP_CENTER, TOP_END,
    CENTER_START, CENTER, CENTER_END,
    BOTTOM_START, BOTTOM_CENTER, BOTTOM_END,
}

data class BannerInput(
    val bannerId: String? = null,
    val title: String,
    val subtitle: String,
    val imageUploadToken: String? = null,
    val actionRoute: String? = null,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val contentAlignment: BannerContentAlignment = BannerContentAlignment.BOTTOM_START,
    val buttonEnabled: Boolean = false,
    val buttonText: String = "Shop Now",
    val expectedRevision: Long? = null,
    val textPlacement: BannerPlacement? = null,
    val buttonPlacement: BannerPlacement? = null,
    val subtitlePlacement: BannerPlacement? = null,
    val buttonColor: String = "RED",
    val buttonTextColor: String = "WHITE",
    val buttonShape: String = "ROUNDED",
    val buttonArrow: Boolean = false,
)

data class BannerImageUpload(val token: String)

data class CustomerHomeData(
    val deliveryLocationLabel: String = "Select delivery location",
    val deliveryAddressLabel: String = "Select location",
    val unreadNotifications: Int = 0,
    val cartQuantity: Int = 0,
    val banners: List<PromotionBanner> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val bestSellers: List<Product> = emptyList(),
    val offers: List<Product> = emptyList(),
    val recommended: List<Product> = emptyList(),
    val popularThisWeek: List<Product> = emptyList(),
    val quickPicks: List<Product> = emptyList(),
    val newProducts: List<Product> = emptyList(),
    val offersEnabled: Boolean = false,
    val promotions: List<Promotion> = emptyList(),
    val catalogStale: Boolean = false,
    val catalogCachedAtEpochMillis: Long? = null,
)

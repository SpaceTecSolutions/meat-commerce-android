package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImage
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImageCrop
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.*
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
internal fun HomeLocationHeader(
    addressLabel: String,
    notifications: Int,
    profileName: String,
    onLocation: () -> Unit,
    onNotifications: () -> Unit,
    showNotifications: Boolean = true,
    onProfile: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 74.dp)
            .background(CustomerHomeTokens.surface).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onLocation)
                .semantics { role = Role.Button }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(Modifier.size(40.dp), shape = CircleShape, color = CustomerHomeTokens.rose) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Location, null, tint = CustomerHomeTokens.red,
                        modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.padding(start = 10.dp).weight(1f, fill = false)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Delivering to ${profileName.trim().substringBefore(' ').ifBlank { "Guest" }}",
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = CustomerHomeTokens.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(AppIcons.ArrowDown, null, modifier = Modifier.padding(start = 3.dp).size(19.dp),
                        tint = CustomerHomeTokens.red)
                }
                Text(addressLabel.ifBlank { "Select location" }, style = MaterialTheme.typography.bodySmall,
                    color = CustomerHomeTokens.muted, maxLines = 1)
            }
        }
        if (showNotifications) BadgedBox(
            badge = {
                if (notifications > 0) Badge(containerColor = CustomerHomeTokens.red) {
                    Text(if (notifications > 99) "99+" else "$notifications")
                }
            },
        ) {
            IconButton(onClick = onNotifications, modifier = Modifier.size(44.dp)) {
                Icon(
                    AppIcons.NotificationsOutline,
                    "Notifications${if (notifications > 0) ", $notifications unread" else ""}",
                    modifier = Modifier.size(22.dp),
                    tint = CustomerHomeTokens.ink,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(onClick = onProfile, modifier = Modifier.size(38.dp), shape = CircleShape,
            color = CustomerHomeTokens.ink) {
            Box(contentAlignment = Alignment.Center) {
                Text(profileName.trim().firstOrNull()?.uppercase() ?: "U",
                    color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
internal fun CustomerHomeSearch(onSearch: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        leadingIcon = { Icon(AppIcons.Search, null, modifier = Modifier.size(19.dp), tint = CustomerHomeTokens.muted) },
        placeholder = {
            Text("Search for meat, chicken, fish...", style = MaterialTheme.typography.bodySmall, maxLines = 1)
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = CustomerHomeTokens.surface,
            focusedContainerColor = CustomerHomeTokens.surface,
            unfocusedBorderColor = CustomerHomeTokens.border,
            focusedBorderColor = CustomerHomeTokens.red,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            query.trim().takeIf(String::isNotEmpty)?.let(onSearch)
        }),
    )
}

@Composable
internal fun HomePromoBanner(
    banner: PromotionBanner?,
    fallbackImageUrl: String?,
    onClick: (() -> Unit)?,
) {
    val title = banner?.title?.takeIf(String::isNotBlank) ?: "Fresh Meat\nDelivered Daily"
    val subtitle = banner?.subtitle?.takeIf(String::isNotBlank) ?: "100% Fresh & Hygienic"
    val clickModifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Surface(
        modifier = Modifier.fillMaxWidth().height(176.dp).then(clickModifier),
        shape = RoundedCornerShape(17.dp),
        color = PromoBackground,
    ) {
        Box(Modifier.fillMaxSize()) {
            AppImage(
                model = banner?.imageUrl ?: fallbackImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
                Color.Black.copy(alpha = .08f), Color.Black.copy(alpha = .72f),
            ))))
            if (banner?.textPlacement != null) com.spacetecsolutions.meatapp.core.designsystem.component.PositionedBannerContent(banner)
            else if (banner == null || banner.contentAlignment == BannerContentAlignment.BOTTOM_START) {
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp)) {
                    Text(subtitle, color = Color.White.copy(alpha = .92f),
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, Modifier.weight(1f), color = Color.White,
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (onClick != null && (banner == null || banner.buttonEnabled)) {
                            Surface(shape = RoundedCornerShape(50), color = CustomerHomeTokens.surface) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(banner?.buttonText ?: "Shop Now", color = CustomerHomeTokens.ink,
                                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                        maxLines = 1)
                                    Icon(AppIcons.ArrowRight, null, Modifier.size(16.dp),
                                        tint = CustomerHomeTokens.red)
                                }
                            }
                        }
                    }
                }
            }
            else Column(
                Modifier.align(banner.contentAlignment.boxAlignment())
                    .fillMaxWidth(.72f).padding(AppSpacing.medium),
                horizontalAlignment = banner.contentAlignment.horizontal(),
            ) {
                Text(
                    title, color = Color.White, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = banner.contentAlignment.textAlign(),
                )
                Spacer(Modifier.height(AppSpacing.extraSmall))
                Text(
                    subtitle, color = Color.White.copy(alpha = .82f),
                    style = MaterialTheme.typography.labelSmall, maxLines = 2,
                )
                if (onClick != null && banner.buttonEnabled) {
                    Spacer(Modifier.height(AppSpacing.compact))
                    Surface(color = if (banner.buttonColor == "WHITE") Color.White else MaterialTheme.colorScheme.primary,
                        shape = if (banner.buttonShape == "CAPSULE") RoundedCornerShape(50) else AppShapes.extraSmall) {
                        Text(
                            banner.buttonText + if (banner.buttonArrow) "  →" else "",
                            Modifier.padding(horizontal = AppSpacing.compact, vertical = 7.dp),
                            color = when (banner.buttonTextColor) {
                                "RED" -> MaterialTheme.colorScheme.primary
                                "BLACK" -> Color.Black
                                else -> Color.White
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeBannerCarousel(
    banners: List<PromotionBanner>,
    onBannerClick: (PromotionBanner) -> Unit,
) {
    val pages = banners.take(5)
    val pager = rememberPagerState(pageCount = pages::size)
    LaunchedEffect(pages.size) {
        while (pages.size > 1) {
            delay(4_000)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % pages.size)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
        HorizontalPager(state = pager, pageSpacing = AppSpacing.small) { page ->
            val banner = pages[page]
            HomePromoBanner(
                banner = banner,
                fallbackImageUrl = null,
                onClick = if (banner.buttonEnabled && banner.actionRoute != null) {
                    { onBannerClick(banner) }
                } else null,
            )
        }
        if (pages.size > 1) Row(
            Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) { repeat(pages.size) { index -> Box(
            Modifier.size(if (index == pager.currentPage) 18.dp else 7.dp, 7.dp)
                .clip(AppShapes.small).background(if (index == pager.currentPage)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        ) } }
    }
}

private fun BannerContentAlignment.boxAlignment() = when {
    name.startsWith("TOP") -> if (name.endsWith("START")) Alignment.TopStart else if (name.endsWith("END")) Alignment.TopEnd else Alignment.TopCenter
    name.startsWith("BOTTOM") -> if (name.endsWith("START")) Alignment.BottomStart else if (name.endsWith("END")) Alignment.BottomEnd else Alignment.BottomCenter
    else -> if (name.endsWith("START")) Alignment.CenterStart else if (name.endsWith("END")) Alignment.CenterEnd else Alignment.Center
}
private fun BannerContentAlignment.horizontal() = when {
    name.endsWith("START") -> Alignment.Start
    name.endsWith("END") -> Alignment.End
    else -> Alignment.CenterHorizontally
}
private fun BannerContentAlignment.textAlign() = when {
    name.endsWith("START") -> TextAlign.Start
    name.endsWith("END") -> TextAlign.End
    else -> TextAlign.Center
}

@Composable
internal fun HomeSectionHeader(title: String, onViewAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold, color = CustomerHomeTokens.ink,
        )
        TextButton(onClick = onViewAll, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text("View All", style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, color = CustomerHomeTokens.red)
            Icon(AppIcons.ArrowRight, null, Modifier.size(16.dp), tint = CustomerHomeTokens.red)
        }
    }
}

@Composable
internal fun HomeProductSpotlight(title: String, products: List<Product>, onProduct: (String) -> Unit) {
    val pages = products.take(8)
    val pager = rememberPagerState(pageCount = pages::size)
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold,
            color = CustomerHomeTokens.ink)
        HorizontalPager(state = pager, pageSpacing = AppSpacing.small) { page ->
            val product = pages[page]
            Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(18.dp))
                .clickable { onProduct(product.id) }) {
                AppImage(product.imageUrls.firstOrNull(), product.name, Modifier.fillMaxSize(), cornerRadius = 18.dp)
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
                    Color.Black.copy(alpha = .78f), Color.Black.copy(alpha = .35f), Color.Transparent))))
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(.72f).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(product.categoryName.uppercase(), style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = .8f), fontWeight = FontWeight.Bold)
                    Text(product.name, style = MaterialTheme.typography.titleLarge, color = Color.White,
                        fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
                        .format((product.offerPriceMinor ?: product.priceMinor) / 100.0),
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(50), color = CustomerHomeTokens.red) {
                        Text("View product", Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            color = Color.White, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (pages.size > 1) Row(Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(pages.size) { index -> Box(Modifier.size(if (index == pager.currentPage) 18.dp else 7.dp, 7.dp)
                .clip(RoundedCornerShape(50)).background(if (index == pager.currentPage)
                    CustomerHomeTokens.red else MaterialTheme.colorScheme.outlineVariant)) }
        }
    }
}

@Composable
internal fun HomeCategoryShortcut(category: ProductCategory, onClick: () -> Unit) {
    Column(
        Modifier.width(66.dp).semantics { contentDescription = "${category.name} category" }
            .clip(AppShapes.small).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(Modifier.size(56.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            AppImage(category.imageUrl, null, Modifier.fillMaxSize(), AppImageCrop.CIRCLE)
        }
        Text(
            category.name, modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.labelSmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun HomeMoreShortcut(onClick: () -> Unit) {
    Column(
        Modifier.width(66.dp).clip(AppShapes.small).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(Modifier.size(56.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                Icon(AppIcons.Categories, "View all categories", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text("More", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImage
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImageCrop
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerCommerceTokens as T
import kotlinx.coroutines.delay

@Composable
internal fun ProductImageCarousel(images: List<String>, productName: String, onBack: () -> Unit) {
    val pages = images.filter(String::isNotBlank).ifEmpty { listOf("") }
    BoxWithConstraints(Modifier.fillMaxWidth().background(T.ink)) {
        val heroHeight = (maxWidth * .79f).coerceAtMost(400.dp)
        Box(Modifier.fillMaxWidth().height(heroHeight)) {
            key(pages) {
                val pager = rememberPagerState(pageCount = { pages.size })
                LaunchedEffect(pages) {
                    if (pages.size > 1) while (true) {
                        delay(4_000)
                        if (!pager.isScrollInProgress) {
                            pager.animateScrollToPage((pager.currentPage + 1) % pages.size)
                        }
                    }
                }
                HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                    AppImage(pages[page].takeIf(String::isNotBlank),
                        "$productName, photo ${page + 1} of ${pages.size}",
                        Modifier.fillMaxSize(), crop = AppImageCrop.NONE,
                        contentScale = ContentScale.Crop)
                }
                if (pages.size > 1) {
                    Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
                        shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = .5f)) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            repeat(pages.size) { index ->
                                Surface(Modifier.size(if (pager.currentPage == index) 16.dp else 6.dp, 6.dp),
                                    shape = CircleShape,
                                    color = if (pager.currentPage == index) T.red
                                        else T.surface.copy(alpha = .65f)) {}
                            }
                        }
                    }
                }
            }
            Surface(Modifier.align(Alignment.TopStart).statusBarsPadding()
                .padding(start = 16.dp, top = 10.dp), shape = CircleShape,
                color = T.surface.copy(alpha = .9f), shadowElevation = 2.dp) {
                IconButton(onClick = onBack, Modifier.size(40.dp)) {
                    Icon(AppIcons.Back, "Back", tint = T.ink)
                }
            }
        }
    }
}

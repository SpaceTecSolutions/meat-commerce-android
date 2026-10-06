package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import kotlinx.coroutines.delay

@Composable
fun AppImageCarousel(
    images: List<Any?>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    autoSlide: Boolean = true,
    onRemove: ((Int) -> Unit)? = null,
) {
    val pages = images.ifEmpty { listOf(null) }
    val pager = rememberPagerState(pageCount = pages::size)
    LaunchedEffect(pages.size, autoSlide) {
        while (autoSlide && pages.size > 1) {
            delay(AUTO_SLIDE_MILLIS)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % pages.size)
        }
    }
    Box(modifier.semantics {
        this.contentDescription = "$contentDescription, ${pages.size} photo${if (pages.size == 1) "" else "s"}"
    }) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            AppImage(
                model = pages[page],
                contentDescription = "$contentDescription, photo ${page + 1} of ${pages.size}",
                modifier = Modifier.fillMaxSize(),
                cornerRadius = cornerRadius,
                contentScale = ContentScale.Crop,
            )
        }
        if (onRemove != null && images.isNotEmpty()) {
            IconButton(
                onClick = { onRemove(pager.currentPage.coerceAtMost(images.lastIndex)) },
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(AppSpacing.extraSmall)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .9f), AppShapes.large),
            ) { Icon(AppIcons.Delete, "Remove current photo", tint = MaterialTheme.colorScheme.error) }
        }
        if (pages.size > 1) Row(
            Modifier.align(Alignment.BottomCenter)
                .padding(bottom = AppSpacing.small)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .72f), AppShapes.large)
                .padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { index ->
                Box(
                    Modifier.padding(horizontal = 3.dp)
                        .size(if (pager.currentPage == index) 18.dp else 7.dp, 7.dp)
                        .clip(AppShapes.small)
                        .background(if (pager.currentPage == index) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

private const val AUTO_SLIDE_MILLIS = 3_500L

package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.model.BannerPlacement
import com.spacetecsolutions.meatapp.core.model.PromotionBanner

@Composable
fun PositionedBannerContent(banner: PromotionBanner, modifier: Modifier = Modifier,
    onMoveText: ((BannerPlacement) -> Unit)? = null,
    onMoveSubtitle: ((BannerPlacement) -> Unit)? = null,
    onMoveButton: ((BannerPlacement) -> Unit)? = null) {
    BoxWithConstraints(modifier.fillMaxSize().padding(12.dp)) {
        val density = LocalDensity.current
        val width = with(density) { maxWidth.toPx() }
        val height = with(density) { maxHeight.toPx() }
        val text = banner.textPlacement ?: BannerPlacement(0f, .15f)
        val subtitle = banner.subtitlePlacement ?: BannerPlacement(0f, .45f)
        val button = banner.buttonPlacement ?: BannerPlacement(0f, .95f)
        val latestText by rememberUpdatedState(text)
        val latestSubtitle by rememberUpdatedState(subtitle)
        val latestButton by rememberUpdatedState(button)
        fun Modifier.drag(position: BannerPlacement, callback: ((BannerPlacement) -> Unit)?,
            widthFraction: Float, latest: () -> BannerPlacement) =
            if (callback == null) this else pointerInput(width, height) {
                var current = position
                detectDragGestures(onDragStart = { current = latest() }) { change, amount ->
                    change.consume()
                    current = current.copy(x = (current.x + amount.x / (width * widthFraction).coerceAtLeast(1f)).coerceIn(0f, 1f),
                        y = (current.y + amount.y / (height * .6f).coerceAtLeast(1f)).coerceIn(0f, 1f))
                    callback(current)
                }
            }
        Box(Modifier.align(BiasAlignment(text.x * 2 - 1, text.y * 2 - 1)).fillMaxWidth(.68f)
            .drag(text, onMoveText, .32f) { latestText }) {
            val alignment = when (text.alignment) { "RIGHT" -> TextAlign.End; "CENTER" -> TextAlign.Center; else -> TextAlign.Start }
            Text(banner.title, Modifier.fillMaxWidth(), color = Color.White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge, textAlign = alignment, maxLines = 2)
        }
        if (banner.subtitle.isNotBlank()) {
            val alignment = when (subtitle.alignment) { "RIGHT" -> TextAlign.End; "CENTER" -> TextAlign.Center; else -> TextAlign.Start }
            Text(banner.subtitle,
                Modifier.align(BiasAlignment(subtitle.x * 2 - 1, subtitle.y * 2 - 1)).fillMaxWidth(.68f)
                    .drag(subtitle, onMoveSubtitle, .32f) { latestSubtitle },
                color = Color.White, style = MaterialTheme.typography.bodySmall,
                textAlign = alignment, maxLines = 2)
        }
        if (banner.buttonEnabled) Surface(Modifier.align(BiasAlignment(button.x * 2 - 1, button.y * 2 - 1))
            .drag(button, onMoveButton, .65f) { latestButton },
            color = if (banner.buttonColor == "WHITE") Color.White else MaterialTheme.colorScheme.primary,
            shape = if (banner.buttonShape == "CAPSULE") RoundedCornerShape(50) else MaterialTheme.shapes.small) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                val contentColor = when (banner.buttonTextColor) {
                    "RED" -> MaterialTheme.colorScheme.primary
                    "BLACK" -> Color.Black
                    else -> Color.White
                }
                Text(banner.buttonText, color = contentColor, style = MaterialTheme.typography.labelMedium)
                if (banner.buttonArrow) Icon(AppIcons.ArrowRight, null,
                    Modifier.padding(start = 5.dp).size(14.dp), tint = contentColor)
            }
        }
    }
}

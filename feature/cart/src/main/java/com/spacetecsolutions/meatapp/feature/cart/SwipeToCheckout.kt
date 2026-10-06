package com.spacetecsolutions.meatapp.feature.cart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerCommerceTokens as T
import com.spacetecsolutions.meatapp.core.model.CustomerCart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun SwipeToCheckout(cart: CustomerCart, checkout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var completed by remember { mutableStateOf(false) }
    val canEnterCheckout = cart.lines.isNotEmpty() && cart.lines.all { it.available } &&
        cart.subtotalMinor >= cart.minimumOrderMinor
    val enabled = canEnterCheckout && !completed
    val pulse = rememberInfiniteTransition(label = "checkout arrow pulse")
    val arrowScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse),
        label = "checkout arrow scale",
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(56.dp)
                .clip(RoundedCornerShape(50))
                .background(T.surface)) {
                val travel = with(density) { (maxWidth - 56.dp).toPx() }.coerceAtLeast(0f)
                val state = rememberDraggableState { delta ->
                    if (enabled) dragOffset = (dragOffset + delta).coerceIn(0f, travel)
                }
                Box(Modifier.width(with(density) { (56.dp.toPx() + dragOffset).toDp() })
                    .fillMaxHeight().clip(RoundedCornerShape(50)).background(T.rose))
                Row(Modifier.fillMaxSize().padding(start = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    Text("Swipe to Checkout", color = if (enabled) T.red else T.muted,
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    /*Icon(AppIcons.ArrowRight, null, Modifier.padding(start = 8.dp),
                        tint = if (enabled) T.red else T.muted)*/
                }
                Surface(
                    modifier = Modifier
                        .scale(if (enabled && dragOffset == 0f) arrowScale else 1f)
                        .offset { IntOffset(dragOffset.roundToInt(), 0) }
                        .size(56.dp).padding(4.dp)
                        .semantics { contentDescription = "Swipe right to checkout" }
                        .draggable(orientation = Orientation.Horizontal, state = state,
                            enabled = enabled, onDragStopped = {
                                val end = dragOffset >= travel * .8f
                                if (end) completed = true
                                scope.launch {
                                    val animation = Animatable(dragOffset)
                                    animation.animateTo(if (end) travel else 0f, tween(160)) {
                                        dragOffset = value
                                    }
                                    if (end) checkout()
                                }
                            }),
                    shape = RoundedCornerShape(50), color = if (enabled) T.red else T.muted,
                ) { Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.ArrowRight, null,
                        modifier = Modifier.scale(if (enabled && dragOffset == 0f) arrowScale else 1f),
                        tint = Color.White)
                } }
            }
            Text(if (canEnterCheckout) "Slide right to select address and delivery time"
                else "Check your items and minimum order to continue",
                modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall,
                color = T.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.compose.runtime.*

@Stable
class BottomBarVisibilityState {
    private var hiddenRequests by mutableIntStateOf(0)
    val visible: Boolean get() = hiddenRequests == 0

    internal fun hide() { hiddenRequests += 1 }
    internal fun show() { hiddenRequests = (hiddenRequests - 1).coerceAtLeast(0) }
}

val LocalBottomBarVisibility = staticCompositionLocalOf { BottomBarVisibilityState() }

@Composable
fun HideAppBottomBar() {
    val state = LocalBottomBarVisibility.current
    DisposableEffect(state) {
        state.hide()
        onDispose(state::show)
    }
}

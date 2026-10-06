package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDeleteContainer(enabled: Boolean, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.EndToStart && enabled) onDelete()
        false // Keep the item while the existing confirmation/mutation runs.
    })
    SwipeToDismissBox(state, enableDismissFromStartToEnd = false, enableDismissFromEndToStart = enabled,
        modifier = Modifier.semantics { customActions = listOf(CustomAccessibilityAction("Delete") {
            if (enabled) onDelete(); enabled
        }) }, backgroundContent = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error, MaterialTheme.shapes.medium)
                .padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(AppIcons.Delete, "Delete", tint = MaterialTheme.colorScheme.onError)
            }
        }) { content() }
}

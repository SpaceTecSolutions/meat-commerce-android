package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeDeleteContainer(enabled: Boolean, label: String, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.EndToStart && enabled) onDelete()
        false
    })
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error, AppShapes.medium)
                    .padding(horizontal = AppSpacing.medium),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(AppIcons.Delete, "Delete $label", tint = MaterialTheme.colorScheme.onError)
                    Text("Delete", color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        },
    ) { content() }
}

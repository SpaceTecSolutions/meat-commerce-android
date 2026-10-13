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
                    Icon(AppIcons.Delete, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_delete_label_ccb0e69, label), tint = MaterialTheme.colorScheme.onError)
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_delete_f6fdbe4), color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        },
    ) { content() }
}

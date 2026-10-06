package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
        shape = MaterialTheme.shapes.large,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                start = AppSpacing.large,
                end = AppSpacing.large,
                bottom = AppSpacing.extraLarge,
            ),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.padding(top = AppSpacing.medium), content = content)
        }
    }
}

package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.User

@Composable
internal fun DeliveryAssignmentDialog(
    order: CodOrder,
    staff: List<User>,
    loading: Boolean,
    onAssign: (User) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Order ${order.displayNumber}") },
        text = {
            when {
                loading -> Box(
                    Modifier.fillMaxWidth().height(AppDimensions.feedbackIcon),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                staff.isEmpty() -> Text("No active delivery users are available.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                    items(staff, key = User::id) { user ->
                        ElevatedCard(
                            onClick = { onAssign(user) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(AppSpacing.medium),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(AppIcons.Delivery, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.padding(start = AppSpacing.medium)) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(user.mobileNumber, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

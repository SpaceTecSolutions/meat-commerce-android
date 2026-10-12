package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.OrderSlotOptions

@Composable
internal fun OrderSlotChangeDialog(
    order: CodOrder,
    options: OrderSlotOptions?,
    loading: Boolean,
    busy: Boolean,
    selectedDateId: String?,
    selectedSlotId: String?,
    reason: String,
    onDate: (String) -> Unit,
    onSlot: (String) -> Unit,
    onReason: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val date = options?.deliveryDates?.firstOrNull { it.id == selectedDateId }
    val slots = options?.deliverySlots.orEmpty().filter { it.active && it.id in date?.availableSlotIds.orEmpty() }
    val valid = selectedSlotId in slots.map { it.id } && reason.trim().length >= 10
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Change delivery slot") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Current: ${order.deliverySlotDateLabel.orEmpty()} · ${order.deliverySlotTimeLabel.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall)
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                else if (options == null || options.deliveryDates.isEmpty()) {
                    Text("No available delivery slots. Check delivery settings.",
                        color = MaterialTheme.colorScheme.error)
                } else {
                    Text("New date", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.deliveryDates.forEach { choice ->
                            FilterChip(selectedDateId == choice.id, { onDate(choice.id) }, label = { Text(choice.label) })
                        }
                    }
                    if (date != null) {
                        Text("New time", style = MaterialTheme.typography.titleSmall)
                        slots.forEach { slot ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selectedSlotId == slot.id, { onSlot(slot.id) })
                                Text(slot.label)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reason, onValueChange = onReason, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reason for customer *") },
                        supportingText = { Text("10–200 characters. This reason is sent in the notification.") },
                        minLines = 2, maxLines = 4, isError = reason.isNotEmpty() && reason.trim().length < 10,
                    )
                }
            }
        },
        confirmButton = { Button(onConfirm, enabled = valid && !loading && !busy) { Text("Update & notify") } },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

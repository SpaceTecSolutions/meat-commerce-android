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
        title = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_change_delivery_slot_0bdad7a)) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_current_order_deliveryslotdatelabel_orempty_o_b964d05, order.deliverySlotDateLabel.orEmpty(), order.deliverySlotTimeLabel.orEmpty()),
                    style = MaterialTheme.typography.bodySmall)
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                else if (options == null || options.deliveryDates.isEmpty()) {
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_no_available_delivery_slots_check_3aef6ff),
                        color = MaterialTheme.colorScheme.error)
                } else {
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_new_date_1a67a4d), style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.deliveryDates.forEach { choice ->
                            FilterChip(selectedDateId == choice.id, { onDate(choice.id) }, label = { Text(choice.label) })
                        }
                    }
                    if (date != null) {
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_new_time_a8efdf1), style = MaterialTheme.typography.titleSmall)
                        slots.forEach { slot ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selectedSlotId == slot.id, { onSlot(slot.id) })
                                Text(slot.label)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reason, onValueChange = onReason, modifier = Modifier.fillMaxWidth(),
                        label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_reason_for_customer_8da50c1)) },
                        supportingText = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_10_200_characters_this_reason_69a908d)) },
                        minLines = 2, maxLines = 4, isError = reason.isNotEmpty() && reason.trim().length < 10,
                    )
                }
            }
        },
        confirmButton = { Button(onConfirm, enabled = valid && !loading && !busy) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_update_notify_2adefe6)) } },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_cancel_77dfd21)) } },
    )
}

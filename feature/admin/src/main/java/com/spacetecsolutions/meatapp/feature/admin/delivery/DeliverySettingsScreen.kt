package com.spacetecsolutions.meatapp.feature.admin.delivery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.DeliverySlot

@Composable
fun DeliverySettingsRoute(
    onBack: () -> Unit,
    onMessage: (DeliverySettingsMessage) -> Unit,
    viewModel: DeliverySettingsViewModel = hiltViewModel(),
) {
    HideAppBottomBar()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) { state.message?.let { onMessage(it); viewModel.consumeMessage() } }
    DeliverySettingsScreen(state, onBack, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliverySettingsScreen(
    state: DeliverySettingsUiState, onBack: () -> Unit, actions: DeliverySettingsViewModel,
) {
    var editor by remember { mutableStateOf<SlotEditorTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<DeliverySlot?>(null) }
    Scaffold(topBar = { AppBackTopBar("Delivery Settings", onBack) }) { padding ->
        when {
            state.loading -> ContentStateView(ContentState.Loading, contentPadding = padding)
            state.config == null -> ContentStateView(
                ContentState.Error(description = state.error), contentPadding = padding, onAction = actions::refresh)
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = AppDimensions.contentMaxWidth),
                contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            ) {
                item { SettingsCard("Delivery availability") {
                    if (state.realtimeTrackingAllowed) SettingSwitch(
                        "Realtime delivery tracking", state.config.realtimeTrackingEnabled,
                        !state.saving, actions::setRealtimeTracking,
                    )
                    SettingSwitch("Scheduled delivery", true, false, {})
                } }
                item { SettingsCard("Order values") {
                    MoneyField("Delivery charge", state.deliveryCharge, state.saving, actions::setCharge)
                    MoneyField("Free delivery threshold (optional)", state.freeThreshold, state.saving, actions::setThreshold)
                    MoneyField("Minimum order", state.minimumOrder, state.saving, actions::setMinimum)
                } }
                item { DeliveryTimingsCard(
                    state.config.slots, state.saving,
                    add = { editor = SlotEditorTarget() }, edit = { editor = SlotEditorTarget(it) },
                    remove = { deleteTarget = it },
                ) }
                item { Button(actions::save, Modifier.fillMaxWidth(), enabled = !state.saving) {
                    if (state.saving) CircularProgressIndicator(Modifier.size(AppDimensions.iconSmall))
                    else { Icon(AppIcons.Save, null); Spacer(Modifier.width(AppSpacing.small)); Text("Save Settings") }
                } }
            }
        }
    }
    editor?.let { target -> SlotEditorDialog(target, { editor = null }) { start, end, active ->
        target.slot?.let { actions.updateSlot(it.id, start, end, active) }
            ?: actions.addSlot(start, end, active)
        editor = null
    } }
    deleteTarget?.let { slot -> AlertDialog(
        onDismissRequest = { deleteTarget = null }, title = { Text("Delete delivery slot?") },
        text = { Text("Customers will no longer be able to choose ${slot.timeRange()}.") },
        confirmButton = { Button({ actions.removeSlot(slot.id); deleteTarget = null }) { Text("Delete") } },
        dismissButton = { TextButton({ deleteTarget = null }) { Text("Cancel") } },
    ) }
}

@Composable
private fun DeliveryTimingsCard(
    slots: List<DeliverySlot>, saving: Boolean, add: () -> Unit,
    edit: (DeliverySlot) -> Unit, remove: (DeliverySlot) -> Unit,
) = SettingsCard("Delivery Timings", action = {
    TextButton(add, enabled = !saving) { Text("+ Add Slot", color = MaterialTheme.colorScheme.primary) }
}) {
    if (slots.isEmpty()) Text("No delivery timings added", color = MaterialTheme.colorScheme.onSurfaceVariant)
    slots.sortedBy(DeliverySlot::startMinutes).forEachIndexed { index, slot ->
        Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.small), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(36.dp), shape = AppShapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Pending, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.small)) {
                Text(slot.timeRange(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(if (slot.active) "Orders before ${slot.cutoffLabel()}" else "Unavailable to customers",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (slot.active) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            }
            IconButton({ edit(slot) }, enabled = !saving) { Icon(AppIcons.Edit, "Edit ${slot.timeRange()}") }
            IconButton({ remove(slot) }, enabled = !saving) {
                Icon(AppIcons.Delete, "Delete ${slot.timeRange()}", tint = MaterialTheme.colorScheme.error)
            }
        }
        if (index != slots.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
    }
    Surface(Modifier.fillMaxWidth().padding(top = AppSpacing.small), shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .55f)) {
        Column(Modifier.padding(AppSpacing.medium)) {
            Text("These are the available delivery time slots", color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge)
            Text("Customers can choose from active timings.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class SlotEditorTarget(val slot: DeliverySlot? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotEditorDialog(target: SlotEditorTarget, dismiss: () -> Unit, save: (Int, Int, Boolean) -> Unit) {
    var start by remember { mutableIntStateOf(target.slot?.startMinutes ?: 8 * 60) }
    var end by remember { mutableIntStateOf(target.slot?.endMinutes ?: 10 * 60) }
    var active by remember { mutableStateOf(target.slot?.active ?: true) }
    var picker by remember { mutableStateOf<TimeTarget?>(null) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (target.slot == null) "Add Delivery Slot" else "Edit Delivery Slot") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
            TimeSelectionRow("Start time", start) { picker = TimeTarget.START }
            TimeSelectionRow("End time", end) { picker = TimeTarget.END }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Available to customers", Modifier.weight(1f))
                Switch(active,
                    { active = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF22A652),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFB7BBC1),
                        uncheckedBorderColor = Color.Transparent,
                        disabledCheckedThumbColor = Color.White.copy(alpha = .9f),
                        disabledCheckedTrackColor = Color(0xFF22A652).copy(alpha = .45f),
                        disabledUncheckedThumbColor = Color.White.copy(alpha = .9f),
                        disabledUncheckedTrackColor = Color(0xFFB7BBC1).copy(alpha = .55f),
                        disabledUncheckedBorderColor = Color.Transparent,
                    ))
            }
            if (start >= end) Text("End time must be after start time", color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { Button({ save(start, end, active) }, enabled = start < end) { Text("Save Slot") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } },
    )
    picker?.let { selected -> AppTimePicker(
        initial = if (selected == TimeTarget.START) start else end,
        dismiss = { picker = null },
    ) { minutes -> if (selected == TimeTarget.START) start = minutes else end = minutes; picker = null } }
}

private enum class TimeTarget { START, END }

@Composable
private fun TimeSelectionRow(label: String, value: Int, click: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = click)) {
        Row(Modifier.fillMaxWidth().padding(AppSpacing.medium), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(36.dp), shape = AppShapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Pending, null, Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = AppSpacing.medium)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value.clock(), style = MaterialTheme.typography.titleMedium)
            }
            Text("Select", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTimePicker(initial: Int, dismiss: () -> Unit, confirm: (Int) -> Unit) {
    val picker = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = false)
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("Select time") }, text = { TimePicker(picker) },
        confirmButton = { TextButton({ confirm(picker.hour * 60 + picker.minute) }) { Text("OK") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SettingsCard(title: String, action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(AppSpacing.medium)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(
            checked,
            change,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF22A652),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFB7BBC1),
                uncheckedBorderColor = Color.Transparent,
                disabledCheckedThumbColor = Color.White.copy(alpha = .9f),
                disabledCheckedTrackColor = Color(0xFF22A652).copy(alpha = .45f),
                disabledUncheckedThumbColor = Color.White.copy(alpha = .9f),
                disabledUncheckedTrackColor = Color(0xFFB7BBC1).copy(alpha = .55f),
                disabledUncheckedBorderColor = Color.Transparent,
            ))
    }
}

@Composable
private fun MoneyField(label: String, value: String, saving: Boolean, change: (String) -> Unit) = OutlinedTextField(
    value, change, Modifier.fillMaxWidth(), enabled = !saving, label = { Text(label) }, prefix = { Text("₹") },
    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
)

private fun DeliverySlot.timeRange() = "${startMinutes.clock()} - ${endMinutes.clock()}"
private fun DeliverySlot.cutoffLabel() = Math.floorMod(startMinutes - 60, 24 * 60).clock()
private fun Int.clock(): String {
    val hour = (this / 60) % 24; val minute = this % 60; val display = if (hour % 12 == 0) 12 else hour % 12
    return "%d:%02d %s".format(display, minute, if (hour < 12) "AM" else "PM")
}

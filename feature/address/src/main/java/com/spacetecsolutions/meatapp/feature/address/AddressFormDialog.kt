package com.spacetecsolutions.meatapp.feature.address

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.component.appSwitchColors
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerAddressTokens as T
import com.spacetecsolutions.meatapp.core.model.AddressType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddressFormDialog(form: AddressFormState, submitting: Boolean,
    verification: AddressVerification, update: ((AddressFormState) -> AddressFormState) -> Unit,
    onDismiss: () -> Unit, onSave: () -> Unit, onUseCurrentLocation: () -> Unit,
    onChooseOnMap: () -> Unit, onDelete: (() -> Unit)? = null) {
    androidx.activity.compose.BackHandler(enabled = !submitting, onBack = onDismiss)
    com.spacetecsolutions.meatapp.core.designsystem.component.HideAppBottomBar()
    Scaffold(
        containerColor = T.canvas, topBar = {
        TopAppBar(title = { Text(if (form.editing) "Edit Delivery Location" else "Add New Address",
            fontWeight = FontWeight.Bold) }, navigationIcon = {
            IconButton({ if (!submitting) onDismiss() }) { Icon(AppIcons.Back, "Back") }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = T.surface))
    }, bottomBar = {
        Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onDismiss, Modifier.weight(1f).height(52.dp), enabled = !submitting,
                    colors = ButtonDefaults.buttonColors(containerColor = T.soft, contentColor = T.ink),
                    shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
                Button(onSave, Modifier.weight(1.5f).height(52.dp), enabled = !submitting,
                    colors = ButtonDefaults.buttonColors(containerColor = T.red),
                    shape = RoundedCornerShape(10.dp)) {
                    if (submitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = T.surface)
                    else { Icon(AppIcons.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text(if (form.editing) "Update Address" else "Save Address") }
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()
            .wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = AppDimensions.formMaxWidth)
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AddressLocationPreview(form, !submitting, onChooseOnMap, onUseCurrentLocation)
            Text(if (form.verifiedLocation) "Delivery pin selected. Review the address details."
                else "Choose a pin or enter the geographic address below.",
                style = MaterialTheme.typography.bodySmall, color = T.muted)
            /*Button(onUseCurrentLocation, Modifier.fillMaxWidth().height(48.dp), enabled = !submitting,
                colors = ButtonDefaults.buttonColors(containerColor = T.rose, contentColor = T.red),
                shape = RoundedCornerShape(10.dp)) {
                Icon(AppIcons.LiveTracking, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text("Use Current Location")
            }*/
            AddressTypePicker(form.type, submitting) { type -> update { it.copy(type = type) } }
            if (!form.editing) {
                AddressGroup("Recipient Details") {
                    AddressFormFields(form, submitting, update, recipient = true, onChooseOnMap)
                }
                AddressGroup("Address Specifics") {
                    AddressFormFields(form, submitting, update, recipient = false, onChooseOnMap)
                }
            } else {
                AddressFormFields(form, submitting, update, recipient = true, onChooseOnMap)
                AddressFormFields(form, submitting, update, recipient = false, onChooseOnMap)
            }
            Surface(shape = RoundedCornerShape(16.dp), color = T.surface,
                border = BorderStroke(1.dp, T.border)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Set as default address", fontWeight = FontWeight.SemiBold, color = T.ink)
                        Text("Used automatically for quick checkout",
                            style = MaterialTheme.typography.bodySmall, color = T.muted)
                    }
                    Switch(form.makeDefault, { value -> update { it.copy(makeDefault = value) } },
                        enabled = !submitting && !form.makeDefault,
                        colors = appSwitchColors())
                }
            }
            if (form.editing && onDelete != null) TextButton(onDelete,
                Modifier.align(Alignment.CenterHorizontally), enabled = !submitting) {
                Icon(AppIcons.Delete, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp))
                Text("Delete this address")
            }
            if (verification == AddressVerification.VALIDATING)
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = T.red)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AddressGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = T.surface,
        border = BorderStroke(1.dp, T.border)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = T.ink)
            content()
        }
    }
}

@Composable
private fun AddressTypePicker(selected: AddressType, disabled: Boolean, onSelect: (AddressType) -> Unit) {
    Text("ADDRESS TYPE", style = MaterialTheme.typography.labelMedium, color = T.muted)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AddressType.entries.forEach { type ->
            val active = selected == type
            Button({ onSelect(type) }, Modifier.weight(1f).height(44.dp), enabled = !disabled,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = if (active) T.red else T.soft,
                    contentColor = if (active) T.surface else T.ink)) {
                Icon(when (type) { AddressType.HOME -> AppIcons.HomeAddress
                    AddressType.WORK -> AppIcons.WorkAddress; AddressType.OTHER -> AppIcons.Location },
                    null, Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(type.name.lowercase().replaceFirstChar(Char::titlecase), maxLines = 1)
            }
        }
    }
}

@Composable
private fun AddressFormFields(form: AddressFormState, disabled: Boolean,
    update: ((AddressFormState) -> AddressFormState) -> Unit, recipient: Boolean,
    onChooseOnMap: () -> Unit) {
    if (recipient) {
        AddressField("Full Name", form.name, form.errors["name"], disabled) { value ->
            update { it.copy(name = value) } }
        AddressField("Mobile Number", form.mobile, form.errors["mobile"], disabled,
            KeyboardType.Phone) { value -> update { it.copy(mobile = value) } }
    } else {
        AddressField(if (form.editing) "Complete Address" else "Flat / House / Building / Street",
            form.address, form.errors["address"], disabled, singleLine = false) { value ->
            update { it.copy(address = value) } }
        if (form.errors["address"]?.contains("Google Maps") == true ||
            form.errors["address"]?.contains("verify") == true)
            TextButton(onChooseOnMap, enabled = !disabled) {
                Icon(AppIcons.Map, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text("Choose on Map")
            }
        AddressField("Landmark (Optional)", form.landmark, form.errors["landmark"], disabled) { value ->
            update { it.copy(landmark = value) } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { AddressField("City", form.city, form.errors["city"], disabled) { value ->
                update { it.copy(city = value) } } }
            Box(Modifier.weight(1f)) { AddressField("State", form.state, form.errors["state"], disabled) { value ->
                update { it.copy(state = value) } } }
        }
        AddressField("Postal Code (PIN)", form.postalCode, form.errors["postalCode"], disabled,
            KeyboardType.Number) { value -> update { it.copy(postalCode = value) } }
    }
}

@Composable
private fun AddressField(label: String, value: String, error: String?, disabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true,
    onValue: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, color = T.ink)
        OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), enabled = !disabled,
            isError = error != null, singleLine = singleLine,
            minLines = if (singleLine) 1 else 2, maxLines = if (singleLine) 1 else 4,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = T.soft,
                focusedContainerColor = T.surface, unfocusedBorderColor = T.border,
                focusedBorderColor = T.red), shape = RoundedCornerShape(10.dp))
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error) }
    }
}

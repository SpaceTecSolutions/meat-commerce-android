package com.spacetecsolutions.meatapp.feature.checkout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.PrimaryButton
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerCommerceTokens as T
import com.spacetecsolutions.meatapp.core.model.*
import java.text.NumberFormat
import java.util.Currency

@Composable
internal fun CheckoutStepIndicator(activeStep: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = AppSpacing.small,
            vertical = AppSpacing.medium),
        verticalAlignment = Alignment.Top,
    ) {
        StepMarker(1, "Address", activeStep, Modifier.width(72.dp))
        StepLine(activeStep >= 2)
        StepMarker(2, if (activeStep == 2) "Delivery" else "Review",
            activeStep, Modifier.width(72.dp))
        StepLine(activeStep >= 3)
        StepMarker(3, when (activeStep) { 1 -> "Payment"; 2 -> "Payment"; else -> "Confirm" },
            activeStep, Modifier.width(72.dp))
    }
}

@Composable
private fun StepMarker(number: Int, label: String, activeStep: Int, modifier: Modifier) {
    val active = number == activeStep
    val complete = number < activeStep
    val color by animateColorAsState(
        if (active) T.red else if (complete) T.green else T.muted,
        tween(AppMotion.MICRO_MILLIS), label = "checkout step",
    )
    val pulse = rememberInfiniteTransition(label = "checkout step marker")
    val arrowScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse),
        label = "checkout arrow scale",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .scale(if (active) arrowScale else 1f)
                .size(28.dp).clip(CircleShape)
                .background(if (active || complete) color else T.soft),
            contentAlignment = Alignment.Center,
        ) {
            if (complete) Icon(AppIcons.Check, null, Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimary)
            else Text("$number", style = MaterialTheme.typography.labelSmall,
                color = if (active) MaterialTheme.colorScheme.onPrimary else color,
                fontWeight = FontWeight.Bold)
        }
        Text(label, Modifier.padding(top = AppSpacing.extraSmall),
            style = MaterialTheme.typography.labelSmall, color = color,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun RowScope.StepLine(active: Boolean) {
    val color by animateColorAsState(
        if (active) T.green else T.border,
        tween(AppMotion.MICRO_MILLIS), label = "checkout line",
    )
    HorizontalDivider(Modifier.weight(1f).padding(top = 14.dp), color = color)
}

@Composable
internal fun CheckoutAddressSection(address: CustomerAddress?, change: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Delivery Address", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = T.ink)
        if (address?.latitude != null && address.longitude != null) Text("✓ Verified",
            style = MaterialTheme.typography.labelSmall, color = T.green)
    }
    Surface(shape = AppShapes.small, color = T.surface,
        border = BorderStroke(1.dp, T.border)) {
        Row(Modifier.fillMaxWidth().padding(AppSpacing.medium), verticalAlignment = Alignment.Top) {
            if (address == null) {
                Text("No delivery address selected", Modifier.weight(1f), color = T.muted)
                TextButton(change, contentPadding = PaddingValues(horizontal = AppSpacing.small)) { Text("Add Address") }
            } else {
                Icon(if (address.type == AddressType.HOME) AppIcons.HomeAddress else AppIcons.Location,
                    null, Modifier.size(19.dp), tint = T.red)
                Column(Modifier.weight(1f).padding(start = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(address.type.display(), style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = T.ink)
                    Text(address.formatted(), style = MaterialTheme.typography.bodySmall, color = T.muted)
                    Text(address.mobile, style = MaterialTheme.typography.bodySmall, color = T.muted)
                }
                TextButton(change, contentPadding = PaddingValues(horizontal = AppSpacing.small)) {
                    Text("Change", color = T.red)
                }
            }
        }
    }
}

@Composable
internal fun CheckoutDeliveryAddressSummary(address: CustomerAddress) {
    var expanded by remember { mutableStateOf(false) }
    Surface(onClick = { expanded = !expanded }, shape = AppShapes.small, color = T.surface,
        border = BorderStroke(1.dp, T.border)) {
        Column(Modifier.fillMaxWidth().padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (address.type == AddressType.HOME) AppIcons.HomeAddress else AppIcons.Location,
                    null, tint = T.red)
                Column(Modifier.weight(1f).padding(horizontal = AppSpacing.small)) {
                    Text(address.type.display(), fontWeight = FontWeight.Bold, color = T.ink)
                    Text("${address.name} • ${address.mobile}", style = MaterialTheme.typography.bodySmall,
                        color = T.muted, maxLines = 1)
                }
                Icon(AppIcons.ArrowDown,
                    if (expanded) "Hide full address" else "Show full address",
                    Modifier.rotate(if (expanded) 180f else 0f), tint = T.muted)
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    HorizontalDivider(color = T.border)
                    Text(address.formatted(), style = MaterialTheme.typography.bodySmall, color = T.ink)
                    address.landmark.takeIf(String::isNotBlank)?.let {
                        Text("Landmark: $it", style = MaterialTheme.typography.labelSmall, color = T.muted)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DeliveryInstructionsField(value: String, change: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.small), verticalAlignment = Alignment.CenterVertically) {
        Text("Delivery Instructions", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = T.ink)
        Text("Optional", style = MaterialTheme.typography.labelSmall, color = T.muted)
    }
    Surface(shape = AppShapes.small, color = T.surface, border = BorderStroke(1.dp, T.border)) {
        Column(Modifier.fillMaxWidth().padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            OutlinedTextField(value = value, onValueChange = change, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Any notes for your delivery partner?") },
                minLines = 2, maxLines = 3, shape = AppShapes.small,
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = T.soft,
                    focusedContainerColor = T.soft, unfocusedBorderColor = T.soft),
                supportingText = if (value.length >= 250) ({ Text("${value.length}/300") }) else null)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Leave at door", "Ring bell once", "Call on arrival").forEach { suggestion ->
                    FilterChip(onClick = { change(suggestion) },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                        selected = value == suggestion)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeliveryDateSelector(
    dates: List<CheckoutDeliveryDate>, selectedId: String?, select: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = dates.firstOrNull { it.id == selectedId }
    Text("Select Preferred Time", style = MaterialTheme.typography.labelMedium, color = T.muted)
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(value = selected?.label.orEmpty(), onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text("Delivery Date") },
            leadingIcon = { Icon(AppIcons.Calendar, null, tint = T.red) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = AppShapes.small,
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = T.surface,
                focusedContainerColor = T.surface, unfocusedBorderColor = T.border))
        ExposedDropdownMenu(expanded, { expanded = false }) {
            dates.forEach { date -> DropdownMenuItem(
                text = { Text(date.label) },
                onClick = { select(date.id); expanded = false },
            ) }
        }
    }
}

@Composable
internal fun DeliverySlotGrid(
    slots: List<DeliverySlot>, selectedId: String?, select: (String) -> Unit,
) {
    if (slots.isEmpty()) {
        Text("No delivery slots available for this date.", color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        Text("Available Slots", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = T.ink)
        slots.chunked(2).forEach { row -> Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            row.forEach { slot -> DeliverySlotButton(slot, slot.id == selectedId, select, Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        } }
    }
}

@Composable
private fun DeliverySlotButton(
    slot: DeliverySlot, selected: Boolean, select: (String) -> Unit, modifier: Modifier,
) {
    val container by animateColorAsState(
        if (selected) T.red else T.surface,
        tween(AppMotion.MICRO_MILLIS), label = "slot background",
    )
    val content by animateColorAsState(
        if (selected) T.surface else T.ink,
        tween(AppMotion.MICRO_MILLIS), label = "slot text",
    )
    Surface(
        modifier.heightIn(min = 44.dp).clickable { select(slot.id) }, shape = AppShapes.extraSmall,
        color = container, contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, T.border),
    ) { Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = AppSpacing.compact),
        contentAlignment = Alignment.Center) {
        Text(slot.label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    } }
}

@Composable
internal fun CheckoutBottomAction(text: String, enabled: Boolean, loading: Boolean = false,
    total: String? = null, action: () -> Unit) {
    Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
            .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            if (total != null) Column(Modifier.widthIn(min = 88.dp)) {
                Text("TOTAL PAYABLE", style = MaterialTheme.typography.labelSmall, color = T.muted)
                Text(total, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = T.ink)
            }
            Button(onClick = action, enabled = enabled && !loading,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = AppShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = T.red)) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(text, fontWeight = FontWeight.Bold)
                if (!loading) Icon(AppIcons.ArrowRight, null, Modifier.padding(start = 6.dp).size(18.dp))
            }
        }
    }
}

@Composable
internal fun CheckoutAddressBottomAction(address: CustomerAddress?, onContinue: () -> Unit) {
    Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            Button(onClick = onContinue, enabled = address != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = AppShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = T.red)) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text("DELIVER TO", style = MaterialTheme.typography.labelSmall,
                        color = T.surface.copy(alpha = .8f))
                    Text(address?.let { "${it.type.display()} (${it.name})" } ?: "Select address",
                        maxLines = 1, fontWeight = FontWeight.Bold)
                }
                Text("Continue", fontWeight = FontWeight.Bold)
                Icon(AppIcons.ArrowRight, null, Modifier.padding(start = 5.dp).size(18.dp))
            }
        }
    }
}

@Composable
internal fun CheckoutHeading(text: String) = Text(
    text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = AppSpacing.small)
)

internal fun CustomerAddress.formatted() = listOf(address, landmark, city, state, postalCode)
    .filter(String::isNotBlank).joinToString(", ")
internal fun AddressType.display() = name.lowercase().replaceFirstChar(Char::titlecase)
internal fun CheckoutPaymentMethod.label() = when (this) {
    CheckoutPaymentMethod.COD -> "Cash on Delivery (COD)"
    CheckoutPaymentMethod.RAZORPAY -> "Razorpay"
    CheckoutPaymentMethod.UPI -> "UPI"
}
internal fun ProductUnit.display() = when (this) {
    ProductUnit.KILOGRAM -> "kg"
    ProductUnit.GRAM -> "g"
    ProductUnit.PIECE -> "piece"
    ProductUnit.PACK -> "pack"
}
internal fun Long.money(currencyCode: String) = NumberFormat.getCurrencyInstance().apply {
    currency = Currency.getInstance(currencyCode); maximumFractionDigits = 0
}.format(this / 100.0)

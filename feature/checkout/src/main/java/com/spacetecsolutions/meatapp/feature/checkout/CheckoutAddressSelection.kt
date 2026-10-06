package com.spacetecsolutions.meatapp.feature.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerCommerceTokens as T
import com.spacetecsolutions.meatapp.core.model.AddressType
import com.spacetecsolutions.meatapp.core.model.CustomerAddress

@Composable
internal fun CheckoutAddressSelection(state: CheckoutUiState, padding: PaddingValues,
    manageAddresses: () -> Unit, select: (String) -> Unit,
    editAddress: (String) -> Unit, deleteAddress: (String) -> Unit) {
    val quote = state.quote ?: return
    Box(Modifier.fillMaxSize().background(T.canvas).padding(padding), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.fillMaxSize().widthIn(max = AppDimensions.formMaxWidth),
            contentPadding = PaddingValues(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { CheckoutStepIndicator(1) }
            quote.cart.deliveryEstimate?.takeIf(String::isNotBlank)?.let { estimate -> item {
                Surface(shape = RoundedCornerShape(14.dp), color = T.rose) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Delivery, null, Modifier.size(22.dp), tint = T.red)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Express delivery", fontWeight = FontWeight.Bold, color = T.ink)
                            Text(estimate, style = MaterialTheme.typography.bodySmall, color = T.muted)
                        }
                    }
                }
            } }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Saved Locations", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = T.ink)
                        Text("Choose where we should deliver", style = MaterialTheme.typography.bodySmall,
                            color = T.muted)
                    }
                    Surface(shape = CircleShape, color = T.soft) {
                        Text("${quote.addresses.size} saved", Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall, color = T.muted)
                    }
                }
            }
            if (quote.addresses.isEmpty()) item {
                Column(Modifier.fillParentMaxHeight()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Add a delivery address to continue.", color = T.muted)
                        TextButton(onClick = manageAddresses, contentPadding = PaddingValues(horizontal = 4.dp) ){
                                Text("+ Add", color = T.red,)
                            }

                    }
                    ContentStateView(ContentState.Empty(title = "No saved addresses", description = "Add an address for delivery."))
                }
            }
            items(quote.addresses, key = CustomerAddress::id) { address ->
                CheckoutSelectableAddress(address, address.id == state.addressId,
                    onSelect = { select(address.id) }, onEdit = { editAddress(address.id) })
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
internal fun CheckoutSelectableAddress(address: CustomerAddress, selected: Boolean,
    onSelect: () -> Unit, onEdit: () -> Unit = {}, onDelete: () -> Unit = {},
    showActions: Boolean = true) {
    Surface(onClick = onSelect, shape = RoundedCornerShape(18.dp),
        color = T.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) T.red else T.border),
        shadowElevation = if (selected) 0.dp else 0.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(if (selected) T.red else T.soft),
                    contentAlignment = Alignment.Center) {
                    Icon(when (address.type) {
                        AddressType.HOME -> AppIcons.HomeAddress
                        AddressType.WORK -> AppIcons.WorkAddress
                        AddressType.OTHER -> AppIcons.Location
                    }, null, Modifier.size(19.dp), tint = if (selected) T.surface else T.muted)
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(address.type.display(), fontWeight = FontWeight.Bold, color = T.ink)
                        if (address.isDefault) Text("DEFAULT", Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = T.red)
                    }
                    if (selected) Text("Active delivery destination",
                        style = MaterialTheme.typography.labelSmall, color = T.red)
                }
                if (showActions) RadioButton(selected = selected, onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = T.red))
            }
            Surface(shape = RoundedCornerShape(9.dp), color = T.soft) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(address.name, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall, color = T.ink)
                    Spacer(Modifier.width(8.dp))
                    Text(address.mobile, style = MaterialTheme.typography.bodySmall, color = T.muted)
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Icon(AppIcons.Location, null, Modifier.size(17.dp), tint = T.red)
                Text(listOf(address.address, address.city, address.state, address.postalCode)
                    .filter(String::isNotBlank).joinToString(", "), Modifier.padding(start = 7.dp),
                    style = MaterialTheme.typography.bodySmall, color = T.ink,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            address.landmark.takeIf(String::isNotBlank)?.let { landmark ->
                Surface(shape = RoundedCornerShape(7.dp), color = T.soft) {
                    Text("Landmark: $landmark", Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall, color = T.muted)
                }
            }
            if (showActions) {
                HorizontalDivider(color = T.border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onEdit) {
                        Icon(AppIcons.Edit, null, Modifier.size(16.dp))
                        Text("Edit", Modifier.padding(start = 5.dp))
                    }
                }
            }
        }
    }
}

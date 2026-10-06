package com.spacetecsolutions.meatapp.feature.address

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerAddressTokens as T
import com.spacetecsolutions.meatapp.core.model.AddressType
import com.spacetecsolutions.meatapp.core.model.CustomerAddress

@Composable
internal fun SelectableAddressCard(address: CustomerAddress, selected: Boolean, busy: Boolean,
    selectionMode: Boolean, select: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
    onDefault: () -> Unit) {
    val highlighted = selectionMode && selected
    Card(modifier = Modifier.fillMaxWidth().clickable(enabled = selectionMode && !busy,
        onClick = select), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (highlighted) T.rose else T.surface),
        border = BorderStroke(if (highlighted) 2.dp else 1.dp,
            if (highlighted) T.red else T.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (highlighted || address.isDefault) T.rose else T.soft,
                    shape = RoundedCornerShape(50)) {
                    Icon(typeIcon(address.type), null, Modifier.padding(12.dp).size(20.dp),
                        tint = if (highlighted || address.isDefault) T.red else T.muted)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(typeLabel(address.type), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = T.ink)
                        if (address.isDefault) Surface(color = T.rose,
                            shape = RoundedCornerShape(50)) {
                            Text("DEFAULT", Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, color = T.red)
                        }
                    }
                    Text(if (highlighted) "Active delivery destination" else if (address.isDefault)
                        "Preferred address" else "Set as Default",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (highlighted || address.isDefault) T.red else T.muted)
                }
                if (selectionMode) RadioButton(highlighted, select, enabled = !busy,
                    colors = RadioButtonDefaults.colors(selectedColor = T.red))
            }
            Surface(shape = RoundedCornerShape(10.dp), color = T.soft) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(address.name, Modifier.weight(1f), maxLines = 1,
                        fontWeight = FontWeight.SemiBold)
                    Text(address.mobile, style = MaterialTheme.typography.bodySmall,
                        color = T.muted, maxLines = 1)
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Icon(AppIcons.Location, null, Modifier.size(18.dp), tint = T.red)
                Spacer(Modifier.width(8.dp))
                Text(listOf(address.address, address.city, address.state, address.postalCode)
                    .filter(String::isNotBlank).joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium, color = T.ink)
            }
            if (address.landmark.isNotBlank()) Text("Landmark: " + address.landmark,
                Modifier.padding(start = 26.dp), style = MaterialTheme.typography.bodySmall,
                color = T.muted)
            HorizontalDivider(color = T.border)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!address.isDefault) TextButton(onDefault, enabled = !busy,
                    contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Set Default", color = T.red, style = MaterialTheme.typography.labelMedium)
                } else Spacer(Modifier.weight(1f))
                if (!address.isDefault) Spacer(Modifier.weight(1f))
                TextButton(onEdit, enabled = !busy) {
                    Icon(AppIcons.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp)); Text("Edit")
                }
                TextButton(onDelete, enabled = !busy) {
                    Icon(AppIcons.Delete, null, Modifier.size(16.dp), tint = T.red)
                    Spacer(Modifier.width(4.dp)); Text("Delete", color = T.red)
                }
            }
        }
    }
}

private fun typeLabel(type: AddressType) = type.name.lowercase().replaceFirstChar(Char::titlecase)
private fun typeIcon(type: AddressType) = when (type) {
    AddressType.HOME -> AppIcons.HomeAddress
    AddressType.WORK -> AppIcons.WorkAddress
    AddressType.OTHER -> AppIcons.Location
}

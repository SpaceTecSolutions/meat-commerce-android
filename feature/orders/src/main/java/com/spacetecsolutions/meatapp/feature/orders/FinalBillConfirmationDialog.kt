package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.model.*

@Composable
internal fun FinalBillConfirmationDialog(
    order: CodOrder,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<FinalWeightAdjustment>) -> Unit,
) {
    val weighted = remember(order) { order.items.filter { it.unit == ProductUnit.KILOGRAM || it.unit == ProductUnit.GRAM } }
    val values = remember(order) { mutableStateMapOf<String, String>().apply {
        weighted.forEach { put(it.productId, it.quantity.toString()) }
    } }
    val valid = values.values.all { it.toDoubleOrNull()?.let { value -> value > 0 && value <= 1000 } == true }
    val adjustedSubtotal = order.items.sumOf { item ->
        val quantity = values[item.productId]?.toDoubleOrNull() ?: item.quantity.toDouble()
        (item.unitPriceMinor * quantity).toLong()
    }
    val adjustedTotal = (adjustedSubtotal - order.discountMinor + order.deliveryFeeMinor + order.taxMinor)
        .coerceAtLeast(0)
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Review final bill") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Confirm the packed weight before accepting. The customer is charged only after this final bill is saved.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (weighted.isEmpty()) item { Text("No weight-based items require adjustment.") }
                items(weighted, key = { it.productId }) { item ->
                    val quantity = values[item.productId]?.toDoubleOrNull()
                    val lineTotal = quantity?.let { (item.unitPriceMinor * it).toLong() }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
                            Text("Estimated ${item.quantity} ${item.unit.weightLabel()} • ${item.lineTotalMinor.money(order.currencyCode)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = values[item.productId].orEmpty(),
                                onValueChange = { values[item.productId] = it.filter { char -> char.isDigit() || char == '.' }.take(8) },
                                label = { Text("Final ${item.unit.weightLabel()}") },
                                suffix = { Text(item.unit.weightLabel()) },
                                supportingText = { if (lineTotal != null) Text("Updated item total: ${lineTotal.money(order.currencyCode)}") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                            )
                        }
                    }
                }
                item {
                    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BillPreviewRow("Original estimate", order.totalMinor.money(order.currencyCode), false)
                            BillPreviewRow("Final amount", adjustedTotal.money(order.currencyCode), true)
                            Text("Discount, delivery and tax already included.", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(
            onClick = { onConfirm(weighted.mapNotNull { item -> values[item.productId]?.toDoubleOrNull()
                ?.let { FinalWeightAdjustment(item.productId, it) } }) },
            enabled = valid && !busy,
        ) { Text("Finalize & confirm") } },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

@Composable
private fun BillPreviewRow(label: String, value: String, emphasized: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

private fun ProductUnit.weightLabel() = if (this == ProductUnit.GRAM) "g" else "kg"

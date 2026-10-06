package com.spacetecsolutions.meatapp.feature.admin.banner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun BannerPlacementControls(form: BannerForm, actions: BannerManagementViewModel) {
    listOf("Text", "Message", "Button").forEach { target ->
        Text("$target alignment", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LEFT", "CENTER", "RIGHT").forEachIndexed { index, alignment ->
                val current = when (target) {
                    "Text" -> form.textPlacement
                    "Message" -> form.subtitlePlacement
                    else -> form.buttonPlacement
                }
                FilterChip(selected = current.alignment == alignment, onClick = {
                    val placement = current.copy(x = index / 2f, alignment = alignment)
                    actions.update { when (target) {
                        "Text" -> it.copy(textPlacement = placement)
                        "Message" -> it.copy(subtitlePlacement = placement)
                        else -> it.copy(buttonPlacement = placement)
                    } }
                }, label = { Text(alignment.lowercase().replaceFirstChar(Char::titlecase)) })
            }
        }
    }
}

@Composable
internal fun BannerDestinationPicker(form: BannerForm, actions: BannerManagementViewModel) {
    val state by actions.state.collectAsStateWithLifecycle()
    val categories = state.products.map { it.categoryId to it.categoryName }.distinctBy { it.first }.sortedBy { it.second }
    Choice("Category", categories.firstOrNull { it.first == form.categoryId }?.second ?: "All products",
        listOf("" to "All products") + categories) { id -> actions.update { it.copy(categoryId = id, productId = "") } }
    val products = state.products.filter { form.categoryId.isBlank() || it.categoryId == form.categoryId }
        .sortedBy { it.name }.map { it.id to it.name }
    Choice("Product", products.firstOrNull { it.first == form.productId }?.second ?: "Open category / all products",
        listOf("" to "Open category / all products") + products) { id -> actions.update { it.copy(productId = id) } }
}

@Composable
private fun Choice(label: String, selected: String, options: List<Pair<String, String>>, select: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ open = true }, Modifier.fillMaxWidth()) { Text("$label: $selected") }
        DropdownMenu(open, { open = false }, modifier = Modifier.heightIn(max = 320.dp)) {
            options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { select(id); open = false }) }
        }
    }
}

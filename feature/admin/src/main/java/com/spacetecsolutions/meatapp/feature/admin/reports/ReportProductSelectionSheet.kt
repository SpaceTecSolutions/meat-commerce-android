package com.spacetecsolutions.meatapp.feature.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.RoundedSquareImage
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportProductSelectionSheet(
    products: List<Product>, selectedId: String?, dismiss: () -> Unit,
    select: (String?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var choice by remember(selectedId) { mutableStateOf(selectedId) }
    val visible = remember(products, query) {
        products.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = ReportsStyle.surface) {
        Column(Modifier.fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth)
            .align(Alignment.CenterHorizontally).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Select Product", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = ReportsStyle.ink)
                    Text("Filter sales and volume reports by item", style = MaterialTheme.typography.bodySmall,
                        color = ReportsStyle.muted)
                }
                IconButton(dismiss) { Icon(AppIcons.Close, "Close product selection") }
            }
            HorizontalDivider(Modifier.padding(top = 16.dp), color = ReportsStyle.border)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(20.dp),
                placeholder = { Text("Search products...") }, singleLine = true,
                leadingIcon = { Icon(AppIcons.Search, null) }, shape = RoundedCornerShape(14.dp))
            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 480.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { ProductChoiceRow("All Products", "Show consolidated data", null,
                    choice == null, null) { choice = null } }
                items(visible, key = Product::id) { product ->
                    ProductChoiceRow(product.name, null, product.imageUrls.firstOrNull(),
                        choice == product.id, product.id) { choice = product.id }
                }
                if (visible.isEmpty()) item {
                    Text("No matching products", Modifier.padding(16.dp), color = ReportsStyle.muted)
                }
            }
            HorizontalDivider(Modifier.padding(top = 14.dp), color = ReportsStyle.border)
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(dismiss, Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = ReportsStyle.pillShape) { Text("Cancel") }
                Button({ select(choice) }, Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ReportsStyle.red),
                    shape = ReportsStyle.pillShape) { Text("Apply Product") }
            }
        }
    }
}

@Composable
private fun ProductChoiceRow(name: String, subtitle: String?, imageUrl: String?,
    selected: Boolean, id: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(if (selected) ReportsStyle.rose else ReportsStyle.surface,
        RoundedCornerShape(14.dp)).border(1.dp, if (selected) ReportsStyle.rose else ReportsStyle.border,
        RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (id == null) Box(Modifier.size(38.dp).background(ReportsStyle.surface, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) {
            Text("ALL", fontWeight = FontWeight.Bold, color = ReportsStyle.red)
        } else RoundedSquareImage(imageUrl, name, 38.dp, cornerRadius = 10.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = ReportsStyle.ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = if (selected) ReportsStyle.red else ReportsStyle.muted)
        }
        RadioButton(selected, onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = ReportsStyle.red))
    }
}

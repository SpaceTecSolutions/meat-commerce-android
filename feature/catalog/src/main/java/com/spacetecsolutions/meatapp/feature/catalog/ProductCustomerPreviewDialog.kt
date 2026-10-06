package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImageCarousel
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun ProductCustomerPreviewDialog(form: ProductFormState, dismiss: () -> Unit) {
    val images = form.existingImages + form.selectedImageUris
    Dialog(onDismissRequest = dismiss) {
        Surface(shape = AppShapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState())) {
                AppImageCarousel(images, form.name.ifBlank { "Product preview" },
                    Modifier.fillMaxWidth().aspectRatio(1.2f), autoSlide = images.size > 1)
                Column(Modifier.padding(AppSpacing.large), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CUSTOMER PREVIEW", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(form.name.ifBlank { "Product name" }, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text(form.unit.shortLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(form.displayPrice(), style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    if (form.description.isNotBlank()) Text(form.description)
                    form.attributes.filter { it.key.isNotBlank() && it.value.isNotBlank() }.forEach {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(it.key, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(it.value, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(dismiss, Modifier.fillMaxWidth()) { Text("Close preview") }
                }
            }
        }
    }
}

private fun ProductFormState.displayPrice(): String {
    val value = offerPrice.toDoubleOrNull() ?: price.toDoubleOrNull() ?: 0.0
    return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
}

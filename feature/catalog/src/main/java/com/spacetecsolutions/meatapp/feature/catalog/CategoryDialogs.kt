package com.spacetecsolutions.meatapp.feature.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppBackTopBar
import com.spacetecsolutions.meatapp.core.designsystem.component.RoundImage
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.ProductCategory

@Composable
internal fun CategoryFormScreen(form: CategoryFormState, submitting: Boolean, actions: CategoryFormActions) {
    val context = LocalContext.current
    var cropSource by remember { mutableStateOf<Uri?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageError = validateSelectedImage(context, it)
            if (imageError == null) cropSource = it
        }
    }
    cropSource?.let { source -> ImageCropDialog(source, circular = true, onDismiss = { cropSource = null }) {
        actions.onImage(it); cropSource = null
    } }
    Scaffold(
        topBar = { AppBackTopBar(if (form.editing) "Edit Category" else "Add Category", actions.onDismiss) },
        bottomBar = {
            Button(
                actions.onSave, Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                enabled = !submitting, shape = AppShapes.small,
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(AppSpacing.large), strokeWidth = 2.dp)
                else Text(if (form.editing) "Update Category" else "Save Category")
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth).verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
            ) {
                val image = form.selectedImageUri ?: form.existingImageUrl
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    RoundImage(image, "Category image", 144.dp)
                    TextButton({ picker.launch("image/*") }, enabled = !submitting) {
                        Icon(AppIcons.Camera, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(AppSpacing.extraSmall))
                        Text(if (image == null) "Add Photo" else "Change Photo")
                    }
                    imageError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall) }
                }
                OutlinedTextField(
                    form.name, actions.onName, Modifier.fillMaxWidth(), label = { Text("Category Name") },
                    singleLine = true, isError = form.nameError != null,
                    supportingText = form.nameError?.let { { Text(it) } }, enabled = !submitting,
                    shape = AppShapes.extraSmall,
                )
                OutlinedTextField(
                    form.description, actions.onDescription, Modifier.fillMaxWidth(), label = { Text("Description") },
                    minLines = 3, maxLines = 5, enabled = !submitting, shape = AppShapes.extraSmall,
                )
                OutlinedTextField(
                    form.sortOrder, actions.onSortOrder, Modifier.fillMaxWidth(), label = { Text("Display Order") },
                    singleLine = true, isError = form.sortOrderError != null,
                    supportingText = form.sortOrderError?.let { { Text(it) } }, enabled = !submitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = AppShapes.extraSmall,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Status", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (form.active) "Active" else "Inactive", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        form.active,
                        actions.onActive,
                        enabled = !submitting,
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
                        )
                    )
                }
            }
        }
    }
}

internal data class CategoryFormActions(
    val onName: (String) -> Unit,
    val onDescription: (String) -> Unit,
    val onSortOrder: (String) -> Unit,
    val onActive: (Boolean) -> Unit,
    val onImage: (String) -> Unit,
    val onDismiss: () -> Unit,
    val onSave: () -> Unit,
)

@Composable
fun CategoryStatusDialog(category: ProductCategory, submitting: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val action = if (category.active) "Deactivate" else "Activate"
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("$action Category?") },
        text = { Text(if (category.active) "Customers will no longer see ${category.name}. Existing products are preserved."
            else "Customers will see ${category.name} and its active products.") },
        confirmButton = { Button(onConfirm, enabled = !submitting) { Text(action) } },
        dismissButton = { TextButton(onDismiss, enabled = !submitting) { Text("Cancel") } },
    )
}

@Composable
fun CategoryDeleteDialog(category: ProductCategory, submitting: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val blocked = category.productCount > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (blocked) "Category contains products" else "Delete Category?") },
        text = { Text(if (blocked) "${category.productCount} products use ${category.name}. Move or delete them before deleting this category."
            else "Delete ${category.name}? This cannot be undone.") },
        confirmButton = {
            if (blocked) TextButton(onDismiss) { Text("Close") }
            else Button(onConfirm, enabled = !submitting) { Text("Delete") }
        },
        dismissButton = { if (!blocked) TextButton(onDismiss, enabled = !submitting) { Text("Cancel") } },
    )
}

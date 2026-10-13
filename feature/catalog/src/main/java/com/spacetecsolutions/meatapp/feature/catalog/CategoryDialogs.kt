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
                else Text(if (form.editing) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_update_category_dd17101) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_save_category_a1f54af))
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
                        Text(if (image == null) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_add_photo_560a4bd) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_change_photo_6080b45))
                    }
                    imageError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall) }
                }
                OutlinedTextField(
                    form.name, actions.onName, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_category_name_912f070)) },
                    singleLine = true, isError = form.nameError != null,
                    supportingText = form.nameError?.let { { Text(it) } }, enabled = !submitting,
                    shape = AppShapes.extraSmall,
                )
                OutlinedTextField(
                    form.description, actions.onDescription, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_description_55f8ebc)) },
                    minLines = 3, maxLines = 5, enabled = !submitting, shape = AppShapes.extraSmall,
                )
                OutlinedTextField(
                    form.sortOrder, actions.onSortOrder, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_display_order_04af3db)) },
                    singleLine = true, isError = form.sortOrderError != null,
                    supportingText = form.sortOrderError?.let { { Text(it) } }, enabled = !submitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = AppShapes.extraSmall,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_status_bae7d5b), style = MaterialTheme.typography.labelMedium)
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
        onDismissRequest = onDismiss, title = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_action_category_d7a3cde, action)) },
        text = { Text(if (category.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_customers_will_no_longer_see_1c987dd, category.name) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_customers_will_see_category_name_7c3237f, category.name)) },
        confirmButton = { Button(onConfirm, enabled = !submitting) { Text(action) } },
        dismissButton = { TextButton(onDismiss, enabled = !submitting) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_cancel_77dfd21)) } },
    )
}

@Composable
fun CategoryDeleteDialog(category: ProductCategory, submitting: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val blocked = category.productCount > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (blocked) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_category_contains_products_dcc10ac) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_delete_category_61aa7cf)) },
        text = { Text(if (blocked) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_category_productcount_products_use_category_120912b, category.productCount, category.name) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_delete_category_name_this_cannot_a56b0c3, category.name)) },
        confirmButton = {
            if (blocked) TextButton(onDismiss) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_close_bbfa773)) }
            else Button(onConfirm, enabled = !submitting) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_delete_f6fdbe4)) }
        },
        dismissButton = { if (!blocked) TextButton(onDismiss, enabled = !submitting) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_cancel_77dfd21)) } },
    )
}

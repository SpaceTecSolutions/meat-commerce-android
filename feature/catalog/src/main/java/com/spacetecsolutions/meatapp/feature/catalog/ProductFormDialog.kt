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
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImageCarousel
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.ProductCategory
import com.spacetecsolutions.meatapp.core.model.ProductSubcategory
import com.spacetecsolutions.meatapp.core.model.ProductUnit

@Composable
internal fun ProductFormScreen(
    form: ProductFormState,
    categories: List<ProductCategory>,
    subcategories: List<ProductSubcategory>,
    submitting: Boolean,
    onManageCategories: () -> Unit,
    actions: ProductFormActions,
) {
    val context = LocalContext.current
    var cropQueue by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var croppedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var previewOpen by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val remaining = (MAX_PRODUCT_IMAGES - form.existingImages.size - form.selectedImageUris.size).coerceAtLeast(0)
        val selected = uris.distinct().take(remaining)
        imageError = selected.firstNotNullOfOrNull { validateSelectedImage(context, it) }
        if (imageError == null) {
            croppedImages = emptyList()
            cropQueue = selected
        }
    }
    cropQueue.firstOrNull()?.let { source ->
        fun finishCrop(result: String?) {
            val completed = if (result == null) croppedImages else croppedImages + result
            val remaining = cropQueue.drop(1)
            croppedImages = completed
            cropQueue = remaining
            if (remaining.isEmpty()) {
                actions.onImages(form.selectedImageUris + completed)
                croppedImages = emptyList()
            }
        }
        ImageCropDialog(source, circular = false, onDismiss = { finishCrop(null) }) { finishCrop(it) }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AppBackTopBar(if (form.editing) "Edit Product" else "Add Product", actions.onDismiss) },
        bottomBar = {
//            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                Button(
                    onClick = actions.onSave,
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                    shape = AppShapes.small,
                ) {
                    if (submitting) CircularProgressIndicator(Modifier.size(AppSpacing.large), strokeWidth = 2.dp)
                    else Text(if (form.editing) "Update Product" else "Save Product")
                }
//            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth).verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.compact),
            ) {
                ProductImageField(form, submitting, imageError, actions.onRemoveImage) { picker.launch("image/*") }
                ProductField("Product Name", form.name, actions.onName, form.nameError, submitting)
                AdaptiveFieldPair(
                    first = { CategorySelector(form, categories, submitting, actions.onCategory, onManageCategories) },
                    second = { UnitSelector(form.unit, submitting, actions.onUnit) },
                )
                if (subcategories.isNotEmpty()) SubcategorySelector(
                    form.subcategoryId, subcategories.filter { it.categoryId == form.categoryId && it.active },
                    submitting, actions.onSubcategory,
                )
                AdaptiveFieldPair(
                    first = { ProductField("Regular Price (₹)", form.price, actions.onPrice, form.priceError,
                        submitting, keyboardType = KeyboardType.Decimal) },
                    second = { ProductField("Stock (${form.unit.shortLabel()})", form.stock, actions.onStock,
                        form.stockError, submitting, keyboardType = KeyboardType.Decimal) },
                )
                ProductField("Offer Price (₹)", form.offerPrice, actions.onOfferPrice, form.offerPriceError,
                    submitting, keyboardType = KeyboardType.Decimal)
                OutlinedTextField(
                    value = form.description, onValueChange = actions.onDescription,
                    modifier = Modifier.fillMaxWidth(), label = { Text("Description") },
                    enabled = !submitting, minLines = 3, maxLines = 5, shape = AppShapes.extraSmall,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Status", style = MaterialTheme.typography.labelMedium)
                        Text(if (form.active) "Active" else "Inactive", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = form.active,
                        onCheckedChange = actions.onActive,
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
                AdditionalProductFields(form, submitting, actions)
                OutlinedButton({ previewOpen = true }, Modifier.fillMaxWidth(), enabled = !submitting) {
                    Icon(AppIcons.PasswordVisible, null); Spacer(Modifier.width(AppSpacing.small)); Text("Preview customer view")
                }
                Spacer(Modifier.height(AppSpacing.medium))
            }
        }
    }
    if (previewOpen) ProductCustomerPreviewDialog(form) { previewOpen = false }
}

@Composable
private fun ProductImageField(
    form: ProductFormState, submitting: Boolean, error: String?, remove: (Int) -> Unit, select: () -> Unit,
) {
    val images = form.existingImages + form.selectedImageUris
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AppImageCarousel(
            images = images,
            contentDescription = "Product photos",
            modifier = Modifier.size(200.dp),
            autoSlide = images.size > 1,
            onRemove = if (submitting) null else remove,
        )
        Text("${images.size} of $MAX_PRODUCT_IMAGES photos", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (images.size < MAX_PRODUCT_IMAGES) TextButton(onClick = select, enabled = !submitting) {
            Icon(AppIcons.Camera, null, Modifier.size(18.dp)); Spacer(Modifier.width(AppSpacing.extraSmall))
            Text(if (images.isEmpty()) "Add Photos" else "Add More Photos")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun AdaptiveFieldPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    BoxWithConstraints {
        if (maxWidth < 350.dp) Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
            first(); second()
        } else Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            Box(Modifier.weight(1.45f)) { first() }; Box(Modifier.weight(.75f)) { second() }
        }
    }
}

@Composable
private fun ProductField(
    label: String, value: String, change: (String) -> Unit, error: String?, submitting: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
) = OutlinedTextField(
    value = value, onValueChange = change, modifier = Modifier.fillMaxWidth(), label = { Text(label) },
    supportingText = error?.let { { Text(it) } }, isError = error != null, enabled = !submitting,
    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = AppShapes.extraSmall,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    form: ProductFormState, categories: List<ProductCategory>, submitting: Boolean,
    select: (String) -> Unit, manage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val available = categories.filter { it.active || it.id == form.categoryId }
    if (available.isEmpty()) Column {
        OutlinedTextField("", {}, Modifier.fillMaxWidth(), enabled = false, label = { Text("Category") },
            placeholder = { Text("No categories available") })
        TextButton(onClick = manage) { Text("Manage Categories") }
    } else ExposedDropdownMenuBox(expanded, { expanded = it && !submitting }) {
        OutlinedTextField(
            available.firstOrNull { it.id == form.categoryId }?.name.orEmpty(), {},
            Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), readOnly = true,
            label = { Text("Category") }, isError = form.categoryError != null,
            supportingText = form.categoryError?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, shape = AppShapes.extraSmall,
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            available.forEach { category -> DropdownMenuItem({ Text(category.name) }, {
                select(category.id); expanded = false
            }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSelector(unit: ProductUnit, submitting: Boolean, select: (ProductUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it && !submitting }) {
        OutlinedTextField(unit.shortLabel(), {}, Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            readOnly = true, label = { Text("Unit") }, trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }, shape = AppShapes.extraSmall)
        ExposedDropdownMenu(expanded, { expanded = false }) {
            ProductUnit.entries.forEach { value -> DropdownMenuItem({ Text(value.label()) }, {
                select(value); expanded = false
            }) }
        }
    }
}

@Composable
private fun AdditionalProductFields(form: ProductFormState, submitting: Boolean, actions: ProductFormActions) {
    var expanded by remember { mutableStateOf(form.attributes.isNotEmpty() || form.lowStockThreshold.isNotBlank()) }
    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide additional details" else "Additional details") }
    if (expanded) {
        ProductField("Low stock alert", form.lowStockThreshold, actions.onLowStock, null, submitting, KeyboardType.Decimal)
        form.attributes.forEachIndexed { index, attribute -> Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            Box(Modifier.weight(1f)) { ProductField("Attribute", attribute.key,
                { actions.onAttributeKey(index, it) }, null, submitting) }
            Box(Modifier.weight(1f)) { ProductField("Value", attribute.value,
                { actions.onAttributeValue(index, it) }, null, submitting) }
            IconButton({ actions.onRemoveAttribute(index) }) { Icon(AppIcons.Remove, "Remove attribute") }
        } }
        form.attributesError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = actions.onAddAttribute, enabled = !submitting) { Text("Add attribute") }
    }
}

data class ProductFormActions(
    val onName: (String) -> Unit, val onDescription: (String) -> Unit, val onCategory: (String) -> Unit,
    val onSubcategory: (String?) -> Unit,
    val onUnit: (ProductUnit) -> Unit, val onPrice: (String) -> Unit, val onOfferPrice: (String) -> Unit,
    val onStock: (String) -> Unit, val onLowStock: (String) -> Unit, val onImages: (List<String>) -> Unit,
    val onRemoveImage: (Int) -> Unit,
    val onActive: (Boolean) -> Unit, val onAddAttribute: () -> Unit, val onRemoveAttribute: (Int) -> Unit,
    val onAttributeKey: (Int, String) -> Unit, val onAttributeValue: (Int, String) -> Unit,
    val onDismiss: () -> Unit, val onSave: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubcategorySelector(selected: String?, values: List<ProductSubcategory>, submitting: Boolean,
    select: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it && !submitting }) {
        OutlinedTextField(values.firstOrNull { it.id == selected }?.name ?: "No subcategory", {},
            Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), readOnly = true,
            label = { Text("Subcategory (optional)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
        ExposedDropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem({ Text("No subcategory") }, { select(null); expanded = false })
            values.sortedBy(ProductSubcategory::sortOrder).forEach { value ->
                DropdownMenuItem({ Text(value.name) }, { select(value.id); expanded = false })
            }
        }
    }
}

private const val MAX_PRODUCT_IMAGES = 5

fun ProductUnit.label(): String = when (this) {
    ProductUnit.KILOGRAM -> "Kilogram"; ProductUnit.GRAM -> "Gram"
    ProductUnit.PIECE -> "Piece"; ProductUnit.PACK -> "Pack"
}

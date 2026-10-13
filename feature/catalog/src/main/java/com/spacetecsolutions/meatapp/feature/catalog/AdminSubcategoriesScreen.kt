package com.spacetecsolutions.meatapp.feature.catalog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.domain.repository.CatalogContentRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SubcategoryForm(val id: String? = null, val name: String = "", val description: String = "",
    val order: String = "0", val active: Boolean = true, val revision: Long? = null,
    val existingImageUrl: String? = null, val selectedImageUri: String? = null)
data class SubcategoryState(val loading: Boolean = true, val saving: Boolean = false,
    val items: List<ProductSubcategory> = emptyList(), val form: SubcategoryForm? = null,
    val error: String? = null, val message: CategoryMessage? = null)

@HiltViewModel
class AdminSubcategoriesViewModel @Inject constructor(private val repository: CatalogContentRepository) : ViewModel() {
    private val mutable = MutableStateFlow(SubcategoryState()); val state = mutable.asStateFlow()
    private var categoryId = ""
    fun load(id: String) { if (categoryId == id && !state.value.loading) return; categoryId = id; refresh() }
    fun refresh() = viewModelScope.launch { mutable.update { it.copy(loading = true, error = null) }
        when (val result = repository.getSubcategories(categoryId, true)) {
            is AppResult.Success -> mutable.update { it.copy(loading = false, items = result.value.sortedBy(ProductSubcategory::sortOrder)) }
            is AppResult.Failure -> mutable.update { it.copy(loading = false,
                error = result.error.subcategoryMessage("Unable to load subcategories")) }
        } }
    fun create() = mutable.update { it.copy(form = SubcategoryForm()) }
    fun edit(item: ProductSubcategory) = mutable.update { it.copy(form = SubcategoryForm(
        id = item.id, name = item.name, description = item.description, order = item.sortOrder.toString(),
        active = item.active, revision = item.revision, existingImageUrl = item.imageUrl,
    )) }
    fun close() = mutable.update { if (it.saving) it else it.copy(form = null) }
    fun update(form: SubcategoryForm) = mutable.update { it.copy(form = form, error = null) }
    fun save() { val form = state.value.form ?: return; val order = form.order.toIntOrNull()
        if (form.name.trim().length !in 2..60 || order == null || order < 0) {
            mutable.update { it.copy(error = "Enter a valid name and display order") }; return }
        viewModelScope.launch { mutable.update { it.copy(saving = true, error = null) }
            val uploadToken = when (val uri = form.selectedImageUri) {
                null -> null
                else -> when (val upload = repository.uploadSubcategoryImage(uri)) {
                    is AppResult.Success -> upload.value.uploadToken
                    is AppResult.Failure -> {
                        mutable.update { it.copy(saving = false,
                            error = upload.error.subcategoryMessage("Unable to upload subcategory image")) }
                        return@launch
                    }
                }
            }
            when (val result = repository.saveSubcategory(SubcategoryInput(
                subcategoryId = form.id, categoryId = categoryId, name = form.name.trim(),
                description = form.description.trim(), sortOrder = order, active = form.active,
                expectedRevision = form.revision, imageUploadToken = uploadToken,
            ))) {
                is AppResult.Success -> mutable.update { current -> current.copy(saving = false, form = null,
                    items = (current.items.filterNot { it.id == result.value.id } + result.value.copy(
                        productCount = current.items.firstOrNull { it.id == result.value.id }?.productCount ?: 0,
                    )).sortedBy(ProductSubcategory::sortOrder),
                    message = CategoryMessage("Subcategory saved", true)) }
                is AppResult.Failure -> mutable.update { it.copy(saving = false,
                    error = result.error.subcategoryMessage("Unable to save subcategory")) }
            } }
    }
    fun delete(item: ProductSubcategory) = viewModelScope.launch { when (repository.deleteSubcategory(item.id)) {
        is AppResult.Success -> mutable.update { it.copy(items = it.items - item, form = null,
            message = CategoryMessage("Subcategory deleted", true)) }
        is AppResult.Failure -> mutable.update { it.copy(
            message = CategoryMessage("Unable to delete subcategory. Refresh and try again.", false)) }
    } }
    fun toggleActive(item: ProductSubcategory) {
        if (state.value.saving) return
        viewModelScope.launch {
            mutable.update { it.copy(saving = true) }
            when (val result = repository.saveSubcategory(SubcategoryInput(
                subcategoryId = item.id, categoryId = item.categoryId, name = item.name,
                description = item.description, sortOrder = item.sortOrder,
                active = !item.active, expectedRevision = item.revision,
            ))) {
                is AppResult.Success -> mutable.update { current -> current.copy(
                    saving = false,
                    items = current.items.map { if (it.id == item.id) result.value.copy(productCount = it.productCount) else it },
                    message = CategoryMessage(if (result.value.active) "Subcategory enabled" else "Subcategory disabled", true),
                ) }
                is AppResult.Failure -> mutable.update { it.copy(saving = false,
                    message = CategoryMessage(result.error.subcategoryMessage("Unable to change subcategory status"), false)) }
            }
        }
    }
    fun consume() = mutable.update { it.copy(message = null) }
}

private fun AppError.subcategoryMessage(fallback: String): String = when (this) {
    is AppError.Validation -> message
    AppError.Unauthorized -> "Your session expired. Sign in again and retry."
    AppError.Forbidden -> "Admin permission is required to manage subcategories."
    AppError.Network, AppError.Offline, AppError.Timeout ->
        "Unable to reach the service. Check your connection and retry."
    else -> fallback
}

@Composable
fun AdminSubcategoriesRoute(parent: ProductCategory, back: () -> Unit, message: (CategoryMessage) -> Unit,
    viewModel: AdminSubcategoriesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); LaunchedEffect(parent.id) { viewModel.load(parent.id) }
    val context = LocalContext.current
    var pendingCacheEviction by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(state.message) { state.message?.let {
        if (it.success && it.text == "Subcategory deleted") {
            evictCachedImages(context, pendingCacheEviction); pendingCacheEviction = emptyList()
        }
        message(it); viewModel.consume()
    } }
    state.form?.let { form -> SubcategoryFormScreen(parent, form, state.saving, viewModel::update,
        viewModel::save, viewModel::close, { form.id?.let { id -> state.items.firstOrNull { it.id == id }?.let {
            pendingCacheEviction = listOfNotNull(it.imageUrl); viewModel.delete(it)
        } } })
        return }
    Scaffold(topBar = { AppBackTopBar(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_subcategories_f3da206), back) }, floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = viewModel::create, icon = { Icon(AppIcons.Add, null) },
            text = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_add_subcategory_75e3f6f)) }, containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }) { padding -> when {
        state.loading -> ContentStateView(ContentState.Loading, contentPadding = padding)
        state.error != null && state.items.isEmpty() -> ContentStateView(ContentState.Error(description = state.error),
            contentPadding = padding, onAction = viewModel::refresh)
        else -> LazyColumn(Modifier.fillMaxSize().padding(padding).widthIn(max = AppDimensions.contentMaxWidth),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_parent_category_parent_name_838f96d, parent.name), fontWeight = FontWeight.SemiBold) }
            if (state.items.isEmpty()) item { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_no_subcategories_yet_add_the_dc57809, parent.name)) }
            items(state.items, key = ProductSubcategory::id) { item -> ElevatedCard(onClick = { viewModel.edit(item) }) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryImage(item.imageUrl, item.name, Modifier.size(54.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(item.name, fontWeight = FontWeight.Bold)
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_status_order,
                            androidx.compose.ui.res.stringResource(if (item.active) R.string.ui_active else R.string.ui_inactive), item.sortOrder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.ui_product_count,
                            item.productCount, item.productCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(item.active, { viewModel.toggleActive(item) }, enabled = !state.saving,
                        colors = appSwitchColors())
                } }
            }
        }
    } }
}

@Composable
private fun SubcategoryFormScreen(parent: ProductCategory, form: SubcategoryForm, saving: Boolean,
    update: (SubcategoryForm) -> Unit, save: () -> Unit, back: () -> Unit, delete: () -> Unit) {
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
        update(form.copy(selectedImageUri = it)); cropSource = null
    } }
    Scaffold(topBar = { AppBackTopBar(if (form.id == null) "Add Subcategory" else "Edit Subcategory", back) },
        bottomBar = { Button(save, Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), enabled = !saving) {
            Text(if (form.id == null) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_save_subcategory_3de39d8) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_save_changes_fa2984b)) } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_parent_category_parent_name_f960238, parent.name), fontWeight = FontWeight.Bold) }
            item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val image = form.selectedImageUri ?: form.existingImageUrl
                RoundImage(image, "Subcategory image", 144.dp)
                TextButton({ picker.launch("image/*") }, enabled = !saving) {
                    Icon(AppIcons.Camera, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (image == null) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_add_photo_560a4bd) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_change_photo_6080b45))
                }
                imageError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
            } }
            item { OutlinedTextField(form.name, { update(form.copy(name = it.take(60))) }, Modifier.fillMaxWidth(),
                label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_subcategory_name_1880d9d)) }, singleLine = true) }
            item { OutlinedTextField(form.description, { update(form.copy(description = it.take(300))) },
                Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_description_optional_388de6f)) }, minLines = 3) }
            item { OutlinedTextField(form.order, { update(form.copy(order = it.filter(Char::isDigit).take(4))) },
                Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_display_order_04af3db)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) {
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_visible_to_customers_4c05670), fontWeight = FontWeight.SemiBold); Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_show_in_the_customer_catalog_429cbac),
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(form.active,
                    { update(form.copy(active = it)) }, colors = appSwitchColors()) } }
            if (form.id != null) item { TextButton(delete, Modifier.fillMaxWidth()) { Icon(AppIcons.Delete, null); Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.catalog.R.string.ui_delete_subcategory_29ace08)) } }
        }
    }
}

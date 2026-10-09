package com.spacetecsolutions.meatapp.feature.admin.faq

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.domain.repository.CatalogContentRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FaqForm(val id: String? = null, val question: String = "", val answer: String = "",
    val order: String = "0", val active: Boolean = true, val revision: Long? = null)
data class AdminFaqState(val loading: Boolean = true, val saving: Boolean = false,
    val items: List<FaqEntry> = emptyList(), val query: String = "", val form: FaqForm? = null,
    val error: String? = null, val message: String? = null)

@HiltViewModel
class AdminFaqViewModel @Inject constructor(private val repository: CatalogContentRepository) : ViewModel() {
    private val mutable = MutableStateFlow(AdminFaqState()); val state = mutable.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch { mutable.update { it.copy(loading = true, error = null) }
        when (val result = repository.getFaqs(true)) {
            is AppResult.Success -> mutable.update { it.copy(loading = false, items = result.value.sortedBy(FaqEntry::sortOrder)) }
            is AppResult.Failure -> mutable.update { it.copy(loading = false, error = "Unable to load FAQs") }
        } }
    fun search(value: String) = mutable.update { it.copy(query = value.take(80)) }
    fun create() = mutable.update { it.copy(form = FaqForm(order = (it.items.maxOfOrNull(FaqEntry::sortOrder)?.plus(1) ?: 0).toString())) }
    fun edit(item: FaqEntry) = mutable.update { it.copy(form = FaqForm(item.id, item.question, item.answer,
        item.sortOrder.toString(), item.active, item.revision)) }
    fun update(form: FaqForm) = mutable.update { it.copy(form = form, error = null) }
    fun close() = mutable.update { if (it.saving) it else it.copy(form = null) }
    fun save() { val form = state.value.form ?: return; val order = form.order.toIntOrNull()
        if (form.question.isBlank() || form.answer.isBlank() || order == null || order < 0) {
            mutable.update { it.copy(error = "Question, answer and a valid display order are required") }; return }
        viewModelScope.launch { mutable.update { it.copy(saving = true, error = null) }
            when (val result = repository.saveFaq(FaqInput(form.id, form.question.trim(), form.answer.trim(),
                form.active, order, form.revision))) {
                is AppResult.Success -> mutable.update { current -> current.copy(saving = false, form = null,
                    items = (current.items.filterNot { it.id == result.value.id } + result.value).sortedBy(FaqEntry::sortOrder),
                    message = "FAQ saved") }
                is AppResult.Failure -> mutable.update { it.copy(saving = false, error = "Unable to save FAQ") }
            } }
    }
    fun delete(item: FaqEntry) = viewModelScope.launch { when (repository.deleteFaq(item.id)) {
        is AppResult.Success -> mutable.update { it.copy(items = it.items - item, form = null, message = "FAQ deleted") }
        is AppResult.Failure -> mutable.update { it.copy(error = "Unable to delete FAQ") }
    } }
    fun consume() = mutable.update { it.copy(message = null) }
}

@Composable
fun AdminFaqManagementRoute(back: () -> Unit, onMessage: (String) -> Unit,
    viewModel: AdminFaqViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) { state.message?.let { onMessage(it); viewModel.consume() } }
    state.form?.let { FaqFormScreen(it, state.saving, viewModel::update, viewModel::save, viewModel::close,
        { it.id?.let { id -> state.items.firstOrNull { entry -> entry.id == id }?.let(viewModel::delete) } }); return }
    val filtered = remember(state.items, state.query) { state.items.filter {
        state.query.isBlank() || it.question.contains(state.query, true) || it.answer.contains(state.query, true) } }
    Scaffold(topBar = { AppBackTopBar("FAQ Management", back) }, floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = viewModel::create,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) { Icon(AppIcons.Add, null); Spacer(Modifier.width(8.dp)); Text("Add FAQ") }
    }) { padding -> Column(Modifier.fillMaxSize().padding(padding).widthIn(max = 560.dp)) {
        OutlinedTextField(state.query, viewModel::search, Modifier.fillMaxWidth().padding(16.dp), singleLine = true,
            leadingIcon = { Icon(AppIcons.Search, null) }, placeholder = { Text("Search FAQs") })
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null && state.items.isEmpty() -> ContentStateView(ContentState.Error(description = state.error), onAction = viewModel::refresh)
            filtered.isEmpty() -> ContentStateView(ContentState.Empty(title = "No FAQs found", description = "Add customer questions and answers."))
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) { items(filtered, key = FaqEntry::id) { item ->
                ElevatedCard(onClick = { viewModel.edit(item) }, modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.question, fontWeight = FontWeight.Bold)
                            Text(item.answer, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${if (item.active) "Active" else "Inactive"} · Order ${item.sortOrder}",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }; Icon(AppIcons.Edit, "Edit")
                    }
                }
            } }
        }
    } }
}

@Composable
private fun FaqFormScreen(form: FaqForm, saving: Boolean, update: (FaqForm) -> Unit, save: () -> Unit,
    back: () -> Unit, delete: () -> Unit) = Scaffold(
    topBar = { AppBackTopBar(if (form.id == null) "Add FAQ" else "Edit FAQ", back) },
    bottomBar = { Button(save, Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), enabled = !saving) {
        Text(if (form.id == null) "Publish FAQ" else "Save Changes") } },
) { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)) {
    item { OutlinedTextField(form.question, { update(form.copy(question = it.take(200))) }, Modifier.fillMaxWidth(),
        label = { Text("Question *") }, minLines = 2) }
    item { OutlinedTextField(form.answer, { update(form.copy(answer = it.take(2000))) }, Modifier.fillMaxWidth(),
        label = { Text("Answer *") }, minLines = 6) }
    item { OutlinedTextField(form.order, { update(form.copy(order = it.filter(Char::isDigit).take(4))) }, Modifier.fillMaxWidth(),
        label = { Text("Display Order") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
    item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) {
        Text("Active", fontWeight = FontWeight.SemiBold); Text("Visible to customers", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }; Switch(form.active, { update(form.copy(active = it)) }, colors = appSwitchColors()) } }
    if (form.id != null) item { TextButton(delete, Modifier.fillMaxWidth()) { Icon(AppIcons.Delete, null); Text("Delete FAQ") } }
} }

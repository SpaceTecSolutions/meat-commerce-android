package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.domain.repository.CatalogContentRepository
import com.spacetecsolutions.meatapp.core.model.FaqEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerFaqState(val loading: Boolean = true, val items: List<FaqEntry> = emptyList(),
    val query: String = "", val expandedId: String? = null, val error: String? = null)

@HiltViewModel
class CustomerFaqViewModel @Inject constructor(private val repository: CatalogContentRepository) : ViewModel() {
    private val mutable = MutableStateFlow(CustomerFaqState()); val state = mutable.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch { mutable.update { it.copy(loading = true, error = null) }
        when (val result = repository.getFaqs(false)) {
            is AppResult.Success -> mutable.update { it.copy(loading = false,
                items = result.value.filter(FaqEntry::active).sortedBy(FaqEntry::sortOrder)) }
            is AppResult.Failure -> mutable.update { it.copy(loading = false, error = "Unable to load FAQs") }
        } }
    fun search(value: String) = mutable.update { it.copy(query = value.take(80), expandedId = null) }
    fun toggle(id: String) = mutable.update { it.copy(expandedId = if (it.expandedId == id) null else id) }
}

@Composable
fun CustomerFaqRoute(back: () -> Unit, contactSupport: () -> Unit,
    viewModel: CustomerFaqViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filtered = remember(state.items, state.query) { state.items.filter {
        state.query.isBlank() || it.question.contains(state.query, true) || it.answer.contains(state.query, true) } }
    Scaffold(topBar = { AppBackTopBar("Frequently Asked Questions", back) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).widthIn(max = 600.dp)) {
            Text("How can we help?", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 20.dp))
            Text("Find quick answers about ordering, payments and delivery.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            /*OutlinedTextField(state.query, viewModel::search, Modifier.fillMaxWidth().padding(16.dp), singleLine = true,
                leadingIcon = { Icon(AppIcons.Search, null) }, placeholder = { Text("Search questions") },
                shape = MaterialTheme.shapes.extraLarge)*/
            when {
                state.loading -> ContentStateView(ContentState.Loading)
                state.error != null -> ContentStateView(ContentState.Error(description = state.error), onAction = viewModel::refresh)
                filtered.isEmpty() -> ContentStateView(ContentState.Empty(title = "No answers found",
                    description = "Try another search or contact support."), onAction = contactSupport)
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = FaqEntry::id) { faq ->
                        ElevatedCard(Modifier.fillMaxWidth().animateContentSize()) {
                            Column(Modifier.clickable { viewModel.toggle(faq.id) }.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(faq.question, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    Icon(AppIcons.ArrowDown, null, modifier = Modifier.rotate(if (state.expandedId == faq.id) 180f else 0f),
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                                AnimatedVisibility(state.expandedId == faq.id) {
                                    Text(faq.answer, modifier = Modifier.padding(top = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    item { OutlinedButton(contactSupport, Modifier.fillMaxWidth()) {
                        Icon(AppIcons.Help, null); Spacer(Modifier.width(8.dp)); Text("Contact Support")
                    } }
                }
            }
        }
    }
}

package com.spacetecsolutions.meatapp.feature.superadmin.audit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.domain.repository.AuditLogRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuditLogState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val filter: PrivilegedAuditAction? = null,
    val entries: List<AuditLogEntry> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AuditLogViewModel @Inject constructor(private val repository: AuditLogRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuditLogState())
    val state = mutableState.asStateFlow()
    init { refresh() }
    fun filter(action: PrivilegedAuditAction?) {
        mutableState.update { it.copy(filter = action) }
        refresh()
    }
    fun refresh() = load(null)
    fun loadMore() { state.value.nextCursor?.let(::load) }
    private fun load(cursor: String?) = viewModelScope.launch {
        mutableState.update { it.copy(loading = cursor == null, loadingMore = cursor != null, error = null) }
        when (val result = repository.getAuditLog(state.value.filter, cursor)) {
            is AppResult.Success -> mutableState.update { current -> current.copy(
                loading = false, loadingMore = false,
                entries = if (cursor == null) result.value.entries else current.entries + result.value.entries,
                nextCursor = result.value.nextCursor,
            ) }
            is AppResult.Failure -> mutableState.update { it.copy(loading = false, loadingMore = false, error = result.error.text()) }
        }
    }
}

@Composable
fun AuditLogRoute(back: () -> Unit, viewModel: AuditLogViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        AppBackTopBar("Audit Log", back)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = AppSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            FilterChip(state.filter == null, { viewModel.filter(null) }, { Text("All") })
            PrivilegedAuditAction.entries.forEach { action -> FilterChip(state.filter == action,
                { viewModel.filter(action) }, { Text(action.label()) }) }
        }
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null && state.entries.isEmpty() -> ContentStateView(ContentState.Error(description = state.error), onAction = viewModel::refresh)
            state.entries.isEmpty() -> ContentStateView(ContentState.Empty("No audit entries", "Privileged actions will appear here."))
            else -> LazyColumn(Modifier.fillMaxSize().widthIn(max = AppDimensions.contentMaxWidth),
                contentPadding = PaddingValues(AppSpacing.medium), verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                items(state.entries, key = AuditLogEntry::id) { entry -> AuditEntryCard(entry) }
                state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (state.nextCursor != null) item { OutlinedButton(viewModel::loadMore, Modifier.fillMaxWidth(), enabled = !state.loadingMore) {
                    if (state.loadingMore) CircularProgressIndicator(Modifier.size(AppDimensions.iconSmall)) else Text("Load more")
                } }
            }
        }
    }
}

@Composable private fun AuditEntryCard(entry: AuditLogEntry) = ElevatedCard(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(AppSpacing.medium), verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entry.action.label(), style = MaterialTheme.typography.titleMedium)
            Icon(AppIcons.Privacy, null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(entry.summary)
        Text("By ${entry.actorDisplayName} · ${entry.occurredAtEpochMillis.date()}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Reference ${entry.correlationId}", style = MaterialTheme.typography.labelSmall)
    }
}
private fun PrivilegedAuditAction.label() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
private fun Long.date() = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(this))
private fun AppError.text() = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "Only an active Super Admin can view the audit log"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    else -> "Unable to load the audit log"
}

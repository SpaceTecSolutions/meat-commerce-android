package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.ui.unit.dp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.domain.repository.MessagingRepository
import com.spacetecsolutions.meatapp.core.model.AppNotification
import com.spacetecsolutions.meatapp.core.model.NotificationCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class NotificationHistoryState(val loading: Boolean = true, val items: List<AppNotification> = emptyList(), val error: String? = null)

@HiltViewModel
class NotificationHistoryViewModel @Inject constructor(private val repository: MessagingRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(NotificationHistoryState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    init { refresh() }
    private fun observe() {
        observation = viewModelScope.launch {
        repository.observeNotificationHistory().collect { result ->
            when (result) {
                is AppResult.Success -> mutableState.value = NotificationHistoryState(false, result.value)
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = "Unable to refresh notification updates") }
            }
        }
        }
    }
    fun refresh() {
        observation?.cancel()
        mutableState.update { it.copy(error = null, loading = it.items.isEmpty()) }
        observe()
    }
    fun read(item: AppNotification) {
        if (item.isRead) return
        mutableState.update { state -> state.copy(items = state.items.map {
            if (it.id == item.id) it.copy(readAtEpochMillis = System.currentTimeMillis()) else it
        }) }
        viewModelScope.launch { repository.markNotificationRead(item.id) }
    }
    fun markAllRead() {
        if (mutableState.value.items.none { !it.isRead }) return
        mutableState.update { state -> state.copy(items = state.items.map {
            if (it.isRead) it else it.copy(readAtEpochMillis = System.currentTimeMillis())
        }) }
        viewModelScope.launch {
            if (repository.markAllNotificationsRead() is AppResult.Failure) refresh()
        }
    }
    fun clearAll() = viewModelScope.launch {
        if (repository.clearAllNotifications() is AppResult.Failure) {
            mutableState.update { it.copy(error = "Unable to clear notifications. Please retry.") }
        }
    }
}

@Composable
fun NotificationHistoryRoute(
    back: () -> Unit,
    onOpen: (AppNotification) -> Unit = {},
    customerStyle: Boolean = false,
    onContinueShopping: () -> Unit = back,
    emptyActionLabel: String = "Continue Shopping",
    viewModel: NotificationHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text("Clear all notifications?") },
        text = { Text("This removes your notification history. Your orders will not be affected.") },
        confirmButton = { TextButton({ confirmClear = false; viewModel.clearAll() }) { Text("Clear all") } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Cancel") } })
    var permissionGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }
    if (customerStyle) {
        CustomerNotificationHistory(
            state = state,
            back = back,
            continueShopping = onContinueShopping,
            emptyActionLabel = emptyActionLabel,
            markAllRead = viewModel::markAllRead,
            clearAll = { confirmClear = true },
            retry = viewModel::refresh,
            open = { item -> viewModel.read(item); onOpen(item) },
            permissionGranted = permissionGranted,
            requestPermission = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        )
        return
    }
    Column(Modifier.fillMaxSize()) {
        AppBackTopBar("Notifications", back)
        if (state.items.isNotEmpty()) Row(
            Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton({ confirmClear = true }) { Text("Clear all") }
            TextButton(viewModel::markAllRead, enabled = state.items.any { !it.isRead }) { Text("Mark all as read") }
        }
        if (!permissionGranted) ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium)) {
            Row(Modifier.fillMaxWidth().padding(AppSpacing.medium), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enable alerts for important updates", Modifier.weight(1f))
                TextButton({ permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Enable") }
            }
        }
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null -> ContentStateView(ContentState.Error(description = state.error), onAction = viewModel::refresh)
            state.items.isEmpty() -> ContentStateView(ContentState.Empty("No notifications yet", "Updates will appear here."))
            else -> LazyColumn(
                Modifier.fillMaxWidth().widthIn(max = 640.dp),
                contentPadding = PaddingValues(horizontal = AppSpacing.medium),
            ) {
                items(state.items, key = AppNotification::id) { item ->
                    NotificationReferenceRow(item) { viewModel.read(item); onOpen(item) }
                }
            }
        }
    }
}

private fun NotificationCategory.symbol() = when (this) {
    NotificationCategory.ORDER -> "▣"
    NotificationCategory.PAYMENT -> "₹"
    NotificationCategory.DELIVERY -> "◆"
    NotificationCategory.STOCK -> "!"
    NotificationCategory.PROMOTION -> "%"
    NotificationCategory.REPORT -> "▤"
    NotificationCategory.SYSTEM -> "●"
}

private fun relativeTime(epoch: Long): String {
    val elapsed = (System.currentTimeMillis() - epoch).coerceAtLeast(0)
    return when {
        elapsed < 60_000 -> "Just now"
        elapsed < 3_600_000 -> "${TimeUnit.MILLISECONDS.toMinutes(elapsed)} min ago"
        elapsed < 86_400_000 -> "${TimeUnit.MILLISECONDS.toHours(elapsed)} hr ago"
        else -> DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epoch))
    }
}

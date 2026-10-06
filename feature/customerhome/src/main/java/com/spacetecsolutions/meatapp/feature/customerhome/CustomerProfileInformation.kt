package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerHomeRepository
import com.spacetecsolutions.meatapp.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CustomerProfileInformationViewModel @Inject constructor(
    private val repository: CustomerHomeRepository,
) : ViewModel() {
    private val result = MutableStateFlow<String?>(null)
    val message = result.asStateFlow()
    private val busy = MutableStateFlow(false)
    val saving = busy.asStateFlow()
    fun clearMessage() { result.value = null }
    fun save(first: String, last: String) = viewModelScope.launch {
        if (busy.value) return@launch
        if (first.trim().length < 2) { result.value = "Enter a valid first name"; return@launch }
        busy.value = true
        result.value = when (repository.updateProfile(first.trim(), last.trim())) {
            is AppResult.Success -> "Profile updated"
            is AppResult.Failure -> "Unable to save profile. Check your connection and try again."
        }
        busy.value = false
    }
}

@Composable
fun CustomerProfileInformationRoute(user: User, back: () -> Unit,
    openDeleteAccount: () -> Unit,
    viewModel: CustomerProfileInformationViewModel = hiltViewModel()) {
    var first by remember(user.id) { mutableStateOf(user.firstName) }
    var last by remember(user.id) { mutableStateOf(user.lastName) }
    var editing by remember(user.id) { mutableStateOf(false) }
    val busy by viewModel.saving.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) { if (message == "Profile updated") editing = false }
    val cancel = {
        if (editing) {
            first = user.firstName; last = user.lastName
            editing = false; viewModel.clearMessage()
        } else back()
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        containerColor = CustomerProfileColors.canvas,
        topBar = { ProfileInformationTopBar(cancel, editing, edit = {
            viewModel.clearMessage(); editing = true
        }) },
        bottomBar = {
            ProfileInformationSaveAction(
                busy = busy, editing = editing,
                onSave = { viewModel.save(first, last) }, onCancel = cancel,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 440.dp)
            .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ProfileInformationIdentity(user)
            ProfileInformationPersonalCard(
                first = first, last = last, enabled = editing && !busy,
                firstError = message == "Enter a valid first name",
                onFirst = { first = it.take(50); viewModel.clearMessage() },
                onLast = { last = it.take(50); viewModel.clearMessage() },
            )
            ProfileInformationSecurityCard(user)
            Surface(color = Color(0xFFFFFBF1), shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))) {
                Text("Your order history and saved addresses are linked to this account.",
                    Modifier.fillMaxWidth().padding(14.dp),
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF9A4D07))
            }
            ProfileInformationDeleteEntry(openDeleteAccount)
            message?.takeUnless { it == "Enter a valid first name" }?.let {
                Text(it, color = if (it == "Profile updated") Color(0xFF059669)
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

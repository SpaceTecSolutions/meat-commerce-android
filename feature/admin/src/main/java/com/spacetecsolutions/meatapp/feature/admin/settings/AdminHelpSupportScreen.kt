package com.spacetecsolutions.meatapp.feature.admin.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*

@Composable
fun AdminHelpSupportRoute(
    back: () -> Unit,
    call: (String) -> Unit,
    whatsapp: (String) -> Unit,
    email: (String) -> Unit,
    onSaved: (String) -> Unit,
    viewModel: AdminHelpViewModel = hiltViewModel(),
) {
    HideAppBottomBar()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) { onSaved("Customer support details saved"); viewModel.consumeSaved() }
    }
    Column(Modifier.fillMaxSize().background(AdminAccountColors.canvas)) {
        AdminAccountTopBar("Help & Support", back)
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.settings == null -> ContentStateView(ContentState.Error(description = state.error), onAction = viewModel::refresh)
            else -> SupportContent(state, viewModel, call, whatsapp, email)
        }
    }
}

@Composable
private fun SupportContent(
    state: AdminHelpState,
    actions: AdminHelpViewModel,
    call: (String) -> Unit,
    whatsapp: (String) -> Unit,
    email: (String) -> Unit,
) {
    val value = state.settings ?: return
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(state.saved) { if (state.saved) editing = false }
    LazyColumn(
        Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 440.dp),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White,
                border = BorderStroke(1.dp, AdminAccountColors.border), shadowElevation = 1.dp) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_need_some_help_f77b2b8), style = MaterialTheme.typography.headlineSmall)
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_these_contacts_are_displayed_in_bf1fd03),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!editing) OutlinedButton({ editing = true }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_edit_5301648)) }
            }
            }
        }
        item { OutlinedTextField(value.supportEmail.orEmpty(),
            { actions.update(value.copy(supportEmail = it.take(120).blankNull())) },
            Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_support_email_017868e)) }, enabled = editing && !state.saving) }
        item { OutlinedTextField(value.supportPhone.orEmpty(),
            { actions.update(value.copy(supportPhone = it.take(20).blankNull())) },
            Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_call_number_1dbac31)) }, enabled = editing && !state.saving) }
        item { OutlinedTextField(value.supportWhatsApp.orEmpty(),
            { actions.update(value.copy(supportWhatsApp = it.take(20).blankNull())) },
            Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_whatsapp_number_4955eb6)) }, enabled = editing && !state.saving) }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (editing) item { Button(actions::save, Modifier.fillMaxWidth(), enabled = !state.saving) {
            Text(if (state.saving) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_saving_56a2285) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_save_support_details_d4e2dd6))
        } }
    }
}

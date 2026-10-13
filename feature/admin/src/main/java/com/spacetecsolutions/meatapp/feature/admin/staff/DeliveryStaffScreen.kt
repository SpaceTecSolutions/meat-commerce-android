package com.spacetecsolutions.meatapp.feature.admin.staff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.User

@Composable
fun DeliveryStaffRoute(
    onBack: () -> Unit,
    onMessage: (StaffMessage) -> Unit,
    viewModel: DeliveryStaffViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) { state.message?.let { onMessage(it); viewModel.consumeMessage() } }
    DeliveryStaffScreen(state, onBack, viewModel)
    state.form?.let { StaffFormDialog(it, state.submitting, viewModel) }
    state.statusChange?.let { user -> AlertDialog(
        onDismissRequest = { viewModel.requestStatus(null) },
        title = { Text(if (user.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_deactivate_delivery_user_42ab99f) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_activate_delivery_user_8037c06)) },
        text = { Text(user.displayName) },
        confirmButton = { Button(viewModel::confirmStatus, enabled = !state.submitting) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_confirm_04a2122)) } },
        dismissButton = { TextButton({ viewModel.requestStatus(null) }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_cancel_77dfd21)) } },
    ) }
}

@Composable
private fun DeliveryStaffScreen(state: DeliveryStaffUiState, back: () -> Unit, actions: DeliveryStaffViewModel) {
    Scaffold(
        topBar = { AppBackTopBar(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_delivery_staff_ffc704d), back) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = actions::create,
            icon = { Icon(AppIcons.Add, null) }, text = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_add_delivery_user_d9cca40)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary) },
    ) { padding -> when {
        state.loading -> ContentStateView(ContentState.Loading, contentPadding = padding)
        state.error != null && state.staff.isEmpty() -> ContentStateView(
            ContentState.Error(description = state.error), contentPadding = padding, onAction = actions::load)
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = AppDimensions.contentMaxWidth),
            contentPadding = PaddingValues(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            if (state.refreshing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (state.staff.isEmpty()) item { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_no_delivery_users_yet_a653ec1), Modifier.padding(AppSpacing.large)) }
            items(state.staff, key = User::id) { user -> StaffCard(user, actions::edit, actions::requestStatus) }
        }
    } }
}

@Composable
private fun StaffCard(user: User, edit: (User) -> Unit, status: (User) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) { Row(
        Modifier.fillMaxWidth().padding(AppSpacing.medium), verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Delivery, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(horizontal = AppSpacing.medium).weight(1f)) {
            Text(user.displayName, style = MaterialTheme.typography.titleMedium)
            Text(user.mobileNumber, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (user.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_active_a733b80) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_inactive_09af574),
                color = if (user.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        IconButton({ edit(user) }) { Icon(AppIcons.Edit, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_edit_5301648)) }
        TextButton({ status(user) }) { Text(if (user.active) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_deactivate_d65ded9) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_activate_92ef083)) }
    } }
}

@Composable
private fun StaffFormDialog(form: StaffForm, submitting: Boolean, actions: DeliveryStaffViewModel) {
    AlertDialog(
        onDismissRequest = actions::closeForm,
        title = { Text(if (form.editing) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_edit_delivery_user_edb86d6) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_add_delivery_user_d9cca40)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            OutlinedTextField(form.name, actions::name, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_name_709a232)) })
            OutlinedTextField(form.mobile, actions::mobile, Modifier.fillMaxWidth(), label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_mobile_number_8e5c12d)) })
            if (!form.editing) OutlinedTextField(form.password, actions::password, Modifier.fillMaxWidth(),
                label = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_temporary_password_62d6062)) })
            form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { Button(actions::save, enabled = !submitting) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_save_efc007a)) } },
        dismissButton = { TextButton(actions::closeForm) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_cancel_77dfd21)) } },
    )
}

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
        title = { Text(if (user.active) "Deactivate delivery user?" else "Activate delivery user?") },
        text = { Text(user.displayName) },
        confirmButton = { Button(viewModel::confirmStatus, enabled = !state.submitting) { Text("Confirm") } },
        dismissButton = { TextButton({ viewModel.requestStatus(null) }) { Text("Cancel") } },
    ) }
}

@Composable
private fun DeliveryStaffScreen(state: DeliveryStaffUiState, back: () -> Unit, actions: DeliveryStaffViewModel) {
    Scaffold(
        topBar = { AppBackTopBar("Delivery Staff", back) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = actions::create,
            icon = { Icon(AppIcons.Add, null) }, text = { Text("Add Delivery User") },
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
            if (state.staff.isEmpty()) item { Text("No delivery users yet.", Modifier.padding(AppSpacing.large)) }
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
            Text(if (user.active) "Active" else "Inactive",
                color = if (user.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        IconButton({ edit(user) }) { Icon(AppIcons.Edit, "Edit") }
        TextButton({ status(user) }) { Text(if (user.active) "Deactivate" else "Activate") }
    } }
}

@Composable
private fun StaffFormDialog(form: StaffForm, submitting: Boolean, actions: DeliveryStaffViewModel) {
    AlertDialog(
        onDismissRequest = actions::closeForm,
        title = { Text(if (form.editing) "Edit Delivery User" else "Add Delivery User") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            OutlinedTextField(form.name, actions::name, Modifier.fillMaxWidth(), label = { Text("Name") })
            OutlinedTextField(form.mobile, actions::mobile, Modifier.fillMaxWidth(), label = { Text("Mobile number") })
            if (!form.editing) OutlinedTextField(form.password, actions::password, Modifier.fillMaxWidth(),
                label = { Text("Temporary password") })
            form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { Button(actions::save, enabled = !submitting) { Text("Save") } },
        dismissButton = { TextButton(actions::closeForm) { Text("Cancel") } },
    )
}

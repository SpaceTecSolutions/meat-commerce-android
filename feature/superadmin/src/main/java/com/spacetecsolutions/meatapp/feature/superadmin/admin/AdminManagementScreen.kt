package com.spacetecsolutions.meatapp.feature.superadmin.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.StatusChip
import com.spacetecsolutions.meatapp.core.designsystem.component.StatusTone
import com.spacetecsolutions.meatapp.core.designsystem.component.HideAppBottomBar
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.User
import java.text.DateFormat
import java.util.Date

@Composable
fun AdminManagementRoute(
    onMessage: (AdminMessage) -> Unit,
    viewModel: AdminManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) {
        state.message?.let { onMessage(it); viewModel.consumeMessage() }
    }
    AdminManagementScreen(
        state, viewModel::updateQuery, viewModel::openCreate, viewModel::openEdit,
        viewModel::requestStatusChange, viewModel::loadAdmins,
    )
    state.form?.let {
        HideAppBottomBar()
        AdminFormDialog(
            it, state.submitting, viewModel::updateName, viewModel::updateMobile,
            viewModel::updatePassword, viewModel::closeForm, viewModel::saveAdmin,
        )
    }
    state.confirmation?.let {
        AdminStatusDialog(it, state.submitting, viewModel::dismissConfirmation, viewModel::confirmStatusChange)
    }
}

@Composable
private fun AdminManagementScreen(
    state: AdminManagementUiState,
    onQueryChange: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (User) -> Unit,
    onStatusChange: (User) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AdminHeader(onCreate)
            when {
                state.loading -> LoadingScreen()
                state.error != null && state.admins.isEmpty() -> ErrorScreen(state.error, onRetry)
                else -> AdminList(state, onQueryChange, onEdit, onStatusChange, onCreate)
            }
        }
    }
}

@Composable
private fun AdminHeader(onCreate: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = AppSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /*IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
            Icon(AppIcons.Menu, "Open menu", Modifier.size(20.dp))
        }*/
        Text(
            "Admins",
            Modifier.padding(start = AppSpacing.small).weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        /*IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
            Icon(AppIcons.Search, "Search Admins", Modifier.size(20.dp))
        }*/
        FilledIconButton(
            onClick = onCreate,
            modifier = Modifier.size(36.dp),
            shape = AppShapes.extraSmall,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Icon(AppIcons.Add, "Create Admin", Modifier.size(20.dp)) }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun AdminList(
    state: AdminManagementUiState,
    onQueryChange: (String) -> Unit,
    onEdit: (User) -> Unit,
    onStatusChange: (User) -> Unit,
    onCreate: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = AppDimensions.contentMaxWidth),
        contentPadding = PaddingValues(AppSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.compact),
    ) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                leadingIcon = { Icon(AppIcons.Search, null, Modifier.size(20.dp)) },
                trailingIcon = if (state.query.isNotEmpty()) {
                    { IconButton(onClick = { onQueryChange("") }) { Icon(AppIcons.Close, "Clear") } }
                } else null,
                placeholder = { Text("Search admins...", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                shape = AppShapes.extraSmall,
            )
        }
        if (state.refreshing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (state.filteredAdmins.isEmpty()) item { EmptyAdmins(state.query.isBlank(), onCreate) }
        else items(state.filteredAdmins, key = User::id) { AdminCard(it, onEdit, onStatusChange) }
    }
}

@Composable
private fun EmptyAdmins(firstAdmin: Boolean, onCreate: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(AppIcons.Admin, null, Modifier.padding(18.dp).size(28.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(if (firstAdmin) "No Admins yet" else "No matching Admins", style = MaterialTheme.typography.titleMedium)
        Text(
            if (firstAdmin) "Create the first Admin account to get started." else "Try a different name or mobile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (firstAdmin) TextButton(onClick = onCreate) { Text("Create Admin") }
    }
}

@Composable
private fun AdminCard(admin: User, onEdit: (User) -> Unit, onStatusChange: (User) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val createdDate = remember(admin.createdAtEpochMillis) {
        admin.createdAtEpochMillis.takeIf { it > 0L }?.let {
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))
        }.orEmpty()
    }
    OutlinedCard(
        Modifier.fillMaxWidth().clickable { onEdit(admin) }, shape = AppShapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(AppSpacing.compact), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(AppIcons.Admin, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.padding(start = AppSpacing.compact).weight(1f)) {
                Text(admin.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(admin.mobileNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (admin.active) "Active" else "Inactive",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (admin.active) Success else MaterialTheme.colorScheme.error,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                        Icon(AppIcons.MoreVertical, "Admin actions", Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { menuExpanded = false; onEdit(admin) },
                            leadingIcon = { Icon(AppIcons.Edit, null) },
                        )
                        DropdownMenuItem(
                            text = { Text(if (admin.active) "Deactivate" else "Activate") },
                            onClick = { menuExpanded = false; onStatusChange(admin) },
                        )
                    }
                }
                if (createdDate.isNotEmpty()) {
                    Text(createdDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable private fun LoadingScreen() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(AppSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(AppIcons.PaymentFailed, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.error)
        Text("Unable to load Admins", Modifier.padding(top = AppSpacing.compact), style = MaterialTheme.typography.titleMedium)
        Text(message, Modifier.padding(top = AppSpacing.small), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry) { Text("Try again") }
    }
}

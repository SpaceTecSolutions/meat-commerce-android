package com.spacetecsolutions.meatapp.feature.admin.staff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.domain.repository.*
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EmployeeForm(val id: String? = null, val name: String = "", val mobile: String = "", val email: String = "",
    val active: Boolean = true, val permissions: Set<StaffPermission> = emptySet())
data class StaffState(val loading: Boolean = true, val saving: Boolean = false, val staff: List<User> = emptyList(),
    val query: String = "", val activeOnly: Boolean = false, val form: EmployeeForm? = null, val error: String? = null,
    val message: StaffMessage? = null)

@HiltViewModel
class StaffManagementViewModel @Inject constructor(private val repository: StaffRepository) : ViewModel() {
    private val mutable = MutableStateFlow(StaffState()); val state = mutable.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch { mutable.update { it.copy(loading = true, error = null) }
        when (val result = repository.getStaff()) {
            is AppResult.Success -> mutable.update { it.copy(loading = false, staff = result.value.sortedBy(User::displayName)) }
            is AppResult.Failure -> mutable.update { it.copy(loading = false, error = "Unable to load staff") }
        } }
    fun search(value: String) = mutable.update { it.copy(query = value.take(80)) }
    fun activeOnly(value: Boolean) = mutable.update { it.copy(activeOnly = value) }
    fun create() = mutable.update { it.copy(form = EmployeeForm()) }
    fun edit(user: User) = mutable.update { it.copy(form = EmployeeForm(user.id, user.displayName, user.mobileNumber,
        user.email.orEmpty(), user.active, user.permissions)) }
    fun update(form: EmployeeForm) = mutable.update { it.copy(form = form, error = null) }
    fun close() = mutable.update { if (it.saving) it else it.copy(form = null) }
    fun save() { val form = state.value.form ?: return; val digits = form.mobile.filter(Char::isDigit).takeLast(10)
        if (form.name.trim().length < 2 || digits.length != 10 || digits.firstOrNull() !in '6'..'9' ||
            (form.email.isNotBlank() && !form.email.contains('@'))) {
            mutable.update { it.copy(error = "Enter valid staff details") }; return }
        viewModelScope.launch { mutable.update { it.copy(saving = true) }
            when (val result = repository.saveStaff(StaffInput(form.id, form.name.trim(), "+91$digits",
                form.email.trim().ifBlank { null }, form.active, form.permissions))) {
                is AppResult.Success -> mutable.update { current -> current.copy(saving = false, form = null,
                    staff = (current.staff.filterNot { it.id == result.value.id } + result.value).sortedBy(User::displayName),
                    message = StaffMessage("Staff saved", true)) }
                is AppResult.Failure -> mutable.update { it.copy(saving = false, message = StaffMessage("Unable to save staff", false)) }
            } }
    }
    fun consume() = mutable.update { it.copy(message = null) }
}

@Composable
fun StaffManagementRoute(back: () -> Unit, onMessage: (StaffMessage) -> Unit,
    viewModel: StaffManagementViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) { state.message?.let { onMessage(it); viewModel.consume() } }
    state.form?.let { EmployeeFormScreen(it, state.saving, viewModel::update, viewModel::save, viewModel::close); return }
    val filtered = remember(state.staff, state.query, state.activeOnly) { state.staff.filter { user ->
        (!state.activeOnly || user.active) && (state.query.isBlank() || user.displayName.contains(state.query, true) ||
            user.mobileNumber.contains(state.query)) } }
    Scaffold(topBar = { AppBackTopBar("Staff Management", back) }, floatingActionButton = {
        ExtendedFloatingActionButton(
            onClick = viewModel::create, icon = { Icon(AppIcons.Add, null) }, text = { Text("Add Staff") },
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }) { padding -> Column(Modifier.fillMaxSize().padding(padding).widthIn(max = 560.dp)) {
        OutlinedTextField(state.query, viewModel::search, Modifier.fillMaxWidth().padding(16.dp), singleLine = true,
            leadingIcon = { Icon(AppIcons.Search, null) }, placeholder = { Text("Search staff") })
        FilterChip(state.activeOnly, { viewModel.activeOnly(!state.activeOnly) }, { Text("Active only") },
            modifier = Modifier.padding(horizontal = 16.dp))
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null -> ContentStateView(ContentState.Error(description = state.error), onAction = viewModel::load)
            filtered.isEmpty() -> ContentStateView(ContentState.Empty(title = "No staff found", description = "Create an employee account."))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = User::id) { user -> ElevatedCard(onClick = { viewModel.edit(user) }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(AppIcons.Admin, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary) }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(user.displayName, fontWeight = FontWeight.Bold); Text(user.mobileNumber)
                            Text("${if (user.active) "Active" else "Inactive"} · ${user.permissions.size} permissions",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }; Icon(AppIcons.ArrowRight, null)
                    }
                } }
            }
        }
    } }
}

@Composable
private fun EmployeeFormScreen(form: EmployeeForm, saving: Boolean, update: (EmployeeForm) -> Unit,
    save: () -> Unit, back: () -> Unit) = Scaffold(
    topBar = { AppBackTopBar(if (form.id == null) "Add Staff" else "Staff Details", back) },
    bottomBar = { Button(save, Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), enabled = !saving) {
        Text(if (form.id == null) "Create Staff" else "Save Changes") } },
) { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)) {
    item { OutlinedTextField(form.name, { update(form.copy(name = it.take(80))) }, Modifier.fillMaxWidth(), label = { Text("Full Name *") }) }
    item { OutlinedTextField(form.mobile, { update(form.copy(mobile = it.take(14))) }, Modifier.fillMaxWidth(), label = { Text("Mobile Number *") }) }
    item { OutlinedTextField(form.email, { update(form.copy(email = it.take(120))) }, Modifier.fillMaxWidth(), label = { Text("Email (optional)") }) }
    item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) {
        Text("Active status", fontWeight = FontWeight.SemiBold); Text("Inactive staff cannot sign in")
    }; Switch(form.active, { update(form.copy(active = it)) }, colors = appSwitchColors()) } }
    item { Text("Configure Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    items(StaffPermission.entries.filterNot { it == StaffPermission.GENERATE_INVOICE }) { permission -> val selected = permission in form.permissions
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(permission.label(), fontWeight = FontWeight.Medium); Text(permission.description(),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(selected, { checked -> update(form.copy(permissions = if (checked)
                form.permissions + permission else form.permissions - permission)) }, colors = appSwitchColors())
        }
    }
} }

private fun StaffPermission.label() = name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
private fun StaffPermission.description() = when (this) {
    StaffPermission.VIEW_ORDERS -> "View shop orders"
    StaffPermission.UPDATE_ORDER_STATUS -> "Move orders through fulfilment"
    StaffPermission.ADJUST_FINAL_BILL -> "Confirm final item weight and amount"
    StaffPermission.GENERATE_INVOICE -> "Invoice access is customer-only"
    StaffPermission.MANAGE_PRODUCTS -> "Create and edit products"
    StaffPermission.MANAGE_STOCK -> "Update stock availability"
    StaffPermission.MANAGE_FAQ -> "Manage customer FAQs"
}

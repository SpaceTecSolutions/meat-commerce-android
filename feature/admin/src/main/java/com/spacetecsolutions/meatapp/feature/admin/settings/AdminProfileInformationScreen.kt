package com.spacetecsolutions.meatapp.feature.admin.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.theme.*

@Composable
fun AdminProfileInformationRoute(
    back: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: AdminProfileViewModel = hiltViewModel(),
) {
    HideAppBottomBar()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(state.saved) {
        if (state.saved) { editing = false; onSaved("Profile updated"); viewModel.consumeSaved() }
    }
    val leave: () -> Unit = { if (editing) { editing = false; viewModel.refresh(); Unit } else back() }
    Scaffold(containerColor = AdminAccountColors.canvas,
        topBar = { Row(Modifier.fillMaxWidth().height(64.dp).background(Color.White)
            .statusBarsPadding()
            .padding( horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(leave) { Icon(AppIcons.Back, "Back") }
            Text("Profile Information", Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!editing) OutlinedButton({ editing = true }, shape = CircleShape,
                border = BorderStroke(1.dp, Color(0xFFFECACA))) {
                Icon(AppIcons.Edit, null, Modifier.size(17.dp), tint = AdminAccountColors.red)
                Text("Edit", color = AdminAccountColors.red)
            }
        } },
        bottomBar = { if (editing) Surface(color = Color.Transparent) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                Button(viewModel::save, Modifier.fillMaxWidth().height(54.dp), enabled = !state.saving,
                    shape = RoundedCornerShape(12.dp)) { Text(if (state.saving) "Saving…" else "Save Profile") }
                TextButton(leave, Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
            }
        } },
    ) { padding ->
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.error != null && state.mobile.isBlank() -> ContentStateView(
                ContentState.Error(description = state.error), onAction = viewModel::refresh,
            )
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 440.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            ) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), color = Color.White,
                    border = BorderStroke(1.dp, AdminAccountColors.border), shadowElevation = 2.dp) {
                    Column(Modifier.padding(18.dp)) {
                        Text(listOf(state.firstName, state.lastName).filter(String::isNotBlank)
                            .joinToString(" "), style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = AdminAccountColors.ink)
                        Text(state.mobile, color = AdminAccountColors.muted)
                    }
                }
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), color = Color.White,
                    border = BorderStroke(1.dp, AdminAccountColors.border), shadowElevation = 2.dp) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("PERSONAL DETAILS", color = AdminAccountColors.muted,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        if (!editing) Text("Read-Only  ·  Tap Edit to change",
                            style = MaterialTheme.typography.labelSmall, color = AdminAccountColors.muted)
                        OutlinedTextField(state.firstName, viewModel::firstName, Modifier.fillMaxWidth(),
                            label = { Text("First Name") }, singleLine = true, enabled = editing && !state.saving)
                        OutlinedTextField(state.lastName, viewModel::lastName, Modifier.fillMaxWidth(),
                            label = { Text("Last Name") }, singleLine = true, enabled = editing && !state.saving)
                    }
                }
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), color = Color.White,
                    border = BorderStroke(1.dp, AdminAccountColors.border), shadowElevation = 2.dp) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("SECURITY & IDENTIFIER", color = AdminAccountColors.muted,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(state.mobile, {}, Modifier.fillMaxWidth(),
                            label = { Text("Login Mobile Number") }, readOnly = true, enabled = false)
                        OutlinedTextField("Administrator", {}, Modifier.fillMaxWidth(),
                            label = { Text("Account Role") }, readOnly = true, enabled = false)
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

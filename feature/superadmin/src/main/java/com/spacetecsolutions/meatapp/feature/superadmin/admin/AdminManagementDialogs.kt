package com.spacetecsolutions.meatapp.feature.superadmin.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*

@Composable
fun AdminFormDialog(
    form: AdminFormState,
    submitting: Boolean,
    onNameChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Scaffold(
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = { AdminFormHeader(if (form.editing) "Edit Admin" else "Add Admin", onDismiss) },
                bottomBar = { AdminFormActions(form.editing, submitting, onDismiss, onSave) },
            ) { padding ->
                AdminFormContent(
                    form, submitting, onNameChange, onMobileChange, onPasswordChange,
                    Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun AdminFormHeader(title: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(AppIcons.Back, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_back_b52b36b), Modifier.size(20.dp))
        }
        Text(
            title,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AdminFormContent(
    form: AdminFormState,
    submitting: Boolean,
    onNameChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val appName = remember(context) { context.applicationInfo.loadLabel(context.packageManager).toString() }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.medium)
            .wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = AppDimensions.formMaxWidth),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.compact),
    ) {
        LabeledReadOnlyField("Client", appName, AppIcons.ArrowDown)
        LabeledAdminField(
            label = "Admin Name",
            placeholder = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_enter_admin_name_1a09df2),
            value = form.name,
            onValueChange = onNameChange,
            error = form.nameError,
            enabled = !submitting,
        )
        LabeledAdminField(
            label = "Mobile Number",
            placeholder = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_enter_mobile_number_ad0aef6),
            value = form.mobile,
            onValueChange = onMobileChange,
            error = form.mobileError,
            enabled = !submitting,
            keyboardType = KeyboardType.Phone,
        )
        if (!form.editing) {
            LabeledAdminField(
                label = "Password",
                placeholder = androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_enter_password_570591c),
                value = form.password,
                onValueChange = onPasswordChange,
                error = form.passwordError,
                enabled = !submitting,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) AppIcons.PasswordVisible else AppIcons.PasswordHidden, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_show_password_044b852), Modifier.size(18.dp))
                    }
                },
            )
        }
        LabeledReadOnlyField("Status", "Active", AppIcons.ArrowDown)
    }
}

@Composable
private fun LabeledAdminField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable (() -> Unit))? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            supportingText = error?.let { { Text(it) } },
            isError = error != null,
            enabled = enabled,
            singleLine = true,
            shape = AppShapes.extraSmall,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
        )
    }
}

@Composable
private fun LabeledReadOnlyField(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            shape = AppShapes.extraSmall,
            trailingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
        )
    }
}

@Composable
private fun AdminFormActions(
    editing: Boolean,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(AppSpacing.medium),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = onDismiss, enabled = !submitting, modifier = Modifier.width(104.dp)) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_cancel_77dfd21)) }
            Spacer(Modifier.width(AppSpacing.small))
            Button(onClick = onSave, enabled = !submitting, modifier = Modifier.width(124.dp)) {
                if (submitting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(if (editing) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_save_admin_ae2b349) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_save_admin_ae2b349))
            }
        }
    }
}

@Composable
fun AdminStatusDialog(
    confirmation: AdminStatusConfirmation,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val action = if (confirmation.activate) "Activate" else "Deactivate"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_action_admin_89631df, action)) },
        text = { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_admin_status_confirmation,
            confirmation.admin.displayName, androidx.compose.ui.res.stringResource(
                if (confirmation.activate) com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_regain_access
                else com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_lose_access))) },
        confirmButton = { Button(onClick = onConfirm, enabled = !submitting) { Text(action) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !submitting) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.superadmin.R.string.ui_cancel_77dfd21)) } },
    )
}

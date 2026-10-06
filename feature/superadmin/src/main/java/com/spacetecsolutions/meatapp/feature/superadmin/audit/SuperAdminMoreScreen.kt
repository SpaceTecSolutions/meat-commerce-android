package com.spacetecsolutions.meatapp.feature.superadmin.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.User

@Composable
fun SuperAdminMoreScreen(
    user: User?,
    openAudit: () -> Unit,
    openFeatures: () -> Unit,
    openPayments: () -> Unit,
    openNotifications: () -> Unit,
    logout: () -> Unit,
) {
    var confirmLogout by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("Profile & Settings")
        Column(
            Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = AppDimensions.formMaxWidth).verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
        ) {
            SettingsProfileHeader(user?.displayName ?: "Super Admin", user?.mobileNumber.orEmpty())
            SettingsMenuCard {
                SettingsMenuRow(AppIcons.Security, "Audit Log", openAudit)
                SettingsMenuRow(AppIcons.Settings, "Feature Management", openFeatures)
                SettingsMenuRow(AppIcons.Card, "Payment Methods", openPayments)
                SettingsMenuRow(AppIcons.Notifications, "Notifications", openNotifications)
                SettingsMenuRow(
                    AppIcons.Logout, "Logout", { confirmLogout = true },
                    destructive = true, showDivider = false,
                )
            }
        }
    }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("Logout?") },
        text = { Text("You will need to sign in again to access Super Admin.") },
        confirmButton = { Button({ confirmLogout = false; logout() }) { Text("Logout") } },
        dismissButton = { TextButton({ confirmLogout = false }) { Text("Cancel") } },
    )
}

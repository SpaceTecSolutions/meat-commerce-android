package com.spacetecsolutions.meatapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.model.StaffCapabilities
import com.spacetecsolutions.meatapp.core.model.User
import com.spacetecsolutions.meatapp.core.navigation.StaffRoutes

@Composable
internal fun StaffMoreScreen(user: User, navigate: (String) -> Unit, logout: () -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }
    val capabilities = remember(user.permissions) { StaffCapabilities.from(user.permissions) }
    Scaffold(containerColor = StaffDesign.canvas,
        topBar = { StaffTopBar("Staff Menu", { navigate(StaffRoutes.PROFILE) }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { StaffIdentityCard(user, compact = true) }
            if (capabilities.canManageFaq) {
                item { StaffSectionLabel("Operational Tools", "1 permitted") }
                item { StaffCard(Modifier.fillMaxWidth()) {
                    StaffMenuRow(AppIcons.Help, "FAQ Management", "Manage customer questions and answers",
                        { navigate(StaffRoutes.FAQ) }, emphasis = true)
                } }
            }
            item { StaffSectionLabel("Staff Account & Assistance") }
            item { StaffCard(Modifier.fillMaxWidth()) {
                StaffMenuRow(AppIcons.Admin, "Employee Profile", "View credentials and assigned permissions",
                    { navigate(StaffRoutes.PROFILE) })
                HorizontalDivider(color = StaffDesign.border)
                StaffMenuRow(AppIcons.Contact, "Help & Support", "Account and terminal assistance",
                    { navigate(StaffRoutes.HELP) })
            } }
            item { StaffCard(Modifier.fillMaxWidth()) {
                StaffMenuRow(AppIcons.Logout, "Sign Out", "Sign out of this staff session",
                    { confirmLogout = true }, emphasis = true)
            } }
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = StaffDesign.inset) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.Security, null, Modifier.size(14.dp), tint = StaffDesign.muted)
                            Text(" Secure staff session", style = MaterialTheme.typography.labelSmall,
                                color = StaffDesign.muted)
                        }
                    }
                    Text("Access is controlled by your store administrator",
                        Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall,
                        color = StaffDesign.muted)
                }
            }
        }
    }
    if (confirmLogout) StaffLogoutDialog(user.displayName, { confirmLogout = false }, logout)
}

@Composable
internal fun StaffProfileScreen(user: User, navigate: (String) -> Unit, logout: () -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }
    Scaffold(containerColor = StaffDesign.canvas, topBar = { StaffTopBar("Staff Account") }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StaffCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    StaffInitials(user.displayName, 82.dp)
                    Text(user.displayName, Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(user.mobileNumber, color = StaffDesign.muted)
                    Surface(Modifier.padding(top = 10.dp), shape = CircleShape, color = StaffDesign.paleRed) {
                        Text("●  Active Staff Member", Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium, color = StaffDesign.red)
                    }
                }
            } }
            item { StaffCard(Modifier.fillMaxWidth()) {
                StaffSectionLabel("Staff Information", "Read only")
                StaffInfoRow(AppIcons.Admin, "Full Name", user.displayName)
                StaffInfoRow(AppIcons.Mobile, "Mobile Number", user.mobileNumber)
                user.email?.takeIf(String::isNotBlank)?.let { StaffInfoRow(AppIcons.Email, "Work Email", it) }
                user.shopId?.takeIf(String::isNotBlank)?.let { StaffInfoRow(AppIcons.Shop, "Store", it) }
            } }
            item { StaffSectionLabel("Your Assigned Permissions", "Locked") }
            if (user.permissions.isEmpty()) item { StaffCard(Modifier.fillMaxWidth()) {
                Text("No operational permissions are currently assigned.", color = StaffDesign.muted)
            } } else items(user.permissions.sortedBy { it.ordinal }, key = { it.name }) { permission ->
                Surface(shape = RoundedCornerShape(12.dp), color = StaffDesign.inset) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = StaffDesign.paleRed) {
                            Icon(AppIcons.Check, null, Modifier.padding(6.dp).size(14.dp), tint = StaffDesign.red)
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(permission.title(), fontWeight = FontWeight.SemiBold)
                            Text(permission.description(), style = MaterialTheme.typography.bodySmall,
                                color = StaffDesign.muted)
                        }
                    }
                }
            }
            item {
                Text("To request access changes, contact your store administrator.",
                    style = MaterialTheme.typography.bodySmall, color = StaffDesign.muted)
            }
            item {
                Button({ confirmLogout = true }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StaffDesign.paleRed,
                        contentColor = StaffDesign.red), shape = RoundedCornerShape(10.dp)) {
                    Icon(AppIcons.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log Out")
                }
            }
            item { TextButton({ navigate(StaffRoutes.HELP) }, Modifier.fillMaxWidth()) { Text("Help & Support") } }
        }
    }
    if (confirmLogout) StaffLogoutDialog(user.displayName, { confirmLogout = false }, logout)
}

@Composable
internal fun StaffHelpScreen(back: () -> Unit) {
    Scaffold(containerColor = StaffDesign.canvas, topBar = { StaffTopBar("Help & Support", onBack = back) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)
            .wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StaffCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Contact, null, Modifier.size(30.dp), tint = StaffDesign.red)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("Need assistance?", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Contact your store administrator for access, account, or operational support.",
                            color = StaffDesign.muted)
                    }
                }
            }
            StaffCard(Modifier.fillMaxWidth()) {
                Text("Access requests", fontWeight = FontWeight.SemiBold)
                Text("Permissions can only be changed by an authorized administrator.", color = StaffDesign.muted)
                Text("For urgent order issues, provide the order number and a short description.",
                    color = StaffDesign.muted)
            }
        }
    }
}

@Composable
private fun StaffInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = StaffDesign.inset) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = StaffDesign.muted)
            Column(Modifier.padding(start = 12.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = StaffDesign.muted)
                Text(value, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StaffLogoutDialog(name: String, dismiss: () -> Unit, logout: () -> Unit) = AlertDialog(
    onDismissRequest = dismiss,
    title = { Text("Log out from staff account?") },
    text = { Text("$name will need to verify the account again to access operational tools.") },
    confirmButton = { Button(logout, colors = ButtonDefaults.buttonColors(containerColor = StaffDesign.red)) {
        Text("Log Out")
    } },
    dismissButton = { TextButton(dismiss) { Text("Cancel") } },
)

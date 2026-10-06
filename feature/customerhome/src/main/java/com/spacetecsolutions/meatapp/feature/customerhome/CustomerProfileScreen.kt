package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.User

@Composable
fun CustomerProfileRoute(
    user: User,
    showNotifications: Boolean = true,
    onBack: () -> Unit,
    openProfileInformation: () -> Unit,
    openAddresses: () -> Unit,
    openOrders: () -> Unit,
    openNotifications: () -> Unit,
    openSupport: () -> Unit,
    openFaq: () -> Unit,
    openAbout: () -> Unit,
    openTerms: () -> Unit,
    openPrivacy: () -> Unit,
    openDeleteAccount: () -> Unit,
    logout: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
    }
    var confirmLogout by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(CustomerProfileColors.canvas)
        .statusBarsPadding().navigationBarsPadding()) {
        CustomerProfileHeader(onBack, onHelp = openSupport)
        Column(
            Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 440.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CustomerIdentityCard(user, onEdit = openProfileInformation)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CustomerProfileSectionLabel("Order Management")
                CustomerOrdersEntry(openOrders)
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CustomerProfileSectionLabel("Account & Delivery")
                CustomerProfileGroup {
                    CustomerProfileRow(AppIcons.Location, "Delivery Addresses",
                        "Manage saved home & work addresses", openAddresses,
                        tint = Color(0xFFEA580C), iconBackground = Color(0xFFFFF7ED))
                    CustomerProfileRow(AppIcons.Profile, "Profile Information",
                        "Name, phone & login credentials", openProfileInformation,
                        divider = false, tint = Color(0xFF2563EB), iconBackground = Color(0xFFEFF6FF))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CustomerProfileSectionLabel("Settings & Support")
                CustomerProfileGroup {
                    if (showNotifications) CustomerProfileRow(AppIcons.NotificationsOutline, "Notifications",
                        "Order and delivery alerts", openNotifications,
                        tint = Color(0xFF9333EA), iconBackground = Color(0xFFFAF5FF))
                    CustomerProfileRow(AppIcons.Help, "Help & Customer Support",
                        "Contact us about your order", openSupport,
                        tint = Color(0xFF0D9488), iconBackground = Color(0xFFF0FDFA))
                    CustomerProfileRow(AppIcons.Info, "Frequently Asked Questions",
                        "Quick answers about ordering and delivery", openFaq, divider = false,
                        tint = Color(0xFFEA580C), iconBackground = Color(0xFFFFF7ED))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CustomerProfileSectionLabel("Information")
                CustomerProfileGroup {
                    CustomerProfileRow(AppIcons.Info, "About", "About our shop and services",
                        openAbout)
                    CustomerProfileRow(AppIcons.Terms, "Terms & Conditions", "Orders, refunds and delivery terms",
                        openTerms)
                    CustomerProfileRow(AppIcons.Privacy, "Privacy Policy", "How your information is used",
                        openPrivacy, divider = false)
                }
            }
            /*CustomerProfileGroup {
                CustomerProfileRow(AppIcons.Delete, "Delete Account",
                    "Permanently remove your account", openDeleteAccount, divider = false,
                    tint = CustomerProfileColors.red, iconBackground = Color(0xFFFFEEEE))
            }*/
            CustomerProfileLogout(user.mobileNumber) { confirmLogout = true }
            version?.let { Text("v$it",
                Modifier.fillMaxWidth().padding(top = 12.dp),
                style = MaterialTheme.typography.labelSmall, color = CustomerProfileColors.muted,
                textAlign = TextAlign.Center) }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("Logout?") },
        text = { Text("You will need to sign in again to access your account.") },
        confirmButton = { Button({ confirmLogout = false; logout() }) { Text("Logout") } },
        dismissButton = { TextButton({ confirmLogout = false }) { Text("Cancel") } },
    )
}

@Composable
private fun CustomerProfileSectionLabel(title: String) {
    Text(title.uppercase(), Modifier.fillMaxWidth().padding(start = 3.dp, top = 3.dp),
        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
        color = Color(0xFF89929F))
}

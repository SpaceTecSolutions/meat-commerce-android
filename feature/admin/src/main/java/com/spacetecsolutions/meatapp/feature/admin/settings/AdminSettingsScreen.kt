package com.spacetecsolutions.meatapp.feature.admin.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.User
import com.spacetecsolutions.meatapp.feature.admin.promotion.PromotionManagementRoute
import com.spacetecsolutions.meatapp.feature.admin.promotion.PromotionMessage
import com.spacetecsolutions.meatapp.feature.admin.staff.DeliveryStaffRoute
import com.spacetecsolutions.meatapp.feature.admin.staff.StaffMessage

@Composable
fun AdminSettingsRoute(
    user: User?,
    deliveryStaffAllowed: Boolean,
    staffManagementAllowed: Boolean,
    offersAllowed: Boolean,
    couponsAllowed: Boolean,
    paymentSettingsAllowed: Boolean,
    showNotifications: Boolean,
    onDashboard: () -> Unit,
    onAbout: () -> Unit,
    onDelivery: () -> Unit,
    onPayments: () -> Unit,
    onCategories: () -> Unit,
    onCustomers: () -> Unit,
    onNotifications: () -> Unit,
    onReports: () -> Unit,
    onShopSettings: () -> Unit,
    onAppSettings: () -> Unit,
    onBanners: () -> Unit,
    onProfileInformation: () -> Unit,
    onChangePassword: () -> Unit,
    onHelpSupport: () -> Unit,
    onFaq: () -> Unit,
    onStaffManagement: () -> Unit,
    onLogout: () -> Unit,
    onMessage: (StaffMessage) -> Unit,
    onPromotionMessage: (PromotionMessage) -> Unit,
) {
    var staffOpen by remember { mutableStateOf(false) }
    var promotionsOpen by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    when {
        staffOpen && deliveryStaffAllowed -> DeliveryStaffRoute({ staffOpen = false }, onMessage)
        promotionsOpen && (offersAllowed || couponsAllowed) -> PromotionManagementRoute(
            offersAllowed, couponsAllowed, { promotionsOpen = false }, onPromotionMessage,
        )
        else -> AdminSettingsScreen(
            user, onDashboard, onAbout, onDelivery, if (paymentSettingsAllowed) onPayments else null, onCategories, onCustomers,
            if (showNotifications) onNotifications else null,
            onReports, onShopSettings, onAppSettings, onBanners, onProfileInformation, onChangePassword, onHelpSupport, onFaq,
            if (staffManagementAllowed) onStaffManagement else null,
            { confirmLogout = true },
            if (deliveryStaffAllowed) ({ staffOpen = true }) else null,
            if (offersAllowed || couponsAllowed) ({ promotionsOpen = true }) else null,
        )
    }
    if (confirmLogout) LogoutDialog("Admin", { confirmLogout = false }) {
        confirmLogout = false; onLogout()
    }
}

@Composable
fun AdminSettingsScreen(
    user: User?,
    onDashboard: () -> Unit,
    onAbout: () -> Unit,
    onDelivery: () -> Unit,
    onPayments: (() -> Unit)?,
    onCategories: () -> Unit,
    onCustomers: () -> Unit,
    onNotifications: (() -> Unit)?,
    onReports: () -> Unit,
    onShopSettings: () -> Unit,
    onAppSettings: () -> Unit,
    onBanners: () -> Unit,
    onProfileInformation: () -> Unit,
    onChangePassword: () -> Unit,
    onHelpSupport: () -> Unit,
    onFaq: () -> Unit,
    onStaffManagement: (() -> Unit)?,
    onLogout: () -> Unit,
    onStaff: (() -> Unit)? = null,
    onPromotions: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().background(AdminAccountColors.canvas)) {
        AdminAccountTopBar("Profile & Account", onDashboard, onHelpSupport)
        Column(
            Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 440.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AdminIdentityCard(user, onProfileInformation)
            AdminAccountGroup("Account") {
                AdminAccountRow(AppIcons.Profile, "Profile Information", "Name, phone & login credentials", onProfileInformation)
                AdminAccountRow(AppIcons.Password, "Change Password", "Keep your account secure", onChangePassword, false)
            }
            AdminAccountGroup("Store Management") {
                AdminAccountRow(AppIcons.Categories, "Categories", "Manage product categories", onCategories)
                AdminAccountRow(AppIcons.Offer, "Home Banners", "Manage storefront promotions", onBanners,
                    divider = onPromotions != null)
                onPromotions?.let { AdminAccountRow(AppIcons.Offer, "Offers & Coupons", "Manage offers", it, false) }
            }
            onStaffManagement?.let { open -> AdminAccountGroup("Team") {
                AdminAccountRow(AppIcons.Customers, "Staff Management", "Employees and permissions", open, false)
            } }
            AdminAccountGroup("Business Settings") {
                AdminAccountRow(AppIcons.Delivery, "Delivery Settings", "Slots, charges & live tracking", onDelivery)
                onStaff?.let { AdminAccountRow(AppIcons.Admin, "Delivery Staff", "Manage delivery team", it) }
                onPayments?.let { AdminAccountRow(AppIcons.Cash, "Payment Settings", "Available payment methods", it) }
                AdminAccountRow(AppIcons.Settings, "Shop Settings", "Business profile & preferences", onShopSettings, false)
            }
            onNotifications?.let { open -> AdminAccountGroup("Notifications") {
                AdminAccountRow(AppIcons.NotificationsOutline, "Notifications", "Order and delivery alerts", open, false)
            } }
            AdminAccountGroup("Analytics") {
                AdminAccountRow(AppIcons.Reports, "Reports", "Revenue and order performance", onReports, false)
            }
            AdminAccountGroup("Information & Support") {
                AdminAccountRow(AppIcons.Help, "FAQ Management", "Customer questions and answers", onFaq)
                AdminAccountRow(AppIcons.Help, "Help & Support", "Customer support contacts", onHelpSupport)
                AdminAccountRow(AppIcons.Info, "About", "About our shop and services", onAbout, false)
            }
            AdminAccountGroup("Account Access") {
                AdminAccountRow(AppIcons.Logout, "Log Out", "Signed in as ${user?.mobileNumber.orEmpty()}", onLogout, false)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LogoutDialog(role: String, dismiss: () -> Unit, confirm: () -> Unit) = AlertDialog(
    onDismissRequest = dismiss,
    title = { Text("Logout?") },
    text = { Text("You will need to sign in again to access the $role application.") },
    confirmButton = { Button(confirm) { Text("Logout") } },
    dismissButton = { TextButton(dismiss) { Text("Cancel") } },
)

package com.spacetecsolutions.meatapp

import android.content.Context
import androidx.compose.runtime.Composable
import com.spacetecsolutions.meatapp.core.designsystem.component.AppSnackbarManager
import com.spacetecsolutions.meatapp.core.designsystem.component.AppSnackbarMessage
import com.spacetecsolutions.meatapp.core.designsystem.component.AppSnackbarType
import com.spacetecsolutions.meatapp.core.model.CodActor
import com.spacetecsolutions.meatapp.core.navigation.AdminRoutes
import com.spacetecsolutions.meatapp.feature.admin.customer.AdminCustomersRoute
import com.spacetecsolutions.meatapp.feature.admin.dashboard.AdminDashboardRoute
import com.spacetecsolutions.meatapp.feature.admin.delivery.DeliverySettingsRoute
import com.spacetecsolutions.meatapp.feature.admin.payment.PaymentSettingsRoute
import com.spacetecsolutions.meatapp.feature.admin.reports.AdminReportsRoute
import com.spacetecsolutions.meatapp.feature.admin.settings.AdminSettingsRoute
import com.spacetecsolutions.meatapp.feature.admin.settings.AdminShopSettingsRoute
import com.spacetecsolutions.meatapp.feature.admin.settings.AdminProfileInformationRoute
import com.spacetecsolutions.meatapp.feature.admin.settings.AdminChangePasswordRoute
import com.spacetecsolutions.meatapp.feature.admin.settings.AdminHelpSupportRoute
import com.spacetecsolutions.meatapp.feature.admin.settings.AdminAboutRoute
import com.spacetecsolutions.meatapp.feature.admin.banner.BannerManagementRoute
import com.spacetecsolutions.meatapp.feature.admin.settings.AdminAppSettingsRoute
import com.spacetecsolutions.meatapp.feature.catalog.AdminCategoriesRoute
import com.spacetecsolutions.meatapp.feature.catalog.ProductManagementRoute
import com.spacetecsolutions.meatapp.feature.orders.CodOrdersRoute
import com.spacetecsolutions.meatapp.feature.orders.NotificationHistoryRoute
import com.spacetecsolutions.meatapp.feature.admin.faq.AdminFaqManagementRoute
import com.spacetecsolutions.meatapp.feature.admin.staff.StaffManagementRoute

@Composable
internal fun AdminDestinationContent(
    route: String,
    navigate: (String) -> Unit,
    uiState: AppUiState,
    context: Context,
    snackbar: AppSnackbarManager,
    logout: () -> Unit,
    notificationOrderId: String? = null,
    onNotificationOrderId: (String?) -> Unit = {},
) {
    when (route) {
        AdminRoutes.DASHBOARD -> AdminDashboardRoute(
            onMenu = { navigate(AdminRoutes.MORE) },
            onNotifications = { navigate(AdminRoutes.NOTIFICATIONS) },
            onReports = { navigate(AdminRoutes.REPORTS) },
            onOrders = { navigate(AdminRoutes.ORDERS) },
            onProducts = { navigate(AdminRoutes.PRODUCTS) },
            showNotifications = uiState.featureConfig?.inAppNotificationsEnabled != false,
        )
        AdminRoutes.ORDERS -> CodOrdersRoute(
            actor = CodActor.ADMIN,
            deliveryStaffAllowed = uiState.featureConfig?.deliveryStaffManagementAllowed == true,
            realtimeTrackingAllowed = uiState.featureConfig?.realtimeTrackingAllowed == true,
            onNavigate = { openNavigation(context, it) },
            onCall = { openDialer(context, it) },
            onTrackingCommand = { handleTrackingCommand(context, it) },
            onMessage = { snackbar.show(it.text, it.success) },
            initialOrderId = notificationOrderId,
            onInitialOrderConsumed = { onNotificationOrderId(null) },
        )
        AdminRoutes.PRODUCTS -> ProductManagementRoute(
            onCategories = { navigate(AdminRoutes.CATEGORIES) },
            onMessage = { snackbar.show(it.text, it.success) },
        )
        AdminRoutes.CATEGORIES -> AdminCategoriesRoute(
            onBack = { navigate(AdminRoutes.MORE) },
            onMessage = { snackbar.show(it.text, it.success) },
            subcategoriesAllowed = uiState.featureConfig?.subcategoriesAllowed == true,
        )
        AdminRoutes.MORE -> AdminSettingsRoute(
            user = uiState.authenticatedUser,
            deliveryStaffAllowed = uiState.featureConfig?.deliveryStaffManagementAllowed == true,
            staffManagementAllowed = uiState.featureConfig?.staffManagementAllowed == true,
            offersAllowed = uiState.featureConfig?.offersAllowed == true,
            couponsAllowed = uiState.featureConfig?.couponsAllowed == true,
            paymentSettingsAllowed = uiState.featureConfig?.let {
                it.codAllowed || it.razorpayAllowed || it.upiAllowed
            } == true,
            showNotifications = uiState.featureConfig?.inAppNotificationsEnabled != false,
            onDashboard = { navigate(AdminRoutes.DASHBOARD) },
            onAbout = { navigate(AdminRoutes.ABOUT) },
            onDelivery = { navigate(AdminRoutes.DELIVERY_SETTINGS) },
            onPayments = { navigate(AdminRoutes.PAYMENT_SETTINGS) },
            onCategories = { navigate(AdminRoutes.CATEGORIES) },
            onCustomers = { navigate(AdminRoutes.CUSTOMERS) },
            onNotifications = { navigate(AdminRoutes.NOTIFICATIONS) },
            onReports = { navigate(AdminRoutes.REPORTS) },
            onShopSettings = { navigate(AdminRoutes.SHOP_SETTINGS) },
            onAppSettings = { navigate(AdminRoutes.APP_SETTINGS) },
            onBanners = { navigate(AdminRoutes.BANNERS) },
            onProfileInformation = { navigate(AdminRoutes.PROFILE_INFORMATION) },
            onChangePassword = { navigate(AdminRoutes.CHANGE_PASSWORD) },
            onHelpSupport = { navigate(AdminRoutes.HELP_SUPPORT) },
            onFaq = { navigate(AdminRoutes.FAQ) },
            onStaffManagement = { navigate(AdminRoutes.STAFF) },
            onLogout = logout,
            onMessage = { snackbar.show(it.text, it.success) },
            onPromotionMessage = { snackbar.show(it.text, it.success) },
        )
        AdminRoutes.DELIVERY_SETTINGS -> DeliverySettingsRoute(
            onBack = { navigate(AdminRoutes.MORE) },
            onMessage = { snackbar.show(it.text, it.success) },
        )
        AdminRoutes.PAYMENT_SETTINGS -> PaymentSettingsRoute(
            onBack = { navigate(AdminRoutes.MORE) },
            onMessage = { snackbar.show(it.text, it.success) },
        )
        AdminRoutes.CUSTOMERS -> AdminCustomersRoute(
            onBack = { navigate(AdminRoutes.MORE) },
            onMessage = { snackbar.show(it.text, it.success) },
        )
        AdminRoutes.NOTIFICATIONS -> if (uiState.featureConfig?.inAppNotificationsEnabled != false) NotificationHistoryRoute(
            back = { navigate(AdminRoutes.MORE) },
            customerStyle = true,
            onContinueShopping = { navigate(AdminRoutes.DASHBOARD) },
            emptyActionLabel = "Back to Dashboard",
            onOpen = {
                onNotificationOrderId(it.orderId)
                it.deepLinkRoute?.let(navigate)
            },
        ) else androidx.compose.runtime.LaunchedEffect(Unit) { navigate(AdminRoutes.MORE) }
        AdminRoutes.REPORTS -> AdminReportsRoute(
            back = { navigate(AdminRoutes.MORE) },
        )
        AdminRoutes.SHOP_SETTINGS -> AdminShopSettingsRoute(
            back = { navigate(AdminRoutes.MORE) },
            onMessage = { snackbar.show(AppSnackbarMessage(it, AppSnackbarType.SUCCESS)) },
        )
        AdminRoutes.APP_SETTINGS -> androidx.compose.runtime.LaunchedEffect(Unit) { navigate(AdminRoutes.MORE) }
        AdminRoutes.BANNERS -> BannerManagementRoute(
            back = { navigate(AdminRoutes.MORE) },
            onMessage = { snackbar.show(AppSnackbarMessage(it, AppSnackbarType.INFO)) },
        )
        AdminRoutes.PROFILE_INFORMATION -> AdminProfileInformationRoute(
            back = { navigate(AdminRoutes.MORE) },
            onSaved = { snackbar.show(AppSnackbarMessage(it, AppSnackbarType.SUCCESS)) },
        )
        AdminRoutes.CHANGE_PASSWORD -> AdminChangePasswordRoute(
            back = { navigate(AdminRoutes.MORE) },
            onChanged = { snackbar.show(AppSnackbarMessage(it, AppSnackbarType.SUCCESS)) },
        )
        AdminRoutes.HELP_SUPPORT -> AdminHelpSupportRoute(
            back = { navigate(AdminRoutes.MORE) },
            call = { openDialer(context, it) },
            whatsapp = { openWhatsApp(context, it) },
            email = { openEmail(context, it) },
            onSaved = { snackbar.show(AppSnackbarMessage(it, AppSnackbarType.SUCCESS)) },
        )
        AdminRoutes.ABOUT -> AdminAboutRoute(
            back = { navigate(AdminRoutes.MORE) },
            call = { openDialer(context, it) },
            email = { openEmail(context, it) },
        )
        AdminRoutes.FAQ -> AdminFaqManagementRoute(
            back = { navigate(AdminRoutes.MORE) },
            onMessage = { snackbar.show(AppSnackbarMessage(it, AppSnackbarType.SUCCESS)) },
        )
        AdminRoutes.STAFF -> if (uiState.featureConfig?.staffManagementAllowed == true) StaffManagementRoute(
            back = { navigate(AdminRoutes.MORE) },
            onMessage = { snackbar.show(it.text, it.success) },
        ) else androidx.compose.runtime.LaunchedEffect(Unit) { navigate(AdminRoutes.MORE) }
    }
}

private fun AppSnackbarManager.show(text: String, success: Boolean) = show(
    AppSnackbarMessage(text, if (success) AppSnackbarType.SUCCESS else AppSnackbarType.ERROR),
)

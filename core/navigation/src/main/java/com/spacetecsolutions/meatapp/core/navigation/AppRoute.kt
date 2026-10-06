package com.spacetecsolutions.meatapp.core.navigation

import com.spacetecsolutions.meatapp.core.model.UserRole
import com.spacetecsolutions.meatapp.core.model.StaffCapabilities
import com.spacetecsolutions.meatapp.core.model.StaffPermission
import java.net.URI

enum class NavigationIcon {
    HOME, CATEGORIES, CART, ORDERS, PROFILE, DASHBOARD, ADMINS, REPORTS, MORE, PRODUCTS, HISTORY,
}

data class RoleDestination(
    val route: String,
    val label: String,
    val role: UserRole,
    val icon: NavigationIcon,
)

sealed interface NavigationDecision {
    data class Allowed(val destination: RoleDestination) : NavigationDecision
    data class Denied(val reason: String) : NavigationDecision
}

object RoleNavigationRegistry {
    private fun destination(role: UserRole, path: String, label: String, icon: NavigationIcon) =
        RoleDestination("${role.name.lowercase()}/$path", label, role, icon)

    private val destinations = mapOf(
        UserRole.SUPER_ADMIN to listOf(
            destination(UserRole.SUPER_ADMIN, "dashboard", "Dashboard", NavigationIcon.DASHBOARD),
            destination(UserRole.SUPER_ADMIN, "admins", "Admins", NavigationIcon.ADMINS),
            destination(UserRole.SUPER_ADMIN, "reports", "Reports", NavigationIcon.REPORTS),
            destination(UserRole.SUPER_ADMIN, "more", "More", NavigationIcon.MORE),
        ),
        UserRole.ADMIN to listOf(
            destination(UserRole.ADMIN, "dashboard", "Dashboard", NavigationIcon.DASHBOARD),
            destination(UserRole.ADMIN, "orders", "Orders", NavigationIcon.ORDERS),
            destination(UserRole.ADMIN, "products", "Products", NavigationIcon.PRODUCTS),
            destination(UserRole.ADMIN, "more", "More", NavigationIcon.MORE),
        ),
        UserRole.STAFF to listOf(
            destination(UserRole.STAFF, "home", "Home", NavigationIcon.HOME),
            destination(UserRole.STAFF, "orders", "Orders", NavigationIcon.ORDERS),
            destination(UserRole.STAFF, "products", "Products", NavigationIcon.PRODUCTS),
            destination(UserRole.STAFF, "more", "More", NavigationIcon.MORE),
        ),
        UserRole.DELIVERY to listOf(
            destination(UserRole.DELIVERY, "home", "Home", NavigationIcon.HOME),
            destination(UserRole.DELIVERY, "orders", "Orders", NavigationIcon.ORDERS),
            destination(UserRole.DELIVERY, "history", "History", NavigationIcon.HISTORY),
            destination(UserRole.DELIVERY, "profile", "Profile", NavigationIcon.PROFILE),
        ),
        UserRole.CUSTOMER to listOf(
            destination(UserRole.CUSTOMER, "home", "Home", NavigationIcon.HOME),
            destination(UserRole.CUSTOMER, "categories", "Categories", NavigationIcon.CATEGORIES),
            destination(UserRole.CUSTOMER, "cart", "Cart", NavigationIcon.CART),
            destination(UserRole.CUSTOMER, "orders", "Orders", NavigationIcon.ORDERS),
            destination(UserRole.CUSTOMER, "profile", "Profile", NavigationIcon.PROFILE),
        ),
    )

    private val secondaryDestinations = mapOf(
        UserRole.SUPER_ADMIN to listOf(
            destination(UserRole.SUPER_ADMIN, "audit-log", "Audit Log", NavigationIcon.MORE),
            destination(UserRole.SUPER_ADMIN, "payment-methods", "Payment Methods", NavigationIcon.MORE),
            destination(UserRole.SUPER_ADMIN, "notifications", "Notifications", NavigationIcon.MORE),
            destination(
                UserRole.SUPER_ADMIN,
                "feature-management",
                "Feature Management",
                NavigationIcon.MORE,
            ),
        ),
        UserRole.ADMIN to listOf(
            destination(UserRole.ADMIN, "shop-settings", "Shop Settings", NavigationIcon.MORE),
            destination(UserRole.ADMIN, "profile-information", "Profile Information", NavigationIcon.PROFILE),
            destination(UserRole.ADMIN, "change-password", "Change Password", NavigationIcon.MORE),
            destination(UserRole.ADMIN, "help-support", "Help & Support", NavigationIcon.MORE),
            destination(UserRole.ADMIN, "about", "About Us", NavigationIcon.MORE),
            destination(UserRole.ADMIN, "reports", "Reports", NavigationIcon.REPORTS),
            destination(UserRole.ADMIN, "notifications", "Notifications", NavigationIcon.MORE),
            destination(UserRole.ADMIN, "categories", "Categories", NavigationIcon.CATEGORIES),
            destination(UserRole.ADMIN, "delivery-settings", "Delivery Settings", NavigationIcon.MORE),
            destination(UserRole.ADMIN, "payment-settings", "Payment Settings", NavigationIcon.MORE),
            destination(UserRole.ADMIN, "home-banners", "Home Banners", NavigationIcon.MORE),
            destination(UserRole.ADMIN, "faq-management", "FAQ Management", NavigationIcon.MORE),
            destination(UserRole.ADMIN, "staff-management", "Staff Management", NavigationIcon.MORE),
        ),
        UserRole.STAFF to listOf(
            destination(UserRole.STAFF, "profile", "Profile", NavigationIcon.PROFILE),
            destination(UserRole.STAFF, "faq", "FAQ Management", NavigationIcon.MORE),
            destination(UserRole.STAFF, "help", "Help & Support", NavigationIcon.MORE),
        ),
        UserRole.DELIVERY to listOf(
            destination(UserRole.DELIVERY, "notifications", "Notifications", NavigationIcon.MORE),
            destination(UserRole.DELIVERY, "help", "Help", NavigationIcon.MORE),
        ),
        UserRole.CUSTOMER to listOf(
            destination(UserRole.CUSTOMER, "notifications", "Notifications", NavigationIcon.MORE),
            destination(UserRole.CUSTOMER, "profile-information", "Profile Information", NavigationIcon.PROFILE),
            destination(UserRole.CUSTOMER, "help-support", "Help & Support", NavigationIcon.MORE),
            destination(UserRole.CUSTOMER, "about", "About Us", NavigationIcon.MORE),
            destination(UserRole.CUSTOMER, "terms", "Terms & Conditions", NavigationIcon.MORE),
            destination(UserRole.CUSTOMER, "privacy", "Privacy Policy", NavigationIcon.MORE),
            destination(UserRole.CUSTOMER, "faq", "Frequently Asked Questions", NavigationIcon.MORE),
            destination(UserRole.CUSTOMER, "delete-account", "Delete Account", NavigationIcon.PROFILE),
            destination(UserRole.CUSTOMER, "addresses", "Addresses", NavigationIcon.PROFILE),
            destination(UserRole.CUSTOMER, "checkout", "Checkout", NavigationIcon.CART),
        ),
    )

    fun destinationsFor(role: UserRole): List<RoleDestination> = destinations.getValue(role)

    fun destinationsFor(role: UserRole, staffPermissions: Set<StaffPermission>): List<RoleDestination> {
        if (role != UserRole.STAFF) return destinationsFor(role)
        val capabilities = StaffCapabilities.from(staffPermissions)
        val all = destinations.getValue(UserRole.STAFF).associateBy(RoleDestination::route)
        val secondary = secondaryDestinations.getValue(UserRole.STAFF).associateBy(RoleDestination::route)
        if (capabilities.isOrdersReadOnly) return listOfNotNull(
            all[StaffRoutes.ORDERS], secondary[StaffRoutes.PROFILE],
        )
        return buildList {
            if (capabilities.hasOperationalHome) all[StaffRoutes.HOME]?.let(::add)
            if (capabilities.canViewOrders) all[StaffRoutes.ORDERS]?.let(::add)
            if (capabilities.canOpenProducts) all[StaffRoutes.PRODUCTS]?.let(::add)
            all[StaffRoutes.MORE]?.let(::add)
        }.ifEmpty { listOfNotNull(secondary[StaffRoutes.PROFILE]) }
    }

    fun graphDestinationsFor(role: UserRole): List<RoleDestination> =
        destinationsFor(role) + secondaryDestinations[role].orEmpty()

    fun graphDestinationsFor(role: UserRole, staffPermissions: Set<StaffPermission>): List<RoleDestination> {
        if (role != UserRole.STAFF) return graphDestinationsFor(role)
        val capabilities = StaffCapabilities.from(staffPermissions)
        val allowedRoutes = buildSet {
            addAll(destinationsFor(role, staffPermissions).map(RoleDestination::route))
            add(StaffRoutes.PROFILE)
            add(StaffRoutes.HELP)
            if (capabilities.canManageFaq) add(StaffRoutes.FAQ)
        }
        return (destinations.getValue(role) + secondaryDestinations.getValue(role))
            .filter { it.route in allowedRoutes }
    }

    fun validateRoute(role: UserRole, route: String): NavigationDecision {
        val destination = (destinations.values + secondaryDestinations.values).flatten()
            .firstOrNull { it.route == route }
            ?: return NavigationDecision.Denied("Unknown destination")
        return if (destination.role == role) NavigationDecision.Allowed(destination)
        else NavigationDecision.Denied("This destination is not available for your account")
    }

    fun validateRoute(
        role: UserRole,
        route: String,
        staffPermissions: Set<StaffPermission>,
    ): NavigationDecision {
        if (role != UserRole.STAFF) return validateRoute(role, route)
        val destination = graphDestinationsFor(role, staffPermissions).firstOrNull { it.route == route }
        return if (destination != null) NavigationDecision.Allowed(destination)
        else NavigationDecision.Denied("You do not have permission to access this section")
    }

    fun validateDeepLink(role: UserRole, rawUri: String, expectedScheme: String): NavigationDecision {
        val uri = runCatching { URI(rawUri) }.getOrNull()
            ?: return NavigationDecision.Denied("Invalid app link")
        if (!uri.scheme.equals(expectedScheme, ignoreCase = true) || uri.host != "app") {
            return NavigationDecision.Denied("Untrusted app link")
        }
        return validateRoute(role, uri.path.trim('/'))
    }

    fun validateDeepLink(
        role: UserRole,
        rawUri: String,
        expectedScheme: String,
        staffPermissions: Set<StaffPermission>,
    ): NavigationDecision {
        if (role != UserRole.STAFF) return validateDeepLink(role, rawUri, expectedScheme)
        val uri = runCatching { URI(rawUri) }.getOrNull()
            ?: return NavigationDecision.Denied("Invalid app link")
        if (!uri.scheme.equals(expectedScheme, ignoreCase = true) || uri.host != "app") {
            return NavigationDecision.Denied("Untrusted app link")
        }
        return validateRoute(role, uri.path.trim('/'), staffPermissions)
    }
}

object SuperAdminRoutes {
    const val MORE = "super_admin/more"
    const val AUDIT_LOG = "super_admin/audit-log"
    const val ADMINS = "super_admin/admins"
    const val REPORTS = "super_admin/reports"
    const val FEATURE_MANAGEMENT = "super_admin/feature-management"
    const val PAYMENT_METHODS = "super_admin/payment-methods"
    const val NOTIFICATIONS = "super_admin/notifications"
}

object AdminRoutes {
    const val SHOP_SETTINGS = "admin/shop-settings"
    const val APP_SETTINGS = "admin/app-settings"
    const val PROFILE_INFORMATION = "admin/profile-information"
    const val CHANGE_PASSWORD = "admin/change-password"
    const val HELP_SUPPORT = "admin/help-support"
    const val ABOUT = "admin/about"
    const val REPORTS = "admin/reports"
    const val NOTIFICATIONS = "admin/notifications"
    const val DASHBOARD = "admin/dashboard"
    const val ORDERS = "admin/orders"
    const val PRODUCTS = "admin/products"
    const val CATEGORIES = "admin/categories"
    const val MORE = "admin/more"
    const val DELIVERY_SETTINGS = "admin/delivery-settings"
    const val PAYMENT_SETTINGS = "admin/payment-settings"
    const val CUSTOMERS = "admin/customers"
    const val BANNERS = "admin/home-banners"
    const val FAQ = "admin/faq-management"
    const val STAFF = "admin/staff-management"
}

object DeliveryRoutes {
    const val HOME = "delivery/home"
    const val ORDERS = "delivery/orders"
    const val HISTORY = "delivery/history"
    const val PROFILE = "delivery/profile"
    const val NOTIFICATIONS = "delivery/notifications"
    const val HELP = "delivery/help"
}

object StaffRoutes {
    const val HOME = "staff/home"
    const val ORDERS = "staff/orders"
    const val PRODUCTS = "staff/products"
    const val MORE = "staff/more"
    const val PROFILE = "staff/profile"
    const val FAQ = "staff/faq"
    const val HELP = "staff/help"
}

object CustomerRoutes {
    const val NOTIFICATIONS = "customer/notifications"
    const val PROFILE_INFORMATION = "customer/profile-information"
    const val HELP_SUPPORT = "customer/help-support"
    const val ABOUT = "customer/about"
    const val TERMS = "customer/terms"
    const val PRIVACY = "customer/privacy"
    const val FAQ = "customer/faq"
    const val DELETE_ACCOUNT = "customer/delete-account"
    const val HOME = "customer/home"
    const val CATEGORIES = "customer/categories"
    const val CART = "customer/cart"
    const val ADDRESSES = "customer/addresses"
    const val CHECKOUT = "customer/checkout"
    const val ORDERS = "customer/orders"
    const val PROFILE = "customer/profile"
}

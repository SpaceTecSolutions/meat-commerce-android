package com.spacetecsolutions.meatapp.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.component.BottomBarVisibilityState
import com.spacetecsolutions.meatapp.core.designsystem.component.LocalBottomBarVisibility
import com.spacetecsolutions.meatapp.core.designsystem.layout.AdaptiveContent
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.designsystem.theme.AdminDashboardTokens
import com.spacetecsolutions.meatapp.core.model.UserRole
import com.spacetecsolutions.meatapp.core.model.StaffPermission
import androidx.compose.ui.unit.dp

@Composable
fun AppNavHost(
    authenticatedRole: UserRole,
    staffPermissions: Set<StaffPermission> = emptySet(),
    customerAuthenticated: Boolean = true,
    inAppNotificationsEnabled: Boolean = true,
    pendingCustomerRoute: String? = null,
    onPendingCustomerRouteConsumed: () -> Unit = {},
    onAuthenticationRequired: (String) -> Unit = {},
    onRouteChanged: (String?) -> Unit = {},
    cartBadgeCount: Int = 0,
    deepLink: String? = null,
    deepLinkScheme: String,
    onDeepLinkConsumed: () -> Unit = {},
    onUnauthorized: (String) -> Unit = {},
    destinationContent: @Composable (RoleDestination, (String) -> Unit) -> Unit =
        { destination, _ -> RoleDestinationPlaceholder(destination) },
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val destinations = RoleNavigationRegistry.destinationsFor(authenticatedRole, staffPermissions)
    val entry by navController.currentBackStackEntryAsState()
    LaunchedEffect(entry?.destination?.route) { onRouteChanged(entry?.destination?.route) }
    val bottomBarVisibility = remember { BottomBarVisibilityState() }
    val showBottomBar = authenticatedRole != UserRole.CUSTOMER &&
        entry?.destination?.route in destinations.map(RoleDestination::route) &&
        bottomBarVisibility.visible
    val stitchAdminDashboard = authenticatedRole == UserRole.ADMIN &&
        entry?.destination?.route == AdminRoutes.DASHBOARD
    val stitchAdminOrders = authenticatedRole == UserRole.ADMIN &&
        entry?.destination?.route == AdminRoutes.ORDERS
    val stitchProfileMore = (authenticatedRole == UserRole.ADMIN &&
        entry?.destination?.route == AdminRoutes.MORE) ||
        (authenticatedRole == UserRole.CUSTOMER &&
            entry?.destination?.route == CustomerRoutes.PROFILE)
    val stitchCustomerHome = authenticatedRole == UserRole.CUSTOMER &&
        entry?.destination?.route == CustomerRoutes.HOME
    val stitchCustomerOrders = authenticatedRole == UserRole.CUSTOMER &&
        entry?.destination?.route == CustomerRoutes.ORDERS
    val stitchBottomNav = stitchAdminDashboard || stitchAdminOrders || stitchProfileMore ||
        stitchCustomerHome || stitchCustomerOrders || authenticatedRole == UserRole.STAFF
    androidx.activity.compose.BackHandler(entry?.destination?.route != null &&
        entry?.destination?.route != destinations.first().route) {
        if (!navController.popBackStack()) navigateSafely(navController, authenticatedRole,
            destinations.first().route, staffPermissions, onUnauthorized)
    }

    LaunchedEffect(deepLink, authenticatedRole) {
        if (deepLink != null) {
            when (val decision = RoleNavigationRegistry.validateDeepLink(
                authenticatedRole, deepLink, deepLinkScheme, staffPermissions,
            )) {
                is NavigationDecision.Allowed -> if (authenticatedRole == UserRole.CUSTOMER &&
                    decision.destination.route == CustomerRoutes.NOTIFICATIONS && !inAppNotificationsEnabled) {
                    navController.navigate(CustomerRoutes.HOME) { launchSingleTop = true }
                } else if (authenticatedRole == UserRole.CUSTOMER && !customerAuthenticated &&
                    decision.destination.route in protectedCustomerRoutes) {
                    onAuthenticationRequired(decision.destination.route)
                } else navController.navigate(decision.destination.route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
                is NavigationDecision.Denied -> onUnauthorized(decision.reason)
            }
            onDeepLinkConsumed()
        }
    }

    LaunchedEffect(pendingCustomerRoute, customerAuthenticated) {
        if ((customerAuthenticated || pendingCustomerRoute in setOf(CustomerRoutes.TERMS,
                CustomerRoutes.PRIVACY)) && pendingCustomerRoute != null) {
            navigateSafely(navController, UserRole.CUSTOMER, pendingCustomerRoute, emptySet(), onUnauthorized)
            onPendingCustomerRouteConsumed()
        }
    }
    LaunchedEffect(inAppNotificationsEnabled, entry?.destination?.route) {
        if (authenticatedRole == UserRole.CUSTOMER && !inAppNotificationsEnabled &&
            entry?.destination?.route == CustomerRoutes.NOTIFICATIONS) {
            navigateSafely(navController, UserRole.CUSTOMER, CustomerRoutes.HOME, emptySet(), onUnauthorized)
        }
    }
    val customerNavigate: (String) -> Unit = { route ->
        when {
            authenticatedRole == UserRole.CUSTOMER && route == CustomerRoutes.NOTIFICATIONS &&
                !inAppNotificationsEnabled -> onUnauthorized("Notifications are unavailable right now")
            authenticatedRole == UserRole.CUSTOMER && !customerAuthenticated &&
                route in protectedCustomerRoutes -> onAuthenticationRequired(route)
            else -> navigateSafely(navController, authenticatedRole, route, staffPermissions, onUnauthorized)
        }
    }

    CompositionLocalProvider(LocalBottomBarVisibility provides bottomBarVisibility) { Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) NavigationBar(
                modifier = Modifier.height(64.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = androidx.compose.ui.unit.Dp.Hairline,
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = entry?.destination?.route == destination.route,
                        colors = if (stitchBottomNav) NavigationBarItemDefaults.colors(
                            selectedIconColor = AdminDashboardTokens.red,
                            selectedTextColor = AdminDashboardTokens.red,
                            indicatorColor = AdminDashboardTokens.paleRed,
                            unselectedIconColor = AdminDashboardTokens.muted,
                            unselectedTextColor = AdminDashboardTokens.muted,
                        ) else NavigationBarItemDefaults.colors(),
                        onClick = {
                            customerNavigate(destination.route)
                        },
                        icon = {
                            if (destination.icon == NavigationIcon.CART && cartBadgeCount > 0) {
                                BadgedBox({ Badge { Text(if (cartBadgeCount > 99) "99+" else "$cartBadgeCount") } }) {
                                    Icon(if (stitchBottomNav) destination.icon.stitchImageVector()
                                        else destination.icon.imageVector(), destination.label)
                                }
                            } else Icon(if (stitchBottomNav) destination.icon.stitchImageVector()
                                else destination.icon.imageVector(), destination.label)
                        },
                        label = {
                            Text(
                                destination.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = destinations.first().route,
            modifier = Modifier.fillMaxSize().padding(padding),
            enterTransition = {
                slideInHorizontally(tween(AppMotion.SCREEN_MILLIS)) { it / 10 } +
                    fadeIn(tween(AppMotion.STANDARD_MILLIS))
            },
            exitTransition = { fadeOut(tween(AppMotion.QUICK_MILLIS)) },
            popEnterTransition = {
                slideInHorizontally(tween(AppMotion.SCREEN_MILLIS)) { -it / 10 } +
                    fadeIn(tween(AppMotion.STANDARD_MILLIS))
            },
            popExitTransition = {
                slideOutHorizontally(tween(AppMotion.STANDARD_MILLIS)) { it / 12 } +
                    fadeOut(tween(AppMotion.QUICK_MILLIS))
            },
        ) {
            // Security boundary: the active graph contains only this role's allowlisted routes.
            RoleNavigationRegistry.graphDestinationsFor(authenticatedRole, staffPermissions).forEach { destination ->
                composable(destination.route) {
                    destinationContent(destination) { route ->
                        customerNavigate(route)
                    }
                }
            }
        }
    } }
}

private val protectedCustomerRoutes = setOf(
    CustomerRoutes.CHECKOUT, CustomerRoutes.PROFILE, CustomerRoutes.PROFILE_INFORMATION,
    CustomerRoutes.ADDRESSES, CustomerRoutes.ORDERS, CustomerRoutes.NOTIFICATIONS,
    CustomerRoutes.DELETE_ACCOUNT,
)

private fun navigateSafely(
    controller: NavHostController,
    role: UserRole,
    destination: RoleDestination,
    staffPermissions: Set<StaffPermission>,
    onDenied: (String) -> Unit,
) = navigateSafely(controller, role, destination.route, staffPermissions, onDenied)

private fun navigateSafely(
    controller: NavHostController,
    role: UserRole,
    route: String,
    staffPermissions: Set<StaffPermission>,
    onDenied: (String) -> Unit,
) {
    val home = RoleNavigationRegistry.destinationsFor(role, staffPermissions).first().route
    if (route == home) {
        if (controller.currentDestination?.route == home) return
        if (!controller.popBackStack(home, false)) controller.navigate(home) {
            popUpTo(controller.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
        return
    }
    when (val decision = RoleNavigationRegistry.validateRoute(role, route, staffPermissions)) {
        is NavigationDecision.Allowed -> controller.navigate(decision.destination.route) {
            if (role == UserRole.CUSTOMER && route in setOf(
                    CustomerRoutes.PROFILE, CustomerRoutes.PROFILE_INFORMATION,
                    CustomerRoutes.NOTIFICATIONS, CustomerRoutes.HELP_SUPPORT,
                    CustomerRoutes.ABOUT, CustomerRoutes.TERMS, CustomerRoutes.PRIVACY, CustomerRoutes.FAQ,
                    CustomerRoutes.DELETE_ACCOUNT)) {
                launchSingleTop = true
            } else {
                popUpTo(controller.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = route in RoleNavigationRegistry.destinationsFor(role, staffPermissions).map { it.route }
            }
        }
        is NavigationDecision.Denied -> onDenied(decision.reason)
    }
}

@Composable
fun RoleDestinationPlaceholder(destination: RoleDestination) {
    AdaptiveContent {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(destination.icon.imageVector(), null, tint = MaterialTheme.colorScheme.primary)
            Text(destination.label, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Navigation is ready. Business content is intentionally deferred.",
                modifier = Modifier.padding(top = AppSpacing.small),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun NavigationIcon.imageVector(): ImageVector = when (this) {
    NavigationIcon.HOME -> AppIcons.Home
    NavigationIcon.CATEGORIES -> AppIcons.Categories
    NavigationIcon.CART -> AppIcons.Cart
    NavigationIcon.ORDERS -> AppIcons.Orders
    NavigationIcon.PROFILE -> AppIcons.Profile
    NavigationIcon.DASHBOARD -> AppIcons.Dashboard
    NavigationIcon.ADMINS -> AppIcons.Admin
    NavigationIcon.REPORTS -> AppIcons.Reports
    NavigationIcon.MORE -> AppIcons.MoreHorizontal
    NavigationIcon.PRODUCTS -> AppIcons.Products
    NavigationIcon.HISTORY -> AppIcons.AuditLog
}

private fun NavigationIcon.stitchImageVector(): ImageVector = when (this) {
    NavigationIcon.DASHBOARD -> AppIcons.DashboardOutline
    NavigationIcon.PRODUCTS -> AppIcons.ProductsOutline
    else -> imageVector()
}

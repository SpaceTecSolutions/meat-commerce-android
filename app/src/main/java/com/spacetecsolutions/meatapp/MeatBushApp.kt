package com.spacetecsolutions.meatapp

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.AppSnackbarHost
import com.spacetecsolutions.meatapp.core.designsystem.component.AppSnackbarMessage
import com.spacetecsolutions.meatapp.core.designsystem.component.AppSnackbarManager
import com.spacetecsolutions.meatapp.core.designsystem.component.AppSnackbarType
import com.spacetecsolutions.meatapp.core.designsystem.theme.BrandColors
import com.spacetecsolutions.meatapp.core.designsystem.theme.MeatBushTheme
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.core.navigation.AppNavHost
import com.spacetecsolutions.meatapp.core.navigation.CustomerRoutes
import com.spacetecsolutions.meatapp.core.navigation.DeliveryRoutes
import com.spacetecsolutions.meatapp.core.navigation.RoleDestinationPlaceholder
import com.spacetecsolutions.meatapp.core.navigation.SuperAdminRoutes
import com.spacetecsolutions.meatapp.core.navigation.StaffRoutes
import com.spacetecsolutions.meatapp.feature.auth.ui.SplashScreen
import com.spacetecsolutions.meatapp.feature.superadmin.dashboard.DashboardQuickAction
import com.spacetecsolutions.meatapp.feature.superadmin.dashboard.DashboardUiState
import com.spacetecsolutions.meatapp.feature.superadmin.dashboard.SuperAdminDashboardData
import com.spacetecsolutions.meatapp.feature.superadmin.dashboard.SuperAdminDashboardScreen
import com.spacetecsolutions.meatapp.feature.superadmin.admin.AdminManagementRoute
import com.spacetecsolutions.meatapp.feature.superadmin.features.FeatureManagementRoute
import com.spacetecsolutions.meatapp.feature.superadmin.features.PaymentMethodsRoute
import com.spacetecsolutions.meatapp.feature.superadmin.reports.SuperAdminReportsRoute
import com.spacetecsolutions.meatapp.feature.superadmin.audit.AuditLogRoute
import com.spacetecsolutions.meatapp.feature.superadmin.audit.SuperAdminMoreScreen
import com.spacetecsolutions.meatapp.feature.catalog.CustomerCategoriesRoute
import com.spacetecsolutions.meatapp.feature.customerhome.CustomerHomeRoute
import com.spacetecsolutions.meatapp.feature.customerhome.CustomerProfileRoute
import com.spacetecsolutions.meatapp.feature.customerhome.CustomerProfileInformationRoute
import com.spacetecsolutions.meatapp.feature.customerhome.CustomerSupportRoute
import com.spacetecsolutions.meatapp.feature.customerhome.CustomerAboutRoute
import com.spacetecsolutions.meatapp.feature.customerhome.CustomerLegalRoute
import com.spacetecsolutions.meatapp.feature.customerhome.CustomerDeleteAccountRoute
import com.spacetecsolutions.meatapp.feature.customerhome.CustomerFaqRoute
import com.spacetecsolutions.meatapp.feature.cart.CartRoute
import com.spacetecsolutions.meatapp.feature.address.AddressRoute
import com.spacetecsolutions.meatapp.feature.address.AddressInitialAction
import com.spacetecsolutions.meatapp.feature.checkout.CheckoutRoute
import com.spacetecsolutions.meatapp.feature.orders.CodOrdersRoute
import com.spacetecsolutions.meatapp.feature.orders.DeliveryHomeRoute
import com.spacetecsolutions.meatapp.feature.orders.DeliveryHistoryRoute
import com.spacetecsolutions.meatapp.feature.orders.DeliveryProfileRoute
import com.spacetecsolutions.meatapp.feature.orders.DeliveryNotificationsScreen
import com.spacetecsolutions.meatapp.feature.orders.NotificationHistoryRoute
import com.spacetecsolutions.meatapp.feature.orders.DeliveryHelpScreen
import com.spacetecsolutions.meatapp.feature.orders.CustomerOrdersRoute
import com.spacetecsolutions.meatapp.feature.catalog.ProductManagementRoute
import com.spacetecsolutions.meatapp.feature.admin.faq.AdminFaqManagementRoute
import com.spacetecsolutions.meatapp.core.model.CodActor
import com.spacetecsolutions.meatapp.core.model.UserRole
import com.spacetecsolutions.meatapp.core.model.StaffPermission
import kotlinx.coroutines.delay

@Composable
fun MeatBushApp(
    deepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    notificationOrderId: String? = null,
    onNotificationOrderConsumed: () -> Unit = {},
    viewModel: AppViewModel = hiltViewModel(),
    razorpaySdkResult: com.spacetecsolutions.meatapp.core.model.RazorpaySdkResult? = null,
    onRazorpayResultConsumed: () -> Unit = {},
    launchRazorpay: (com.spacetecsolutions.meatapp.core.model.RazorpayPaymentSession) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authViewModel: CustomerAuthViewModel = hiltViewModel()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    var authDestination by remember { mutableStateOf<String?>(null) }
    var readyDestination by remember { mutableStateOf<String?>(null) }
    var authSheetPaused by remember { mutableStateOf(false) }
    var authLegalOrigin by remember { mutableStateOf<String?>(null) }
    var authSuccessAnimationFinished by remember { mutableStateOf(false) }
    LaunchedEffect(authState.completedId, uiState.session, authSuccessAnimationFinished) {
        val authenticated = uiState.session as? SessionState.Authenticated
        if (authState.phase == CustomerAuthPhase.SUCCESS && authenticated != null &&
            authSuccessAnimationFinished) {
            readyDestination = authDestination.takeIf { authenticated.role == UserRole.CUSTOMER }
            authDestination = null
            authSuccessAnimationFinished = false
            authViewModel.consumeSuccess()
        }
    }
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val snackbarManager = remember { AppSnackbarManager() }
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCustomerProductId by remember { mutableStateOf<String?>(null) }
    var pendingCustomerCategoryId by remember { mutableStateOf<String?>(null) }
    var pendingCustomerCategoryName by remember { mutableStateOf<String?>(null) }
    var pendingCustomerSearch by remember { mutableStateOf<String?>(null) }
    var addressReturnRoute by remember { mutableStateOf(CustomerRoutes.HOME) }
    var checkoutAddressId by remember { mutableStateOf<String?>(null) }
    var pendingAddressAction by remember { mutableStateOf<AddressInitialAction?>(null) }
    var pendingAddressActionId by remember { mutableStateOf<String?>(null) }
    var selectedCheckoutAddressId by remember { mutableStateOf<String?>(null) }
    var selectedCartAddress by remember { mutableStateOf<com.spacetecsolutions.meatapp.core.model.CustomerAddress?>(null) }
    var requestCurrentAddress by remember { mutableStateOf(false) }
    var pendingRoleOrderId by remember { mutableStateOf(notificationOrderId) }
    LaunchedEffect(notificationOrderId) {
        if (notificationOrderId != null) pendingRoleOrderId = notificationOrderId
    }
    val brandColors = BrandColors(
        primary = colorResource(R.color.brand_primary),
        secondary = colorResource(R.color.brand_secondary),
    )

    MeatBushTheme(
        darkTheme = uiState.darkThemeEnabled ?: isSystemInDarkTheme(),
        brandColors = brandColors,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                AppSnackbarHost(
                    manager = snackbarManager,
                    hostState = snackbarHostState,
                )
            },
        ) { padding ->
            Crossfade(
                targetState = uiState.session,
                modifier = Modifier.fillMaxSize(),
                label = "session",
                animationSpec = tween(AppMotion.STANDARD_MILLIS),
            ) { session ->
                when (session) {
                    SessionState.Loading -> SplashScreen(
                        appName = stringResource(R.string.app_name),
                        bgRes = R.drawable.img_splash_bg_image,
                        logoRes = R.drawable.meat_station_app_logo,
                    )
                    is SessionState.Unauthenticated, is SessionState.Authenticated -> key(
                        (session as? SessionState.Authenticated)?.role,
                        session is SessionState.Authenticated,
                    ) { AppNavHost(
                        authenticatedRole = (session as? SessionState.Authenticated)?.role ?: UserRole.CUSTOMER,
                        staffPermissions = uiState.authenticatedUser?.permissions.orEmpty(),
                        customerAuthenticated = session is SessionState.Authenticated,
                        inAppNotificationsEnabled = uiState.featureConfig?.inAppNotificationsEnabled != false,
                        pendingCustomerRoute = readyDestination,
                        onPendingCustomerRouteConsumed = { readyDestination = null },
                        onAuthenticationRequired = { route ->
                            authDestination = route
                            authViewModel.open()
                        },
                        onRouteChanged = { route ->
                            if (authSheetPaused && route != CustomerRoutes.TERMS &&
                                route != CustomerRoutes.PRIVACY) authSheetPaused = false
                        },
                        cartBadgeCount = uiState.cartQuantity,
                        deepLink = deepLink,
                        deepLinkScheme = stringResource(R.string.deep_link_scheme),
                        onDeepLinkConsumed = onDeepLinkConsumed,
                        onUnauthorized = { reason ->
                            snackbarManager.show(
                                AppSnackbarMessage(reason, AppSnackbarType.WARNING),
                            )
                        },
                        destinationContent = { destination, navigate ->
                            when (destination.route) {
                                in com.spacetecsolutions.meatapp.core.navigation.RoleNavigationRegistry
                                    .graphDestinationsFor(com.spacetecsolutions.meatapp.core.model.UserRole.ADMIN)
                                    .map { it.route } -> AdminDestinationContent(
                                        destination.route, navigate, uiState, context, snackbarManager,
                                        viewModel::signOut, pendingRoleOrderId, {
                                            pendingRoleOrderId = it
                                            if (it == null) onNotificationOrderConsumed()
                                        },
                                    )
                                StaffRoutes.HOME -> uiState.authenticatedUser?.let { StaffHomeRoute(it, navigate) }
                                    ?: StaffAccessDenied()
                                StaffRoutes.ORDERS -> if (uiState.authenticatedUser?.permissions?.contains(StaffPermission.VIEW_ORDERS) == true)
                                    CodOrdersRoute(actor = CodActor.ADMIN, onMessage = { message -> snackbarManager.show(
                                        AppSnackbarMessage(message.text, if (message.success) AppSnackbarType.SUCCESS else AppSnackbarType.ERROR)) },
                                        operatorPermissions = uiState.authenticatedUser?.permissions,
                                        staffPresentation = true)
                                else StaffAccessDenied()
                                StaffRoutes.PRODUCTS -> if (uiState.authenticatedUser?.permissions?.any {
                                        it == StaffPermission.MANAGE_PRODUCTS || it == StaffPermission.MANAGE_STOCK
                                    } == true)
                                    ProductManagementRoute(onCategories = {}, onMessage = { message -> snackbarManager.show(
                                        AppSnackbarMessage(message.text, if (message.success) AppSnackbarType.SUCCESS else AppSnackbarType.ERROR)) },
                                        operatorPermissions = uiState.authenticatedUser?.permissions)
                                else StaffAccessDenied()
                                StaffRoutes.MORE -> uiState.authenticatedUser?.let {
                                    StaffMoreScreen(it, navigate, viewModel::signOut)
                                } ?: StaffAccessDenied()
                                StaffRoutes.PROFILE -> uiState.authenticatedUser?.let {
                                    StaffProfileScreen(it, navigate, viewModel::signOut)
                                } ?: StaffAccessDenied()
                                StaffRoutes.HELP -> StaffHelpScreen { navigate(StaffRoutes.MORE) }
                                StaffRoutes.FAQ -> if (uiState.authenticatedUser?.permissions?.contains(StaffPermission.MANAGE_FAQ) == true)
                                    AdminFaqManagementRoute(back = { navigate(StaffRoutes.MORE) }, onMessage = {
                                        snackbarManager.show(AppSnackbarMessage(it, AppSnackbarType.SUCCESS)) })
                                else StaffAccessDenied()
                                "super_admin/dashboard" -> SuperAdminDashboardScreen(
                                    state = DashboardUiState.Content(SuperAdminDashboardData()),
                                    onQuickAction = { action -> navigate(action.route) },
                                    onRetry = {},
                                )
                                SuperAdminRoutes.ADMINS -> AdminManagementRoute(onMessage = { message ->
                                    snackbarManager.show(
                                        AppSnackbarMessage(
                                            message.text,
                                            if (message.success) AppSnackbarType.SUCCESS
                                            else AppSnackbarType.ERROR,
                                        ),
                                    )
                                })
                                SuperAdminRoutes.FEATURE_MANAGEMENT -> FeatureManagementRoute(
                                    onBack = { navigate(SuperAdminRoutes.MORE) },
                                    onMessage = { message ->
                                    snackbarManager.show(
                                        AppSnackbarMessage(
                                            message.text,
                                            if (message.success) AppSnackbarType.SUCCESS
                                            else AppSnackbarType.ERROR,
                                        ),
                                    )
                                    },
                                )
                                SuperAdminRoutes.PAYMENT_METHODS -> PaymentMethodsRoute(
                                    onBack = { navigate(SuperAdminRoutes.MORE) },
                                    onMessage = { message ->
                                        snackbarManager.show(
                                            AppSnackbarMessage(
                                                message.text,
                                                if (message.success) AppSnackbarType.SUCCESS else AppSnackbarType.ERROR,
                                            ),
                                        )
                                    },
                                )
                                SuperAdminRoutes.REPORTS -> SuperAdminReportsRoute()
                                SuperAdminRoutes.NOTIFICATIONS -> NotificationHistoryRoute(
                                    back = { navigate(SuperAdminRoutes.MORE) },
                                    onOpen = { it.deepLinkRoute?.let(navigate) },
                                )
                                SuperAdminRoutes.MORE -> SuperAdminMoreScreen(
                                    user = uiState.authenticatedUser,
                                    openAudit = { navigate(SuperAdminRoutes.AUDIT_LOG) },
                                    openFeatures = { navigate(SuperAdminRoutes.FEATURE_MANAGEMENT) },
                                    openPayments = { navigate(SuperAdminRoutes.PAYMENT_METHODS) },
                                    openNotifications = { navigate(SuperAdminRoutes.NOTIFICATIONS) },
                                    logout = viewModel::signOut,
                                )
                                SuperAdminRoutes.AUDIT_LOG -> AuditLogRoute(
                                    back = { navigate(SuperAdminRoutes.MORE) },
                                )
                                DeliveryRoutes.HOME -> DeliveryHomeRoute(
                                    openOrders = { navigate(DeliveryRoutes.ORDERS) },
                                    openHistory = { navigate(DeliveryRoutes.HISTORY) },
                                    openNotifications = { navigate(DeliveryRoutes.NOTIFICATIONS) },
                                )
                                DeliveryRoutes.ORDERS -> CodOrdersRoute(
                                    actor = CodActor.DELIVERY,
                                    realtimeTrackingAllowed = uiState.featureConfig?.realtimeTrackingAllowed == true,
                                    onNavigate = { address -> openNavigation(context, address) },
                                    onTrackingCommand = { handleTrackingCommand(context, it) },
                                    onMessage = { message ->
                                        snackbarManager.show(AppSnackbarMessage(
                                            message.text,
                                            if (message.success) AppSnackbarType.SUCCESS else AppSnackbarType.ERROR,
                                        ))
                                    },
                                    initialOrderId = pendingRoleOrderId,
                                    onInitialOrderConsumed = {
                                        pendingRoleOrderId = null
                                        onNotificationOrderConsumed()
                                    },
                                )
                                DeliveryRoutes.HISTORY -> DeliveryHistoryRoute()
                                DeliveryRoutes.PROFILE -> DeliveryProfileRoute(
                                    notifications = { navigate(DeliveryRoutes.NOTIFICATIONS) },
                                    help = { navigate(DeliveryRoutes.HELP) },
                                    logout = viewModel::signOut,
                                )
                                DeliveryRoutes.NOTIFICATIONS -> DeliveryNotificationsScreen(
                                    back = { navigate(DeliveryRoutes.PROFILE) },
                                    onOpen = {
                                        pendingRoleOrderId = it.orderId
                                        it.deepLinkRoute?.let(navigate)
                                    },
                                )
                                DeliveryRoutes.HELP -> DeliveryHelpScreen(
                                    back = { navigate(DeliveryRoutes.PROFILE) },
                                )
                                CustomerRoutes.HOME -> CustomerHomeRoute(
                                    onLocation = {
                                        addressReturnRoute = CustomerRoutes.HOME
                                        navigate(CustomerRoutes.ADDRESSES)
                                    },
                                    onNotifications = { navigate(CustomerRoutes.NOTIFICATIONS) },
                                    onProfile = { navigate(CustomerRoutes.PROFILE) },
                                    onCart = { navigate(CustomerRoutes.CART) },
                                    profileName = uiState.authenticatedUser?.displayName.orEmpty(),
                                    showNotifications = uiState.featureConfig?.inAppNotificationsEnabled != false,
                                    isGuest = session is SessionState.Unauthenticated,
                                    onSearch = {
                                        pendingCustomerSearch = it
                                        navigate(CustomerRoutes.CATEGORIES)
                                    },
                                    onViewAllCategories = { navigate(CustomerRoutes.CATEGORIES) },
                                    onCategory = { id, name ->
                                        pendingCustomerCategoryId = id
                                        pendingCustomerCategoryName = name
                                        navigate(CustomerRoutes.CATEGORIES)
                                    },
                                    onProduct = {
                                        pendingCustomerProductId = it
                                        navigate(CustomerRoutes.CATEGORIES)
                                    },
                                    onAbout = { navigate(CustomerRoutes.ABOUT) },
                                    onFaq = { navigate(CustomerRoutes.FAQ) },
                                    onPrivacy = { navigate(CustomerRoutes.PRIVACY) },
                                    onTerms = { navigate(CustomerRoutes.TERMS) },
                                    onUseCurrentLocation = {
                                        requestCurrentAddress = true
                                        addressReturnRoute = CustomerRoutes.HOME
                                        navigate(CustomerRoutes.ADDRESSES)
                                    },
                                )
                                CustomerRoutes.CATEGORIES -> CustomerCategoriesRoute(
                                    onBack = { navigate(CustomerRoutes.HOME) },
                                    onCart = { navigate(CustomerRoutes.CART) },
                                    initialProductId = pendingCustomerProductId,
                                    onInitialProductConsumed = { pendingCustomerProductId = null },
                                    initialCategoryId = pendingCustomerCategoryId,
                                    initialCategoryName = pendingCustomerCategoryName,
                                    onInitialCategoryConsumed = {
                                        pendingCustomerCategoryId = null
                                        pendingCustomerCategoryName = null
                                    },
                                    initialSearchQuery = pendingCustomerSearch,
                                    onInitialSearchConsumed = { pendingCustomerSearch = null },
                                    onMessage = { message ->
                                        snackbarManager.show(AppSnackbarMessage(
                                            message.text,
                                            if (message.success) AppSnackbarType.SUCCESS else AppSnackbarType.ERROR,
                                        ))
                                    },
                                )
                                CustomerRoutes.CART -> CartRoute(
                                    onBack = { navigate(CustomerRoutes.HOME) },
                                    onStartShopping = { navigate(CustomerRoutes.CATEGORIES) },
                                    onChangeAddress = {
                                        addressReturnRoute = CustomerRoutes.CART
                                        navigate(CustomerRoutes.ADDRESSES)
                                    },
                                    onProduct = {
                                        pendingCustomerProductId = it
                                        navigate(CustomerRoutes.CATEGORIES)
                                    },
                                    onCheckout = {
                                        selectedCartAddress?.id?.let { checkoutAddressId = it }
                                        navigate(CustomerRoutes.CHECKOUT)
                                    },
                                    selectedAddress = selectedCartAddress,
                                    onMessage = { message ->
                                        snackbarManager.show(AppSnackbarMessage(
                                            message.text,
                                            if (message.success) AppSnackbarType.SUCCESS else AppSnackbarType.WARNING,
                                        ))
                                    },
                                )
                                CustomerRoutes.ORDERS -> CustomerOrdersRoute(
                                    onStartShopping = { navigate(CustomerRoutes.CATEGORIES) },
                                    onHelp = { navigate(CustomerRoutes.PROFILE) },
                                    onBack = { navigate(CustomerRoutes.PROFILE) },
                                    onNavigate = { address -> openNavigation(context, address) },
                                    onCall = { mobile -> openDialer(context, mobile) },
                                    onMessage = { message -> snackbarManager.show(AppSnackbarMessage(
                                        message.text,
                                        if (message.success) AppSnackbarType.SUCCESS else AppSnackbarType.ERROR,
                                    )) },
                                    initialOrderId = pendingRoleOrderId,
                                    onInitialOrderConsumed = {
                                        pendingRoleOrderId = null
                                        onNotificationOrderConsumed()
                                    },
                                )
                                CustomerRoutes.PROFILE -> uiState.authenticatedUser?.let { user ->
                                    CustomerProfileRoute(
                                        user = user,
                                        showNotifications = uiState.featureConfig?.inAppNotificationsEnabled != false,
//                                        onBack = { backDispatcher?.onBackPressed() },
                                        onBack = { navigate(CustomerRoutes.HOME) },
                                        openProfileInformation = { navigate(CustomerRoutes.PROFILE_INFORMATION) },
                                        openAddresses = {
                                            addressReturnRoute = CustomerRoutes.PROFILE
                                            navigate(CustomerRoutes.ADDRESSES)
                                        },
                                        openOrders = { navigate(CustomerRoutes.ORDERS) },
                                        openNotifications = { navigate(CustomerRoutes.NOTIFICATIONS) },
                                        openSupport = { navigate(CustomerRoutes.HELP_SUPPORT) },
                                        openFaq = { navigate(CustomerRoutes.FAQ) },
                                        openAbout = { navigate(CustomerRoutes.ABOUT) },
                                        openTerms = { navigate(CustomerRoutes.TERMS) },
                                        openPrivacy = { navigate(CustomerRoutes.PRIVACY) },
                                        openDeleteAccount = { navigate(CustomerRoutes.DELETE_ACCOUNT) },
                                        logout = viewModel::signOut,
                                    )
                                }
                                CustomerRoutes.PROFILE_INFORMATION -> uiState.authenticatedUser?.let { user ->
                                    CustomerProfileInformationRoute(user,
                                        back = { backDispatcher?.onBackPressed() },
                                        openDeleteAccount = { navigate(CustomerRoutes.DELETE_ACCOUNT) })
                                }
                                CustomerRoutes.HELP_SUPPORT -> CustomerSupportRoute(
                                    back = { backDispatcher?.onBackPressed() },
                                    call = { openDialer(context, it) },
                                    whatsapp = { openWhatsApp(context, it) },
                                    email = { openEmail(context, it) },
                                    onStaffSignIn = null)
                                CustomerRoutes.FAQ -> CustomerFaqRoute(
                                    back = { backDispatcher?.onBackPressed() },
                                    contactSupport = { navigate(CustomerRoutes.HELP_SUPPORT) },
                                )
                                CustomerRoutes.ABOUT -> CustomerAboutRoute(
                                    back = { backDispatcher?.onBackPressed() },
                                    call = { openDialer(context, it) },
                                    email = { openEmail(context, it) },
                                    openMap = { openNavigation(context, it) })
                                CustomerRoutes.TERMS -> CustomerLegalRoute(false,
                                    back = { backDispatcher?.onBackPressed()
                                        if (authLegalOrigin == CustomerRoutes.TERMS) authSheetPaused = false },
                                    openOtherPolicy = { navigate(CustomerRoutes.PRIVACY) },
                                    openSupport = { navigate(CustomerRoutes.HELP_SUPPORT) },
                                    openDeleteAccount = { navigate(CustomerRoutes.DELETE_ACCOUNT) })
                                CustomerRoutes.PRIVACY -> CustomerLegalRoute(true,
                                    back = { backDispatcher?.onBackPressed()
                                        if (authLegalOrigin == CustomerRoutes.PRIVACY) authSheetPaused = false },
                                    openOtherPolicy = { navigate(CustomerRoutes.TERMS) },
                                    openSupport = { navigate(CustomerRoutes.HELP_SUPPORT) },
                                    openDeleteAccount = { navigate(CustomerRoutes.DELETE_ACCOUNT) })
                                CustomerRoutes.DELETE_ACCOUNT -> uiState.authenticatedUser?.let { user ->
                                    CustomerDeleteAccountRoute(user,
                                        back = { backDispatcher?.onBackPressed() },
                                        viewOrder = { id ->
                                            pendingRoleOrderId = id
                                            navigate(CustomerRoutes.ORDERS)
                                        },
                                        openSupport = { navigate(CustomerRoutes.HELP_SUPPORT) },
                                        openPrivacy = { navigate(CustomerRoutes.PRIVACY) },
                                        onDeleted = viewModel::signOut)
                                }
                                CustomerRoutes.NOTIFICATIONS -> if (uiState.featureConfig?.inAppNotificationsEnabled != false)
                                    NotificationHistoryRoute(
                                    back = { backDispatcher?.onBackPressed() },
                                    customerStyle = true,
                                    onContinueShopping = { navigate(CustomerRoutes.HOME) },
                                    onOpen = {
                                        pendingRoleOrderId = it.orderId
                                        it.deepLinkRoute?.let(navigate)
                                    },
                                )
                                CustomerRoutes.CHECKOUT -> CheckoutRoute(
                                    onBack = { navigate(CustomerRoutes.CART) },
                                    onManageAddresses = {
                                        addressReturnRoute = CustomerRoutes.CHECKOUT
                                        navigate(CustomerRoutes.ADDRESSES)
                                    },
                                    onEditAddress = { id ->
                                        addressReturnRoute = CustomerRoutes.CHECKOUT
                                        pendingAddressAction = AddressInitialAction.EDIT
                                        pendingAddressActionId = id
                                        navigate(CustomerRoutes.ADDRESSES)
                                    },
                                    onDeleteAddress = { id ->
                                        addressReturnRoute = CustomerRoutes.CHECKOUT
                                        pendingAddressAction = AddressInitialAction.DELETE
                                        pendingAddressActionId = id
                                        navigate(CustomerRoutes.ADDRESSES)
                                    },
                                    onViewOrders = { navigate("customer/orders") },
                                    onContinueShopping = { navigate(CustomerRoutes.CATEGORIES) },
                                    razorpaySdkResult = razorpaySdkResult,
                                    onRazorpayResultConsumed = onRazorpayResultConsumed,
                                    launchRazorpay = launchRazorpay,
                                    selectedAddressId = checkoutAddressId,
                                    onSelectedAddressConsumed = { checkoutAddressId = null },
                                    onMessage = { message ->
                                        snackbarManager.show(AppSnackbarMessage(
                                            message.text,
                                            if (message.success) AppSnackbarType.SUCCESS else AppSnackbarType.ERROR,
                                        ))
                                    },
                                )
                                CustomerRoutes.ADDRESSES -> AddressRoute(
                                    onBack = { navigate(addressReturnRoute) },
                                    onSelect = if (addressReturnRoute == CustomerRoutes.CHECKOUT) ({ id ->
                                        checkoutAddressId = id
                                        selectedCheckoutAddressId = id
                                        navigate(CustomerRoutes.CHECKOUT)
                                    }) else null,
                                    onSelectAddress = if (addressReturnRoute == CustomerRoutes.CART) ({ address ->
                                        selectedCartAddress = address
                                        navigate(CustomerRoutes.CART)
                                    }) else null,
                                    resolveCurrentLocation = { resolveCustomerLocation(context) },
                                    requestCurrentLocationOnStart = requestCurrentAddress,
                                    onCurrentLocationRequestConsumed = { requestCurrentAddress = false },
                                    initialAction = pendingAddressAction,
                                    onInitialActionConsumed = {
                                        pendingAddressAction = null
                                        pendingAddressActionId = null
                                    },
                                    initialSelectedAddressId = when (addressReturnRoute) {
                                        CustomerRoutes.CHECKOUT -> pendingAddressActionId ?: selectedCheckoutAddressId
                                        CustomerRoutes.CART -> selectedCartAddress?.id
                                        else -> null
                                    },
                                    onMessage = { message ->
                                        snackbarManager.show(AppSnackbarMessage(
                                            message.text,
                                            if (message.success) AppSnackbarType.SUCCESS else AppSnackbarType.ERROR,
                                        ))
                                    },
                                )
                                else -> RoleDestinationPlaceholder(destination)
                            }
                        },
                    ) }
                    is SessionState.Error -> com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView(
                        state = com.spacetecsolutions.meatapp.core.designsystem.component.ContentState.Error(
                            description = session.message,
                            retryLabel = null,
                        ),
                        onAction = viewModel::retrySession,
                    )
                }
            }
            if (authState.open && !authSheetPaused) CustomerAuthSheet(authState,
                onDismiss = { authViewModel.close(); authDestination = null },
                onPhone = authViewModel::phone, onRequest = authViewModel::requestOtp,
                onOtp = authViewModel::otp, onChangeNumber = authViewModel::changeNumber,
                onRetryOtp = authViewModel::retryOtp,
                onSuccessAnimationFinished = { authSuccessAnimationFinished = true },
                onTerms = { authSheetPaused = true; authLegalOrigin = CustomerRoutes.TERMS
                    readyDestination = CustomerRoutes.TERMS },
                onPrivacy = { authSheetPaused = true; authLegalOrigin = CustomerRoutes.PRIVACY
                    readyDestination = CustomerRoutes.PRIVACY })
        }
    }
}

private fun showInfo(manager: AppSnackbarManager, text: String) {
    manager.show(AppSnackbarMessage(text, AppSnackbarType.INFO))
}

private val DashboardQuickAction.route: String
    get() = when (this) {
        DashboardQuickAction.ADMINS -> SuperAdminRoutes.ADMINS
        DashboardQuickAction.REPORTS -> SuperAdminRoutes.REPORTS
        DashboardQuickAction.PRODUCT_LIMIT -> SuperAdminRoutes.FEATURE_MANAGEMENT
        DashboardQuickAction.FEATURE_MANAGEMENT -> SuperAdminRoutes.FEATURE_MANAGEMENT
    }

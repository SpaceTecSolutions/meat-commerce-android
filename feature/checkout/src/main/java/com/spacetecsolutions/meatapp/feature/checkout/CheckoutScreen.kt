package com.spacetecsolutions.meatapp.feature.checkout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.RazorpayPaymentSession
import com.spacetecsolutions.meatapp.core.model.RazorpaySdkResult

@Composable
fun CheckoutRoute(
    onBack: () -> Unit,
    onManageAddresses: () -> Unit,
    onEditAddress: (String) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onViewOrders: () -> Unit,
    onContinueShopping: () -> Unit,
    onMessage: (CheckoutMessage) -> Unit,
    razorpaySdkResult: RazorpaySdkResult? = null,
    onRazorpayResultConsumed: () -> Unit = {},
    launchRazorpay: (RazorpayPaymentSession) -> Unit = {},
    selectedAddressId: String? = null,
    onSelectedAddressConsumed: () -> Unit = {},
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.beginVisit() }
    LaunchedEffect(state.message) { state.message?.let { onMessage(it); viewModel.consumeMessage() } }
    LaunchedEffect(selectedAddressId) {
        selectedAddressId?.let { viewModel.selectAddress(it); onSelectedAddressConsumed() }
    }
    LaunchedEffect(razorpaySdkResult) {
        razorpaySdkResult?.let { viewModel.handleRazorpayResult(it); onRazorpayResultConsumed() }
    }
    LaunchedEffect(state.razorpaySession, state.razorpayLaunchPending) {
        if (state.razorpayLaunchPending) state.razorpaySession?.let {
            viewModel.markRazorpayOpened()
            launchRazorpay(it)
        }
    }
    CheckoutScreen(
        state = state,
        onBack = onBack,
        onManageAddresses = onManageAddresses,
        onEditAddress = onEditAddress,
        onDeleteAddress = onDeleteAddress,
        onViewOrders = onViewOrders,
        onContinueShopping = onContinueShopping,
        actions = viewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutScreen(
    state: CheckoutUiState,
    onBack: () -> Unit,
    onManageAddresses: () -> Unit,
    onEditAddress: (String) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onViewOrders: () -> Unit,
    onContinueShopping: () -> Unit,
    actions: CheckoutViewModel,
) {
    BackHandler {
        when {
            state.placedOrder != null -> onContinueShopping()
            state.reviewing -> actions.edit()
            state.deliveryStep -> actions.backToAddress()
            else -> onBack()
        }
    }
    val paymentFlow = state.paymentAttempt != null || state.recoveringPayment ||
        (state.placedOrder != null && state.paymentMethod == com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod.RAZORPAY)
    val ready = state.quote != null && !state.loading && state.error == null && state.placedOrder == null && !paymentFlow
    Scaffold(
        containerColor = CustomerCommerceTokens.canvas,
        topBar = {
            if (ready) CenterAlignedTopAppBar(
                title = { Text(if (state.reviewing) "Checkout Payment" else "Checkout Delivery") },
                navigationIcon = { IconButton(onClick = {
                    when {
                        state.reviewing -> actions.edit()
                        state.deliveryStep -> actions.backToAddress()
                        else -> onBack()
                    }
                }) { Icon(AppIcons.Back, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CustomerCommerceTokens.surface),
            )
        },
        bottomBar = {
            if (ready && !state.deliveryStep && !state.reviewing) CheckoutAddressBottomAction(
                address = state.quote?.addresses?.firstOrNull { it.id == state.addressId },
                onContinue = actions::continueFromAddress,
            )
            else if (ready) CheckoutBottomAction(
                text = if (state.reviewing && state.paymentMethod == com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod.RAZORPAY)
                    "Pay securely" else if (state.reviewing) "Place Order" else "Continue",
                enabled = when {
                    state.reviewing -> !state.placing
                    state.deliveryStep -> state.selectionComplete
                    else -> state.addressId != null
                },
                loading = state.placing,
                total = state.quote?.let { it.cart.totalMinor.money(it.currencyCode) },
                action = { when {
                    state.reviewing -> actions.placeOrder()
                    state.deliveryStep -> actions.review()
                    else -> actions.continueFromAddress()
                } },
            )
        },
    ) { padding ->
        when {
            state.loading -> ContentStateView(ContentState.Loading, contentPadding = padding)
            (state.error != null || state.quote == null) && !paymentFlow && state.placedOrder == null -> ContentStateView(
                ContentState.Error(description = state.error), contentPadding = padding, onAction = actions::prepare,
            )
            paymentFlow -> RazorpayPaymentContent(
                state, padding, onViewOrders, onContinueShopping, actions::retryRazorpay, actions::reconcileRazorpay,
            )
            state.placedOrder != null -> OrderPlacedContent(
                state.placedOrder, padding, onViewOrders, onContinueShopping,
            )
            else -> AnimatedContent(
                targetState = if (state.reviewing) 3 else if (state.deliveryStep) 2 else 1,
                transitionSpec = {
                    (slideInHorizontally(tween(AppMotion.SCREEN_MILLIS)) { it / 8 } +
                        fadeIn(tween(AppMotion.STANDARD_MILLIS))) togetherWith
                        (slideOutHorizontally(tween(AppMotion.STANDARD_MILLIS)) { -it / 10 } +
                            fadeOut(tween(AppMotion.QUICK_MILLIS)))
                },
                label = "checkout step",
            ) { step ->
                when (step) {
                    3 -> CheckoutReview(state, padding, actions::selectPaymentMethod,
                        actions::edit)
                    2 -> CheckoutDeliverySelection(state, padding, onManageAddresses, actions)
                    else -> CheckoutAddressSelection(state, padding, onManageAddresses,
                        actions::selectAddress, onEditAddress, onDeleteAddress)
                }
            }
        }
    }
}

@Composable
private fun CheckoutDeliverySelection(
    state: CheckoutUiState,
    padding: PaddingValues,
    onManageAddresses: () -> Unit,
    actions: CheckoutViewModel,
) {
    val quote = state.quote ?: return
    val address = quote.addresses.firstOrNull { it.id == state.addressId }
    val selectedDate = quote.deliveryDates.firstOrNull { it.id == state.deliveryDateId }
    val availableSlots = quote.deliverySlots.filter { it.active && it.id in selectedDate?.availableSlotIds.orEmpty() }
    Box(Modifier.fillMaxSize().background(CustomerCommerceTokens.canvas).padding(padding),
        contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.fillMaxHeight().fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth).imePadding(),
            contentPadding = PaddingValues(horizontal = AppSpacing.medium, vertical = AppSpacing.small),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.compact),
        ) {
            item { CheckoutStepIndicator(2) }
            item {
                CheckoutHeading("Delivery Address")
                if (address != null) CheckoutDeliveryAddressSummary(address)
            }
            item { DeliveryInstructionsField(state.instructions, actions::setInstructions) }
            item { CheckoutHeading("Delivery Time") }
            item { Surface(shape = AppShapes.small, color = CustomerCommerceTokens.surface,
                border = BorderStroke(1.dp, CustomerCommerceTokens.border)) {
                Column(Modifier.fillMaxWidth().padding(AppSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                    DeliveryDateSelector(quote.deliveryDates, state.deliveryDateId, actions::selectDate)
                    DeliverySlotGrid(availableSlots, state.deliverySlotId, actions::selectSlot)
                }
            } }
        }
    }
}

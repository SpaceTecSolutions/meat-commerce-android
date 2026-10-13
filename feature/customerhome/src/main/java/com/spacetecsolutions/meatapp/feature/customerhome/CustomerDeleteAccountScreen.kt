package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.User
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CustomerDeleteAccountRoute(user: User, back: () -> Unit,
    viewOrder: (String) -> Unit, openSupport: () -> Unit, openPrivacy: () -> Unit,
    onDeleted: () -> Unit, viewModel: CustomerDeleteAccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var acknowledged by remember(user.id) { mutableStateOf(false) }
    LaunchedEffect(state.completed) { if (state.completed) onDeleted() }
    Column(Modifier.fillMaxSize().background(AccountStyle.canvas)
        .statusBarsPadding().navigationBarsPadding()) {
        AccountTopBar("Delete Account", back)
        Column(Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 480.dp).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when {
                state.loading -> CircularProgressIndicator(color = AccountStyle.red)
                state.activeOrder != null -> DeleteBlocked(state.activeOrder!!, back,
                    viewOrder, openSupport)
                else -> {
                    DeleteConfirmation(user, openPrivacy)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it })
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_i_understand_this_deletion_is_d711640),
                            fontSize = 12.sp, lineHeight = 18.sp, color = AccountStyle.ink)
                    }
                    state.error?.let { Text(it, fontSize = 12.sp, color = AccountStyle.red) }
                    Button(onClick = viewModel::delete, modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = acknowledged && state.checked && !state.deleting,
                        colors = ButtonDefaults.buttonColors(containerColor = AccountStyle.red),
                        shape = RoundedCornerShape(10.dp)) {
                        if (state.deleting) CircularProgressIndicator(Modifier.size(20.dp),
                            strokeWidth = 2.dp, color = Color.White)
                        else { Icon(AppIcons.Delete, null); Spacer(Modifier.width(8.dp)); Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_delete_my_account_26cc101)) }
                    }
                    TextButton(onClick = back, modifier = Modifier.fillMaxWidth()) {
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_keep_account_return_3158651), color = AccountStyle.muted)
                    }
                    if (!state.checked) TextButton(onClick = viewModel::retryCheck,
                        modifier = Modifier.fillMaxWidth()) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_retry_order_check_59491fa)) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DeleteConfirmation(user: User, openPrivacy: () -> Unit) {
    AccountCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).background(Color(0xFFFFE5E5), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(AppIcons.Delete, null, Modifier.size(28.dp), tint = AccountStyle.red)
            }
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_delete_your_account_35293dc), Modifier.padding(top = 12.dp),
                fontSize = 21.sp, fontWeight = FontWeight.Bold, color = AccountStyle.ink)
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_this_will_permanently_remove_your_d708e86),
                Modifier.padding(top = 8.dp), fontSize = 13.sp,
                lineHeight = 19.sp, color = AccountStyle.muted)
            Text(user.mobileNumber, Modifier.padding(top = 10.dp), fontSize = 12.sp,
                color = AccountStyle.muted)
        }
    }
    AccountSectionLabel("What will be removed")
    AccountCard(Modifier.fillMaxWidth()) {
        DeleteDataRow("Profile & sign-in", "Name, mobile number, account identity")
        DeleteDataRow("Saved addresses", "Delivery details and selected location pins")
        DeleteDataRow("Cart & preferences", "Cart items and temporary checkout choices")
        DeleteDataRow("Notifications & devices", "In-app notices and push registrations")
    }
    AccountSectionLabel("Records we may retain")
    AccountCard(Modifier.fillMaxWidth()) {
        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_anonymized_accounting_audit_data_5df97e0), fontWeight = FontWeight.Bold,
            color = AccountStyle.ink)
        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_historical_order_and_payment_facts_1b399c6),
            Modifier.padding(top = 6.dp), fontSize = 12.sp, lineHeight = 18.sp,
            color = AccountStyle.muted)
        TextButton(onClick = openPrivacy) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_read_privacy_policy_7fb9f3b)) }
    }
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF8F1),
        border = BorderStroke(1.dp, Color(0xFFFDE3B0))) {
        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_deletion_is_final_you_will_30a888e),
            Modifier.padding(14.dp), fontSize = 12.sp, color = AccountStyle.ink)
    }
}

@Composable
private fun DeleteDataRow(title: String, detail: String) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AccountStyle.ink)
    Text(detail, fontSize = 12.sp, color = AccountStyle.muted)
    HorizontalDivider(Modifier.padding(vertical = 9.dp), color = AccountStyle.line)
}

@Composable
private fun DeleteBlocked(order: CodOrder, back: () -> Unit,
    viewOrder: (String) -> Unit, openSupport: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF7ECEC)) {
        Column(Modifier.padding(16.dp)) {
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_temporary_hold_74315d9), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = AccountStyle.red)
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_account_deletion_unavailable_10642fb), fontSize = 17.sp,
                fontWeight = FontWeight.Bold, color = AccountStyle.ink)
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_your_account_is_needed_to_7b56853),
                fontSize = 12.sp, lineHeight = 18.sp, color = AccountStyle.muted)
        }
    }
    AccountCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(order.displayNumber, fontWeight = FontWeight.Bold, color = AccountStyle.ink)
            Text(order.orderStatus.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase),
                fontSize = 11.sp, color = AccountStyle.red)
        }
        order.deliveryDateIso?.let { iso ->
            val date = runCatching { LocalDate.parse(iso)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())) }.getOrDefault(iso)
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_scheduled_delivery_date_dd43ce9, date), fontSize = 12.sp, color = AccountStyle.muted)
        }
        order.deliverySlotTimeLabel?.let { Text(it, fontSize = 12.sp, color = AccountStyle.muted) }
        order.items.take(2).forEach { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_it_name_it_quantity_c8666bf, it.name, it.quantity),
            fontSize = 12.sp, color = AccountStyle.ink) }
        Text(order.addressSummary, Modifier.padding(top = 8.dp), fontSize = 12.sp,
            color = AccountStyle.muted)
    }
    AccountCard(Modifier.fillMaxWidth()) {
        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_next_steps_options_c47097c), fontWeight = FontWeight.Bold, color = AccountStyle.ink)
        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_1_wait_for_delivery_or_5c5e410),
            fontSize = 12.sp, lineHeight = 18.sp, color = AccountStyle.muted)
        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_2_contact_support_if_you_54a206c),
            fontSize = 12.sp, color = AccountStyle.muted)
    }
    Button(onClick = { viewOrder(order.id) }, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = AccountStyle.red)) {
        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_view_active_order_details_0110957))
    }
    OutlinedButton(onClick = openSupport, modifier = Modifier.fillMaxWidth()) {
        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_contact_customer_support_ef8fc8d))
    }
    TextButton(onClick = back, modifier = Modifier.fillMaxWidth()) {
        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_back_to_profile_account_0641626), color = AccountStyle.muted)
    }
}

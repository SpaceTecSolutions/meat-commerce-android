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
                        Text("I understand this deletion is permanent and cannot be undone.",
                            fontSize = 12.sp, lineHeight = 18.sp, color = AccountStyle.ink)
                    }
                    state.error?.let { Text(it, fontSize = 12.sp, color = AccountStyle.red) }
                    Button(onClick = viewModel::delete, modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = acknowledged && state.checked && !state.deleting,
                        colors = ButtonDefaults.buttonColors(containerColor = AccountStyle.red),
                        shape = RoundedCornerShape(10.dp)) {
                        if (state.deleting) CircularProgressIndicator(Modifier.size(20.dp),
                            strokeWidth = 2.dp, color = Color.White)
                        else { Icon(AppIcons.Delete, null); Spacer(Modifier.width(8.dp)); Text("Delete My Account") }
                    }
                    TextButton(onClick = back, modifier = Modifier.fillMaxWidth()) {
                        Text("Keep Account & Return", color = AccountStyle.muted)
                    }
                    if (!state.checked) TextButton(onClick = viewModel::retryCheck,
                        modifier = Modifier.fillMaxWidth()) { Text("Retry order check") }
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
            Text("Delete your account?", Modifier.padding(top = 12.dp),
                fontSize = 21.sp, fontWeight = FontWeight.Bold, color = AccountStyle.ink)
            Text("This will permanently remove your account and personal records.",
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
        Text("Anonymized accounting & audit data", fontWeight = FontWeight.Bold,
            color = AccountStyle.ink)
        Text("Historical order and payment facts may remain for accounting, legal, security, and fraud-prevention purposes. Your name, phone number, address, and account identifier are removed from those records.",
            Modifier.padding(top = 6.dp), fontSize = 12.sp, lineHeight = 18.sp,
            color = AccountStyle.muted)
        TextButton(onClick = openPrivacy) { Text("Read Privacy Policy") }
    }
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF8F1),
        border = BorderStroke(1.dp, Color(0xFFFDE3B0))) {
        Text("Deletion is final. You will be signed out when it completes.",
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
            Text("TEMPORARY HOLD", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = AccountStyle.red)
            Text("Account deletion unavailable", fontSize = 17.sp,
                fontWeight = FontWeight.Bold, color = AccountStyle.ink)
            Text("Your account is needed to complete an active order. Once it is delivered or cancelled, you can return here to delete your account.",
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
            Text("Scheduled Delivery: $date", fontSize = 12.sp, color = AccountStyle.muted)
        }
        order.deliverySlotTimeLabel?.let { Text(it, fontSize = 12.sp, color = AccountStyle.muted) }
        order.items.take(2).forEach { Text("• ${it.name}  ·  ${it.quantity}",
            fontSize = 12.sp, color = AccountStyle.ink) }
        Text(order.addressSummary, Modifier.padding(top = 8.dp), fontSize = 12.sp,
            color = AccountStyle.muted)
    }
    AccountCard(Modifier.fillMaxWidth()) {
        Text("Next Steps & Options", fontWeight = FontWeight.Bold, color = AccountStyle.ink)
        Text("1. Wait for delivery or use Order Details if cancellation is still available.",
            fontSize = 12.sp, lineHeight = 18.sp, color = AccountStyle.muted)
        Text("2. Contact support if you need help with the order.",
            fontSize = 12.sp, color = AccountStyle.muted)
    }
    Button(onClick = { viewOrder(order.id) }, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = AccountStyle.red)) {
        Text("View Active Order Details")
    }
    OutlinedButton(onClick = openSupport, modifier = Modifier.fillMaxWidth()) {
        Text("Contact Customer Support")
    }
    TextButton(onClick = back, modifier = Modifier.fillMaxWidth()) {
        Text("Back to Profile & Account", color = AccountStyle.muted)
    }
}

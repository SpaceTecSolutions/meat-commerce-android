package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.PaymentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerCancelSheet(order: CodOrder, reason: String, change: (String) -> Unit,
    confirm: () -> Unit, dismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = T.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cancel " + order.numberLabel(), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = T.ink)
                IconButton(dismiss) { Icon(AppIcons.Close, "Close") }
            }
            Surface(color = T.amberSurface, shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, T.amber.copy(alpha = .25f))) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(AppIcons.Pending, null, tint = T.amber)
                    Text("Cancelling this order cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium, color = T.ink)
                }
            }
            Text("Why are you cancelling?", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = T.ink)
            OutlinedTextField(reason, change, Modifier.fillMaxWidth(),
                placeholder = { Text("Tell us the reason") },
                supportingText = { Text("Required · " + reason.length + "/200") },
                minLines = 2, maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = T.red,
                    unfocusedBorderColor = T.border))
            val paymentMessage = when {
                order.paymentMethod == CheckoutPaymentMethod.COD &&
                    order.paymentStatus == PaymentStatus.PENDING ->
                    "No payment has been collected for this Cash on Delivery order."
                order.paymentStatus == PaymentStatus.PAID ->
                    "Contact support for information about your payment after cancellation."
                else -> null
            }
            paymentMessage?.let {
                Surface(color = T.soft, shape = RoundedCornerShape(12.dp)) {
                    Text(it, Modifier.fillMaxWidth().padding(14.dp),
                        style = MaterialTheme.typography.bodySmall, color = T.muted)
                }
            }
            Button(confirm, Modifier.fillMaxWidth().height(50.dp),
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = T.red),
                shape = RoundedCornerShape(10.dp)) {
                Text("Yes, Cancel Order", fontWeight = FontWeight.Bold)
            }
            Button(dismiss, Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = T.soft,
                    contentColor = T.ink),
                shape = RoundedCornerShape(10.dp)) { Text("Don't Cancel (Keep Order)") }
        }
    }
}

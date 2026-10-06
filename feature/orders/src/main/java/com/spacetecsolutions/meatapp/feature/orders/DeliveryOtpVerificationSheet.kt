package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod
import com.spacetecsolutions.meatapp.core.model.CodOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeliveryOtpVerificationSheet(order: CodOrder, value: String, error: String?,
    busy: Boolean, change: (String) -> Unit, verify: () -> Unit, dismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = { if (!busy) dismiss() },
        containerColor = T.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.LiveTracking, null, Modifier.size(18.dp), tint = T.red)
                Spacer(Modifier.width(7.dp))
                Text("SECURE DELIVERY HANDOVER", style = MaterialTheme.typography.labelSmall,
                    color = T.red, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = dismiss, enabled = !busy) {
                    Icon(AppIcons.Close, "Close verification")
                }
            }
            Text("Reached Customer", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = T.ink)
            Surface(color = T.canvas, shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, T.border)) {
                Text("Live tracking is active. Ask the customer for the four-digit delivery " +
                    "code displayed on their order screen.", Modifier.padding(13.dp),
                    style = MaterialTheme.typography.bodySmall, color = T.muted)
            }
            Text("Enter 4-digit delivery OTP", style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = T.ink)
            BasicTextField(value = value, onValueChange = change, enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Four-digit delivery verification code"
                }, decorationBox = { inner ->
                    Box {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(4) { index ->
                                Surface(Modifier.weight(1f).height(58.dp),
                                    color = if (index == value.length) T.rose else T.canvas,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp,
                                        if (error != null) T.red else T.border)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(value.getOrNull(index)?.toString().orEmpty(),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center, color = T.ink)
                                    }
                                }
                            }
                        }
                        Box(Modifier.size(1.dp)) { inner() }
                    }
                })
            if (error != null) Text(error, style = MaterialTheme.typography.bodySmall,
                color = T.red)
            if (order.paymentMethod == CheckoutPaymentMethod.COD) {
                Surface(color = T.canvas, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("COD expected", color = T.muted,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(order.amountDueMinor.money(order.currencyCode),
                            fontWeight = FontWeight.Bold, color = T.ink)
                    }
                }
            }
            Button(onClick = verify, enabled = value.length == 4 && !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = T.red)) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp),
                    color = T.surface, strokeWidth = 2.dp)
                else Text("Verify OTP")
            }
            TextButton(onClick = dismiss, enabled = !busy,
                modifier = Modifier.fillMaxWidth()) { Text("Back", color = T.muted) }
        }
    }
}

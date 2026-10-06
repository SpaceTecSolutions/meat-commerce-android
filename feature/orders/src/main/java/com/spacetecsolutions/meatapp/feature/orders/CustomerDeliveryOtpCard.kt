package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.CustomerOrdersTokens as T
import com.spacetecsolutions.meatapp.core.model.DeliveryOtp

@Composable
internal fun CustomerDeliveryOtpCard(otp: DeliveryOtp?, failed: Boolean,
    onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = T.amberSurface,
        shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, T.amber.copy(alpha = .3f))) {
        Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Pending, null, Modifier.size(20.dp), tint = T.amber)
            Column(Modifier.weight(1f)) {
                Text("Delivery verification code", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = T.ink)
                when {
                    otp != null -> {
                        Text(otp.code.toCharArray().joinToString("  "),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold, color = T.red)
                        Text("Share only when the delivery partner reaches you.",
                            style = MaterialTheme.typography.labelSmall, color = T.muted)
                    }
                    failed -> Text("Code temporarily unavailable. Try again.",
                        style = MaterialTheme.typography.labelSmall, color = T.muted)
                    else -> Text("Preparing your secure code…",
                        style = MaterialTheme.typography.labelSmall, color = T.muted)
                }
            }
            if (failed) TextButton(onRetry) { Text("Retry", color = T.red) }
        }
    }
}

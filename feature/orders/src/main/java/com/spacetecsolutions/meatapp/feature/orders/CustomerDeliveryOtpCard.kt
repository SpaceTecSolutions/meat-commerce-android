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
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_delivery_verification_code_03d3aa6), style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = T.ink)
                when {
                    otp != null -> {
                        Text(otp.code.toCharArray().joinToString("  "),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold, color = T.red)
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_share_only_when_the_delivery_08fa692),
                            style = MaterialTheme.typography.labelSmall, color = T.muted)
                    }
                    failed -> Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_code_temporarily_unavailable_try_again_cef31ba),
                        style = MaterialTheme.typography.labelSmall, color = T.muted)
                    else -> Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_preparing_your_secure_code_59b3da4),
                        style = MaterialTheme.typography.labelSmall, color = T.muted)
                }
            }
            if (failed) TextButton(onRetry) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.orders.R.string.ui_retry_9f5cd8a), color = T.red) }
        }
    }
}

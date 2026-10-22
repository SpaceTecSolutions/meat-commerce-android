package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.ProfileMoreTokens
import com.spacetecsolutions.meatapp.core.model.User

internal object CustomerProfileColors {
    val canvas = Color(0xFFF6F7F9)
    val border = Color(0xFFE5E7EB)
    val ink = Color(0xFF171B21)
    val muted = Color(0xFF6B7280)
    val red = Color(0xFFD71920)
}

@Composable
internal fun CustomerProfileHeader(onBack: () -> Unit, onHelp: () -> Unit) {
    Surface(color = Color.White, border = BorderStroke(1.dp, Color(0xFFF3F4F6))) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(AppIcons.Back, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_back_b52b36b), Modifier.size(24.dp), tint = CustomerProfileColors.ink)
            }
            Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_profile_account_0b1b0a6), Modifier.weight(1f).padding(start = 10.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = CustomerProfileColors.ink)
            IconButton(onClick = onHelp, modifier = Modifier.size(40.dp)) {
                Icon(AppIcons.Help, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_customer_support_81f83be), Modifier.size(22.dp),
                    tint = CustomerProfileColors.muted)
            }
        }
    }
}

@Composable
internal fun CustomerIdentityCard(user: User, onEdit: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = Color.White, border = BorderStroke(1.dp, CustomerProfileColors.border),
        shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(Color(0xFFFEF2F2), CircleShape),
                contentAlignment = Alignment.Center) {
                Text(user.displayName.trim().split(' ').filter(String::isNotBlank)
                    .take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "C" },
                    color = ProfileMoreTokens.red, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.displayName.ifBlank { "Customer" }, Modifier.weight(1f, false),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = CustomerProfileColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Surface(Modifier.padding(start = 7.dp), shape = CircleShape,
                        color = Color(0xFFF3F4F6), border = BorderStroke(1.dp, CustomerProfileColors.border)) {
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_customer_0e85749), Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall, color = CustomerProfileColors.muted)
                    }
                }
                Text(user.mobileNumber, style = MaterialTheme.typography.bodySmall,
                    color = CustomerProfileColors.muted, maxLines = 1)
                if (user.active) Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_active_account_9d3c24d), style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
            }
            Surface(onClick = onEdit, modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(12.dp), color = Color.White,
                border = BorderStroke(1.dp, CustomerProfileColors.border)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Edit, androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_edit_profile_15141ea), Modifier.size(19.dp), tint = CustomerProfileColors.muted)
                }
            }
        }
    }
}

@Composable
internal fun CustomerOrdersEntry(onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = Color.White, border = BorderStroke(1.dp, CustomerProfileColors.border),
        shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Icon(AppIcons.Orders, null, Modifier.size(22.dp), tint = CustomerProfileColors.red)
            }
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_my_orders_db2821a), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = ProfileMoreTokens.ink)
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_track_live_delivery_past_reorders_8fd4f40), style = MaterialTheme.typography.bodySmall,
                    color = CustomerProfileColors.muted)
            }
            Icon(AppIcons.ArrowRight, null, Modifier.size(20.dp), tint = CustomerProfileColors.muted)
        }
    }
}

@Composable
internal fun CustomerProfileGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = Color.White, border = BorderStroke(1.dp, CustomerProfileColors.border),
        shadowElevation = 1.dp) {
        Column(content = content)
    }
}

@Composable
internal fun CustomerProfileRow(icon: ImageVector, title: String, subtitle: String,
    onClick: () -> Unit, divider: Boolean = true, tint: Color = CustomerProfileColors.ink,
    iconBackground: Color = Color(0xFFF3F4F6)) {
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick)
            .heightIn(min = 70.dp).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(iconBackground, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = tint)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = CustomerProfileColors.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = CustomerProfileColors.muted, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
            Icon(AppIcons.ArrowRight, null, Modifier.size(18.dp), tint = CustomerProfileColors.muted)
        }
        if (divider) HorizontalDivider(Modifier.padding(start = 64.dp),
            color = CustomerProfileColors.border)
    }
}

@Composable
internal fun CustomerProfileLogout(userPhone: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = Color.White, border = BorderStroke(1.dp, CustomerProfileColors.border),
        shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick)
            .heightIn(min = 70.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(ProfileMoreTokens.rose, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Icon(AppIcons.Logout, null, Modifier.size(20.dp), tint = ProfileMoreTokens.red)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_log_out_4e8131c), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = ProfileMoreTokens.red)
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_signed_in_as_userphone_a137a9c, userPhone), style = MaterialTheme.typography.bodySmall,
                    color = ProfileMoreTokens.muted)
            }
            Icon(AppIcons.ArrowRight, null, Modifier.size(18.dp), tint = ProfileMoreTokens.red)
        }
    }
}

package com.spacetecsolutions.meatapp.core.designsystem.component

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.ProfileMoreTokens

private val groupShape = RoundedCornerShape(16.dp)
private val iconShape = RoundedCornerShape(12.dp)

@Composable
fun ProfileMoreHeader(title: String, subtitle: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = 520.dp).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = ProfileMoreTokens.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ProfileMoreTokens.muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(Modifier.size(36.dp), shape = CircleShape, color = ProfileMoreTokens.surface,
            border = BorderStroke(1.dp, ProfileMoreTokens.border), shadowElevation = 1.dp) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(19.dp), tint = ProfileMoreTokens.ink)
            }
        }
    }
}

@Composable
fun ProfileIdentityCard(name: String, phone: String, onClick: () -> Unit,
    badge: String? = null) {
    Surface(Modifier.fillMaxWidth(), shape = groupShape, color = ProfileMoreTokens.surface,
        border = BorderStroke(1.dp, ProfileMoreTokens.border), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth()/*.clickable(onClick = onClick)*/.semantics { role = Role.Button }
            .padding(14.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(ProfileMoreTokens.rose, iconShape),
                contentAlignment = Alignment.Center) {
                Text(name.trim().firstOrNull()?.uppercase() ?: "U", fontWeight = FontWeight.Bold,
                    color = ProfileMoreTokens.red, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name.ifBlank { "User" }, Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = ProfileMoreTokens.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (badge != null) Text(badge, Modifier.padding(start = 6.dp)
                        .background(ProfileMoreTokens.rose, RoundedCornerShape(5.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall, color = ProfileMoreTokens.red)
                }
                Text(phone, style = MaterialTheme.typography.bodySmall,
                    color = ProfileMoreTokens.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
//            Icon(AppIcons.ArrowRight, null, Modifier.size(18.dp), tint = ProfileMoreTokens.muted)
        }
    }
}

@Composable
fun ProfileSettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            color = ProfileMoreTokens.muted)
        Surface(Modifier.fillMaxWidth(), shape = groupShape, color = ProfileMoreTokens.surface,
            border = BorderStroke(1.dp, ProfileMoreTokens.border), shadowElevation = 1.dp) {
            Column(content = content)
        }
    }
}

@Composable
fun ProfileSettingsRow(icon: ImageVector, label: String, onClick: () -> Unit,
    divider: Boolean = true) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable(onClick = onClick)
            .semantics { role = Role.Button }.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(ProfileMoreTokens.inset, iconShape),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(18.dp), tint = ProfileMoreTokens.ink)
            }
            Text(label, Modifier.weight(1f).padding(start = 14.dp),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = ProfileMoreTokens.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(AppIcons.ArrowRight, null, Modifier.size(18.dp), tint = ProfileMoreTokens.muted)
        }
        if (divider) HorizontalDivider(Modifier.padding(start = 64.dp),
            thickness = .5.dp, color = ProfileMoreTokens.border)
    }
}

@Composable
fun ProfileLogoutAction(onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = groupShape, color = ProfileMoreTokens.surface,
        border = BorderStroke(1.dp, ProfileMoreTokens.avatarBorder), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable(onClick = onClick)
            .semantics { role = Role.Button }.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(ProfileMoreTokens.rose, iconShape),
                contentAlignment = Alignment.Center) {
                Icon(AppIcons.Logout, null, Modifier.size(18.dp), tint = ProfileMoreTokens.red)
            }
            Text("Logout", Modifier.weight(1f).padding(start = 14.dp),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = ProfileMoreTokens.red)
            Icon(AppIcons.ArrowRight, null, Modifier.size(18.dp), tint = ProfileMoreTokens.red)
        }
    }
}

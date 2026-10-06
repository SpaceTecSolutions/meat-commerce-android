package com.spacetecsolutions.meatapp.feature.admin.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.model.User

internal object AdminAccountColors {
    val canvas = Color(0xFFF6F7F9)
    val border = Color(0xFFE5E7EB)
    val ink = Color(0xFF232326)
    val muted = Color(0xFF6B7280)
    val red = Color(0xFFD71920)
}

@Composable
internal fun AdminAccountTopBar(title: String, back: () -> Unit, help: (() -> Unit)? = null) {
    Surface(color = Color.White, border = BorderStroke(1.dp, Color(0xFFF3F4F6))) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding( 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
//            IconButton(back, Modifier.size(40.dp)) { Icon(AppIcons.Back, "Back", tint = AdminAccountColors.ink) }
            Text(title, Modifier.weight(1f).padding(start = 10.dp),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = AdminAccountColors.ink)
            help?.let { IconButton(it, Modifier.size(40.dp)) {
                Icon(AppIcons.Help, "Help and support", tint = AdminAccountColors.muted)
            } }
        }
    }
}

@Composable
internal fun AdminIdentityCard(user: User?, edit: () -> Unit) {
    val name = user?.displayName?.ifBlank { "Admin" } ?: "Admin"
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), color = Color.White,
        border = BorderStroke(1.dp, AdminAccountColors.border), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(Color(0xFFFEF2F2), CircleShape),
                contentAlignment = Alignment.Center) {
                Text(name.trim().split(' ').filter(String::isNotBlank).take(2)
                    .joinToString("") { it.take(1).uppercase() }.ifBlank { "A" },
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = AdminAccountColors.red)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = AdminAccountColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(user?.mobileNumber.orEmpty(), style = MaterialTheme.typography.bodySmall,
                    color = AdminAccountColors.muted)
                if (user?.active == true) Text("●  Active account",
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669))
            }
            /*Surface(onClick = edit, modifier = Modifier.size(38.dp), shape = RoundedCornerShape(12.dp),
                color = Color.White, border = BorderStroke(1.dp, AdminAccountColors.border)) {
                Box(contentAlignment = Alignment.Center) { Icon(AppIcons.Edit, "Edit profile",
                    Modifier.size(19.dp), tint = AdminAccountColors.muted) }
            }*/
        }
    }
}

@Composable
internal fun AdminAccountGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title.uppercase(), Modifier.fillMaxWidth().padding(start = 3.dp, top = 3.dp),
        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
        color = Color(0xFF232326)
    )
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), color = Color.White,
        border = BorderStroke(1.dp, AdminAccountColors.border), shadowElevation = 1.dp) {
        Column(content = content)
    }
}

@Composable
internal fun AdminAccountRow(icon: ImageVector, title: String, subtitle: String,
    click: () -> Unit, divider: Boolean = true) {
    val (iconTint, iconBackground) = adminAccountIconColors(title)
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = click).heightIn(min = 70.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(iconBackground, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = iconTint)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = AdminAccountColors.ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = AdminAccountColors.muted, maxLines = 2)
            }
            Icon(AppIcons.ArrowRight, null, Modifier.size(18.dp), tint = AdminAccountColors.muted)
        }
        if (divider) HorizontalDivider(Modifier.padding(start = 64.dp), color = AdminAccountColors.border)
    }
}

private fun adminAccountIconColors(title: String): Pair<Color, Color> = when {
    title.contains("Profile", true) -> Color(0xFF2563EB) to Color(0xFFEFF6FF)
    title.contains("Password", true) -> Color(0xFF9333EA) to Color(0xFFFAF5FF)
    title.contains("Categor", true) -> Color(0xFFEA580C) to Color(0xFFFFF7ED)
    title.contains("Banner", true) || title.contains("Offer", true) ->
        Color(0xFFDB2777) to Color(0xFFFDF2F8)
    title.contains("Staff", true) || title.contains("Customer", true) ->
        Color(0xFF0891B2) to Color(0xFFECFEFF)
    title.contains("Delivery", true) -> Color(0xFF0D9488) to Color(0xFFF0FDFA)
    title.contains("Payment", true) -> Color(0xFF16A34A) to Color(0xFFF0FDF4)
    title.contains("Notification", true) -> Color(0xFF9333EA) to Color(0xFFFAF5FF)
    title.contains("Report", true) -> Color(0xFF2563EB) to Color(0xFFEFF6FF)
    title.contains("Log Out", true) -> AdminAccountColors.red to Color(0xFFFFEEEE)
    else -> Color(0xFF64748B) to Color(0xFFF1F5F9)
}

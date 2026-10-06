package com.spacetecsolutions.meatapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.spacetecsolutions.meatapp.core.designsystem.theme.AdminDashboardTokens
import com.spacetecsolutions.meatapp.core.model.StaffPermission
import com.spacetecsolutions.meatapp.core.model.User

internal object StaffDesign {
    val canvas = AdminDashboardTokens.canvas
    val surface = AdminDashboardTokens.card
    val inset = AdminDashboardTokens.inset
    val border = AdminDashboardTokens.border
    val ink = AdminDashboardTokens.ink
    val muted = AdminDashboardTokens.muted
    val red = AdminDashboardTokens.red
    val paleRed = Color(0xFFFFDAD6)
    val success = Color(0xFF16863C)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StaffTopBar(title: String, onProfile: (() -> Unit)? = null, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = { Column {
            Text("MEAT STATION STAFF", style = MaterialTheme.typography.labelSmall,
                color = StaffDesign.red, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleLarge,
                color = StaffDesign.ink, fontWeight = FontWeight.SemiBold)
        } },
        navigationIcon = { if (onBack != null) IconButton(onBack) { Icon(AppIcons.Back, "Back") } },
        actions = { onProfile?.let { action ->
            Surface(onClick = action, modifier = Modifier.padding(end = 16.dp),
                shape = CircleShape, color = StaffDesign.red) {
                Icon(AppIcons.Profile, "Employee profile", Modifier.padding(11.dp).size(22.dp), tint = Color.White)
            }
        } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = StaffDesign.canvas),
    )
}

@Composable
internal fun StaffCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Surface(
    modifier = modifier, shape = RoundedCornerShape(16.dp), color = StaffDesign.surface,
    border = BorderStroke(1.dp, StaffDesign.border), shadowElevation = 1.dp,
) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }

@Composable
internal fun StaffIdentityCard(user: User, compact: Boolean = false) {
    StaffCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StaffInitials(user.displayName, if (compact) 54.dp else 62.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(user.displayName, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Active Staff", style = MaterialTheme.typography.labelSmall,
                    color = StaffDesign.success, fontWeight = FontWeight.SemiBold)
                if (!compact) Text(user.mobileNumber, style = MaterialTheme.typography.bodySmall,
                    color = StaffDesign.muted)
            }
            Surface(shape = RoundedCornerShape(50), color = StaffDesign.inset) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Security, null, Modifier.size(15.dp), tint = StaffDesign.muted)
                    Text(" Staff", style = MaterialTheme.typography.labelSmall, color = StaffDesign.muted)
                }
            }
        }
    }
}

@Composable
internal fun StaffInitials(name: String, size: androidx.compose.ui.unit.Dp) {
    val initials = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        .take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "S" }
    Surface(Modifier.size(size), shape = CircleShape, color = Color(0xFFF0DFE1)) {
        Box(contentAlignment = Alignment.Center) {
            Text(initials, color = StaffDesign.red, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun StaffSectionLabel(title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall,
            color = StaffDesign.muted, fontWeight = FontWeight.Bold)
        trailing?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = StaffDesign.red) }
    }
}

@Composable
internal fun StaffMenuRow(icon: ImageVector, title: String, description: String,
    onClick: () -> Unit, emphasis: Boolean = false) {
    val (regularTint, regularBackground) = staffIconColors(title)
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(if (emphasis) StaffDesign.paleRed else regularBackground,
                RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(21.dp), tint = if (emphasis) StaffDesign.red else regularTint)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold,
                    color = if (emphasis) StaffDesign.red else StaffDesign.ink)
                Text(description, style = MaterialTheme.typography.bodySmall, color = StaffDesign.muted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(AppIcons.ArrowRight, null, Modifier.size(18.dp),
                tint = if (emphasis) StaffDesign.red else StaffDesign.muted)
        }
    }
}

private fun staffIconColors(title: String): Pair<Color, Color> = when {
    title.contains("Profile", true) -> Color(0xFF2563EB) to Color(0xFFEFF6FF)
    title.contains("Notification", true) -> Color(0xFF9333EA) to Color(0xFFFAF5FF)
    title.contains("Help", true) -> Color(0xFF0D9488) to Color(0xFFF0FDFA)
    title.contains("Order", true) -> Color(0xFFEA580C) to Color(0xFFFFF7ED)
    else -> Color(0xFF64748B) to Color(0xFFF1F5F9)
}

internal fun StaffPermission.title(): String = when (this) {
    StaffPermission.VIEW_ORDERS -> "View Orders"
    StaffPermission.UPDATE_ORDER_STATUS -> "Update Order Status"
    StaffPermission.ADJUST_FINAL_BILL -> "Adjust Final Bill"
    StaffPermission.GENERATE_INVOICE -> "Generate Invoice"
    StaffPermission.MANAGE_PRODUCTS -> "Manage Products"
    StaffPermission.MANAGE_STOCK -> "Manage Stock"
    StaffPermission.MANAGE_FAQ -> "Manage FAQ"
}

internal fun StaffPermission.description(): String = when (this) {
    StaffPermission.VIEW_ORDERS -> "Orders and details view access"
    StaffPermission.UPDATE_ORDER_STATUS -> "Prepare and delivery status transitions"
    StaffPermission.ADJUST_FINAL_BILL -> "Final weight and bill verification"
    StaffPermission.GENERATE_INVOICE -> "Customer order invoices"
    StaffPermission.MANAGE_PRODUCTS -> "Catalog products and pricing"
    StaffPermission.MANAGE_STOCK -> "Availability and stock controls"
    StaffPermission.MANAGE_FAQ -> "Customer knowledge base"
}

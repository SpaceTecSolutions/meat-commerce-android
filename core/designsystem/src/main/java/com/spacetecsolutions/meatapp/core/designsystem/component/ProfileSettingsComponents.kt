package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

@Composable
fun SettingsProfileHeader(
    displayName: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = AppSpacing.medium, vertical = AppSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    displayName.trim().firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = AppSpacing.medium)) {
            Text(
                displayName.ifBlank { "User" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SettingsMenuCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Column(content = content) }
}

@Composable
fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    destructive: Boolean = false,
    showDivider: Boolean = true,
) {
    val (regularTint, background) = settingsIconColors(title)
    val tint = if (destructive) MaterialTheme.colorScheme.error else regularTint
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = AppDimensions.minimumTouchTarget)
                .clickable(onClick = onClick).semantics { role = Role.Button }
                .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(Modifier.size(38.dp), shape = AppShapes.small,
                color = if (destructive) MaterialTheme.colorScheme.errorContainer else background) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
                }
            }
            Text(
                title,
                Modifier.weight(1f).padding(horizontal = AppSpacing.medium),
                style = MaterialTheme.typography.bodyMedium,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            trailingText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(AppSpacing.extraSmall))
            }
            Icon(AppIcons.ArrowRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
        }
        if (showDivider) HorizontalDivider(
            Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f),
        )
    }
}

private fun settingsIconColors(title: String): Pair<Color, Color> = when {
    title.contains("Profile", true) -> Color(0xFF2563EB) to Color(0xFFEFF6FF)
    title.contains("Notification", true) -> Color(0xFF9333EA) to Color(0xFFFAF5FF)
    title.contains("Help", true) -> Color(0xFF0D9488) to Color(0xFFF0FDFA)
    title.contains("Audit", true) || title.contains("Security", true) ->
        Color(0xFFEA580C) to Color(0xFFFFF7ED)
    title.contains("Payment", true) -> Color(0xFF16A34A) to Color(0xFFF0FDF4)
    else -> Color(0xFF64748B) to Color(0xFFF1F5F9)
}

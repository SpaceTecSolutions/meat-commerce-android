package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    navigationDescription: String? = null,
    onNavigation: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    actionBadgeCount: Int = 0,
    onAction: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            androidx.compose.foundation.layout.Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        expandedHeight = if (subtitle == null) 56.dp else 64.dp,
        navigationIcon = {
            /*if (navigationIcon != null && onNavigation != null) {
                IconButton(onClick = onNavigation) { Icon(navigationIcon, navigationDescription) }
            }*/
        },
        actions = {
            if (actionIcon != null && onAction != null) {
                IconButton(onClick = onAction) {
                    BadgedBox(badge = {
                        if (actionBadgeCount > 0) Badge {
                            Text(if (actionBadgeCount > 99) "99+" else actionBadgeCount.toString())
                        }
                    }) { Icon(actionIcon, contentDescription = actionDescription) }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBackTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        expandedHeight = 56.dp,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(AppIcons.Back, contentDescription = "Back")
            }
        },
    )
}

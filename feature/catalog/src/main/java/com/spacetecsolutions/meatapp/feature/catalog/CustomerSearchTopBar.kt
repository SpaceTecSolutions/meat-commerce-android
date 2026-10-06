package com.spacetecsolutions.meatapp.feature.catalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerSearchTopBar(
    title: String,
    placeholder: String,
    searchVisible: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    cartCount: Int? = null,
    onCart: (() -> Unit)? = null,
) {
    AnimatedContent(
        targetState = searchVisible,
        transitionSpec = {
            fadeIn(tween(AppMotion.QUICK_MILLIS)) togetherWith
                fadeOut(tween(AppMotion.QUICK_MILLIS))
        },
        label = "customer search app bar",
    ) { searching ->
        if (searching) SearchModeTopBar(placeholder, searchQuery, onSearchChange, onCloseSearch)
        else TopAppBar(
            title = {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            navigationIcon = { AppBarIcon(AppIcons.Back, "Back", onBack) },
            actions = {
                AppBarIcon(AppIcons.Search, "Search $title", onSearch)
                if (cartCount != null && onCart != null) {
                    IconButton(onClick = onCart) {
                        BadgedBox(badge = {
                            if (cartCount > 0) Badge {
                                Text(if (cartCount > 99) "99+" else cartCount.toString())
                            }
                        }) { Icon(AppIcons.Cart, "Cart, $cartCount items") }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchModeTopBar(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { requester.requestFocus() }
    TopAppBar(
//        navigationIcon = { AppBarIcon(AppIcons.Back, "Close search", onClose) },
        title = {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().height(44.dp).focusRequester(requester),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.merge(
                    TextStyle(color = MaterialTheme.colorScheme.onSurface),
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { field ->
                    Row(
                        Modifier.fillMaxSize().background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            AppShapes.large,
                        ).padding(start = AppSpacing.compact),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (value.isEmpty()) Text(
                                placeholder,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize ,
                                fontStyle = MaterialTheme.typography.bodyLarge.fontStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            field()
                        }
                        AppBarIcon(AppIcons.Close, "Clear and close search", onClose)
                    }
                },
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun AppBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) { Icon(icon, description) }
}

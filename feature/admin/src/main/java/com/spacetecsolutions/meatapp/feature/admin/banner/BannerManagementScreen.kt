package com.spacetecsolutions.meatapp.feature.admin.banner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.PromotionBanner

@Composable
fun BannerManagementRoute(
    back: () -> Unit,
    onMessage: (String) -> Unit,
    viewModel: BannerManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state.message?.let { message -> LaunchedEffect(message) { onMessage(message); viewModel.consumeMessage() } }
    state.form?.let { form -> BannerFormScreen(form, state.busy, viewModel) }
        ?: BannerListScreen(state, back, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BannerListScreen(state: BannerUiState, back: () -> Unit, actions: BannerManagementViewModel) {
    Scaffold(
        topBar = { AppBackTopBar("Home Banners", back) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { actions.edit(null) },
                icon = { Icon(AppIcons.Add, null) }, text = { Text("Add banner") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        when {
            state.loading -> ContentStateView(ContentState.Loading, contentPadding = padding)
            state.error != null -> ContentStateView(ContentState.Error(description = state.error),
                contentPadding = padding, onAction = actions::refresh)
            state.banners.isEmpty() -> ContentStateView(ContentState.Empty(
                title = "No home banners", description = "Add one banner to activate the customer carousel.",
                actionLabel = "Add banner"), contentPadding = padding, onAction = { actions.edit(null) })
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).widthIn(max = AppDimensions.contentMaxWidth)
                    .wrapContentWidth(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
            ) {
                item { Text("${state.banners.count { it.active }} visible • ${state.banners.size}/5 banners",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(state.banners, key = PromotionBanner::id) { banner ->
                    SwipeToDeleteBanner(banner, state.deletingId == banner.id, actions)
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteBanner(
    banner: PromotionBanner, busy: Boolean, actions: BannerManagementViewModel,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && !busy) actions.delete(banner)
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { Box(Modifier.fillMaxSize().padding(AppSpacing.medium), contentAlignment = Alignment.CenterEnd) {
            Icon(AppIcons.Delete, "Delete banner", tint = MaterialTheme.colorScheme.error)
        } },
        enableDismissFromStartToEnd = false,
    ) {
        OutlinedCard(onClick = { actions.edit(banner) }, shape = AppShapes.medium) {
            BannerPreview(banner, Modifier.fillMaxWidth().height(164.dp))
            Row(Modifier.fillMaxWidth().padding(AppSpacing.small), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(banner.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("Position ${banner.sortOrder + 1} • ${banner.contentAlignment.label()}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (busy) AppLottieAnimation(AppAnimation.Loading, Modifier.size(38.dp), loop = true)
                else Switch(checked = banner.active, onCheckedChange = { actions.toggle(banner) },
                    colors = appSwitchColors())
            }
        }
    }
}

internal fun com.spacetecsolutions.meatapp.core.model.BannerContentAlignment.label() =
    name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)

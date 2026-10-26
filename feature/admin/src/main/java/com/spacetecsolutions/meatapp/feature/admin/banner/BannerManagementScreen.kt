package banner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.*
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.PromotionBanner
import com.spacetecsolutions.meatapp.feature.admin.R
import com.spacetecsolutions.meatapp.feature.admin.banner.BannerFormScreen
import com.spacetecsolutions.meatapp.feature.admin.banner.BannerManagementViewModel
import com.spacetecsolutions.meatapp.feature.admin.banner.BannerPreview
import com.spacetecsolutions.meatapp.feature.admin.banner.BannerUiState

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
        topBar = { AppBackTopBar(stringResource(R.string.ui_home_banners_97a13c0), back) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { actions.edit(null) },
                icon = { Icon(AppIcons.Add, null) }, text = { Text(stringResource(R.string.ui_add_banner_ba3ca5b)) },
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
                title = stringResource(R.string.ui_no_home_banners_a6c1159), description = stringResource(R.string.ui_add_one_banner_to_activate_8bc16f7),
                actionLabel = stringResource(R.string.ui_add_banner_ba3ca5b)), contentPadding = padding, onAction = { actions.edit(null) })
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).widthIn(max = AppDimensions.contentMaxWidth)
                    .wrapContentWidth(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
            ) {
                item { Text(stringResource(R.string.ui_state_banners_count_it_active_31c40e0, state.banners.size),
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
            Icon(AppIcons.Delete, stringResource(R.string.ui_delete_banner_85288a6), tint = MaterialTheme.colorScheme.error)
        } },
        enableDismissFromStartToEnd = false,
    ) {
        OutlinedCard(onClick = { actions.edit(banner) }, shape = AppShapes.medium) {
            BannerPreview(banner, Modifier.fillMaxWidth().height(164.dp))
            Row(Modifier.fillMaxWidth().padding(AppSpacing.small), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(banner.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(stringResource(R.string.ui_position_banner_sortorder_1_banner_7464d61, banner.sortOrder + 1, banner.contentAlignment.label()),
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

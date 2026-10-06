package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.R
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

sealed interface ContentState {
    data object Loading : ContentState

    data class Empty(
        val title: String? = null,
        val description: String? = null,
        @param:DrawableRes val illustration: Int = R.drawable.img_empty_search,
        val actionLabel: String? = null,
    ) : ContentState

    data class Error(
        val title: String? = null,
        val description: String? = null,
        val retryLabel: String? = null,
    ) : ContentState

    data class Offline(
        val title: String? = null,
        val description: String? = null,
        val retryLabel: String? = null,
    ) : ContentState

    data object Content : ContentState
}

@Composable
fun ContentStateView(
    state: ContentState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onAction: () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ContentState.Loading -> LoadingState()
            is ContentState.Empty -> EmptyState(state, onAction)
            is ContentState.Error -> ErrorState(state, onAction)
            is ContentState.Offline -> OfflineState(state, onAction)
            ContentState.Content -> content()
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    AppLottieAnimation(
        animation = AppAnimation.Loading,
        loop = true,
        modifier = modifier.size(96.dp)
            .semanticsDescription(stringResource(R.string.loading)),
    )
}

@Composable
private fun EmptyState(state: ContentState.Empty, onAction: () -> Unit) {
    StateMessage(
        illustration = null,
        animation = AppAnimation.EmptyData,
        illustrationDescription = state.title ?: stringResource(R.string.no_content),
        title = state.title ?: stringResource(R.string.no_content),
        description = state.description ?: stringResource(R.string.no_content_description),
        actionLabel = state.actionLabel,
        onAction = onAction,
        loop = true
    )
}

@Composable
private fun ErrorState(state: ContentState.Error, onAction: () -> Unit) {
    StateMessage(
        illustration = R.drawable.img_generic_error,
        animation = null,
        illustrationDescription = null,
        title = state.title ?: stringResource(R.string.generic_error),
        description = state.description ?: stringResource(R.string.generic_error_description),
        actionLabel = state.retryLabel ?: stringResource(R.string.retry),
        onAction = onAction,
    )
}

@Composable
private fun OfflineState(state: ContentState.Offline, onAction: () -> Unit) {
    StateMessage(
        illustration = R.drawable.img_no_internet,
        animation = null,
        illustrationDescription = null,
        title = state.title ?: stringResource(R.string.offline),
        description = state.description ?: stringResource(R.string.offline_description),
        actionLabel = state.retryLabel ?: stringResource(R.string.retry),
        onAction = onAction,
    )
}

@Composable
private fun StateMessage(
    @DrawableRes illustration: Int?,
    animation: AppAnimation?,
    illustrationDescription: String?,
    title: String,
    loop : Boolean = false,
    description: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = AppDimensions.contentMaxWidth)
            .padding(AppSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        if (animation != null) AppLottieAnimation(
            animation = animation,
            modifier = Modifier.size(AppDimensions.stateIllustration)
                .semanticsDescription(illustrationDescription.orEmpty()),
            loop = loop
        ) else if (illustration != null) Image(
            painter = painterResource(illustration), contentDescription = illustrationDescription,
            modifier = Modifier.size(AppDimensions.stateIllustration), contentScale = ContentScale.Fit,
        )
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        actionLabel?.let { PrimaryButton(text = it, onClick = onAction, modifier = Modifier.widthIn(max = 220.dp)) }
    }
}

private fun Modifier.semanticsDescription(description: String): Modifier =
    semantics { contentDescription = description }

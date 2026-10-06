package com.spacetecsolutions.meatapp.feature.superadmin.productlimit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.AppTopBar
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppShapes
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.model.ProductLimitStatus

@Composable
fun ProductLimitRoute(
    onMessage: (ProductLimitMessage) -> Unit,
    viewModel: ProductLimitViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) {
        state.message?.let {
            onMessage(it)
            viewModel.consumeMessage()
        }
    }
    ProductLimitScreen(
        state = state,
        onLimitChange = viewModel::updateLimit,
        onSave = viewModel::save,
        onRefresh = viewModel::refresh,
    )
}

@Composable
private fun ProductLimitScreen(
    state: ProductLimitUiState,
    onLimitChange: (String) -> Unit,
    onSave: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Product Limit",
                actionIcon = AppIcons.Refresh,
                actionDescription = "Refresh product count",
                onAction = onRefresh,
            )
        },
    ) { padding ->
        when {
            state.loading -> ContentStateView(ContentState.Loading, contentPadding = padding)
            state.status == null -> ContentStateView(
                ContentState.Error(description = state.error),
                contentPadding = padding,
                onAction = onRefresh,
            )
            else -> ProductLimitContent(state, padding, onLimitChange, onSave)
        }
    }
}

@Composable
private fun ProductLimitContent(
    state: ProductLimitUiState,
    padding: PaddingValues,
    onLimitChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val status = state.status ?: return
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = AppDimensions.contentMaxWidth).padding(AppSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        UsageCard(status)
        ElevatedCard(Modifier.fillMaxWidth(), shape = AppShapes.large) {
            Column(
                Modifier.fillMaxWidth().padding(AppSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            ) {
                Text("Set maximum products", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.limitText,
                    onValueChange = onLimitChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Maximum Products") },
                    leadingIcon = { Icon(AppIcons.ProductLimit, null) },
                    supportingText = {
                        Text(state.limitError ?: "The limit cannot be lower than ${status.countedProducts}")
                    },
                    isError = state.limitError != null,
                    enabled = !state.saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                AnimatedVisibility(state.dirty) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.saving && state.limitError == null,
                    ) {
                        if (state.saving) CircularProgressIndicator(Modifier.size(AppSpacing.large))
                        else Text("Update limit")
                    }
                }
            }
        }
        PolicyCard()
    }
}

@Composable
private fun UsageCard(status: ProductLimitStatus) {
    val target = if (status.maxProducts == 0) 1f
    else (status.countedProducts.toFloat() / status.maxProducts).coerceIn(0f, 1f)
    val progress by animateFloatAsState(target, label = "product limit usage")
    ElevatedCard(Modifier.fillMaxWidth(), shape = AppShapes.large) {
        Column(
            Modifier.fillMaxWidth().padding(AppSpacing.large),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            AnimatedContent(status, label = "product count") { value ->
                Text(
                    "${value.countedProducts} / ${value.maxProducts} products",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                if (status.limitReached) "Limit reached. Admins cannot create or restore products."
                else "${status.remaining} product slots remaining",
                color = if (status.limitReached) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PolicyCard() {
    Card(
        Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            Text("Counting policy", style = MaterialTheme.typography.titleMedium)
            Text("• Active and inactive products count toward the limit.")
            Text("• Archived products do not count.")
            Text("• Restoring an archived product requires an available slot.")
            Text("• Archive products before lowering the limit below the current count.")
        }
    }
}

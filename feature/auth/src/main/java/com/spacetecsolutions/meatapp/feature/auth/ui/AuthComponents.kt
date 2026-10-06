package com.spacetecsolutions.meatapp.feature.auth.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.AppLottieAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.bounceOnPress
import com.spacetecsolutions.meatapp.core.designsystem.layout.AdaptiveContent
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppDimensions
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing

@Composable
internal fun AuthFormScaffold(
    appName: String,
    @DrawableRes bgRes: Int,
    @DrawableRes logoRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(bgRes),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.matchParentSize().background(Color(0xFF350308).copy(alpha = 0.32f)))
        AdaptiveContent(maxContentWidth = 600.dp) {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
                contentPadding = PaddingValues(vertical = AppSpacing.comfortable),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.compact),
                    ) {
                        Image(
                            painter = painterResource(logoRes),
                            contentDescription = appName,
                            modifier = Modifier.size(76.dp),
                        )
                        Text(
                            appName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(AppSpacing.extraSmall))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(AppSpacing.large),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.compact),
                            ) {
                                Text(title, style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(Modifier.height(AppSpacing.extraSmall))
                                content()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MobileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Mobile, contentDescription = null)
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.auth.R.string.auth_country_code))
            }
        },
        supportingText = error?.let { { Text(it) } },
        isError = error != null,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    enabled: Boolean,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(AppIcons.Password, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) AppIcons.PasswordHidden else AppIcons.PasswordVisible,
                    contentDescription = if (visible) "Hide password" else "Show password",
                )
            }
        },
        supportingText = error?.let { { Text(it) } },
        isError = error != null,
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
internal fun RequestError(message: String?) {
    message?.let {
        Text(
            text = it,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun LoadingButton(text: String, loading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = Modifier.bounceOnPress(interactionSource).fillMaxWidth()
            .heightIn(min = AppDimensions.minimumTouchTarget),
        enabled = !loading,
        interactionSource = interactionSource,
    ) {
        if (loading) AppLottieAnimation(AppAnimation.Loading, Modifier.size(32.dp), loop = true)
        else Text(text)
    }
}

@Composable
internal fun GradientLoginButton(text: String, loading: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge
    val primary = MaterialTheme.colorScheme.primary
    val gradient = Brush.horizontalGradient(
        colorStops = arrayOf(0f to primary, .72f to primary, 1f to Color.White),
    )
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = Modifier.bounceOnPress(interactionSource).fillMaxWidth()
            .heightIn(min = AppDimensions.minimumTouchTarget)
            .clip(shape).background(gradient),
        enabled = !loading,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = .7f),
        ),
    ) {
        if (loading) AppLottieAnimation(
            AppAnimation.Loading, Modifier.size(32.dp), loop = true, tint = Color.White,
        ) else Text(text)
    }
}

@Composable
internal fun OtpField(value: String, onValueChange: (String) -> Unit, enabled: Boolean, error: String?) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(6)) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(color = Color.Transparent),
        decorationBox = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                    repeat(6) { index ->
                        val active = index == value.length
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = when {
                                        error != null -> MaterialTheme.colorScheme.error
                                        active -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(value.getOrNull(index)?.toString().orEmpty(), textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
    )
}

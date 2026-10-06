package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

enum class AppSnackbarType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class AppSnackbarMessage(
    val message: String,
    val type: AppSnackbarType = AppSnackbarType.INFO,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short,
)

class AppSnackbarManager {
    private val queue = Channel<AppSnackbarMessage>(capacity = Channel.BUFFERED)
    internal val messages = queue.receiveAsFlow()

    fun show(message: AppSnackbarMessage): Boolean =
        queue.trySend(message).isSuccess
}

@Composable
fun AppSnackbarHost(
    manager: AppSnackbarManager,
    hostState: SnackbarHostState,
    onAction: (AppSnackbarMessage) -> Unit = {},
) {
    var currentType by remember {
        mutableStateOf(AppSnackbarType.INFO)
    }

    LaunchedEffect(manager, hostState) {
        manager.messages.collect { message ->
            currentType = message.type

            val result = hostState.showSnackbar(
                message = message.message,
                actionLabel = message.actionLabel,
                withDismissAction = true,
                duration = message.duration,
            )

            if (result == SnackbarResult.ActionPerformed) {
                onAction(message)
            }
        }
    }

    SnackbarHost(hostState) { data ->
        AppSnackbar(
            data = data,
            type = currentType
        )
    }
}

@Composable
private fun AppSnackbar(
    data: SnackbarData,
    type: AppSnackbarType,
) {
    val visuals = snackbarVisuals(type)
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = shape,
        color = visuals.baseColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(visuals.gradient)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = visuals.icon,
                    contentDescription = visuals.description,
                    tint = visuals.content
                )

                Text(
                    text = data.visuals.message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = visuals.content
                )

                data.visuals.actionLabel?.let { label ->
                    TextButton(
                        onClick = data::performAction,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = visuals.content
                        )
                    ) {
                        Text(label)
                    }
                }

                IconButton(onClick = data::dismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = visuals.content
                    )
                }
            }
        }
    }
}

private data class SnackbarVisuals(
    val icon: ImageVector,
    val gradient: Brush,
    val baseColor: Color,
    val content: Color,
    val description: String,
)

@Composable
private fun snackbarVisuals(
    type: AppSnackbarType
): SnackbarVisuals {

    return when (type) {

        AppSnackbarType.SUCCESS -> SnackbarVisuals(
            icon = Icons.Rounded.CheckCircle,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(0x4D2F8333),
                    Color(0xFF2F8333),
                )
            ),
            baseColor = Color(0xE62F8333),
            content = Color.White,
            description = "Success",
        )

        AppSnackbarType.ERROR -> SnackbarVisuals(
            icon = Icons.Rounded.Error,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(0x4DD31027),
                    Color(0xFFEA384D),
                )
            ),
            baseColor = Color(0xFFEA384D),
            content = Color.White,
            description = "Error",
        )

        AppSnackbarType.WARNING -> SnackbarVisuals(
            icon = Icons.Rounded.Warning,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(0x4DF7971E),
                    Color(0xFFF7971E),
                )
            ),
            baseColor = Color(0xFFF7971E),
            content = Color.White,
            description = "Warning",
        )

        AppSnackbarType.INFO -> SnackbarVisuals(
            icon = Icons.Rounded.Info,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(0x4D396AFC),
                    Color(0xFF2948FF),
                )
            ),
            baseColor = Color(0xFF2948FF),
            content = Color.White,
            description = "Information",
        )
    }
}
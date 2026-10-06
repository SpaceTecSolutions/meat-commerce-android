package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.designsystem.theme.SoftAmber
import com.spacetecsolutions.meatapp.core.designsystem.theme.SoftGreen
import com.spacetecsolutions.meatapp.core.designsystem.theme.SoftRed
import com.spacetecsolutions.meatapp.core.designsystem.theme.Success
import com.spacetecsolutions.meatapp.core.designsystem.theme.Warning

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick == null) {
        Card(modifier, colors = colors, elevation = elevation, border = border) {
            Column(Modifier.padding(AppSpacing.medium), content = content)
        }
    } else {
        Card(onClick, modifier, colors = colors, elevation = elevation, border = border) {
            Column(Modifier.padding(AppSpacing.medium), content = content)
        }
    }
}

enum class StatusTone { NEUTRAL, INFO, SUCCESS, WARNING, ERROR }

@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier, tone: StatusTone = StatusTone.NEUTRAL) {
    val colors = when (tone) {
        StatusTone.SUCCESS -> SoftGreen to Success
        StatusTone.WARNING -> SoftAmber to Warning
        StatusTone.ERROR -> SoftRed to MaterialTheme.colorScheme.error
        StatusTone.INFO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(modifier.wrapContentWidth(), color = colors.first, contentColor = colors.second, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = AppSpacing.compact, vertical = AppSpacing.extraSmall),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        action?.let {
            Text(
                text = it,
                modifier = Modifier.wrapContentWidth(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** One switch treatment across every role and settings surface. */
@Composable
fun appSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Color(0xFF22A652),
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = Color(0xFFB7BBC1),
    uncheckedBorderColor = Color.Transparent,
    disabledCheckedThumbColor = Color.White.copy(alpha = .9f),
    disabledCheckedTrackColor = Color(0xFF22A652).copy(alpha = .45f),
    disabledUncheckedThumbColor = Color.White.copy(alpha = .9f),
    disabledUncheckedTrackColor = Color(0xFFB7BBC1).copy(alpha = .55f),
    disabledUncheckedBorderColor = Color.Transparent,
)

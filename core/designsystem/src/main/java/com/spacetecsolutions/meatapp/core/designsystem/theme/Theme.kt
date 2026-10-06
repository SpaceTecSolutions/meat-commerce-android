package com.spacetecsolutions.meatapp.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun MeatBushTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    brandColors: BrandColors = DefaultBrandColors,
    content: @Composable () -> Unit,
) {
    val lightColors = lightColorScheme(
        primary = brandColors.primary,
        onPrimary = brandColors.onPrimary,
        secondary = brandColors.secondary,
        onSecondary = brandColors.onSecondary,
        secondaryContainer = SoftRed,
        onSecondaryContainer = brandColors.primary,
        background = LightBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
        surfaceContainer = LightSurface,
        surfaceContainerLow = LightSurface,
        surfaceContainerHigh = LightSurfaceVariant,
    )
    val darkColors = darkColorScheme(
        primary = brandColors.primary,
        onPrimary = brandColors.onPrimary,
        secondary = brandColors.secondary,
        onSecondary = brandColors.onSecondary,
        background = DarkBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
    )
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        shapes = MaterialTheme.shapes.copy(
            extraSmall = AppShapes.extraSmall,
            small = AppShapes.small,
            medium = AppShapes.medium,
            large = AppShapes.large,
            extraLarge = AppShapes.extraLarge,
        ),
        typography = AppTypography,
        content = content,
    )
}

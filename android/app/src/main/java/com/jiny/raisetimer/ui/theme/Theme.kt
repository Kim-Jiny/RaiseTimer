package com.jiny.raisetimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RaiseTimerColors = darkColorScheme(
    primary = ChipGold,
    onPrimary = FeltGreenDark,
    secondary = FeltGreenLight,
    onSecondary = OnSurface,
    tertiary = FeltGreenGlow,
    onTertiary = OnSurface,
    background = FeltGreenDark,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceHighlight,
    onSurfaceVariant = OnSurfaceMuted,
    error = ChipRed,
    onError = OnSurface,
)

@Composable
fun RaiseTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RaiseTimerColors,
        typography = RaiseTimerTypography,
        content = content,
    )
}

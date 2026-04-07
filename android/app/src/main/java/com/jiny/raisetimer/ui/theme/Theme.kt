package com.jiny.raisetimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.jiny.raisetimer.domain.model.ThemePreset

@Immutable
data class RaiseTimerPalette(
    val timerGradient: List<androidx.compose.ui.graphics.Color>,
    val timerGlow: androidx.compose.ui.graphics.Color,
    val accentSoft: androidx.compose.ui.graphics.Color,
    val feltGreen: androidx.compose.ui.graphics.Color,
    val feltGreenDark: androidx.compose.ui.graphics.Color,
    val chipGold: androidx.compose.ui.graphics.Color,
    val chipGoldSoft: androidx.compose.ui.graphics.Color,
    val chipRed: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val surfaceElevated: androidx.compose.ui.graphics.Color,
    val surfaceHighlight: androidx.compose.ui.graphics.Color,
    val onSurface: androidx.compose.ui.graphics.Color,
    val onSurfaceMuted: androidx.compose.ui.graphics.Color,
)

val LocalRaiseTimerPalette = staticCompositionLocalOf {
    RaiseTimerPalette(
        timerGradient = listOf(FeltGreenGlow, FeltGreenDark, FeltGreen),
        timerGlow = FeltGreenGlow,
        accentSoft = ChipGoldSoft,
        feltGreen = FeltGreen,
        feltGreenDark = FeltGreenDark,
        chipGold = ChipGold,
        chipGoldSoft = ChipGoldSoft,
        chipRed = ChipRed,
        surface = Surface,
        surfaceElevated = SurfaceElevated,
        surfaceHighlight = SurfaceHighlight,
        onSurface = OnSurface,
        onSurfaceMuted = OnSurfaceMuted,
    )
}

private data class RaiseTimerThemeTokens(
    val primary: androidx.compose.ui.graphics.Color,
    val onPrimary: androidx.compose.ui.graphics.Color,
    val secondary: androidx.compose.ui.graphics.Color,
    val tertiary: androidx.compose.ui.graphics.Color,
    val background: androidx.compose.ui.graphics.Color,
    val onBackground: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val surfaceElevated: androidx.compose.ui.graphics.Color,
    val surfaceVariant: androidx.compose.ui.graphics.Color,
    val onSurface: androidx.compose.ui.graphics.Color,
    val onSurfaceVariant: androidx.compose.ui.graphics.Color,
    val error: androidx.compose.ui.graphics.Color,
    val timerGradient: List<androidx.compose.ui.graphics.Color>,
    val accentSoft: androidx.compose.ui.graphics.Color,
)

private fun tokensFor(themePreset: ThemePreset, darkTheme: Boolean): RaiseTimerThemeTokens =
    when (themePreset) {
        ThemePreset.EMERALD -> if (darkTheme) {
            RaiseTimerThemeTokens(
                primary = ChipGold,
                onPrimary = FeltGreenDark,
                secondary = FeltGreenLight,
                tertiary = FeltGreenGlow,
                background = FeltGreenDark,
                onBackground = OnSurface,
                surface = Surface,
                surfaceElevated = SurfaceElevated,
                surfaceVariant = SurfaceHighlight,
                onSurface = OnSurface,
                onSurfaceVariant = OnSurfaceMuted,
                error = ChipRed,
                timerGradient = listOf(FeltGreenGlow.copy(alpha = 0.24f), FeltGreenDark, FeltGreen),
                accentSoft = ChipGoldSoft,
            )
        } else {
            RaiseTimerThemeTokens(
                primary = FeltGreenForest,
                onPrimary = LightSurfaceElevated,
                secondary = ChipGold,
                tertiary = FeltGreenGlow,
                background = FeltGreenMist,
                onBackground = OnSurfaceLight,
                surface = LightSurface,
                surfaceElevated = LightSurfaceElevated,
                surfaceVariant = LightSurfaceHighlight,
                onSurface = OnSurfaceLight,
                onSurfaceVariant = OnSurfaceMutedLight,
                error = ChipRed,
                timerGradient = listOf(FeltGreenGlow.copy(alpha = 0.22f), FeltGreenMist, LightSurface),
                accentSoft = ChipGoldSoft,
            )
        }
        ThemePreset.OCEAN -> if (darkTheme) {
            RaiseTimerThemeTokens(
                primary = OceanAqua,
                onPrimary = OceanDeep,
                secondary = OceanSky,
                tertiary = OceanGlow,
                background = OceanNight,
                onBackground = OnSurface,
                surface = OceanDeep,
                surfaceElevated = OceanSurfaceElevated,
                surfaceVariant = OceanCard,
                onSurface = OnSurface,
                onSurfaceVariant = OnSurfaceMuted,
                error = ChipRedSoft,
                timerGradient = listOf(OceanGlow.copy(alpha = 0.26f), OceanNight, OceanDeep),
                accentSoft = OceanSky,
            )
        } else {
            RaiseTimerThemeTokens(
                primary = OceanDeep,
                onPrimary = LightSurfaceElevated,
                secondary = OceanAqua,
                tertiary = OceanGlow,
                background = OceanMist,
                onBackground = OnSurfaceLight,
                surface = LightSurface,
                surfaceElevated = LightSurfaceElevated,
                surfaceVariant = OceanPale,
                onSurface = OnSurfaceLight,
                onSurfaceVariant = OnSurfaceMutedLight,
                error = ChipRed,
                timerGradient = listOf(OceanGlow.copy(alpha = 0.2f), OceanMist, LightSurface),
                accentSoft = OceanDeep,
            )
        }
        ThemePreset.RUBY -> if (darkTheme) {
            RaiseTimerThemeTokens(
                primary = RubyGlow,
                onPrimary = RubyNight,
                secondary = RubyRose,
                tertiary = RubyAccent,
                background = RubyNight,
                onBackground = OnSurface,
                surface = RubyDeep,
                surfaceElevated = RubySurfaceElevated,
                surfaceVariant = RubyCard,
                onSurface = OnSurface,
                onSurfaceVariant = OnSurfaceMuted,
                error = ChipRedSoft,
                timerGradient = listOf(RubyAccent.copy(alpha = 0.25f), RubyNight, RubyDeep),
                accentSoft = RubyRose,
            )
        } else {
            RaiseTimerThemeTokens(
                primary = RubyDeep,
                onPrimary = LightSurfaceElevated,
                secondary = RubyGlow,
                tertiary = RubyAccent,
                background = RubyMist,
                onBackground = OnSurfaceLight,
                surface = LightSurface,
                surfaceElevated = LightSurfaceElevated,
                surfaceVariant = RubyPale,
                onSurface = OnSurfaceLight,
                onSurfaceVariant = OnSurfaceMutedLight,
                error = ChipRed,
                timerGradient = listOf(RubyAccent.copy(alpha = 0.18f), RubyMist, LightSurface),
                accentSoft = RubyGlow,
            )
        }
        ThemePreset.SUNSET -> if (darkTheme) {
            RaiseTimerThemeTokens(
                primary = SunsetAccent,
                onPrimary = SunsetNight,
                secondary = SunsetGlow,
                tertiary = SunsetRose,
                background = SunsetNight,
                onBackground = OnSurface,
                surface = SunsetDeep,
                surfaceElevated = SunsetSurfaceElevated,
                surfaceVariant = SunsetCard,
                onSurface = OnSurface,
                onSurfaceVariant = OnSurfaceMuted,
                error = ChipRedSoft,
                timerGradient = listOf(SunsetAccent.copy(alpha = 0.26f), SunsetNight, SunsetDeep),
                accentSoft = SunsetGlow,
            )
        } else {
            RaiseTimerThemeTokens(
                primary = SunsetDeep,
                onPrimary = LightSurfaceElevated,
                secondary = SunsetAccent,
                tertiary = SunsetGlow,
                background = SunsetMist,
                onBackground = OnSurfaceLight,
                surface = LightSurface,
                surfaceElevated = LightSurfaceElevated,
                surfaceVariant = SunsetPale,
                onSurface = OnSurfaceLight,
                onSurfaceVariant = OnSurfaceMutedLight,
                error = ChipRed,
                timerGradient = listOf(SunsetGlow.copy(alpha = 0.18f), SunsetMist, LightSurface),
                accentSoft = SunsetDeep,
            )
        }
        ThemePreset.LAVENDER -> if (darkTheme) {
            RaiseTimerThemeTokens(
                primary = LavenderAccent,
                onPrimary = LavenderNight,
                secondary = LavenderGlow,
                tertiary = LavenderRose,
                background = LavenderNight,
                onBackground = OnSurface,
                surface = LavenderDeep,
                surfaceElevated = LavenderSurfaceElevated,
                surfaceVariant = LavenderCard,
                onSurface = OnSurface,
                onSurfaceVariant = OnSurfaceMuted,
                error = ChipRedSoft,
                timerGradient = listOf(LavenderAccent.copy(alpha = 0.24f), LavenderNight, LavenderDeep),
                accentSoft = LavenderGlow,
            )
        } else {
            RaiseTimerThemeTokens(
                primary = LavenderDeep,
                onPrimary = LightSurfaceElevated,
                secondary = LavenderAccent,
                tertiary = LavenderGlow,
                background = LavenderMist,
                onBackground = OnSurfaceLight,
                surface = LightSurface,
                surfaceElevated = LightSurfaceElevated,
                surfaceVariant = LavenderPale,
                onSurface = OnSurfaceLight,
                onSurfaceVariant = OnSurfaceMutedLight,
                error = ChipRed,
                timerGradient = listOf(LavenderGlow.copy(alpha = 0.18f), LavenderMist, LightSurface),
                accentSoft = LavenderDeep,
            )
        }
        ThemePreset.SLATE -> if (darkTheme) {
            RaiseTimerThemeTokens(
                primary = SlateAccent,
                onPrimary = SlateNight,
                secondary = SlateGlow,
                tertiary = SlateRose,
                background = SlateNight,
                onBackground = OnSurface,
                surface = SlateDeep,
                surfaceElevated = SlateSurfaceElevated,
                surfaceVariant = SlateCard,
                onSurface = OnSurface,
                onSurfaceVariant = OnSurfaceMuted,
                error = ChipRedSoft,
                timerGradient = listOf(SlateAccent.copy(alpha = 0.22f), SlateNight, SlateDeep),
                accentSoft = SlateGlow,
            )
        } else {
            RaiseTimerThemeTokens(
                primary = SlateDeep,
                onPrimary = LightSurfaceElevated,
                secondary = SlateAccent,
                tertiary = SlateGlow,
                background = SlateMist,
                onBackground = OnSurfaceLight,
                surface = LightSurface,
                surfaceElevated = LightSurfaceElevated,
                surfaceVariant = SlatePale,
                onSurface = OnSurfaceLight,
                onSurfaceVariant = OnSurfaceMutedLight,
                error = ChipRed,
                timerGradient = listOf(SlateGlow.copy(alpha = 0.18f), SlateMist, LightSurface),
                accentSoft = SlateDeep,
            )
        }
        ThemePreset.MINT -> if (darkTheme) {
            RaiseTimerThemeTokens(
                primary = MintAccent,
                onPrimary = MintNight,
                secondary = MintGlow,
                tertiary = MintRose,
                background = MintNight,
                onBackground = OnSurface,
                surface = MintDeep,
                surfaceElevated = MintSurfaceElevated,
                surfaceVariant = MintCard,
                onSurface = OnSurface,
                onSurfaceVariant = OnSurfaceMuted,
                error = ChipRedSoft,
                timerGradient = listOf(MintAccent.copy(alpha = 0.24f), MintNight, MintDeep),
                accentSoft = MintGlow,
            )
        } else {
            RaiseTimerThemeTokens(
                primary = MintDeep,
                onPrimary = LightSurfaceElevated,
                secondary = MintAccent,
                tertiary = MintGlow,
                background = MintMist,
                onBackground = OnSurfaceLight,
                surface = LightSurface,
                surfaceElevated = LightSurfaceElevated,
                surfaceVariant = MintPale,
                onSurface = OnSurfaceLight,
                onSurfaceVariant = OnSurfaceMutedLight,
                error = ChipRed,
                timerGradient = listOf(MintGlow.copy(alpha = 0.18f), MintMist, LightSurface),
                accentSoft = MintDeep,
            )
        }
        ThemePreset.CORAL -> if (darkTheme) {
            RaiseTimerThemeTokens(
                primary = CoralAccent,
                onPrimary = CoralNight,
                secondary = CoralGlow,
                tertiary = CoralRose,
                background = CoralNight,
                onBackground = OnSurface,
                surface = CoralDeep,
                surfaceElevated = CoralSurfaceElevated,
                surfaceVariant = CoralCard,
                onSurface = OnSurface,
                onSurfaceVariant = OnSurfaceMuted,
                error = ChipRedSoft,
                timerGradient = listOf(CoralAccent.copy(alpha = 0.22f), CoralNight, CoralDeep),
                accentSoft = CoralGlow,
            )
        } else {
            RaiseTimerThemeTokens(
                primary = CoralDeep,
                onPrimary = LightSurfaceElevated,
                secondary = CoralAccent,
                tertiary = CoralGlow,
                background = CoralMist,
                onBackground = OnSurfaceLight,
                surface = LightSurface,
                surfaceElevated = LightSurfaceElevated,
                surfaceVariant = CoralPale,
                onSurface = OnSurfaceLight,
                onSurfaceVariant = OnSurfaceMutedLight,
                error = ChipRed,
                timerGradient = listOf(CoralGlow.copy(alpha = 0.18f), CoralMist, LightSurface),
                accentSoft = CoralDeep,
            )
        }
        ThemePreset.MIDNIGHT -> if (darkTheme) {
            RaiseTimerThemeTokens(
                primary = MidnightAccent,
                onPrimary = MidnightNight,
                secondary = MidnightGlow,
                tertiary = MidnightRose,
                background = MidnightNight,
                onBackground = OnSurface,
                surface = MidnightDeep,
                surfaceElevated = MidnightSurfaceElevated,
                surfaceVariant = MidnightCard,
                onSurface = OnSurface,
                onSurfaceVariant = OnSurfaceMuted,
                error = ChipRedSoft,
                timerGradient = listOf(MidnightAccent.copy(alpha = 0.2f), MidnightNight, MidnightDeep),
                accentSoft = MidnightGlow,
            )
        } else {
            RaiseTimerThemeTokens(
                primary = MidnightDeep,
                onPrimary = LightSurfaceElevated,
                secondary = MidnightAccent,
                tertiary = MidnightGlow,
                background = MidnightMist,
                onBackground = OnSurfaceLight,
                surface = LightSurface,
                surfaceElevated = LightSurfaceElevated,
                surfaceVariant = MidnightPale,
                onSurface = OnSurfaceLight,
                onSurfaceVariant = OnSurfaceMutedLight,
                error = ChipRed,
                timerGradient = listOf(MidnightGlow.copy(alpha = 0.16f), MidnightMist, LightSurface),
                accentSoft = MidnightDeep,
            )
        }
    }

private fun darkScheme(tokens: RaiseTimerThemeTokens) = darkColorScheme(
    primary = tokens.primary,
    onPrimary = tokens.onPrimary,
    secondary = tokens.secondary,
    onSecondary = tokens.onSurface,
    tertiary = tokens.tertiary,
    onTertiary = tokens.onSurface,
    background = tokens.background,
    onBackground = tokens.onBackground,
    surface = tokens.surface,
    onSurface = tokens.onSurface,
    surfaceVariant = tokens.surfaceVariant,
    onSurfaceVariant = tokens.onSurfaceVariant,
    error = tokens.error,
    onError = tokens.onSurface,
)

private fun lightScheme(tokens: RaiseTimerThemeTokens) = lightColorScheme(
    primary = tokens.primary,
    onPrimary = tokens.onPrimary,
    secondary = tokens.secondary,
    onSecondary = tokens.onSurface,
    tertiary = tokens.tertiary,
    onTertiary = tokens.onPrimary,
    background = tokens.background,
    onBackground = tokens.onBackground,
    surface = tokens.surface,
    onSurface = tokens.onSurface,
    surfaceVariant = tokens.surfaceVariant,
    onSurfaceVariant = tokens.onSurfaceVariant,
    error = tokens.error,
    onError = tokens.onPrimary,
)

@Composable
fun RaiseTimerTheme(
    themePreset: ThemePreset = ThemePreset.EMERALD,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val tokens = tokensFor(themePreset, darkTheme)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalRaiseTimerPalette provides RaiseTimerPalette(
            timerGradient = tokens.timerGradient,
            timerGlow = tokens.tertiary,
            accentSoft = tokens.accentSoft,
            feltGreen = if (darkTheme) tokens.timerGradient.last() else tokens.background,
            feltGreenDark = tokens.background,
            chipGold = tokens.primary,
            chipGoldSoft = tokens.accentSoft,
            chipRed = tokens.error,
            surface = tokens.surface,
            surfaceElevated = tokens.surfaceElevated,
            surfaceHighlight = tokens.surfaceVariant,
            onSurface = tokens.onSurface,
            onSurfaceMuted = tokens.onSurfaceVariant,
        )
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme(tokens) else lightScheme(tokens),
            typography = RaiseTimerTypography,
            content = content,
        )
    }
}

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.StudioThemeMode

private val LightStudioColorScheme = lightColorScheme(
    primary = StudioObsidianInk,
    onPrimary = Color.White,
    primaryContainer = StudioSurfaceVariant,
    onPrimaryContainer = StudioObsidianInk,
    secondary = SignalCrimson,
    onSecondary = Color.White,
    secondaryContainer = SignalCrimsonSoft,
    onSecondaryContainer = SignalCrimson,
    tertiary = BlueprintCobalt,
    onTertiary = Color.White,
    tertiaryContainer = BlueprintCobaltSoft,
    onTertiaryContainer = BlueprintCobalt,
    background = StudioAlabasterBg,
    onBackground = StudioObsidianInk,
    surface = StudioPureSurface,
    onSurface = StudioObsidianInk,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioSlateSecondary,
    outline = StudioBorderLight,
    outlineVariant = StudioBorderLight
)

private val NothingDarkColorScheme = darkColorScheme(
    primary = NothingPureWhite,
    onPrimary = NothingDarkBg,
    primaryContainer = NothingDarkSurfaceVariant,
    onPrimaryContainer = NothingPureWhite,
    secondary = SignalCrimson,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C0D1D),
    onSecondaryContainer = Color(0xFFFFB3C1),
    tertiary = BlueprintCobalt,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF172B54),
    onTertiaryContainer = Color(0xFFBFDBFE),
    background = NothingDarkBg,
    onBackground = NothingPureWhite,
    surface = NothingDarkSurface,
    onSurface = NothingPureWhite,
    surfaceVariant = NothingDarkSurfaceVariant,
    onSurfaceVariant = NothingSecondaryText,
    outline = NothingBorderDark,
    outlineVariant = NothingBorderDark
)

val KapsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun KapsTheme(
    themeMode: StudioThemeMode = StudioThemeMode.LIGHT_IPADOS,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        StudioThemeMode.LIGHT_IPADOS -> false
        StudioThemeMode.DARK_NOTHING -> true
        StudioThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) NothingDarkColorScheme else LightStudioColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = KapsShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    KapsTheme(
        themeMode = if (darkTheme) StudioThemeMode.DARK_NOTHING else StudioThemeMode.LIGHT_IPADOS,
        content = content
    )
}

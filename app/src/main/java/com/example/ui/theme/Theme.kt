package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage

private val LeviDarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = OnMintPrimary,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    secondary = CyanSecondary,
    onSecondary = OnCyanSecondary,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    tertiary = AmberTertiary,
    onTertiary = OnAmberTertiary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = SlateOutline,
    outlineVariant = SlateOutlineSubtle,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SlateSurfaceLow,
    surfaceContainer = SlateSurface,
    surfaceContainerHigh = SlateSurfaceHigh,
    surfaceContainerHighest = SlateSurfaceHighest,
    error = ErrorCoral,
    onError = OnErrorCoral,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val LeviShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun LeviLabTheme(
    language: AppLanguage = AppLanguage.AR,
    content: @Composable () -> Unit
) {
    val direction = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = LeviDarkColorScheme,
            typography = leviTypography(language),
            shapes = LeviShapes,
            content = content
        )
    }
}

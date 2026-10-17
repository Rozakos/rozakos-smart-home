package com.rozakos.home.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandPalette.Crimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF8E4E3),
    onPrimaryContainer = Color(0xFF57100F),
    secondary = BrandPalette.Navy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E9EF),
    onSecondaryContainer = BrandPalette.Navy,
    tertiary = BrandPalette.SuccessInk,
    tertiaryContainer = Color(0xFFDDF3EF),
    onTertiaryContainer = BrandPalette.SuccessInk,
    background = BrandPalette.Background,
    onBackground = BrandPalette.Navy,
    surface = Color(0xFFFFFFFF),
    onSurface = BrandPalette.Navy,
    surfaceVariant = Color(0xFFEEEEF2),
    onSurfaceVariant = BrandPalette.Muted,
    surfaceTint = BrandPalette.Crimson,
    surfaceContainerLow = Color(0xFFFAFAFB),
    surfaceContainer = Color(0xFFF0F0F3),
    surfaceContainerHigh = Color(0xFFEBEBEF),
    surfaceContainerHighest = Color(0xFFE6E6EC),
    outline = Color(0xFF858591),
    outlineVariant = BrandPalette.Border,
    error = BrandPalette.Crimson,
    errorContainer = Color(0xFFFBE6E6),
    onErrorContainer = Color(0xFF631210),
    inverseSurface = BrandPalette.Navy,
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFF4AAA7),
)

@Composable
fun RozakosHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}

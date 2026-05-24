package com.bandfocus.app.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BandFocusBlue = Color(0xFF3B82F6)
val BandFocusCyan = Color(0xFF06B6D4)
val BandFocusLime = Color(0xFFA3E635)
val BandFocusSlate950 = Color(0xFF0F172A)
val BandFocusSlate900 = Color(0xFF111827)
val BandFocusSlate800 = Color(0xFF1E293B)
val BandFocusSlate100 = Color(0xFFE2E8F0)
val BandFocusSlate50 = Color(0xFFF8FAFC)

val BandFocusHeroGradient = Brush.linearGradient(
    colors = listOf(BandFocusBlue, BandFocusCyan, BandFocusLime)
)

private val LightScheme = lightColorScheme(
    primary = BandFocusBlue,
    secondary = BandFocusCyan,
    tertiary = BandFocusLime,
    background = BandFocusSlate50,
    surface = Color.White,
    surfaceVariant = BandFocusSlate100,
    onPrimary = Color.White,
    onSecondary = BandFocusSlate950,
    onTertiary = BandFocusSlate950,
    onBackground = BandFocusSlate950,
    onSurface = BandFocusSlate950,
    onSurfaceVariant = BandFocusSlate900
)

private val DarkScheme = darkColorScheme(
    primary = BandFocusBlue,
    secondary = BandFocusCyan,
    tertiary = BandFocusLime,
    background = BandFocusSlate950,
    surface = BandFocusSlate800,
    surfaceVariant = BandFocusSlate900,
    onPrimary = Color.White,
    onSecondary = BandFocusSlate950,
    onTertiary = BandFocusSlate950,
    onBackground = BandFocusSlate50,
    onSurface = BandFocusSlate50,
    onSurfaceVariant = BandFocusSlate100
)

@Composable
fun BandFocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

package com.bandfocus.app.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElectricBlue = Color(0xFF3B82F6)
private val Cyan = Color(0xFF06B6D4)
private val Lime = Color(0xFFA3E635)
private val Slate950 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate50 = Color(0xFFF8FAFC)

private val LightScheme = lightColorScheme(
    primary = ElectricBlue,
    secondary = Cyan,
    tertiary = Lime,
    background = Slate50,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Slate950,
    onTertiary = Slate950,
    onBackground = Slate950,
    onSurface = Slate950
)

private val DarkScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = Cyan,
    tertiary = Lime,
    background = Slate950,
    surface = Slate800,
    onPrimary = Color.White,
    onSecondary = Slate950,
    onTertiary = Slate950,
    onBackground = Slate50,
    onSurface = Slate50
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

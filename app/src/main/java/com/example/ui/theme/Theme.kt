package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberPrimary,
    onPrimary = Color(0xFF003548),
    primaryContainer = CyberPrimaryContainer,
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = CyberSecondary,
    onSecondary = Color(0xFF003824),
    secondaryContainer = CyberSecondaryContainer,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = CyberTertiary,
    onTertiary = Color(0xFF4A0072),
    tertiaryContainer = CyberTertiaryContainer,
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = CyberBackground,
    onBackground = CyberTextPrimary,
    surface = CyberSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberTextSecondary,
    error = CyberError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // For PythonX AI IDE, we always render our premium Cyber-Dark AMOLED theme for professional coding ergonomics
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

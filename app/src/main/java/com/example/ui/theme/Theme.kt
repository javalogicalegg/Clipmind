package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalClipThemePreset = staticCompositionLocalOf { ClipThemePreset.OLED_MIDNIGHT }

fun ClipThemePreset.toColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            primaryContainer = surfaceVariantColor,
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            onSecondary = bgColor,
            secondaryContainer = surfaceVariantColor,
            onSecondaryContainer = secondaryColor,
            tertiary = tertiaryColor,
            onTertiary = bgColor,
            background = bgColor,
            onBackground = textPrimary,
            surface = surfaceColor,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = textSecondary,
            outline = borderSubtle,
            outlineVariant = borderSubtle.copy(alpha = 0.6f),
            error = Color(0xFFFF6B6B),
            onError = Color(0xFF2D0606)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            primaryContainer = surfaceVariantColor,
            onPrimaryContainer = textPrimary,
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = surfaceVariantColor,
            onSecondaryContainer = secondaryColor,
            tertiary = tertiaryColor,
            onTertiary = Color.White,
            background = bgColor,
            onBackground = textPrimary,
            surface = surfaceColor,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = textSecondary,
            outline = borderSubtle,
            outlineVariant = borderSubtle.copy(alpha = 0.65f),
            error = Color(0xFFB91C1C),
            onError = Color.White
        )
    }
}

@Composable
fun ClipMindTheme(
    preset: ClipThemePreset = ClipThemePreset.OLED_MIDNIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(preset) { preset.toColorScheme() }
    CompositionLocalProvider(LocalClipThemePreset provides preset) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ZX Lite Dark Premium is the signature look (Charcoal, dark greenish surface, emerald green accents)
private val ZxDarkColorScheme = darkColorScheme(
    primary = ZxEmeraldPrimary,
    onPrimary = Color(0xFF042F22),
    primaryContainer = ZxEmeraldContainer,
    onPrimaryContainer = ZxOnEmeraldContainer,
    secondary = ZxEmeraldBright,
    onSecondary = Color(0xFF042F22),
    secondaryContainer = Color(0xFF1E3A2F),
    onSecondaryContainer = Color(0xFFC7F9E5),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF064E3B),
    background = ZxBackgroundCharcoal,
    onBackground = ZxTextPrimary,
    surface = ZxSurfaceDark,
    onSurface = ZxTextPrimary,
    surfaceVariant = ZxSurfaceVariant,
    onSurfaceVariant = ZxTextSecondary,
    outline = ZxSurfaceBorder,
    outlineVariant = Color(0xFF2D3C34),
    error = ZxStatusError,
    onError = Color.White
)

private val ZxLightColorScheme = lightColorScheme(
    primary = ZxEmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = ZxEmeraldPrimary,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = ZxStatusError,
    onError = Color.White
)

@Composable
fun ZxLiteTheme(
    darkTheme: Boolean = true, // Default to dark premium aesthetic per ZX design requirements
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ZxDarkColorScheme else ZxLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

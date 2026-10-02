package com.bloomcycle.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Placeholder palette — replaced by the full design system in Phase 2.
 * dynamicColor stays off: Material You extraction would override the phase colours,
 * which carry real meaning in this app.
 */
private val BloomLight = lightColorScheme(
    primary = Color(0xFF8E4157),
    onPrimary = Color(0xFFFFFBF7),
    secondary = Color(0xFF6E7C63),
    background = Color(0xFFF7F1E8),
    surface = Color(0xFFFCF8F2),
)

@Composable
fun BloomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BloomLight,
        content = content,
    )
}

package com.bloomcycle.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.bloomcycle.app.domain.model.PhaseType

/** Phase colours, resolved for whichever theme is currently active. */
@Immutable
data class PhasePalette(
    val colors: Map<PhaseType, Color>,
) {
    @ReadOnlyComposable
    @Composable
    fun of(phase: PhaseType?): Color =
        phase?.let { colors[it] } ?: MaterialTheme.colorScheme.outline
}

private val LocalPhasePalette = staticCompositionLocalOf { PhasePalette(PhaseColors) }
private val LocalBloomMotion = staticCompositionLocalOf { BloomMotion.Default }

object Bloom {
    val phasePalette: PhasePalette
        @ReadOnlyComposable @Composable get() = LocalPhasePalette.current

    val motion: BloomMotion
        @ReadOnlyComposable @Composable get() = LocalBloomMotion.current

    @ReadOnlyComposable
    @Composable
    fun phaseColor(phase: PhaseType?): Color = phasePalette.of(phase)
}

/**
 * `dynamicColor` stays off. Material You would replace the phase palette with extracted
 * wallpaper colours, and these hues carry product meaning — losing them to a wallpaper
 * would be a bug, not a feature.
 */
@Composable
fun BloomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) BloomDarkScheme else BloomLightScheme
    CompositionLocalProvider(
        LocalPhasePalette provides PhasePalette(if (darkTheme) PhaseColorsDark else PhaseColors),
        LocalBloomMotion provides BloomMotion.Default,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BloomTypography,
            shapes = BloomShapes,
            content = content,
        )
    }
}

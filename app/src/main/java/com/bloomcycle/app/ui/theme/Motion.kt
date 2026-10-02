package com.bloomcycle.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Motion is where the "subtle and delicate" brief actually lives. Nothing snaps: entries
 * settle on a soft overshoot-free curve, and the slow end of the scale (600ms) is reserved
 * for the one moment that matters — the cycle turning over.
 */
@Immutable
data class BloomMotion(
    val easing: Easing,
    val easingEntrance: Easing,
    val easingExit: Easing,
    val durationQuick: Int,
    val durationBase: Int,
    val durationSlow: Int,
    val durationTurn: Int,
) {
    fun quick() = tween<Float>(durationQuick, easing = easing)
    fun base() = tween<Float>(durationBase, easing = easing)
    fun slow() = tween<Float>(durationSlow, easing = easing)

    companion object {
        val Default = BloomMotion(
            // Gentle in, gentle out — no linear corners, no bouncy overshoot.
            easing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f),
            easingEntrance = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
            easingExit = CubicBezierEasing(0.4f, 0f, 1f, 1f),
            durationQuick = 140,
            durationBase = 260,
            durationSlow = 420,
            durationTurn = 600,
        )
    }
}

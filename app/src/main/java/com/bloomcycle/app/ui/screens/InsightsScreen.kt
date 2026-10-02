package com.bloomcycle.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bloomcycle.app.ui.components.PlaceholderScreen

@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        modifier = modifier,
        eyebrow = "Insights",
        glyph = "⌁",
        title = "Patterns over time",
        body = "Cycle length, regularity and symptom trends drawn from your own history.",
    )
}

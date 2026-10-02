package com.bloomcycle.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bloomcycle.app.ui.components.PlaceholderScreen

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        modifier = modifier,
        eyebrow = "Today",
        glyph = "◐",
        title = "Today",
        body = "The countdown, the phase ring and one-tap logging all live here.",
    )
}

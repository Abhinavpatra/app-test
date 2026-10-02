package com.bloomcycle.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bloomcycle.app.ui.components.PlaceholderScreen

@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        modifier = modifier,
        eyebrow = "Calendar",
        glyph = "▦",
        title = "Month view",
        body = "Logged periods, predictions and fertile days across the month.",
    )
}

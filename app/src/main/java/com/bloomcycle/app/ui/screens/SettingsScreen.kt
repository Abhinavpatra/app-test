package com.bloomcycle.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bloomcycle.app.ui.components.PlaceholderScreen

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        modifier = modifier,
        eyebrow = "Settings",
        glyph = "⚙",
        title = "Your preferences",
        body = "Reminders, prediction assumptions, privacy and premium.",
    )
}

package com.bloomcycle.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bloomcycle.app.ui.components.PlaceholderScreen

@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        modifier = modifier,
        eyebrow = "Chat",
        glyph = "◌",
        title = "Talk it through",
        body = "Rooms matched to where you are in your cycle, not one crowded global thread.",
    )
}

package com.bloomcycle.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The five top-level destinations. Order matters: it is the reading order of the app and
 * the order TalkBack walks them.
 */
enum class BloomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Today", Icons.Outlined.WatchLater),
    Calendar("calendar", "Calendar", Icons.Outlined.CalendarMonth),
    Insights("insights", "Insights", Icons.Outlined.Insights),
    Chat("chat", "Chat", Icons.Outlined.Forum),
    Settings("settings", "Settings", Icons.Outlined.Settings),
    ;

    companion object {
        val start: BloomDestination = Home
    }
}

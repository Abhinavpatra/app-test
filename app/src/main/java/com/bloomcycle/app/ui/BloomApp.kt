package com.bloomcycle.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Icon
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bloomcycle.app.ui.navigation.BloomDestination
import com.bloomcycle.app.ui.screens.CalendarScreen
import com.bloomcycle.app.ui.screens.ChatScreen
import com.bloomcycle.app.ui.screens.HomeScreen
import com.bloomcycle.app.ui.screens.InsightsScreen
import com.bloomcycle.app.ui.screens.SettingsScreen
import com.bloomcycle.app.ui.theme.BloomMotion

/**
 * App shell.
 *
 * `NavigationSuiteScaffold` picks the navigation pattern from the window size class —
 * bottom bar on a phone, a rail on a foldable or tablet — so adapting to larger screens
 * needs no layout branches here.
 *
 * Transitions are fades only. Sliding a whole screen sideways is a web idiom; for a
 * quiet, private app a cross-fade with a shared background reads calmer and keeps the
 * tab switch feeling like turning a page rather than travelling somewhere.
 */
@Composable
fun BloomApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            BloomDestination.entries.forEach { destination ->
                item(
                    selected = currentRoute == destination.route,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(destination.label) },
                    alwaysShowLabel = true,
                )
            }
        },
    ) {
        // NavigationSuiteScaffold measures content into whatever space the navigation
        // component leaves, so it needs no PaddingValues of its own.
        NavHost(
            navController = navController,
            startDestination = BloomDestination.start.route,
            enterTransition = { fadeIn(tween(BloomMotion.Default.durationBase)) },
            exitTransition = { fadeOut(tween(BloomMotion.Default.durationQuick)) },
            popEnterTransition = { fadeIn(tween(BloomMotion.Default.durationBase)) },
            popExitTransition = { fadeOut(tween(BloomMotion.Default.durationQuick)) },
        ) {
            composable(BloomDestination.Home.route) { HomeScreen() }
            composable(BloomDestination.Calendar.route) { CalendarScreen() }
            composable(BloomDestination.Insights.route) { InsightsScreen() }
            composable(BloomDestination.Chat.route) { ChatScreen() }
            composable(BloomDestination.Settings.route) { SettingsScreen() }
        }
    }
}

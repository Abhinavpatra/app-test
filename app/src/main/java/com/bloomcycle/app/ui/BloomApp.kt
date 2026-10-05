package com.bloomcycle.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bloomcycle.app.domain.model.UserSettings
import com.bloomcycle.app.ui.navigation.BloomDestination
import com.bloomcycle.app.ui.screens.CalendarScreen
import com.bloomcycle.app.ui.screens.ChatScreen
import com.bloomcycle.app.ui.screens.HomeScreen
import com.bloomcycle.app.ui.screens.InsightsScreen
import com.bloomcycle.app.ui.screens.OnboardingScreen
import com.bloomcycle.app.ui.screens.PaywallScreen
import com.bloomcycle.app.ui.screens.SettingsScreen
import com.bloomcycle.app.ui.theme.BloomMotion

/**
 * The nav controller, published so a screen that is not a tab — the paywall — can be opened
 * from anywhere (a locked chart, Settings) without threading a callback through five layers
 * of parameters. `null` outside [BloomNavigation]; callers must be null-safe.
 */
val LocalNavController = staticCompositionLocalOf<NavController?> { null }

/**
 * App shell.
 *
 * Three jobs, in order: publish the container so any screen can reach it, hold onboarding
 * shut until it has been completed, and be the single place that talks to WorkManager —
 * onboarding and Settings only write flags, and [LaunchedEffect] below reacts to them.
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
    val container = rememberAppContainer()

    var settings by remember { mutableStateOf<UserSettings?>(null) }
    LaunchedEffect(container) { container.settings.settings.collect { settings = it } }

    CompositionLocalProvider(LocalAppContainer provides container) {
        when (val current = settings) {
            null -> LoadingBox()

            else -> {
                LaunchedEffect(
                    container,
                    current.remindersEnabled,
                    current.reminderHour,
                    current.reminderMinute,
                ) {
                    if (current.remindersEnabled) {
                        container.reminderScheduler.ensureScheduled(
                            hour = current.reminderHour,
                            minute = current.reminderMinute,
                        )
                    } else {
                        container.reminderScheduler.cancel()
                    }
                }

                if (!current.onboardingComplete) {
                    OnboardingScreen(onFinished = { /* the settings flow re-emits itself */ })
                } else {
                    BloomNavigation()
                }
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BloomNavigation() {
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
        CompositionLocalProvider(LocalNavController provides navController) {
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
                composable(BloomDestination.PAYWALL) {
                    PaywallScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

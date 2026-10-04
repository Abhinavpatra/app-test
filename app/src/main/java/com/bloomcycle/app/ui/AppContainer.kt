package com.bloomcycle.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.bloomcycle.app.AppContainer
import com.bloomcycle.app.BloomApplication

/**
 * Lets a screen, preview or test take the container without knowing about the Application
 * class. Production never sets it — [rememberAppContainer] falls back to the app's own
 * container — so tests and `@Preview`s can inject a stub with a single
 * `CompositionLocalProvider(LocalAppContainer provides stub)`.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer?> { null }

/** The container for this composition: injected if someone injected one, otherwise the real one. */
@Composable
fun rememberAppContainer(): AppContainer =
    LocalAppContainer.current
        ?: (LocalContext.current.applicationContext as BloomApplication).container

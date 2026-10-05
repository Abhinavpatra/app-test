package com.bloomcycle.app

import android.app.Application
import com.bloomcycle.app.notifications.BloomNotifications
import com.bloomcycle.app.ui.UiSounds
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class BloomApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        BloomNotifications.ensureChannels(this)
        UiSounds.init(this)
        initFirebase()
    }

    /**
     * Firebase only exists when the google-services plugin ran (config file present
     * at build time, `FIREBASE_CHAT` true). Otherwise this is a no-op and chat
     * stays on the in-memory repository — fresh clones never touch the network.
     *
     * The plugin normally initialises `FirebaseApp` itself via its content
     * provider; the explicit call is a belt-and-braces fallback for build setups
     * where manifest merging drops the provider. Everything is wrapped so a
     * misconfigured backend can never take down app startup.
     */
    private fun initFirebase() {
        if (!BuildConfig.FIREBASE_CHAT) return
        runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            val appCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance(),
                )
            } else {
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance(),
                )
            }
        }
    }
}

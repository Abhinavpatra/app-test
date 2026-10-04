package com.bloomcycle.app

import android.app.Application
import com.bloomcycle.app.notifications.BloomNotifications
import com.bloomcycle.app.ui.UiSounds

class BloomApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        BloomNotifications.ensureChannels(this)
        UiSounds.init(this)
    }
}

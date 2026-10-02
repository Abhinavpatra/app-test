package com.bloomcycle.app

import android.app.Application
import com.bloomcycle.app.notifications.BloomNotifications

class BloomApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        BloomNotifications.ensureChannels(this)
    }
}

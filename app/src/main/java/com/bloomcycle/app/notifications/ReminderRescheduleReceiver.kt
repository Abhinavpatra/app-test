package com.bloomcycle.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bloomcycle.app.BloomApplication
import kotlinx.coroutines.runBlocking

/**
 * Re-asserts the daily reminder after the three things that can invalidate WorkManager's
 * next run without any app code executing: a reboot, a timezone change, and a manual clock
 * change. The schedule is recomputed from the current clock, so the next run lands on the
 * hour the user actually asked for.
 *
 * WorkManager usually survives reboots on its own — this exists because "usually" is not a
 * guarantee a period reminder can afford.
 */
class ReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!handles(intent.action)) return
        val app = context.applicationContext
        if (app !is BloomApplication) return
        runBlocking { app.container.resyncReminder() }
    }

    companion object {
        private val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
        )

        fun handles(action: String?): Boolean = action in ACTIONS
    }
}

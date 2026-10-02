package com.bloomcycle.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bloomcycle.app.R

/**
 * Two channels so someone can mute the gentle daily note without losing the reminder
 * that actually matters, and vice versa.
 */
object BloomNotifications {

    const val CHANNEL_PERIOD = "bloom.period_reminders"
    const val CHANNEL_NOTES = "bloom.daily_notes"

    private const val ID_PERIOD = 41001
    private const val ID_NOTE = 41002

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PERIOD,
                context.getString(R.string.notification_channel_period_title),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notification_channel_period_desc) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NOTES,
                context.getString(R.string.notification_channel_tips_title),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.notification_channel_tips_desc) },
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showPeriodReminder(context: Context, title: String, body: String) {
        if (!canPost(context)) return
        post(context, ID_PERIOD, CHANNEL_PERIOD, title, body)
    }

    fun showDailyNote(context: Context, title: String, body: String) {
        if (!canPost(context)) return
        post(context, ID_NOTE, CHANNEL_NOTES, title, body)
    }

    private fun post(context: Context, id: Int, channel: String, title: String, body: String) {
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_bloom)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify(id, notification)
    }
}

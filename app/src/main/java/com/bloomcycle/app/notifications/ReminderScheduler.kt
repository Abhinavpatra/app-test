package com.bloomcycle.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the single daily check. Re-enqueued on every settings change and on every
 * logged period, so the reminder always reflects the current prediction.
 */
class ReminderScheduler(private val context: Context) {

    fun ensureScheduled(hour: Int, minute: Int) {
        BloomNotifications.ensureChannels(context)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(hour, minute).toMinutes(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().build())
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(TAG)
    }

    private fun delayUntil(hour: Int, minute: Int): Duration {
        val now = LocalDateTime.now()
        var target = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }

    private companion object {
        const val TAG = "bloom.daily-reminder"
    }
}

package com.bloomcycle.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bloomcycle.app.BloomApplication
import com.bloomcycle.app.R
import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.notifications.ReminderAction
import com.bloomcycle.app.domain.notifications.ReminderDecision
import kotlinx.coroutines.flow.first

/**
 * Runs once a day and says something only when [ReminderDecision] says something. Quiet by
 * default: a notification every day regardless of cycle position is the fastest way to be
 * muted, and the whole policy is unit tested in the domain rather than here.
 *
 * Uses WorkManager rather than AlarmManager exact alarms, which are heavily restricted
 * from Android 12 and would need SCHEDULE_EXACT_ALARM — unnecessary at day granularity.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as BloomApplication).container
        val settings = container.settings.settings.first()
        val today = container.cycleClock.today()
        val cycles = CycleCalculator.buildCycles(container.cycleRepository.periods())
        // HORMONAL_CONTRACEPTION yields null — nothing to remind about when there is no cycle.
        val prediction = CycleCalculator.predict(cycles, today, settings.cycleContext)

        val action = ReminderDecision.decide(
            remindersEnabled = settings.remindersEnabled,
            dailyNoteEnabled = settings.dailyNoteEnabled,
            predictionsMuted = settings.predictionsMuted,
            prediction = prediction,
            today = today,
        )

        when (action) {
            ReminderAction.PERIOD -> BloomNotifications.showPeriodReminder(
                applicationContext,
                applicationContext.getString(R.string.notification_period_title),
                applicationContext.getString(R.string.notification_period_body),
            )

            ReminderAction.EARLY_SIGNAL -> BloomNotifications.showDailyNote(
                applicationContext,
                applicationContext.getString(R.string.notification_early_title),
                applicationContext.getString(R.string.notification_early_body),
            )

            ReminderAction.NOTHING -> Unit
        }
        return Result.success()
    }
}

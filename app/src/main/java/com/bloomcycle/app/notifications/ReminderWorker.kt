package com.bloomcycle.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bloomcycle.app.BloomApplication
import com.bloomcycle.app.R
import com.bloomcycle.app.domain.cycle.CycleCalculator
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * Decides once a day whether there is anything worth saying. Quiet by default: a
 * notification every day regardless of cycle position is the fastest way to be muted.
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
        if (!settings.remindersEnabled) return Result.success()

        val today = container.cycleClock.today()
        val cycles = CycleCalculator.buildCycles(container.cycleRepository.periods())
        // HORMONAL_CONTRACEPTION yields null — nothing to remind about when there is no cycle.
        val prediction = CycleCalculator.predict(cycles, today, settings.cycleContext)

        if (prediction != null && !settings.predictionsMuted) {
            val daysUntil = prediction.daysUntilNextPeriod

            // A caveated context (perimenopause, postpartum) must never claim a due date:
            // "your period may be close" on a low-confidence estimate is exactly the
            // overreach plan.md §8.4 exists to prevent. The check-in carries no date.
            if (!prediction.isCaveated && daysUntil in 0..2) {
                BloomNotifications.showPeriodReminder(
                    applicationContext,
                    applicationContext.getString(R.string.notification_period_title),
                    applicationContext.getString(R.string.notification_period_body),
                )
                return Result.success()
            }

            // Soft early sign: the PMS window opens six days before the predicted start.
            if (daysUntil in 3..6) {
                BloomNotifications.showDailyNote(
                    applicationContext,
                    applicationContext.getString(R.string.notification_checkin_title),
                    applicationContext.getString(R.string.notification_checkin_body),
                )
            }
        }
        return Result.success()
    }
}

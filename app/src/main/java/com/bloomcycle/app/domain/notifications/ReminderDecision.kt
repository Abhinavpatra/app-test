package com.bloomcycle.app.domain.notifications

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.CyclePrediction

/** What is worth saying today. One action at most — a day never gets two notifications. */
enum class ReminderAction {
    NOTHING,
    /** Inside the last three days before the estimated start: the reminder that matters. */
    PERIOD,
    /** Inside the predicted PMS/luteal window: the gentle early-signal nudge. */
    EARLY_SIGNAL,
}

/**
 * The whole notification policy, as pure Kotlin.
 *
 * It lives apart from the worker because this is the logic that decides whether the app is
 * quiet or noisy, and quiet is the product decision that cannot be a bug: every branch below
 * is a unit test, and anything not explicitly allowed says nothing.
 *
 * Rules, in order:
 *  - notifications switched off, or nothing predicted (hormonal contraception yields no
 *    prediction at all) -> nothing
 *  - a caveated context (perimenopause, postpartum) never claims a due date (plan.md §8.4)
 *  - "keep predictions quiet" silences both of these
 *  - three days or fewer -> the period reminder
 *  - the rest of the six-day PMS window -> the early-signal nudge, and only when the
 *    daily-note toggle is on (same channel, so the toggle is the honest switch)
 */
object ReminderDecision {

    /** Six days before the estimated start — the prediction's own PMS window. */
    const val PMS_WINDOW_DAYS = 6
    const val PERIOD_WINDOW_DAYS = 2

    fun decide(
        remindersEnabled: Boolean,
        dailyNoteEnabled: Boolean,
        predictionsMuted: Boolean,
        prediction: CyclePrediction?,
        today: CycleDate,
    ): ReminderAction {
        if (!remindersEnabled) return ReminderAction.NOTHING
        if (prediction == null) return ReminderAction.NOTHING
        if (prediction.isCaveated) return ReminderAction.NOTHING
        if (predictionsMuted) return ReminderAction.NOTHING

        val daysUntil = prediction.daysUntilNextPeriod
        if (daysUntil in 0..PERIOD_WINDOW_DAYS) return ReminderAction.PERIOD

        val inPmsWindow = daysUntil in (PERIOD_WINDOW_DAYS + 1)..PMS_WINDOW_DAYS &&
            !today.isBefore(prediction.pmsWindowStart) &&
            !today.isAfter(prediction.pmsWindowEnd)
        return if (inPmsWindow && dailyNoteEnabled) ReminderAction.EARLY_SIGNAL
        else ReminderAction.NOTHING
    }
}

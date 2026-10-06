package com.bloomcycle.app.domain.home

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.cycle.PhaseResolver
import com.bloomcycle.app.domain.model.CycleContext
import com.bloomcycle.app.domain.model.CyclePrediction
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.PhaseState

/**
 * Everything the Today card shows, derived once per change instead of on every
 * recomposition. Pure: no Android, no Flow, no clock — the caller supplies `today`.
 */
data class HomeSummary(
    /** Phase for today, or null when there is not enough history to name one. */
    val phase: PhaseState?,
    /** Null when the engine has nothing to count down to. */
    val prediction: CyclePrediction?,
    /**
     * Plain-language stand-in for the phase card: "No cycles logged yet" and friends.
     * Non-null exactly when [phase] is null, so the UI never shows both.
     */
    val status: String?,
    /** Non-null whenever the user's [CycleContext] means dates must be hedged. */
    val caveat: String?,
    /** e.g. "Period expected in about 5 days". Null when there is no prediction to count down to. */
    val countdown: String?,
    /** False before the first real (non-spotting) period is logged. */
    val hasLoggedPeriod: Boolean,
) {
    val cycleDay: Int? get() = phase?.cycleDay
}

object HomeSummarizer {

    fun summarize(
        periods: List<PeriodEvent>,
        today: CycleDate,
        context: CycleContext = CycleContext.NONE,
        assumptions: CycleCalculator.Assumptions = CycleCalculator.Assumptions(),
    ): HomeSummary {
        val cycles = CycleCalculator.buildCycles(periods)
        val prediction = CycleCalculator.predict(cycles, today, context, assumptions)
        val phase = PhaseResolver.resolve(today, cycles, prediction, assumptions)

        return HomeSummary(
            phase = phase,
            prediction = prediction,
            status = if (phase == null) PhaseResolver.describeReadiness(cycles) else null,
            caveat = caveatFor(context),
            countdown = countdownFor(prediction),
            hasLoggedPeriod = periods.any { !it.isSpottingOnly },
        )
    }

    /**
     * The one place the caveat sentence is written, so the home card, the onboarding
     * picker and any future surface all say the same thing.
     */
    private fun caveatFor(context: CycleContext): String? = when (context) {
        CycleContext.NONE -> null
        else -> "${context.blurb} Treat the dates here as a rough guide, not a due date."
    }

    private fun countdownFor(prediction: CyclePrediction?): String? = when {
        prediction == null -> null
        // The engine reports a late period as a negative day count.
        prediction.daysUntilNextPeriod < 0 -> "About ${-prediction.daysUntilNextPeriod} days past your estimated start"
        prediction.daysUntilNextPeriod == 0L -> "Period expected today"
        prediction.daysUntilNextPeriod == 1L -> "Period expected tomorrow"
        else -> "Period expected in about ${prediction.daysUntilNextPeriod} days"
    }
}

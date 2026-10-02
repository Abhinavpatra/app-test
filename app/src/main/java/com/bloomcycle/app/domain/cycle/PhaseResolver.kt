package com.bloomcycle.app.domain.cycle

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.Cycle
import com.bloomcycle.app.domain.model.CyclePrediction
import com.bloomcycle.app.domain.model.PhaseState
import com.bloomcycle.app.domain.model.PhaseType
import java.time.temporal.ChronoUnit

/**
 * Resolves where a given date sits in the cycle: which phase, which day of that phase,
 * and which day of the cycle. Returns null when there is no history to resolve against —
 * the UI then offers onboarding rather than guessing a phase.
 */
object PhaseResolver {

    fun resolve(
        today: CycleDate,
        cycles: List<Cycle>,
        prediction: CyclePrediction?,
        assumptions: CycleCalculator.Assumptions = CycleCalculator.Assumptions(),
    ): PhaseState? {
        val current = containingCycle(today, cycles) ?: return null

        // The user stopped logging across this span. Claiming "follicular, day 91" inside a
        // three-month hole would be inventing data rather than reading it.
        if (current.lengthDays != null && current.lengthDays > CycleCalculator.GAP_THRESHOLD_DAYS) {
            return null
        }

        val avgLength = prediction?.averageCycleLength ?: assumptions.defaultCycleLength
        val avgDuration = prediction?.averagePeriodDuration ?: assumptions.defaultPeriodDuration
        val luteal = prediction?.lutealPhaseLength ?: CycleCalculator.lutealPhaseLength(cycles, assumptions)

        val cycleDay = ChronoUnit.DAYS.between(current.startDate, today).toInt() + 1
        val effectivePeriodEnd = current.periodEndDate
            ?: current.startDate.plusDays((avgDuration - 1).toLong())

        // --- Menstrual: day 1 through the last day of bleeding -----------------------------
        if (!today.isAfter(effectivePeriodEnd)) {
            val duration =
                if (current.isOngoing && current.periodEndDate == null) avgDuration
                else current.periodDurationDays ?: avgDuration
            return PhaseState(
                phase = PhaseType.MENSTRUAL,
                cycleDay = cycleDay,
                dayOfPhase = cycleDay,
                phaseDaysTotal = duration,
                cycle = current,
                prediction = prediction,
                today = today,
            )
        }

        // --- Everything after menstruation is measured backwards from the next start -------
        // Anchored to *this* cycle's successor, not to prediction.nextPeriodStart: that one
        // always points at the most recent cycle, so reusing it here would drop ovulation
        // into the wrong cycle whenever today sits in an earlier one.
        val nextStart = current.endDate?.plusDays(1)
            ?: (prediction?.nextPeriodStart ?: current.startDate.plusDays(avgLength.toLong()))
        val ovulation = nextStart.minusDays(luteal.toLong())
        val dayAfterBleeding = effectivePeriodEnd.plusDays(1)

        return when {
            today.isBefore(ovulation) -> phase(
                PhaseType.FOLLICULAR, dayAfterBleeding, today, current, prediction, today
            )

            !today.isAfter(ovulation.plusDays(1)) -> phase(
                PhaseType.OVULATORY, ovulation, today, current, prediction, today
            )

            else -> phase(
                PhaseType.LUTEAL, ovulation.plusDays(1), today, current, prediction, today
            )
        }
    }

    private fun phase(
        type: PhaseType,
        phaseStart: CycleDate,
        today: CycleDate,
        current: Cycle,
        prediction: CyclePrediction?,
        now: CycleDate,
    ): PhaseState = PhaseState(
        phase = type,
        cycleDay = ChronoUnit.DAYS.between(current.startDate, today).toInt() + 1,
        dayOfPhase = ChronoUnit.DAYS.between(phaseStart, today).toInt() + 1,
        phaseDaysTotal = null,
        cycle = current,
        prediction = prediction,
        today = now,
    )

    /** The most recent cycle whose span contains [today]. */
    fun containingCycle(today: CycleDate, cycles: List<Cycle>): Cycle? =
        cycles.lastOrNull { cycle ->
            !today.isBefore(cycle.startDate) &&
                (cycle.endDate == null || !today.isAfter(cycle.endDate))
        }

    /**
     * A soft read on regularity for the UI. Nothing here is a diagnosis — it is a
     * description of the logged data and nothing more.
     */
    fun describeReadiness(cycles: List<Cycle>): String = when {
        cycles.isEmpty() -> "No cycles logged yet"
        CycleCalculator.lengthsForStats(cycles).size < 3 -> "Still learning your rhythm"
        CycleCalculator.regularityDays(cycles) <= 2.0 -> "Your rhythm looks consistent"
        CycleCalculator.regularityDays(cycles) <= 4.0 -> "Your rhythm varies a little"
        else -> "Your rhythm is fairly unpredictable right now"
    }
}

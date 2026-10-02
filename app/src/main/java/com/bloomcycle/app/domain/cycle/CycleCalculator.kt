package com.bloomcycle.app.domain.cycle

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.Cycle
import com.bloomcycle.app.domain.model.CyclePrediction
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.PredictionConfidence
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The engine the whole product rests on. Pure Kotlin, no Android imports, deterministic —
 * so every edge case below is a unit test rather than a bug report.
 *
 * Terminology follows the plan:
 *   cycle        = first day of a period -> the day before the next period's first day
 *   period       = days of actual bleeding (typically 3-7)
 *   luteal phase = ovulation -> day before next period (~14 days, clinically)
 */
object CycleCalculator {

    const val DEFAULT_CYCLE_LENGTH = 28
    const val DEFAULT_PERIOD_DURATION = 5
    const val DEFAULT_LUTEAL_LENGTH = 14

    /** Only the most recent N cycles influence a prediction. */
    const val WINDOW_CYCLES = 6

    /**
     * A gap larger than this means the user simply stopped logging, not that they had a
     * two-month cycle. Such lengths are shown but never averaged in.
     */
    const val GAP_THRESHOLD_DAYS = 60

    /** Anything outside this range is not a believable cycle length. */
    private const val PLAUSIBLE_MIN = 15
    private const val PLAUSIBLE_MAX = 60

    private const val PLAUSIBLE_PERIOD_MIN = 1
    private const val PLAUSIBLE_PERIOD_MAX = 14

    data class Assumptions(
        val defaultCycleLength: Int = DEFAULT_CYCLE_LENGTH,
        val defaultPeriodDuration: Int = DEFAULT_PERIOD_DURATION,
        val defaultLutealLength: Int = DEFAULT_LUTEAL_LENGTH,
    )

    // ---------------------------------------------------------------------------------------
    // Cycle construction
    // ---------------------------------------------------------------------------------------

    /**
     * Turns a flat list of logged period starts into ordered cycles.
     *
     * Handles: two events logged on the same day (merged), spotting-only events
     * (dropped entirely — they are not cycles), and unlogged next periods (the most
     * recent cycle is `isOngoing` with unknown length and duration).
     */
    fun buildCycles(events: List<PeriodEvent>): List<Cycle> {
        val starts = events
            .filterNot { it.isSpottingOnly }
            .sortedBy { it.startDate }
            .groupBy { it.startDate }
            .toSortedMap()
            .values
            .map { sameDay ->
                // Same-day duplicates: prefer the one that has an end date recorded.
                sameDay.minByOrNull { if (it.endDate == null) 1 else 0 }!!
            }

        if (starts.isEmpty()) return emptyList()

        val lengths = starts.zipWithNext { a, b ->
            ChronoUnit.DAYS.between(a.startDate, b.startDate).toInt()
        }

        return starts.mapIndexed { i, event ->
            val length = lengths.getOrNull(i)
            val nextStart = starts.getOrNull(i + 1)?.startDate
            val hasGapBefore = if (i == 0) false else lengths[i - 1] > GAP_THRESHOLD_DAYS

            val endDate = nextStart?.minusDays(1)
            val duration = event.endDate?.let { ChronoUnit.DAYS.between(event.startDate, it).toInt() + 1 }
            // The most recent cycle is always the open one: we are waiting for the next
            // period whether or not bleeding from this one has finished.
            val isOngoing = nextStart == null

            Cycle(
                index = i,
                startDate = event.startDate,
                endDate = endDate,
                periodStartDate = event.startDate,
                periodEndDate = event.endDate,
                isOngoing = isOngoing,
                lengthDays = length,
                periodDurationDays = duration?.takeIf { it in PLAUSIBLE_PERIOD_MIN..PLAUSIBLE_PERIOD_MAX },
                hasGapBefore = hasGapBefore,
            )
        }
    }

    // ---------------------------------------------------------------------------------------
    // Statistics
    // ---------------------------------------------------------------------------------------

    /** Most recent plausible cycle lengths, oldest first. */
    fun lengthsForStats(cycles: List<Cycle>): List<Int> =
        cycles.mapNotNull { it.lengthDays }
            .filter { it in PLAUSIBLE_MIN..PLAUSIBLE_MAX }
            .takeLast(WINDOW_CYCLES)

    fun averageCycleLength(cycles: List<Cycle>, assumptions: Assumptions = Assumptions()): Int {
        val values = lengthsForStats(cycles)
        if (values.isEmpty()) return assumptions.defaultCycleLength
        return robustMean(values).roundToInt().coerceIn(PLAUSIBLE_MIN, PLAUSIBLE_MAX)
    }

    fun averagePeriodDuration(cycles: List<Cycle>, assumptions: Assumptions = Assumptions()): Int {
        val values = cycles.mapNotNull { it.periodDurationDays }
            .filter { it in PLAUSIBLE_PERIOD_MIN..PLAUSIBLE_PERIOD_MAX }
            .takeLast(WINDOW_CYCLES)
        if (values.isEmpty()) return assumptions.defaultPeriodDuration
        return robustMean(values).roundToInt().coerceIn(PLAUSIBLE_PERIOD_MIN, PLAUSIBLE_PERIOD_MAX)
    }

    /** Population standard deviation of recent cycle lengths, in days. */
    fun regularityDays(cycles: List<Cycle>): Double = regularityOf(lengthsForStats(cycles))

    fun regularityOf(values: List<Int>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance)
    }

    fun regularityLabel(cycles: List<Cycle>): String = when {
        lengthsForStats(cycles).size < 3 -> "Not enough history yet"
        regularityDays(cycles) <= 2.0 -> "Very consistent"
        regularityDays(cycles) <= 4.0 -> "Varies a little"
        else -> "Irregular"
    }

    /**
     * The luteal phase cannot be observed without ovulation testing, so it is an assumption —
     * but a clamped one: a 21-day cycle cannot have a 14-day luteal phase, or ovulation would
     * fall before menstruation ends.
     */
    fun lutealPhaseLength(cycles: List<Cycle>, assumptions: Assumptions = Assumptions()): Int {
        val median = median(lengthsForStats(cycles).takeIf { it.isNotEmpty() } ?: return assumptions.defaultLutealLength)
        val ceiling = (median.toLong() - 10).toInt().coerceAtLeast(7)
        return assumptions.defaultLutealLength.coerceIn(7, ceiling)
    }

    fun confidence(cycles: List<Cycle>): PredictionConfidence {
        val values = lengthsForStats(cycles)
        return confidence(values.size, regularityOf(values))
    }

    fun confidence(cycleCount: Int, regularity: Double): PredictionConfidence {
        if (cycleCount < 3) return PredictionConfidence.LOW
        // Wide variance downgrades the verdict regardless of how much history exists.
        if (regularity > 7.0) return if (cycleCount >= 6) PredictionConfidence.MEDIUM else PredictionConfidence.LOW
        if (cycleCount < 6) return if (regularity <= 3.0) PredictionConfidence.MEDIUM else PredictionConfidence.LOW
        return if (regularity <= 3.0) PredictionConfidence.HIGH else PredictionConfidence.MEDIUM
    }

    // ---------------------------------------------------------------------------------------
    // Prediction
    // ---------------------------------------------------------------------------------------

    /**
     * Returns null when there is nothing logged yet to predict from — the UI shows onboarding
     * instead of a fabricated prediction.
     */
    fun predict(
        cycles: List<Cycle>,
        today: CycleDate,
        assumptions: Assumptions = Assumptions(),
    ): CyclePrediction? {
        val anchor = cycles.lastOrNull() ?: return null
        val lengths = lengthsForStats(cycles)
        val avgLength = averageCycleLength(cycles, assumptions)
        val avgDuration = averagePeriodDuration(cycles, assumptions)
        val regularity = regularityOf(lengths)
        val luteal = lutealPhaseLength(cycles, assumptions)

        // A period that has not arrived yet is reported as *late*, with a negative day count.
        // Quietly rolling the projection forward would hide exactly the signal the user is
        // opening the app to see.
        val nextStart = anchor.startDate.plusDays(avgLength.toLong())
        val daysUntil = ChronoUnit.DAYS.between(today, nextStart).toInt()

        val spread = regularity.roundToInt().coerceIn(1, 7)
        val ovulation = nextStart.minusDays(luteal.toLong())

        return CyclePrediction(
            nextPeriodStart = nextStart,
            earliestStart = nextStart.minusDays(spread.toLong()),
            latestStart = nextStart.plusDays(spread.toLong()),
            ovulationDay = ovulation,
            fertileWindowStart = ovulation.minusDays(5),
            fertileWindowEnd = ovulation.plusDays(1),
            pmsWindowStart = nextStart.minusDays(6),
            pmsWindowEnd = nextStart.minusDays(1),
            confidence = confidence(cycles),
            averageCycleLength = avgLength,
            averagePeriodDuration = avgDuration,
            lutealPhaseLength = luteal,
            cyclesUsed = lengths.size,
            regularityDays = regularity,
            regularityLabel = regularityLabel(cycles),
            isLate = daysUntil < 0,
            daysUntilNextPeriod = daysUntil.toLong(),
        )
    }

    // ---------------------------------------------------------------------------------------
    // Statistics helpers
    // ---------------------------------------------------------------------------------------

    internal fun median(values: List<Int>): Double = medianOf(values.map { it.toDouble() })

    private fun medianOf(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    /**
     * A plain mean drags hard on one stray 45-day cycle among five 28s. A robust mean
     * drops only values more than 3 median-absolute-deviations away, so a genuinely
     * consistent 40-day pattern stays a 40-day pattern instead of being "corrected" to 28.
     */
    internal fun robustMean(values: List<Int>): Double {
        if (values.size < 5) return values.average()
        val med = medianOf(values.map { it.toDouble() })
        val mad = medianOf(values.map { abs(it - med) })
        if (mad <= 0.0) return values.average()
        val kept = values.filter { abs(it - med) <= 3 * mad }
        return kept.ifEmpty { values }.average()
    }
}

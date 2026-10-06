package com.bloomcycle.app.domain.cycle

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.Cycle
import com.bloomcycle.app.domain.model.CycleContext
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

    /**
     * The band most people and most guidance describe as a typical adult cycle.
     * Longer and shorter cycles are perfectly real and are *computed* — this band only
     * decides how gently the wording needs to be pitched. See [outsideTypicalRange].
     */
    const val TYPICAL_MIN = 21
    const val TYPICAL_MAX = 35

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

    /**
     * True when the user's own average sits outside 21-35 days.
     *
     * This is deliberately *not* a health judgement and does not change the numbers —
     * a steady 40-day pattern is still averaged as 40 days. It only tells the UI to
     * soften its language, because "very consistent" attached to a 40-day average reads
     * as reassurance that nothing here has actually been established.
     */
    fun outsideTypicalRange(cycles: List<Cycle>): Boolean = outsideTypical(lengthsForStats(cycles))

    // Named rather than overloaded: List<Cycle> and List<Int> erase to the same JVM
    // signature, and two functions there are a platform declaration clash (the same trap
    // that forced median -> medianOf).
    private fun outsideTypical(lengths: List<Int>): Boolean {
        if (lengths.isEmpty()) return false
        val mean = robustMean(lengths)
        return mean < TYPICAL_MIN || mean > TYPICAL_MAX
    }

    /**
     * `"around 40 days, which is longer than typical"`, or null when the average sits
     * inside the typical band. Shared by the regularity label and the readiness read so
     * the same history never gets described two different ways.
     */
    fun typicalityNote(lengths: List<Int>): String? {
        if (!outsideTypical(lengths)) return null
        val mean = robustMean(lengths).roundToInt()
        val direction = if (mean > TYPICAL_MAX) "longer than typical" else "shorter than typical"
        return "around $mean days, which is $direction"
    }

    /**
     * A plain consistency verdict, softened with an honest note about the actual length
     * when that length falls outside the typical band (plan.md §8.4).
     */
    fun regularityLabel(cycles: List<Cycle>): String {
        val lengths = lengthsForStats(cycles)
        if (lengths.size < 3) return "Not enough history yet"

        val base = when {
            regularityOf(lengths) <= 2.0 -> "Very consistent"
            regularityOf(lengths) <= 4.0 -> "Varies a little"
            else -> "Varies a lot right now"
        }

        val note = typicalityNote(lengths) ?: return base
        return "$base — $note"
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
     *
     * Also returns null for [CycleContext.HORMONAL_CONTRACEPTION], because ovulation is
     * suppressed and there is no cycle to predict: showing a date there would be fiction
     * presented as data. Perimenopause and postpartum still predict, but caveated and
     * pinned to LOW confidence (plan.md §8.4).
     */
    fun predict(
        cycles: List<Cycle>,
        today: CycleDate,
        context: CycleContext = CycleContext.NONE,
        assumptions: Assumptions = Assumptions(),
    ): CyclePrediction? {
        if (context == CycleContext.HORMONAL_CONTRACEPTION) return null

        val anchor = cycles.lastOrNull() ?: return null
        val lengths = lengthsForStats(cycles)
        val avgLength = averageCycleLength(cycles, assumptions)
        val avgDuration = averagePeriodDuration(cycles, assumptions)
        val regularity = regularityOf(lengths)
        val luteal = lutealPhaseLength(cycles, assumptions)
        val caveated = context != CycleContext.NONE

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
            // A caveated context caps confidence regardless of how tidy the history looks:
            // a regular-looking streak of withdrawal bleeds is not evidence of ovulation.
            confidence = if (caveated) PredictionConfidence.LOW else confidence(cycles),
            averageCycleLength = avgLength,
            averagePeriodDuration = avgDuration,
            lutealPhaseLength = luteal,
            cyclesUsed = lengths.size,
            regularityDays = regularity,
            regularityLabel = regularityLabel(cycles),
            isLate = daysUntil < 0,
            daysUntilNextPeriod = daysUntil.toLong(),
            context = context,
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

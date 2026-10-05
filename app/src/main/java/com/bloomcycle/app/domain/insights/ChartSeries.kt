package com.bloomcycle.app.domain.insights

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.cycle.PhaseResolver
import com.bloomcycle.app.domain.model.Cycle
import com.bloomcycle.app.domain.model.CyclePrediction
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.PhaseType
import com.bloomcycle.app.domain.model.SymptomLog
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Series the charts draw, computed here rather than in the Canvas.
 *
 * Charts are the one place a divide-by-zero or a min==max range would crash the screen, so
 * every series is defined for 0, 1 and 24 cycles and never returns a range the chart cannot
 * map onto pixels. Each of those cases is a unit test.
 */

data class CycleLengthPoint(val cycle: Int, val length: Int)

data class CycleLengthSeries(
    val points: List<CycleLengthPoint>,
    /** Average the engine expects next, null when there is nothing to predict from. */
    val predicted: Int?,
    /** [predicted] minus/plus the regularity spread — the honest band, not a hard date. */
    val bandLow: Int?,
    val bandHigh: Int?,
) {
    /** y-axis bounds, guaranteed strictly increasing so plotting cannot divide by zero. */
    val lowest: Int get() = (points.minOfOrNull { it.length } ?: 0) - 2
    val highest: Int get() = (points.maxOfOrNull { it.length } ?: 1) + 2
}

fun cycleLengthSeries(
    cycles: List<Cycle>,
    prediction: CyclePrediction?,
): CycleLengthSeries {
    val points = CycleCalculator.lengthsForStats(cycles)
        .mapIndexed { index, length -> CycleLengthPoint(index + 1, length) }
    if (prediction == null) return CycleLengthSeries(points, null, null, null)

    val spread = prediction.regularityDays.roundToInt().coerceIn(1, 7)
    return CycleLengthSeries(
        points = points,
        predicted = prediction.averageCycleLength,
        bandLow = prediction.averageCycleLength - spread,
        bandHigh = prediction.averageCycleLength + spread,
    )
}

/** Period durations in days, oldest first — gaps where a duration was never logged. */
fun periodDurationSeries(cycles: List<Cycle>): List<Int> = cycles.mapNotNull { it.periodDurationDays }

data class Heatmap(
    /** Symptom ids, most frequent first. */
    val rows: List<String>,
    /** Widest cycle day any symptom landed on, at least 1 so columns always exist. */
    val cycleDays: Int,
    val counts: Map<Pair<String, Int>, Int>,
    /** Largest cell, for scaling opacity; 0 when nothing is logged. */
    val peak: Int,
    val totalLogged: Int,
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}

/**
 * Symptom frequency by cycle day — README's line 2.
 *
 * A symptom counts against the cycle day of whichever cycle contained it, and is dropped
 * when it falls outside every cycle: there is no cycle day to place it on.
 */
fun symptomHeatmap(symptoms: List<SymptomLog>, cycles: List<Cycle>): Heatmap {
    val counts = symptoms.mapNotNull { log ->
        val cycle = PhaseResolver.containingCycle(log.date, cycles) ?: return@mapNotNull null
        val day = ChronoUnit.DAYS.between(cycle.startDate, log.date).toInt() + 1
        if (day < 1) null else log.symptomId to day
    }.groupingBy { it }.eachCount()

    if (counts.isEmpty()) return Heatmap(emptyList(), 1, emptyMap(), 0, 0)

    val totals = counts.entries
        .groupBy({ it.key.first }, { it.value })
        .mapValues { (_, cells) -> cells.sum() }

    val rows = totals.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }

    return Heatmap(
        rows = rows,
        cycleDays = counts.keys.maxOf { it.second },
        counts = counts,
        peak = counts.values.max(),
        totalLogged = counts.values.sum(),
    )
}

data class WheelSegment(val phase: PhaseType, val days: Int, val share: Float)

/**
 * The current cycle as a ring: menstrual, follicular, ovulatory, luteal. The split is the
 * same arithmetic PhaseResolver uses, so the wheel cannot disagree with the day's phase.
 * Empty when there is no cycle to draw yet.
 */
fun phaseWheel(
    cycles: List<Cycle>,
    prediction: CyclePrediction?,
): List<WheelSegment> {
    if (cycles.isEmpty()) return emptyList()

    val total = prediction?.averageCycleLength
        ?: CycleCalculator.averageCycleLength(cycles)
    val menstrual = prediction?.averagePeriodDuration
        ?: CycleCalculator.averagePeriodDuration(cycles)
    val luteal = prediction?.lutealPhaseLength
        ?: CycleCalculator.lutealPhaseLength(cycles)
    val ovulatory = 2
    val follicular = (total - menstrual - ovulatory - luteal).coerceAtLeast(1)

    val days = listOf(
        PhaseType.MENSTRUAL to menstrual,
        PhaseType.FOLLICULAR to follicular,
        PhaseType.OVULATORY to ovulatory,
        PhaseType.LUTEAL to luteal,
    )
    val sum = days.sumOf { it.second }.toFloat()
    if (sum <= 0f) return emptyList()

    return days.map { (phase, count) -> WheelSegment(phase, count, count / sum) }
}

/**
 * One number for "how regular are these", 0–100, from the same spread the engine reports.
 * Below three cycles there is no honest score yet, so it is 0 with the label saying why.
 */
fun consistencyScore(regularityDays: Double, sampleSize: Int): Int {
    if (sampleSize < 3) return 0
    return ((10.0 - regularityDays) / 10.0 * 100.0).roundToInt().coerceIn(0, 100)
}

/** First half vs second half of the recent lengths: a plain drift read, nothing more. */
fun trendOf(lengths: List<Int>): Trend {
    if (lengths.size < 4) return Trend.UNKNOWN
    val split = lengths.size / 2
    val first = lengths.take(split).average()
    val second = lengths.drop(split).average()
    val delta = second - first
    return when {
        abs(delta) < 2.0 -> Trend.STEADY
        delta > 0 -> Trend.LONGER
        else -> Trend.SHORTER
    }
}

fun trendDelta(lengths: List<Int>): Int {
    if (lengths.size < 4) return 0
    val split = lengths.size / 2
    return (lengths.drop(split).average() - lengths.take(split).average()).roundToInt()
}

/** Kept next to the series so the trend's inputs and outputs are tested together. */
fun usableLengths(periods: List<PeriodEvent>): List<Int> =
    CycleCalculator.lengthsForStats(CycleCalculator.buildCycles(periods))

/** Today-independent helper: where does this date sit in the cycle, in days? */
fun cycleDayOf(date: CycleDate, cycles: List<Cycle>): Int? {
    val cycle = PhaseResolver.containingCycle(date, cycles) ?: return null
    val day = ChronoUnit.DAYS.between(cycle.startDate, date).toInt() + 1
    return day.takeIf { it >= 1 }
}

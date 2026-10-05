package com.bloomcycle.app.domain.insights

import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.cycle.PhaseResolver
import com.bloomcycle.app.domain.model.CycleContext
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.SymptomCatalog
import com.bloomcycle.app.domain.model.SymptomLog
import kotlin.math.roundToInt

data class SymptomFrequency(val symptomId: String, val label: String, val count: Int)

enum class Trend(val label: String) {
    LONGER("Drifting longer"),
    SHORTER("Drifting shorter"),
    STEADY("Holding steady"),
    UNKNOWN("Not enough history to read a trend"),
}

/**
 * The plain-language read behind the charts. Every number here is also drawn somewhere on
 * the Insights screen, and [summary] is the text alternative that screen shows — one
 * definition, so the paragraph and the picture can never tell different stories.
 */
data class InsightsReport(
    /** Non-null exactly when there is not enough logged to say anything yet. */
    val status: String?,
    val cycleCount: Int,
    val averageLength: Int?,
    val shortest: Int?,
    val longest: Int?,
    val regularityLabel: String,
    /** 0-100, 0 while there are fewer than three cycles to score. */
    val consistencyScore: Int,
    val trend: Trend,
    val trendDays: Int,
    val topSymptoms: List<SymptomFrequency>,
    val summary: String,
) {
    val hasHistory: Boolean get() = status == null
}

object InsightsAnalysis {

    fun analyze(
        periods: List<PeriodEvent>,
        symptoms: List<SymptomLog> = emptyList(),
        context: CycleContext = CycleContext.NONE,
        assumptions: CycleCalculator.Assumptions = CycleCalculator.Assumptions(),
    ): InsightsReport {
        val cycles = CycleCalculator.buildCycles(periods)
        val lengths = CycleCalculator.lengthsForStats(cycles)

        if (lengths.isEmpty()) {
            // Predictions are meaningless without history, but the reason still is: the UI
            // shows this sentence instead of an empty chart pretending to be a reading.
            return InsightsReport(
                status = PhaseResolver.describeReadiness(cycles),
                cycleCount = 0,
                averageLength = null,
                shortest = null,
                longest = null,
                regularityLabel = "Not enough history yet",
                consistencyScore = 0,
                trend = Trend.UNKNOWN,
                trendDays = 0,
                topSymptoms = emptyList(),
                summary = "",
            )
        }

        val average = lengths.average().roundToInt()
        val regularity = CycleCalculator.regularityOf(lengths)
        val trend = trendOf(lengths)

        return InsightsReport(
            status = null,
            cycleCount = lengths.size,
            averageLength = average,
            shortest = lengths.min(),
            longest = lengths.max(),
            regularityLabel = CycleCalculator.regularityLabel(cycles),
            consistencyScore = consistencyScore(regularity, lengths.size),
            trend = trend,
            trendDays = trendDelta(lengths),
            topSymptoms = topSymptoms(symptoms),
            summary = summaryOf(lengths, average, trend, topSymptoms(symptoms)),
        )
    }

    private fun topSymptoms(symptoms: List<SymptomLog>): List<SymptomFrequency> =
        symptoms.groupBy { it.symptomId }
            .map { (id, logs) -> SymptomFrequency(id, SymptomCatalog.label(id), logs.size) }
            .sortedWith(compareByDescending<SymptomFrequency> { it.count }.thenBy { it.label })
            .take(5)

    private fun summaryOf(
        lengths: List<Int>,
        average: Int,
        trend: Trend,
        top: List<SymptomFrequency>,
    ): String = buildString {
        append("Your last ")
        append(lengths.size)
        append(if (lengths.size == 1) " cycle" else " cycles")
        append(" averaged ")
        append(average)
        append(if (average == 1) " day, running from " else " days, running from ")
        append(lengths.min())
        append(" to ")
        append(lengths.max())
        append(" days.")
        when (trend) {
            Trend.LONGER -> append(" They have been drifting longer.")
            Trend.SHORTER -> append(" They have been drifting shorter.")
            Trend.STEADY -> append(" They look steady.")
            Trend.UNKNOWN -> Unit
        }
        top.firstOrNull()?.let {
            append(" Most often logged: ")
            append(it.label)
            append(", ")
            append(it.count)
            append(if (it.count == 1) " time." else " times.")
        }
    }
}

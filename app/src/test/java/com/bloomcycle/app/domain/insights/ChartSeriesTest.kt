package com.bloomcycle.app.domain.insights

import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.PhaseType
import com.bloomcycle.app.domain.model.SymptomLog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartSeriesTest {

    private fun event(start: String) = PeriodEvent(startDate = LocalDate.parse(start))

    private fun runOf(count: Int, start: String, stepDays: Long = 28): List<PeriodEvent> {
        val first = LocalDate.parse(start)
        return (0 until count).map { event(first.plusDays(stepDays * it).toString()) }
    }

    // --- Cycle length line ----------------------------------------------------------------

    @Test
    fun `no cycles gives an empty series with a range the chart can still map to pixels`() {
        val series = cycleLengthSeries(emptyList(), prediction = null)

        assertTrue(series.points.isEmpty())
        assertTrue(series.predicted == null)
        assertTrue(series.lowest < series.highest)
    }

    @Test
    fun `one cycle has no length to plot yet but still has a drawable range`() {
        val series = cycleLengthSeries(CycleCalculator.buildCycles(runOf(1, "2026-01-01")), null)

        assertTrue(series.points.isEmpty())
        assertTrue(series.lowest < series.highest)
    }

    @Test
    fun `twenty four cycles plot as the six most recent, never divided by zero`() {
        val cycles = CycleCalculator.buildCycles(runOf(25, "2026-01-01"))

        assertEquals(25, cycles.size)
        assertEquals(24, cycles.count { it.lengthDays != null })
        val series = cycleLengthSeries(cycles, prediction = null)

        assertEquals(CycleCalculator.WINDOW_CYCLES, series.points.size)
        assertTrue(series.points.all { it.length == 28 })
        assertTrue(series.points.first().cycle < series.points.last().cycle)
    }

    @Test
    fun `a single cycle length becomes a valid range instead of a flat line`() {
        val cycles = CycleCalculator.buildCycles(runOf(2, "2026-01-01"))
        val series = cycleLengthSeries(cycles, prediction = null)

        assertEquals(1, series.points.size)
        assertEquals(28, series.points.single().length)
        assertTrue(series.lowest < series.highest)
    }

    @Test
    fun `the band is the average widened by the regularity spread`() {
        val cycles = CycleCalculator.buildCycles(runOf(3, "2026-01-01"))
        val prediction = CycleCalculator.predict(cycles, LocalDate.parse("2026-02-20"))

        val series = cycleLengthSeries(cycles, prediction)

        assertEquals(28, series.predicted)
        assertEquals(27, series.bandLow)
        assertEquals(29, series.bandHigh)
    }

    // --- Period duration bars -------------------------------------------------------------

    @Test
    fun `durations only include cycles where a duration was actually logged`() {
        val withEnds = listOf(
            PeriodEvent(startDate = LocalDate.parse("2026-01-01"), endDate = LocalDate.parse("2026-01-05")),
            PeriodEvent(startDate = LocalDate.parse("2026-01-29"), endDate = LocalDate.parse("2026-02-03")),
        )
        val open = listOf(event("2026-01-01"))

        assertEquals(listOf(5, 6), periodDurationSeries(CycleCalculator.buildCycles(withEnds)))
        assertTrue(periodDurationSeries(CycleCalculator.buildCycles(open)).isEmpty())
        assertTrue(periodDurationSeries(emptyList()).isEmpty())
    }

    // --- Symptom heatmap ------------------------------------------------------------------

    @Test
    fun `an empty log gives an empty heatmap with columns that still exist`() {
        val heatmap = symptomHeatmap(emptyList(), emptyList())

        assertTrue(heatmap.isEmpty)
        assertEquals(0, heatmap.peak)
        assertEquals(0, heatmap.totalLogged)
        assertTrue(heatmap.cycleDays >= 1)
    }

    @Test
    fun `symptoms land on their cycle day and rank by frequency`() {
        val cycles = CycleCalculator.buildCycles(runOf(2, "2026-01-01"))
        val symptoms = listOf(
            SymptomLog(date = LocalDate.parse("2026-01-03"), symptomId = "cramps"),
            SymptomLog(date = LocalDate.parse("2026-01-05"), symptomId = "cramps"),
            SymptomLog(date = LocalDate.parse("2026-01-04"), symptomId = "low_mood"),
            // Before the first cycle: there is no cycle day to place it on.
            SymptomLog(date = LocalDate.parse("2025-12-31"), symptomId = "cramps"),
        )

        val heatmap = symptomHeatmap(symptoms, cycles)

        assertEquals(listOf("cramps", "low_mood"), heatmap.rows)
        assertEquals(1, heatmap.counts["cramps" to 3])
        assertEquals(1, heatmap.counts["cramps" to 5])
        assertEquals(1, heatmap.counts["low_mood" to 4])
        assertEquals(1, heatmap.peak)
        assertEquals(3, heatmap.totalLogged)
        assertTrue(heatmap.cycleDays in 1..28)
    }

    @Test
    fun `repeated symptoms on one day scale the peak`() {
        val cycles = CycleCalculator.buildCycles(runOf(2, "2026-01-01"))
        val symptoms = listOf(
            SymptomLog(date = LocalDate.parse("2026-01-03"), symptomId = "cramps"),
            SymptomLog(date = LocalDate.parse("2026-01-03"), symptomId = "cramps", severity = 4),
            SymptomLog(date = LocalDate.parse("2026-01-03"), symptomId = "irritable"),
        )

        val heatmap = symptomHeatmap(symptoms, cycles)

        assertEquals(2, heatmap.peak)
        assertEquals(2, heatmap.counts["cramps" to 3])
    }

    // --- Phase wheel ----------------------------------------------------------------------

    @Test
    fun `no cycles means no wheel`() {
        assertTrue(phaseWheel(emptyList(), prediction = null).isEmpty())
    }

    @Test
    fun `the wheel splits a cycle into four phases that add up to the whole`() {
        val cycles = CycleCalculator.buildCycles(runOf(3, "2026-01-01"))
        val prediction = CycleCalculator.predict(cycles, LocalDate.parse("2026-02-20"))

        val segments = phaseWheel(cycles, prediction)

        assertEquals(
            listOf(PhaseType.MENSTRUAL, PhaseType.FOLLICULAR, PhaseType.OVULATORY, PhaseType.LUTEAL),
            segments.map { it.phase },
        )
        assertEquals(28, segments.sumOf { it.days })
        assertEquals(1f, segments.sumOf { it.share.toDouble() }.toFloat(), 0.01f)
        assertTrue(segments.all { it.days > 0 })
    }

    // --- Scores ---------------------------------------------------------------------------

    @Test
    fun `consistency runs from a perfect spread down to zero`() {
        assertEquals(100, consistencyScore(regularityDays = 0.0, sampleSize = 6))
        assertEquals(50, consistencyScore(regularityDays = 5.0, sampleSize = 6))
        assertEquals(0, consistencyScore(regularityDays = 12.0, sampleSize = 6))
        // Fewer than three cycles: no honest score yet.
        assertEquals(0, consistencyScore(regularityDays = 0.0, sampleSize = 2))
    }

    @Test
    fun `the trend needs four cycles before it claims anything`() {
        assertEquals(Trend.UNKNOWN, trendOf(listOf(28, 28, 28)))
        assertEquals(Trend.STEADY, trendOf(listOf(28, 28, 28, 29)))
        assertEquals(Trend.LONGER, trendOf(listOf(26, 26, 30, 30)))
        assertEquals(Trend.SHORTER, trendOf(listOf(30, 30, 26, 26)))
        assertEquals(4, trendDelta(listOf(26, 26, 30, 30)))
    }
}

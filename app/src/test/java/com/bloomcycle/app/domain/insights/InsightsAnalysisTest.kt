package com.bloomcycle.app.domain.insights

import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.SymptomLog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsAnalysisTest {

    private fun event(start: String, end: String? = null) = PeriodEvent(
        startDate = LocalDate.parse(start),
        endDate = end?.let(LocalDate::parse),
    )

    /** Four cycles measuring 26, 28, 30 and 28 days. */
    private val fourCycles = listOf(
        event("2026-01-01", "2026-01-05"),
        event("2026-01-27", "2026-01-31"),
        event("2026-02-24", "2026-02-28"),
        event("2026-03-26", "2026-03-30"),
        event("2026-04-23", "2026-04-27"),
    )

    @Test
    fun `nothing logged means a reason rather than a report`() {
        val report = InsightsAnalysis.analyze(emptyList())

        assertFalse(report.hasHistory)
        assertEquals(0, report.cycleCount)
        assertEquals(0, report.consistencyScore)
        assertEquals(Trend.UNKNOWN, report.trend)
        assertTrue(report.summary.isEmpty())
        assertNull(report.averageLength)
        assertTrue(report.status != null && report.status.isNotEmpty())
    }

    @Test
    fun `four cycles describe their own average range and drift`() {
        val report = InsightsAnalysis.analyze(fourCycles)

        assertTrue(report.hasHistory)
        assertEquals(4, report.cycleCount)
        assertEquals(28, report.averageLength)
        assertEquals(26, report.shortest)
        assertEquals(30, report.longest)
        assertEquals(Trend.LONGER, report.trend)
        assertEquals(2, report.trendDays)
        assertTrue(report.consistencyScore in 1..100)
        assertEquals("Very consistent", report.regularityLabel)
    }

    @Test
    fun `the summary is a full sentence carrying the same numbers as the report`() {
        val summary = InsightsAnalysis.analyze(fourCycles).summary

        assertTrue(summary.contains("averaged 28 days"))
        assertTrue(summary.contains("from 26 to 30 days"))
        assertTrue(summary.contains("drifting longer"))
    }

    @Test
    fun `symptoms are ranked by frequency and capped at five`() {
        val symptoms = listOf(
            SymptomLog(date = LocalDate.parse("2026-01-03"), symptomId = "cramps"),
            SymptomLog(date = LocalDate.parse("2026-01-04"), symptomId = "cramps"),
            SymptomLog(date = LocalDate.parse("2026-02-26"), symptomId = "cramps"),
            SymptomLog(date = LocalDate.parse("2026-01-28"), symptomId = "low_mood"),
            SymptomLog(date = LocalDate.parse("2026-02-25"), symptomId = "low_mood"),
            SymptomLog(date = LocalDate.parse("2026-03-27"), symptomId = "headache"),
            SymptomLog(date = LocalDate.parse("2026-04-24"), symptomId = "irritable"),
            SymptomLog(date = LocalDate.parse("2026-04-25"), symptomId = "anxious"),
            SymptomLog(date = LocalDate.parse("2026-04-26"), symptomId = "bloating"),
            SymptomLog(date = LocalDate.parse("2026-04-27"), symptomId = "calm"),
            SymptomLog(date = LocalDate.parse("2026-04-28"), symptomId = "insomnia"),
        )

        val report = InsightsAnalysis.analyze(fourCycles, symptoms)

        assertEquals(5, report.topSymptoms.size)
        assertEquals("cramps", report.topSymptoms[0].symptomId)
        assertEquals(3, report.topSymptoms[0].count)
        assertEquals("Cramps", report.topSymptoms[0].label)
        assertEquals("low_mood", report.topSymptoms[1].symptomId)
        assertTrue(report.summary.contains("Most often logged: Cramps, 3 times"))
    }

    @Test
    fun `one logged cycle reports itself without claiming a trend`() {
        val oneCycle = listOf(event("2026-01-01", "2026-01-06"), event("2026-01-29"))

        val report = InsightsAnalysis.analyze(oneCycle)

        assertTrue(report.hasHistory)
        assertEquals(1, report.cycleCount)
        assertEquals(Trend.UNKNOWN, report.trend)
        assertEquals(0, report.consistencyScore)
        assertEquals(0, report.topSymptoms.size)
    }
}

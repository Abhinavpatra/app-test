package com.bloomcycle.app.domain.cycle

import com.bloomcycle.app.domain.model.CycleContext
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.PredictionConfidence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleCalculatorTest {

    private fun event(start: String, end: String? = null, spotting: Boolean = false) = PeriodEvent(
        startDate = LocalDate.parse(start),
        endDate = end?.let(LocalDate::parse),
        isSpottingOnly = spotting,
    )

    /** [starts] are ISO dates, each the beginning of a logged period. */
    private fun eventsAt(vararg starts: String) = starts.map { event(it) }

    private fun regularCycles(count: Int, start: String = "2026-01-01", length: Int = 28): List<PeriodEvent> {
        var date = LocalDate.parse(start)
        return (0..count).map {
            val e = event(date.toString())
            date = date.plusDays(length.toLong())
            e
        }
    }

    // ---------------------------------------------------------------------------------------
    // No history
    // ---------------------------------------------------------------------------------------

    @Test
    fun `no logged cycles falls back to documented defaults`() {
        val cycles = CycleCalculator.buildCycles(emptyList())
        assertTrue(cycles.isEmpty())
        assertEquals(28, CycleCalculator.averageCycleLength(cycles))
        assertEquals(5, CycleCalculator.averagePeriodDuration(cycles))
    }

    @Test
    fun `prediction is null rather than fabricated when there is no history`() {
        assertNull(CycleCalculator.predict(CycleCalculator.buildCycles(emptyList()), LocalDate.parse("2026-01-01")))
    }

    @Test
    fun `a single logged cycle produces low confidence`() {
        val cycles = CycleCalculator.buildCycles(regularCycles(1))
        val prediction = assertNotNull(CycleCalculator.predict(cycles, LocalDate.parse("2026-01-05")))
        assertEquals(28, prediction.averageCycleLength)
        assertEquals(PredictionConfidence.LOW, prediction.confidence)
        assertEquals(1, prediction.cyclesUsed)
    }

    // ---------------------------------------------------------------------------------------
    // Cycle construction
    // ---------------------------------------------------------------------------------------

    @Test
    fun `consecutive periods produce a cycle of the correct length`() {
        val cycles = CycleCalculator.buildCycles(eventsAt("2026-01-01", "2026-01-29"))
        assertEquals(2, cycles.size)
        assertEquals(28, cycles[0].lengthDays)
        assertNull(cycles[1].lengthDays)
        assertEquals(LocalDate.parse("2026-01-28"), cycles[0].endDate)
    }

    @Test
    fun `spotting alone does not start a cycle`() {
        val cycles = CycleCalculator.buildCycles(
            listOf(
                event("2026-01-01", "2026-01-05"),
                event("2026-01-10", spotting = true),
                event("2026-01-29", "2026-02-02"),
            )
        )
        assertEquals(2, cycles.size)
        assertEquals(28, CycleCalculator.averageCycleLength(cycles))
    }

    @Test
    fun `two events logged on the same day are merged`() {
        val cycles = CycleCalculator.buildCycles(
            listOf(
                event("2026-01-01"),
                event("2026-01-01", "2026-01-05"),
                event("2026-01-29", "2026-02-02"),
            )
        )
        assertEquals(2, cycles.size)
        assertEquals(LocalDate.parse("2026-01-05"), cycles[0].periodEndDate)
    }

    @Test
    fun `a start date with no end date leaves duration genuinely unknown`() {
        val cycles = CycleCalculator.buildCycles(eventsAt("2026-01-01", "2026-01-29"))
        assertNull(cycles[0].periodDurationDays)
        assertNull(cycles[0].periodEndDate)
        assertTrue(cycles[1].isOngoing)
        // Unknown durations fall back to the documented default rather than inventing one.
        assertEquals(5, CycleCalculator.averagePeriodDuration(cycles))
    }

    @Test
    fun `a logged end date produces a real period duration`() {
        val cycles = CycleCalculator.buildCycles(
            listOf(event("2026-01-01", "2026-01-06"), event("2026-01-29", "2026-02-02"))
        )
        assertEquals(6, cycles[0].periodDurationDays)
        assertEquals(6, CycleCalculator.averagePeriodDuration(cycles))
    }

    // ---------------------------------------------------------------------------------------
    // Gaps
    // ---------------------------------------------------------------------------------------

    @Test
    fun `a three month logging gap is not treated as a cycle length`() {
        val cycles = CycleCalculator.buildCycles(eventsAt("2026-01-01", "2026-01-29", "2026-07-01"))
        assertEquals(153, cycles[1].lengthDays)
        assertTrue(cycles[2].hasGapBefore)
        // Only the one believable length feeds the average.
        assertEquals(listOf(28), CycleCalculator.lengthsForStats(cycles))
        assertEquals(28, CycleCalculator.averageCycleLength(cycles))
        assertEquals(PredictionConfidence.LOW, CycleCalculator.confidence(cycles))
    }

    @Test
    fun `an implausibly short length is excluded from statistics`() {
        val cycles = CycleCalculator.buildCycles(eventsAt("2026-01-01", "2026-01-05"))
        assertEquals(4, cycles[0].lengthDays)
        assertTrue(CycleCalculator.lengthsForStats(cycles).isEmpty())
        assertEquals(28, CycleCalculator.averageCycleLength(cycles))
    }

    // ---------------------------------------------------------------------------------------
    // Averages and robustness
    // ---------------------------------------------------------------------------------------

    @Test
    fun `six regular 28 day cycles predict exactly 28 days ahead`() {
        val cycles = CycleCalculator.buildCycles(regularCycles(6))
        val prediction = assertNotNull(CycleCalculator.predict(cycles, LocalDate.parse("2026-06-20")))
        assertEquals(28, prediction.averageCycleLength)
        assertEquals(0.0, prediction.regularityDays, 0.001)
        assertEquals(PredictionConfidence.HIGH, prediction.confidence)
        assertEquals(LocalDate.parse("2026-07-16"), prediction.nextPeriodStart)
    }

    @Test
    fun `a single stray 45 day cycle does not drag the average off a consistent pattern`() {
        // lengths: 28, 28, 29, 27, 45
        val cycles = CycleCalculator.buildCycles(
            eventsAt("2026-01-01", "2026-01-29", "2026-02-26", "2026-03-27", "2026-04-23", "2026-06-07")
        )
        assertEquals(listOf(28, 28, 29, 27, 45), CycleCalculator.lengthsForStats(cycles))
        assertEquals(28, CycleCalculator.averageCycleLength(cycles))
    }

    @Test
    fun `a consistently long cycle is not corrected down to 28`() {
        val cycles = CycleCalculator.buildCycles(regularCycles(6, length = 40))
        assertEquals(40, CycleCalculator.averageCycleLength(cycles))
        assertEquals(PredictionConfidence.HIGH, CycleCalculator.confidence(cycles))
    }

    @Test
    fun `a wide spread of cycle lengths narrows the confidence`() {
        val stable = CycleCalculator.buildCycles(regularCycles(6))
        val unstable = CycleCalculator.buildCycles(
            eventsAt("2026-01-01", "2026-01-22", "2026-02-26", "2026-03-20", "2026-04-23", "2026-05-15", "2026-06-18")
        )
        assertEquals(PredictionConfidence.HIGH, CycleCalculator.confidence(stable))
        assertTrue(CycleCalculator.regularityDays(unstable) > 4.0)
        assertTrue(CycleCalculator.confidence(unstable) < PredictionConfidence.HIGH)
    }

    @Test
    fun `an irregular history widens the predicted range`() {
        val stable = assertNotNull(
            CycleCalculator.predict(CycleCalculator.buildCycles(regularCycles(6)), LocalDate.parse("2026-06-20"))
        )
        val unstable = assertNotNull(
            CycleCalculator.predict(
                CycleCalculator.buildCycles(
                    eventsAt("2026-01-01", "2026-01-22", "2026-02-26", "2026-03-20", "2026-04-23", "2026-05-15", "2026-06-18")
                ),
                LocalDate.parse("2026-06-20"),
            )
        )
        val stableSpread = java.time.temporal.ChronoUnit.DAYS.between(stable.earliestStart, stable.latestStart)
        val unstableSpread = java.time.temporal.ChronoUnit.DAYS.between(unstable.earliestStart, unstable.latestStart)
        assertTrue(unstableSpread > stableSpread)
    }

    // ---------------------------------------------------------------------------------------
    // Ovulation, fertility, luteal
    // ---------------------------------------------------------------------------------------

    @Test
    fun `fertile window spans five days before ovulation through the day after`() {
        val prediction = assertNotNull(
            CycleCalculator.predict(CycleCalculator.buildCycles(regularCycles(6)), LocalDate.parse("2026-06-20"))
        )
        assertEquals(LocalDate.parse("2026-07-02"), prediction.ovulationDay)
        assertEquals(prediction.ovulationDay.minusDays(5), prediction.fertileWindowStart)
        assertEquals(prediction.ovulationDay.plusDays(1), prediction.fertileWindowEnd)
        assertEquals(14, prediction.lutealPhaseLength)
    }

    @Test
    fun `luteal phase is clamped so ovulation cannot precede menstruation`() {
        val shortCycles = CycleCalculator.buildCycles(regularCycles(6, length = 21))
        assertEquals(11, CycleCalculator.lutealPhaseLength(shortCycles))

        val typical = CycleCalculator.buildCycles(regularCycles(6, length = 28))
        assertEquals(14, CycleCalculator.lutealPhaseLength(typical))

        val longCycles = CycleCalculator.buildCycles(regularCycles(6, length = 40))
        assertEquals(14, CycleCalculator.lutealPhaseLength(longCycles))
    }

    @Test
    fun `ovulation sits the luteal length before the predicted start`() {
        val prediction = assertNotNull(
            CycleCalculator.predict(CycleCalculator.buildCycles(regularCycles(6)), LocalDate.parse("2026-06-20"))
        )
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
            prediction.ovulationDay, prediction.nextPeriodStart
        )
        assertEquals(prediction.lutealPhaseLength.toLong(), daysBetween)
    }

    // ---------------------------------------------------------------------------------------
    // Long horizon (premium)
    // ---------------------------------------------------------------------------------------

    @Test
    fun `long horizon projection walks forward by the average cycle length`() {
        val prediction = assertNotNull(
            CycleCalculator.predict(CycleCalculator.buildCycles(regularCycles(6)), LocalDate.parse("2026-06-20"))
        )
        val projected = prediction.projectNext(6)
        assertEquals(6, projected.size)
        assertEquals(prediction.nextPeriodStart.plusDays(28), projected[0])
        assertEquals(prediction.nextPeriodStart.plusDays(6 * 28L), projected[5])
    }

    @Test
    fun `an overdue period is reported as late rather than projected away`() {
        val cycles = CycleCalculator.buildCycles(
            listOf(event("2026-01-01", "2026-01-05"), event("2026-01-29", "2026-02-02"))
        )
        val prediction = assertNotNull(CycleCalculator.predict(cycles, LocalDate.parse("2026-03-15")))
        assertEquals(LocalDate.parse("2026-02-26"), prediction.nextPeriodStart)
        assertTrue(prediction.isLate)
        assertEquals(-17L, prediction.daysUntilNextPeriod)
    }

    @Test
    fun `duplicate starting points never produce a zero length cycle`() {
        val cycles = CycleCalculator.buildCycles(eventsAt("2026-01-01", "2026-01-01"))
        assertEquals(1, cycles.size)
        assertTrue(CycleCalculator.lengthsForStats(cycles).isEmpty())
    }

    @Test
    fun `defaults are used until at least one real length exists`() {
        assertEquals(28, CycleCalculator.averageCycleLength(CycleCalculator.buildCycles(regularCycles(0))))
        assertEquals(PredictionConfidence.LOW, CycleCalculator.confidence(CycleCalculator.buildCycles(regularCycles(0))))
    }

    // ---------------------------------------------------------------------------------------
    // Cycle context — plan.md 8.4 (contraception / perimenopause / postpartum)
    // ---------------------------------------------------------------------------------------

    @Test
    fun `hormonal contraception suppresses the prediction instead of inventing one`() {
        val cycles = CycleCalculator.buildCycles(regularCycles(6))
        val prediction = CycleCalculator.predict(
            cycles,
            LocalDate.parse("2026-06-20"),
            CycleContext.HORMONAL_CONTRACEPTION,
        )
        // Plenty of logged history, and still no prediction: ovulation is suppressed,
        // so there is no cycle to read. Silence beats a confident fiction.
        assertNull(prediction)
    }

    @Test
    fun `a default context prediction is not caveated`() {
        val prediction = assertNotNull(
            CycleCalculator.predict(CycleCalculator.buildCycles(regularCycles(6)), LocalDate.parse("2026-06-20"))
        )
        assertEquals(CycleContext.NONE, prediction.context)
        assertFalse(prediction.isCaveated)
    }

    @Test
    fun `perimenopause predictions are caveated and pinned to low confidence`() {
        val cycles = CycleCalculator.buildCycles(regularCycles(6))
        // Six perfectly regular cycles would normally earn HIGH.
        assertEquals(PredictionConfidence.HIGH, CycleCalculator.confidence(cycles))

        val prediction = assertNotNull(
            CycleCalculator.predict(cycles, LocalDate.parse("2026-06-20"), CycleContext.PERIMENOPAUSE)
        )
        assertTrue(prediction.isCaveated)
        assertEquals(CycleContext.PERIMENOPAUSE, prediction.context)
        assertEquals(PredictionConfidence.LOW, prediction.confidence)
        // The dates themselves are still computed — the UI decides how to hedge them.
        assertEquals(LocalDate.parse("2026-07-16"), prediction.nextPeriodStart)
    }

    @Test
    fun `postpartum predictions are caveated too`() {
        val prediction = assertNotNull(
            CycleCalculator.predict(
                CycleCalculator.buildCycles(regularCycles(6)),
                LocalDate.parse("2026-06-20"),
                CycleContext.POSTPARTUM,
            )
        )
        assertTrue(prediction.isCaveated)
        assertEquals(PredictionConfidence.LOW, prediction.confidence)
    }

    // ---------------------------------------------------------------------------------------
    // Outside the typical range — plan.md 8.4 (long / short cycles still soften the copy)
    // ---------------------------------------------------------------------------------------

    @Test
    fun `a consistently long cycle is still computed but says so in the label`() {
        val cycles = CycleCalculator.buildCycles(regularCycles(6, length = 40))
        assertEquals(40, CycleCalculator.averageCycleLength(cycles))
        assertTrue(CycleCalculator.outsideTypicalRange(cycles))
        // The wording softens; the number and the confidence do not.
        assertEquals(PredictionConfidence.HIGH, CycleCalculator.confidence(cycles))
        val label = CycleCalculator.regularityLabel(cycles)
        assertTrue(label, label.startsWith("Very consistent"))
        assertTrue(label, label.contains("longer than typical"))
        assertTrue(label, label.contains("40 days"))
    }

    @Test
    fun `a consistently short cycle is flagged as shorter than typical`() {
        val cycles = CycleCalculator.buildCycles(regularCycles(6, length = 18))
        assertEquals(18, CycleCalculator.averageCycleLength(cycles))
        assertTrue(CycleCalculator.outsideTypicalRange(cycles))
        val label = CycleCalculator.regularityLabel(cycles)
        assertTrue(label, label.contains("shorter than typical"))
    }

    @Test
    fun `a typical cycle is computed and never flagged`() {
        val cycles = CycleCalculator.buildCycles(regularCycles(6, length = 28))
        assertFalse(CycleCalculator.outsideTypicalRange(cycles))
        assertEquals("Very consistent", CycleCalculator.regularityLabel(cycles))
    }

    @Test
    fun `nothing is flagged when there is no history to judge`() {
        val none = CycleCalculator.buildCycles(emptyList())
        assertFalse(CycleCalculator.outsideTypicalRange(none))
        assertFalse(CycleCalculator.outsideTypicalRange(emptyList()))
        assertNull(CycleCalculator.typicalityNote(emptyList()))
        assertEquals("Not enough history yet", CycleCalculator.regularityLabel(none))
    }

    @Test
    fun `the readiness read carries the same note as the regularity label`() {
        val longCycles = CycleCalculator.buildCycles(regularCycles(6, length = 40))
        val ready = PhaseResolver.describeReadiness(longCycles)
        assertTrue(ready, ready.contains("40 days"))
        assertTrue(ready, ready.contains("longer than typical"))

        val typical = PhaseResolver.describeReadiness(CycleCalculator.buildCycles(regularCycles(6)))
        assertEquals("Your rhythm looks consistent", typical)
    }

    private fun <T> assertNotNull(value: T?): T {
        if (value == null) throw AssertionError("Expected non-null value")
        return value
    }
}

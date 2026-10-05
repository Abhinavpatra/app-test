package com.bloomcycle.app.domain.insights

import com.bloomcycle.app.domain.model.CycleContext
import com.bloomcycle.app.domain.model.CyclePrediction
import com.bloomcycle.app.domain.model.PredictionConfidence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FertilityReadoutTest {

    private val start = LocalDate.parse("2026-04-26")

    private fun prediction(
        confidence: PredictionConfidence = PredictionConfidence.MEDIUM,
        context: CycleContext = CycleContext.NONE,
    ) = CyclePrediction(
        nextPeriodStart = start,
        earliestStart = start,
        latestStart = start,
        ovulationDay = start.minusDays(14),
        fertileWindowStart = start.minusDays(19),
        fertileWindowEnd = start.minusDays(15),
        pmsWindowStart = start.minusDays(6),
        pmsWindowEnd = start.minusDays(1),
        confidence = confidence,
        averageCycleLength = 28,
        averagePeriodDuration = 5,
        lutealPhaseLength = 14,
        cyclesUsed = 6,
        regularityDays = 1.5,
        regularityLabel = "Very consistent",
        isLate = false,
        daysUntilNextPeriod = 6,
        context = context,
    )

    @Test
    fun `the window comes straight from the prediction, with no caveat to add`() {
        val readout = FertilityReadout.of(prediction())

        assertEquals(start.minusDays(19), readout.windowStart)
        assertEquals(start.minusDays(15), readout.windowEnd)
        assertNull(readout.contextLine)
    }

    @Test
    fun `uncertainty grows as confidence falls`() {
        val high = FertilityReadout.of(prediction(PredictionConfidence.HIGH)).uncertainty
        val medium = FertilityReadout.of(prediction(PredictionConfidence.MEDIUM)).uncertainty
        val low = FertilityReadout.of(prediction(PredictionConfidence.LOW)).uncertainty

        assertTrue(high != medium && medium != low)
        assertTrue(high.contains("narrow"))
        assertTrue(low.contains("rough guide"))
    }

    @Test
    fun `the three required disclaimers are all there`() {
        val disclaimer = FertilityReadout.of(prediction()).disclaimer

        assertTrue(disclaimer.contains("Not contraception advice"))
        assertTrue(disclaimer.contains("not a pregnancy test"))
        assertTrue(disclaimer.contains("not a medical device"))
    }

    @Test
    fun `a caveated context downgrades the wording and says which one`() {
        val readout = FertilityReadout.of(prediction(context = CycleContext.PERIMENOPAUSE))

        assertEquals(
            "Because of perimenopause, these dates are a rough guide at best.",
            readout.contextLine,
        )
        // The caveat is a separate line — the confidence sentence stays honest on its own.
        assertTrue(readout.uncertainty.contains("estimate"))
    }

    @Test
    fun `the note explains why the window is a range`() {
        assertTrue(FertilityReadout.of(prediction()).note.contains("several days"))
    }
}

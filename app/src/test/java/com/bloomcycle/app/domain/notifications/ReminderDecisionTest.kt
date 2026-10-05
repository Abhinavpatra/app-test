package com.bloomcycle.app.domain.notifications

import com.bloomcycle.app.domain.model.CycleContext
import com.bloomcycle.app.domain.model.CyclePrediction
import com.bloomcycle.app.domain.model.PredictionConfidence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderDecisionTest {

    private val today = LocalDate.parse("2026-04-20")

    private fun prediction(
        daysUntil: Long,
        context: CycleContext = CycleContext.NONE,
        pmsWindowStart: LocalDate = today.plusDays(daysUntil)
            .minusDays(ReminderDecision.PMS_WINDOW_DAYS.toLong()),
    ) = CyclePrediction(
        nextPeriodStart = today.plusDays(daysUntil),
        earliestStart = today.plusDays(daysUntil),
        latestStart = today.plusDays(daysUntil),
        ovulationDay = today.plusDays(daysUntil).minusDays(14),
        fertileWindowStart = today.plusDays(daysUntil).minusDays(15),
        fertileWindowEnd = today.plusDays(daysUntil).minusDays(11),
        pmsWindowStart = pmsWindowStart,
        pmsWindowEnd = today.plusDays(daysUntil).minusDays(1),
        confidence = PredictionConfidence.MEDIUM,
        averageCycleLength = 28,
        averagePeriodDuration = 5,
        lutealPhaseLength = 14,
        cyclesUsed = 4,
        regularityDays = 1.0,
        regularityLabel = "Very consistent",
        isLate = false,
        daysUntilNextPeriod = daysUntil,
        context = context,
    )

    private fun decide(
        prediction: CyclePrediction?,
        remindersEnabled: Boolean = true,
        dailyNoteEnabled: Boolean = true,
        predictionsMuted: Boolean = false,
    ) = ReminderDecision.decide(
        remindersEnabled = remindersEnabled,
        dailyNoteEnabled = dailyNoteEnabled,
        predictionsMuted = predictionsMuted,
        prediction = prediction,
        today = today,
    )

    @Test
    fun `reminders switched off means silence`() {
        assertEquals(
            ReminderAction.NOTHING,
            decide(prediction(1), remindersEnabled = false),
        )
    }

    @Test
    fun `hormonal contraception leaves nothing to remind about`() {
        assertEquals(ReminderAction.NOTHING, decide(prediction = null))
    }

    @Test
    fun `a caveated context never claims a due date`() {
        assertEquals(
            ReminderAction.NOTHING,
            decide(prediction(1, context = CycleContext.PERIMENOPAUSE)),
        )
        assertEquals(
            ReminderAction.NOTHING,
            decide(prediction(1, context = CycleContext.POSTPARTUM)),
        )
    }

    @Test
    fun `keeping predictions quiet silences both messages`() {
        assertEquals(
            ReminderAction.NOTHING,
            decide(prediction(1), predictionsMuted = true),
        )
        assertEquals(
            ReminderAction.NOTHING,
            decide(prediction(4), predictionsMuted = true),
        )
    }

    @Test
    fun `the period itself and its last two days are the period reminder`() {
        assertEquals(ReminderAction.PERIOD, decide(prediction(0)))
        assertEquals(ReminderAction.PERIOD, decide(prediction(1)))
        assertEquals(ReminderAction.PERIOD, decide(prediction(2)))
    }

    @Test
    fun `the pms window gets the early-signal nudge`() {
        assertEquals(ReminderAction.EARLY_SIGNAL, decide(prediction(3)))
        assertEquals(ReminderAction.EARLY_SIGNAL, decide(prediction(4)))
        assertEquals(ReminderAction.EARLY_SIGNAL, decide(prediction(6)))
    }

    @Test
    fun `the early signal respects the daily-note toggle`() {
        assertEquals(
            ReminderAction.NOTHING,
            decide(prediction(4), dailyNoteEnabled = false),
        )
        // The toggle governs the nudge only — the period reminder still goes out.
        assertEquals(ReminderAction.PERIOD, decide(prediction(1), dailyNoteEnabled = false))
    }

    @Test
    fun `beyond the window there is nothing to say`() {
        assertEquals(ReminderAction.NOTHING, decide(prediction(7)))
        assertEquals(ReminderAction.NOTHING, decide(prediction(14)))
    }

    @Test
    fun `a day before the pms window opens stays silent`() {
        val outside = prediction(6, pmsWindowStart = today.plusDays(3))

        assertEquals(ReminderAction.NOTHING, decide(outside))
    }
}

package com.bloomcycle.app.domain.cycle

import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.PhaseType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhaseResolverTest {

    private val cycles = CycleCalculator.buildCycles(
        listOf(
            PeriodEvent(startDate = LocalDate.parse("2026-01-01"), endDate = LocalDate.parse("2026-01-05")),
            PeriodEvent(startDate = LocalDate.parse("2026-01-29"), endDate = LocalDate.parse("2026-02-02")),
        )
    )

    private val prediction = CycleCalculator.predict(cycles, LocalDate.parse("2026-01-01"))

    private fun phaseOn(date: String): PhaseType? =
        PhaseResolver.resolve(LocalDate.parse(date), cycles, prediction)?.phase

    private fun stateOn(date: String) = PhaseResolver.resolve(LocalDate.parse(date), cycles, prediction)

    @Test
    fun `day one of bleeding is menstrual`() {
        val state = assertNotNull(stateOn("2026-01-01"))
        assertEquals(PhaseType.MENSTRUAL, state.phase)
        assertEquals(1, state.cycleDay)
        assertEquals(1, state.dayOfPhase)
        assertEquals(5, state.phaseDaysTotal)
    }

    @Test
    fun `the last day of bleeding is still menstrual`() {
        assertEquals(PhaseType.MENSTRUAL, phaseOn("2026-01-05"))
        assertEquals(5, assertNotNull(stateOn("2026-01-05")).dayOfPhase)
    }

    @Test
    fun `the day after bleeding ends begins the follicular phase`() {
        val state = assertNotNull(stateOn("2026-01-06"))
        assertEquals(PhaseType.FOLLICULAR, state.phase)
        assertEquals(6, state.cycleDay)
        assertEquals(1, state.dayOfPhase)
    }

    @Test
    fun `ovulation day and the day after are ovulatory`() {
        // nextStart = 2026-01-29, luteal = 14 -> ovulation = 2026-01-15
        assertEquals(PhaseType.OVULATORY, phaseOn("2026-01-15"))
        assertEquals(PhaseType.OVULATORY, phaseOn("2026-01-16"))
    }

    @Test
    fun `the day after the ovulatory window is luteal`() {
        val state = assertNotNull(stateOn("2026-01-17"))
        assertEquals(PhaseType.LUTEAL, state.phase)
        assertEquals(2, state.dayOfPhase)
    }

    @Test
    fun `the final day of the cycle is still luteal`() {
        assertEquals(PhaseType.LUTEAL, phaseOn("2026-01-28"))
        assertEquals(13, assertNotNull(stateOn("2026-01-28")).dayOfPhase)
    }

    @Test
    fun `cycle day keeps counting across phase boundaries`() {
        assertEquals(15, assertNotNull(stateOn("2026-01-15")).cycleDay)
        assertEquals(28, assertNotNull(stateOn("2026-01-28")).cycleDay)
    }

    @Test
    fun `nothing resolves before the first logged period`() {
        assertNull(PhaseResolver.resolve(LocalDate.parse("2025-12-01"), cycles, prediction))
    }

    @Test
    fun `nothing resolves with no history at all`() {
        assertNull(
            PhaseResolver.resolve(
                LocalDate.parse("2026-01-01"),
                emptyList(),
                null,
            )
        )
    }

    @Test
    fun `a long logging gap does not pretend a phase`() {
        val gappy = CycleCalculator.buildCycles(
            listOf(
                PeriodEvent(startDate = LocalDate.parse("2026-01-01"), endDate = LocalDate.parse("2026-01-05")),
                PeriodEvent(startDate = LocalDate.parse("2026-07-01"), endDate = LocalDate.parse("2026-07-05")),
            )
        )
        // Between the two logged cycles nothing is being predicted, so no phase is claimed.
        assertNull(
            PhaseResolver.resolve(LocalDate.parse("2026-04-01"), gappy, CycleCalculator.predict(gappy, LocalDate.parse("2026-04-01")))
        )
    }

    @Test
    fun `phase resolves for a currently open cycle`() {
        val single = CycleCalculator.buildCycles(
            listOf(PeriodEvent(startDate = LocalDate.parse("2026-01-01"), endDate = LocalDate.parse("2026-01-05")))
        )
        val p = CycleCalculator.predict(single, LocalDate.parse("2026-01-10"))
        val state = PhaseResolver.resolve(LocalDate.parse("2026-01-10"), single, p)
        assertEquals(PhaseType.FOLLICULAR, assertNotNull(state).phase)
    }

    private fun <T> assertNotNull(value: T?): T {
        if (value == null) throw AssertionError("Expected non-null value")
        return value
    }
}

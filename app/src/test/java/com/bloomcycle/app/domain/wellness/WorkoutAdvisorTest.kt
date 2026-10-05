package com.bloomcycle.app.domain.wellness

import com.bloomcycle.app.domain.model.PhaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutAdvisorTest {

    @Test
    fun `every phase has advice, and so does having no phase`() {
        val phases: List<PhaseType?> = PhaseType.entries + null

        phases.forEach { phase ->
            val advice = WorkoutAdvisor.forPhase(phase)
            assertEquals(phase, advice.phase)
            assertTrue(advice.summary.isNotBlank())
            assertTrue(advice.suggestion.isNotBlank())
        }
    }

    @Test
    fun `nothing tells anyone to train through pain`() {
        val phases: List<PhaseType?> = PhaseType.entries + null

        phases.forEach { phase ->
            val advice = WorkoutAdvisor.forPhase(phase)
            assertTrue(
                "$phase: ${advice.suggestion}",
                !advice.suggestion.contains(Regex("push through|train through|ignore the pain")),
            )
        }
    }

    @Test
    fun `the listen-to-your-body note ships with every reading`() {
        assertTrue(WorkoutAdvisor.LISTEN_NOTE.contains("Listen to your body"))
        assertTrue(WorkoutAdvisor.LISTEN_NOTE.contains("means stop"))
    }

    @Test
    fun `intensity matches the phase rather than being uniform`() {
        assertEquals(Intensity.REST, WorkoutAdvisor.forPhase(PhaseType.MENSTRUAL).intensity)
        assertEquals(Intensity.PEAK, WorkoutAdvisor.forPhase(PhaseType.FOLLICULAR).intensity)
        assertEquals(Intensity.PEAK, WorkoutAdvisor.forPhase(PhaseType.OVULATORY).intensity)
        assertEquals(Intensity.STEADY, WorkoutAdvisor.forPhase(PhaseType.LUTEAL).intensity)
    }

    @Test
    fun `no phase asks for a log instead of guessing`() {
        val advice = WorkoutAdvisor.forPhase(null)

        assertEquals(Intensity.GENTLE, advice.intensity)
        assertTrue(advice.suggestion.contains("Log a period"))
    }
}

package com.bloomcycle.app.domain.wellness

import com.bloomcycle.app.domain.model.PhaseType

/**
 * Phase 10 — "Movement by phase" (README line 11): intensity suggestions per phase.
 *
 * The contract is in the type: every entry is a *permission*, never an instruction, and
 * [LISTEN_NOTE] ships with the reading wherever it is rendered. Nothing here tells anyone
 * to train through pain, and a phase nobody has is answered with "log a period", not a
 * guess.
 */
enum class Intensity { REST, GENTLE, STEADY, PEAK }

data class PhaseAdvice(
    val phase: PhaseType?,
    val intensity: Intensity,
    val summary: String,
    val suggestion: String,
)

object WorkoutAdvisor {

    /** Shown under every movement reading, always, on screen. */
    const val LISTEN_NOTE =
        "Listen to your body — this is a suggestion, not a rule. Pain, dizziness or feeling " +
            "unwell means stop. No phase is worth training through anything."

    fun forPhase(phase: PhaseType?): PhaseAdvice = when (phase) {
        PhaseType.MENSTRUAL -> PhaseAdvice(
            phase = phase,
            intensity = Intensity.REST,
            summary = "Rest week",
            suggestion = "Walking, stretching and easy mobility suit most people here. A short " +
                "gentle session counts, and so does none at all.",
        )

        PhaseType.FOLLICULAR -> PhaseAdvice(
            phase = phase,
            intensity = Intensity.PEAK,
            summary = "Energy climbing",
            suggestion = "A good stretch for harder work — intervals, hills, heavier lifts, or " +
                "starting something new while motivation is up.",
        )

        PhaseType.OVULATORY -> PhaseAdvice(
            phase = phase,
            intensity = Intensity.PEAK,
            summary = "Peak window",
            suggestion = "Strength and speed often come easiest now. A natural slot for a personal " +
                "best, a long run or a game with friends.",
        )

        PhaseType.LUTEAL -> PhaseAdvice(
            phase = phase,
            intensity = Intensity.STEADY,
            summary = "Steady, not maximal",
            suggestion = "Strength work, yoga, swimming and easier-paced cardio tend to fit. " +
                "Dropping the volume a notch is normal, not a step back.",
        )

        null -> PhaseAdvice(
            phase = null,
            intensity = Intensity.GENTLE,
            summary = "No phase yet",
            suggestion = "Log a period and this becomes specific to your cycle. Until then, " +
                "whatever you already enjoy doing is the right thing.",
        )
    }
}

package com.bloomcycle.app.domain.insight

import com.bloomcycle.app.core.time.CycleDate

enum class Inclination(val label: String) {
    SCHOLAR("Study-led"),
    MAKER("Making-led"),
    ATHLETE("Movement-led"),
}

/**
 * Phase 10 — the "inclination" read: a study focus and an athletic focus, kept deliberately
 * apart from [Astrology] (plan.md: "kept clearly separated from the astrological output").
 * Separate type, separate file, its own section on screen, and the two are never blended
 * into one sentence.
 *
 * The mapping is a fixed table indexed by the birth date — deterministic, computed here,
 * never sent anywhere. It is a prompt for reflection, not a measurement of anybody.
 */
data class InclinationProfile(
    val inclination: Inclination,
    val focus: String,
    val studyFocus: String,
    val athleticFocus: String,
) {
    companion object {

        fun of(birthDate: CycleDate): InclinationProfile {
            val inclination =
                Inclination.entries[floorMod(birthDate.dayOfYear, Inclination.entries.size)]
            return when (inclination) {
                Inclination.SCHOLAR -> InclinationProfile(
                    inclination = inclination,
                    focus = "You tend to orient around understanding things first.",
                    studyFocus = "Deep single-subject blocks suit you: one book, one skill, " +
                        "at a time.",
                    athleticFocus = "Technique-led movement — a class, a coach, learning a lift " +
                        "properly.",
                )

                Inclination.MAKER -> InclinationProfile(
                    inclination = inclination,
                    focus = "You tend to orient around making something.",
                    studyFocus = "Project-shaped study: build the thing, then read around the gaps.",
                    athleticFocus = "Play-shaped movement — a sport, a route, a game you want to win.",
                )

                Inclination.ATHLETE -> InclinationProfile(
                    inclination = inclination,
                    focus = "You tend to orient around doing and moving.",
                    studyFocus = "Short scheduled sessions beat long ones; spaced practice fits you.",
                    athleticFocus = "Progress-shaped movement: a programme, a distance, a number " +
                        "to beat.",
                )
            }
        }

        private fun floorMod(value: Int, modulus: Int): Int {
            val remainder = value % modulus
            return if (remainder < 0) remainder + modulus else remainder
        }
    }
}

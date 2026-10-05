package com.bloomcycle.app.domain.insights

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.CyclePrediction
import com.bloomcycle.app.domain.model.PredictionConfidence

/**
 * Phase 10 — how the fertile window is *presented*: the range, the honest uncertainty
 * statement that goes with the confidence the engine actually measured, and the three
 * disclaimers plan.md requires (estimate, not contraception, not a pregnancy test).
 *
 * Pure copy over a prediction — the dates stay as dates and the UI formats them with the
 * shared locale-aware formatter, so this file never touches `java.time` formatting.
 */
data class FertilityReadout(
    val windowStart: CycleDate,
    val windowEnd: CycleDate,
    val uncertainty: String,
    val note: String,
    /** Non-null when the user's cycle context makes these dates a rough guide. */
    val contextLine: String?,
) {
    val disclaimer: String get() = DISCLAIMER

    companion object {
        const val DISCLAIMER =
            "An estimate built from what you log. Not contraception advice, not a pregnancy " +
                "test, and not a medical device."

        const val NOTE =
            "Sperm can survive several days, so the window is wider than a single day — the " +
                "range is the honest version of it."

        fun of(prediction: CyclePrediction): FertilityReadout = FertilityReadout(
            windowStart = prediction.fertileWindowStart,
            windowEnd = prediction.fertileWindowEnd,
            uncertainty = when (prediction.confidence) {
                PredictionConfidence.HIGH ->
                    "Your cycles have been consistent, so this window is narrow — it is still " +
                        "an estimate, not a measurement."

                PredictionConfidence.MEDIUM ->
                    "A working estimate: after more logging it can move a day or two either way."

                PredictionConfidence.LOW ->
                    "Still learning your rhythm. Treat this as a rough guide rather than a date."
            },
            note = NOTE,
            contextLine = if (prediction.isCaveated) {
                "Because of ${prediction.context.label.lowercase()}, these dates are a rough " +
                    "guide at best."
            } else {
                null
            },
        )
    }
}

package com.bloomcycle.app.ui.phase

import com.bloomcycle.app.domain.model.PhaseType

/**
 * The single place a phase becomes something you can see.
 *
 * Calendar cells, the home card, the charts and the chat rooms all read from here. Before
 * this existed the same phase tended to pick up a slightly different green in each screen,
 * and within a week nobody could tell which one was "right".
 */
enum class PhaseVisual(
    val phase: PhaseType,
    val glyph: String,
    val label: String,
    val blurb: String,
) {
    MENSTRUAL(
        phase = PhaseType.MENSTRUAL,
        glyph = "●",
        label = "Menstrual",
        blurb = "Your period. Rest counts as doing something.",
    ),
    FOLLICULAR(
        phase = PhaseType.FOLLICULAR,
        glyph = "◐",
        label = "Follicular",
        blurb = "Energy tends to climb. A good stretch for starting things.",
    ),
    OVULATORY(
        phase = PhaseType.OVULATORY,
        glyph = "✦",
        label = "Ovulatory",
        blurb = "Near the most fertile point of your cycle.",
    ),
    LUTEAL(
        phase = PhaseType.LUTEAL,
        glyph = "◑",
        label = "Luteal",
        blurb = "The long stretch before your period. Premenstrual shifts often land here.",
    ),
    ;

    companion object {
        fun of(phase: PhaseType?): PhaseVisual? = entries.firstOrNull { it.phase == phase }
    }
}

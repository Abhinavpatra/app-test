package com.bloomcycle.app.domain.insight

import com.bloomcycle.app.core.time.CycleDate
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ZodiacSign(val label: String, val dates: String) {
    ARIES("Aries", "21 Mar – 19 Apr"),
    TAURUS("Taurus", "20 Apr – 20 May"),
    GEMINI("Gemini", "21 May – 20 Jun"),
    CANCER("Cancer", "21 Jun – 22 Jul"),
    LEO("Leo", "23 Jul – 22 Aug"),
    VIRGO("Virgo", "23 Aug – 22 Sep"),
    LIBRA("Libra", "23 Sep – 22 Oct"),
    SCORPIO("Scorpio", "23 Oct – 21 Nov"),
    SAGITTARIUS("Sagittarius", "22 Nov – 21 Dec"),
    CAPRICORN("Capricorn", "22 Dec – 19 Jan"),
    AQUARIUS("Aquarius", "20 Jan – 18 Feb"),
    PISCES("Pisces", "19 Feb – 20 Mar"),
}

/**
 * Classic three-cycle biorhythm read: 23 / 28 / 33 days from the day of birth, expressed
 * as a percentage of the sine wave (-100 at the trough, 0 at each crossing, 100 at the
 * peak) plus the plain word the UI shows beside it.
 */
data class Biorhythm(
    val dayNumber: Long,
    val physical: Int,
    val emotional: Int,
    val intellectual: Int,
) {
    fun word(value: Int): String = when {
        value >= 60 -> "full"
        value >= 15 -> "building"
        value > -15 -> "turning"
        value > -60 -> "easing"
        else -> "low"
    }

    companion object {
        const val PHYSICAL_CYCLE = 23
        const val EMOTIONAL_CYCLE = 28
        const val INTELLECTUAL_CYCLE = 33
    }
}

/**
 * Phase 10 — the astrology reading (README lines 10 & 11).
 *
 * Deterministic and entirely on-device: the same birth date always yields the same answer,
 * nothing is looked up, and no birth date ever leaves the phone (plan.md Phase 10
 * acceptance, risk R1). This is entertainment, and [ENTERTAINMENT_LABEL] says so on every
 * screen that shows any of it — it is never dismissed and never hidden behind an "i".
 *
 * The moon sign uses the mean lunar longitude plus a first-order equation of centre —
 * accurate to about a degree, which is far inside the ~30° width of a sign. Good enough
 * for a reflection prompt, and honest about being one.
 */
object Astrology {

    const val ENTERTAINMENT_LABEL =
        "For reflection and entertainment only — not medical guidance."

    /** Day zero of the lunar maths; arbitrary but fixed, so results never drift. */
    private val referenceDate: CycleDate = CycleDate.parse("2000-01-01")

    fun natalMoonSign(birthDate: CycleDate): ZodiacSign {
        val days = ChronoUnit.DAYS.between(referenceDate, birthDate).toDouble()
        val meanLongitude = 218.316 + 13.176396 * days
        val sunAnomaly = (357.529 + 0.98560028 * days) * PI / 180.0
        val longitude = (meanLongitude + 6.289 * sin(sunAnomaly)).mod(360.0)
        val index = (longitude / 30.0).toInt().coerceIn(0, ZodiacSign.entries.size - 1)
        return ZodiacSign.entries[index]
    }

    fun biorhythm(birthDate: CycleDate, today: CycleDate): Biorhythm {
        val days = ChronoUnit.DAYS.between(birthDate, today)
        fun wave(cycle: Int): Int {
            val position = days.mod(cycle.toLong())
            return (sin(2.0 * PI * position / cycle) * 100).roundToInt()
        }
        return Biorhythm(
            dayNumber = days,
            physical = wave(Biorhythm.PHYSICAL_CYCLE),
            emotional = wave(Biorhythm.EMOTIONAL_CYCLE),
            intellectual = wave(Biorhythm.INTELLECTUAL_CYCLE),
        )
    }
}

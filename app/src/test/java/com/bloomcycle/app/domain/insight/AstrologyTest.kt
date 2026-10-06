package com.bloomcycle.app.domain.insight

import com.bloomcycle.app.core.time.CycleDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AstrologyTest {

    private val birth = CycleDate.parse("1994-06-12")

    @Test
    fun `the same birth date always gives the same sign`() {
        assertEquals(Astrology.natalMoonSign(birth), Astrology.natalMoonSign(birth))
    }

    @Test
    fun `the moon moves on, so one month to the next usually changes sign`() {
        val signs = (0L..13L).map { months ->
            Astrology.natalMoonSign(birth.plusMonths(months))
        }
        val changes = signs.zipWithNext().count { (a, b) -> a != b }

        // Roughly 13° a day: a month steps the sign along almost every time.
        assertTrue("changes=$changes of ${signs.size - 1}", changes >= 10)
    }

    @Test
    fun `biorhythm starts at zero on the day of birth`() {
        val rhythm = Astrology.biorhythm(birth, birth)

        assertEquals(0L, rhythm.dayNumber)
        assertEquals(0, rhythm.physical)
        assertEquals(0, rhythm.emotional)
        assertEquals(0, rhythm.intellectual)
    }

    @Test
    fun `a quarter of the way through a cycle is its peak`() {
        val rhythm = Astrology.biorhythm(birth, birth.plusDays(7))

        assertEquals(100, rhythm.emotional)
        assertTrue(rhythm.physical in -100..100)
        assertTrue(rhythm.intellectual in -100..100)
    }

    @Test
    fun `the wave word tracks the sign of the value`() {
        val rhythm = Astrology.biorhythm(birth, birth)

        assertEquals("turning", rhythm.word(0))
        assertEquals("full", rhythm.word(88))
        assertEquals("resting", rhythm.word(-88))
    }

    @Test
    fun `the entertainment label is the required one, and says so in words`() {
        assertEquals(
            "For reflection and entertainment only — not medical guidance.",
            Astrology.ENTERTAINMENT_LABEL,
        )
    }
}

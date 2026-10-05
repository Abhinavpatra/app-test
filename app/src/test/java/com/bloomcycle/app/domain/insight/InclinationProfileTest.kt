package com.bloomcycle.app.domain.insight

import com.bloomcycle.app.core.time.CycleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InclinationProfileTest {

    @Test
    fun `the same birth date always gives the same profile`() {
        val birth = CycleDate.parse("1991-03-04")

        assertEquals(InclinationProfile.of(birth), InclinationProfile.of(birth))
    }

    @Test
    fun `every inclination carries its three lines`() {
        (0..364).map { day -> InclinationProfile.of(CycleDate.of(2000, 1, 1).plusDays(day.toLong())) }
            .distinctBy { it.inclination }
            .forEach { profile ->
                assertTrue(profile.focus.isNotBlank())
                assertTrue(profile.studyFocus.isNotBlank())
                assertTrue(profile.athleticFocus.isNotBlank())
            }
    }

    @Test
    fun `all three inclinations are reachable across one year`() {
        val found = (0..364)
            .map { day -> InclinationProfile.of(CycleDate.of(2000, 1, 1).plusDays(day.toLong())) }
            .map { it.inclination }
            .toSet()

        assertEquals(Inclination.entries.toSet(), found)
    }

    @Test
    fun `the study and athletic focuses are separate sentences, not one blend`() {
        val profile = InclinationProfile.of(CycleDate.parse("1988-11-30"))

        assertTrue(profile.studyFocus != profile.athleticFocus)
        assertTrue(Inclination.entries.any { it.label.isNotBlank() })
    }
}

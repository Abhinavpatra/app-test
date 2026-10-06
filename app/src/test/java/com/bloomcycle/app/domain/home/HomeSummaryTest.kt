package com.bloomcycle.app.domain.home

import com.bloomcycle.app.domain.model.CycleContext
import com.bloomcycle.app.domain.model.PeriodEvent
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSummaryTest {

    private fun event(start: String, end: String? = null, spotting: Boolean = false) = PeriodEvent(
        startDate = LocalDate.parse(start),
        endDate = end?.let(LocalDate::parse),
        isSpottingOnly = spotting,
    )

    private fun date(iso: String) = LocalDate.parse(iso)

    private fun summary(
        periods: List<PeriodEvent>,
        today: String,
        context: CycleContext = CycleContext.NONE,
    ) = HomeSummarizer.summarize(periods, date(today), context)

    // ---------------------------------------------------------------------------------------
    // Nothing logged yet
    // ---------------------------------------------------------------------------------------

    @Test
    fun `empty history shows readiness copy instead of inventing a phase`() {
        val result = summary(emptyList(), today = "2026-01-10")

        assertNull(result.phase)
        assertNull(result.prediction)
        assertNull(result.countdown)
        assertNull(result.caveat)
        assertEquals("No cycles logged yet", result.status)
        assertFalse(result.hasLoggedPeriod)
        assertNull(result.cycleDay)
    }

    @Test
    fun `spotting alone does not count as a logged period`() {
        val result = summary(listOf(event("2026-01-01", spotting = true)), today = "2026-01-10")

        // Spotting is dropped when cycles are built, so there is still nothing to count from.
        assertNull(result.prediction)
        assertEquals("No cycles logged yet", result.status)
        assertFalse(result.hasLoggedPeriod)
    }

    // ---------------------------------------------------------------------------------------
    // Countdown wording
    // ---------------------------------------------------------------------------------------

    @Test
    fun `one logged period counts down to the defaulted next start`() {
        // Default cycle length is 28 days, so 2026-01-01 lands the next one on 2026-01-29.
        val result = summary(listOf(event("2026-01-01")), today = "2026-01-10")

        assertNotNull(result.prediction)
        assertEquals("Period expected in about 19 days", result.countdown)
        assertTrue(result.hasLoggedPeriod)
        assertNotNull(result.phase)
        assertNull(result.status)
        assertEquals(10, result.cycleDay)
    }

    @Test
    fun `countdown says today and tomorrow rather than zero and one`() {
        val periods = listOf(event("2026-01-01"))

        assertEquals("Period expected today", summary(periods, today = "2026-01-29").countdown)
        assertEquals("Period expected tomorrow", summary(periods, today = "2026-01-28").countdown)
    }

    @Test
    fun `a late period is reported as late, not rolled forward`() {
        val result = summary(listOf(event("2026-01-01")), today = "2026-02-01")

        assertNotNull(result.prediction)
        assertTrue(result.prediction!!.isLate)
        assertEquals("About 3 days past your estimated start", result.countdown)
    }

    // ---------------------------------------------------------------------------------------
    // Caveats
    // ---------------------------------------------------------------------------------------

    @Test
    fun `a typical context says nothing extra`() {
        val result = summary(listOf(event("2026-01-01")), today = "2026-01-10")

        assertNull(result.caveat)
        assertEquals(CycleContext.NONE, result.prediction?.context)
    }

    @Test
    fun `hormonal contraception never predicts and always explains why`() {
        val result = summary(
            listOf(event("2026-01-01")),
            today = "2026-01-10",
            context = CycleContext.HORMONAL_CONTRACEPTION,
        )

        assertNull(result.prediction)
        assertNull(result.countdown)
        assertNotNull(result.caveat)
        assertTrue(result.caveat!!.contains("rough guide"))
        assertTrue(result.hasLoggedPeriod)
    }

    @Test
    fun `perimenopause still predicts but hedged`() {
        val result = summary(
            listOf(event("2026-01-01")),
            today = "2026-01-10",
            context = CycleContext.PERIMENOPAUSE,
        )

        assertNotNull(result.prediction)
        assertNotNull(result.caveat)
        assertEquals(CycleContext.PERIMENOPAUSE, result.prediction!!.context)
    }
}

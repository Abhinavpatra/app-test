package com.bloomcycle.app.domain.calendar

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.PhaseType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarModelTest {

    private fun event(start: String, end: String? = null) = PeriodEvent(
        startDate = LocalDate.parse(start),
        endDate = end?.let(LocalDate::parse),
    )

    /** Two cycles: 1 Jan -> 29 Jan (28 days), then the open one. */
    private val history = listOf(event("2026-01-01"), event("2026-01-29"))
    private val cycles = CycleCalculator.buildCycles(history)

    private val today: CycleDate = LocalDate.parse("2026-02-01")

    private fun prediction() = CycleCalculator.predict(cycles, today)

    private fun marks(
        date: String,
        periods: List<PeriodEvent> = history,
    ): DayMarks = CalendarModel.dayMarks(
        date = LocalDate.parse(date),
        periods = periods,
        cycles = cycles,
        prediction = prediction(),
        today = today,
    )

    // --- Month layout ---------------------------------------------------------------------

    @Test
    fun `january 2026 leads with three blanks when the week starts on monday`() {
        val matrix = CalendarModel.monthMatrix(YearMonth.of(2026, 1))

        assertEquals(3, matrix.takeWhile { it == null }.size)
        assertEquals(LocalDate.of(2026, 1, 1), matrix[3])
        assertEquals(LocalDate.of(2026, 1, 31), matrix.last { it != null })
        assertEquals(0, matrix.size % 7)
    }

    @Test
    fun `a sunday week start shifts the blanks without moving the days`() {
        val matrix = CalendarModel.monthMatrix(YearMonth.of(2026, 1), firstDay = DayOfWeek.SUNDAY)

        assertEquals(4, matrix.takeWhile { it == null }.size)
        assertEquals(LocalDate.of(2026, 1, 1), matrix[4])
        assertEquals(LocalDate.of(2026, 1, 31), matrix.last { it != null })
    }

    @Test
    fun `february 2028 has twenty nine days and 2027 has twenty eight`() {
        val leap = CalendarModel.monthMatrix(YearMonth.of(2028, 2))
        val common = CalendarModel.monthMatrix(YearMonth.of(2027, 2))

        assertEquals(29, leap.count { it != null })
        assertEquals(28, common.count { it != null })
        assertEquals(LocalDate.of(2028, 2, 29), leap.last { it != null })
        assertTrue(leap.size % 7 == 0 && common.size % 7 == 0)
    }

    @Test
    fun `a full year renders day by day without dropping or repeating a date`() {
        val days = (1..12).flatMap { month ->
            CalendarModel.monthMatrix(YearMonth.of(2026, month)).filterNotNull()
        }

        assertEquals(365, days.size)
        assertEquals(LocalDate.of(2026, 1, 1), days.first())
        assertEquals(LocalDate.of(2026, 12, 31), days.last())
        assertEquals(days.size, days.toSet().size)
    }

    @Test
    fun `monthsAround keeps the current month centred`() {
        val months = CalendarModel.monthsAround(today, back = 13, forward = 13)

        assertEquals(27, months.size)
        assertEquals(YearMonth.of(2026, 2), months[13])
        assertEquals(YearMonth.of(2025, 1), months.first())
        assertEquals(YearMonth.of(2027, 3), months.last())
    }

    // --- Logged periods -------------------------------------------------------------------

    @Test
    fun `a logged period marks its own days and stops at the end date`() {
        val periods = listOf(event("2026-01-05", "2026-01-08"))

        assertTrue(marks("2026-01-05", periods).loggedPeriod)
        assertTrue(marks("2026-01-08", periods).loggedPeriod)
        assertFalse(marks("2026-01-04", periods).loggedPeriod)
        assertFalse(marks("2026-01-09", periods).loggedPeriod)
    }

    @Test
    fun `an open ended period runs to today and no further`() {
        val periods = listOf(event("2026-01-30"))

        assertTrue(marks("2026-02-01", periods).loggedPeriod)
        assertFalse(marks("2026-02-02", periods).loggedPeriod)
    }

    @Test
    fun `a logged day inside the arrival window keeps both flags`() {
        val window = marks("2026-02-26")
        assertTrue(window.predictedPeriod)

        val loggedInside = marks("2026-02-26", history + event("2026-02-26"))
        assertTrue(loggedInside.loggedPeriod)
        assertTrue(loggedInside.predictedPeriod)
    }

    // --- Prediction -----------------------------------------------------------------------

    @Test
    fun `the arrival window marks its inclusive edges and nothing beyond them`() {
        val window = marks("2026-02-26")

        assertTrue(window.predictedPeriod)
        assertTrue(marks("2026-02-25").predictedPeriod)
        assertTrue(marks("2026-02-27").predictedPeriod)
        assertFalse(marks("2026-02-24").predictedPeriod)
        assertFalse(marks("2026-02-28").predictedPeriod)
        assertFalse(marks("2026-02-01").predictedPeriod)
    }

    @Test
    fun `the fertile window spans ovulation minus five to plus one`() {
        // Next start 26 Feb, luteal 14 -> ovulation 12 Feb -> fertile 7..13 Feb.
        assertTrue(marks("2026-02-07").fertileWindow)
        assertTrue(marks("2026-02-13").fertileWindow)
        assertFalse(marks("2026-02-06").fertileWindow)
        assertFalse(marks("2026-02-14").fertileWindow)
    }

    @Test
    fun `the fertile window never claims a day that is already a logged period day`() {
        val periods = listOf(event("2026-01-01"), event("2026-01-29"), event("2026-02-09", "2026-02-11"))
        val cycleList = CycleCalculator.buildCycles(periods)

        val day = CalendarModel.dayMarks(
            date = LocalDate.parse("2026-02-10"),
            periods = periods,
            cycles = cycleList,
            prediction = CycleCalculator.predict(cycleList, today),
            today = today,
        )

        assertTrue(day.loggedPeriod)
        assertFalse(day.fertileWindow)
    }

    @Test
    fun `today is marked on today alone`() {
        assertTrue(marks("2026-02-01").today)
        assertFalse(marks("2026-02-02").today)
        assertFalse(marks("2026-01-31").today)
    }

    // --- Outside the typical range --------------------------------------------------------

    @Test
    fun `days inside an unusually long cycle carry the gentle outside range flag`() {
        val periods = listOf(event("2026-01-01"), event("2026-02-10"))
        val longCycles = CycleCalculator.buildCycles(periods)

        fun flagOn(date: String) = CalendarModel.dayMarks(
            date = LocalDate.parse(date),
            periods = periods,
            cycles = longCycles,
            prediction = CycleCalculator.predict(longCycles, today),
            today = today,
        ).outsideTypicalRange

        assertTrue(flagOn("2026-01-15"))
        assertFalse(flagOn("2026-02-15"))
        assertFalse(flagOn("2025-12-31"))
    }

    @Test
    fun `a typical twenty eight day cycle flags nothing`() {
        assertFalse(marks("2026-01-15").outsideTypicalRange)
    }

    // --- Phase and plain days ------------------------------------------------------------

    @Test
    fun `days inside a cycle resolve to a phase and days outside do not`() {
        assertEquals(PhaseType.MENSTRUAL, marks("2026-01-03").phase)

        val farOutside = marks("2025-11-04")
        assertNull(farOutside.phase)
        assertTrue(farOutside.isPlain)
    }

    @Test
    fun `today always lights up its own cell`() {
        val quietDay = marks("2026-02-01")

        assertTrue(quietDay.today)
        assertFalse(quietDay.isPlain)
        assertTrue(quietDay.carries(DayMark.TODAY))
        assertFalse(quietDay.carries(DayMark.PREDICTED_PERIOD))
    }

    // --- Year overview --------------------------------------------------------------------

    @Test
    fun `the year strip says which months carry marks`() {
        val months = CalendarModel.monthsMarkedIn(
            year = 2026,
            periods = history,
            cycles = cycles,
            prediction = prediction(),
            today = today,
        )

        assertEquals(12, months.size)
        assertTrue(DayMark.LOGGED_PERIOD in months.getValue(YearMonth.of(2026, 1)))
        assertTrue(DayMark.TODAY in months.getValue(YearMonth.of(2026, 2)))
        assertTrue(DayMark.PREDICTED_PERIOD in months.getValue(YearMonth.of(2026, 2)))
        assertTrue(DayMark.FERTILE_WINDOW in months.getValue(YearMonth.of(2026, 2)))
        assertTrue(months.getValue(YearMonth.of(2026, 4)).isEmpty())
    }
}

package com.bloomcycle.app.domain.calendar

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.cycle.PhaseResolver
import com.bloomcycle.app.domain.model.Cycle
import com.bloomcycle.app.domain.model.CyclePrediction
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.PhaseType
import java.time.DayOfWeek
import java.time.YearMonth

/**
 * The five things a calendar day can be showing. The grid draws them, the legend explains
 * them and the day sheet repeats them — one definition, so the three can never drift.
 *
 * The order is also the rendering precedence: a day that is both a logged period day and a
 * predicted day is drawn as a logged period day, because what actually happened outranks
 * what was estimated.
 */
enum class DayMark {
    LOGGED_PERIOD,
    PREDICTED_PERIOD,
    FERTILE_WINDOW,
    TODAY,
    OUTSIDE_TYPICAL_RANGE,
}

/**
 * Everything the calendar needs to know about a single day, as data.
 *
 * Every field is derived from what the user logged plus the current prediction. Colour is
 * never the only signal — the grid pairs each mark with a shape (filled disc, ring, dots)
 * and the day sheet says each one in words, so the month still reads in greyscale, with
 * colour-vision deficiency, and through TalkBack.
 */
data class DayMarks(
    val date: CycleDate,
    val loggedPeriod: Boolean = false,
    val predictedPeriod: Boolean = false,
    val fertileWindow: Boolean = false,
    val today: Boolean = false,
    /** A cycle whose length sat outside the typical 21-35 day band — a note, not a warning. */
    val outsideTypicalRange: Boolean = false,
    /** Null when the date falls outside any resolvable cycle. */
    val phase: PhaseType? = null,
) {
    fun carries(mark: DayMark): Boolean = when (mark) {
        DayMark.LOGGED_PERIOD -> loggedPeriod
        DayMark.PREDICTED_PERIOD -> predictedPeriod
        DayMark.FERTILE_WINDOW -> fertileWindow
        DayMark.TODAY -> today
        DayMark.OUTSIDE_TYPICAL_RANGE -> outsideTypicalRange
    }

    /** Nothing the grid needs to draw. Phase alone never lights up a cell. */
    val isPlain: Boolean
        get() = DayMark.entries.none { carries(it) }
}

/**
 * The calendar's read on the logged data. Pure Kotlin, no Android, no Calendar instance —
 * month arithmetic is done with java.time so leap years and short months are simply correct
 * and every rule below is a unit test rather than a bug report.
 */
object CalendarModel {

    /**
     * Every day of [yearMonth], laid out left to right, top to bottom.
     *
     * The leading nulls pad day 1 back to the first column, so the grid lines up under the
     * weekday header for whatever day the week starts on. The result is always a multiple
     * of 7 — trailing blanks are nulls too.
     */
    fun monthMatrix(
        yearMonth: YearMonth,
        firstDay: DayOfWeek = DayOfWeek.MONDAY,
    ): List<CycleDate?> {
        val lead = (yearMonth.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
        val days = yearMonth.lengthOfMonth()
        val size = ((lead + days + 6) / 7) * 7
        return List(size) { index ->
            val day = index - lead + 1
            if (day in 1..days) yearMonth.atDay(day) else null
        }
    }

    /** Months spanning [today] with room for a year of history and a year of predictions. */
    fun monthsAround(today: CycleDate, back: Int, forward: Int): List<YearMonth> {
        val current = YearMonth.from(today)
        return (-back..forward).map { current.plusMonths(it.toLong()) }
    }

    /** The single definition of "this day is part of that logged period". */
    fun covers(period: PeriodEvent, date: CycleDate, today: CycleDate): Boolean {
        if (date.isBefore(period.startDate)) return false
        // An end date of null means "still going": the range runs to today, never past it.
        val end = period.endDate ?: if (today.isAfter(period.startDate)) today else period.startDate
        return !date.isAfter(end)
    }

    fun dayMarks(
        date: CycleDate,
        periods: List<PeriodEvent>,
        cycles: List<Cycle>,
        prediction: CyclePrediction?,
        today: CycleDate,
    ): DayMarks {
        val loggedPeriod = periods.any { covers(it, date, today) }

        // The estimated arrival window, past or future: when the user is late, the elapsed
        // days inside it are exactly the signal they opened the app to see.
        val predictedPeriod = prediction != null &&
            !date.isBefore(prediction.earliestStart) && !date.isAfter(prediction.latestStart)

        val fertileWindow = prediction != null && !loggedPeriod &&
            !date.isBefore(prediction.fertileWindowStart) && !date.isAfter(prediction.fertileWindowEnd)

        val outsideTypicalRange = cycles.any { cycle ->
            val length = cycle.lengthDays ?: return@any false
            length !in CycleCalculator.TYPICAL_MIN..CycleCalculator.TYPICAL_MAX &&
                !date.isBefore(cycle.periodStartDate) &&
                (cycle.endDate == null || !date.isAfter(cycle.endDate))
        }

        return DayMarks(
            date = date,
            loggedPeriod = loggedPeriod,
            predictedPeriod = predictedPeriod,
            fertileWindow = fertileWindow,
            today = date == today,
            outsideTypicalRange = outsideTypicalRange,
            phase = PhaseResolver.resolve(date, cycles, prediction)?.phase,
        )
    }

    /** Every day of [yearMonth] with its marks, keyed by date. */
    fun monthMarks(
        yearMonth: YearMonth,
        periods: List<PeriodEvent>,
        cycles: List<Cycle>,
        prediction: CyclePrediction?,
        today: CycleDate,
        firstDay: DayOfWeek = DayOfWeek.MONDAY,
    ): Map<CycleDate, DayMarks> = monthMatrix(yearMonth, firstDay)
        .filterNotNull()
        .associateWith { dayMarks(it, periods, cycles, prediction, today) }

    /** Which months of [year] carry at least one marked day — the year overview strip. */
    fun monthsMarkedIn(
        year: Int,
        periods: List<PeriodEvent>,
        cycles: List<Cycle>,
        prediction: CyclePrediction?,
        today: CycleDate,
    ): Map<YearMonth, Set<DayMark>> = (1..12).associate { monthValue ->
        val month = YearMonth.of(year, monthValue)
        val marks = monthMarks(month, periods, cycles, prediction, today)
            .values
            .flatMap { day -> DayMark.entries.filter { day.carries(it) } }
            .toSet()
        month to marks
    }
}

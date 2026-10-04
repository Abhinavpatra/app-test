package com.bloomcycle.app.ui.format

import com.bloomcycle.app.core.time.CycleDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Date copy lives here so onboarding, the home card and the log sheet all render the same
 * day the same way. Locale-aware by using the default locale rather than a fixed pattern
 * locale, which is what a calendar app should do anyway.
 */
private val DAY_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val WEEKDAY_DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val SPEAKABLE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

fun formatDate(date: CycleDate): String = date.format(DAY_MONTH_YEAR)

/** "2–6 Oct 2026", collapsing to a single date when there is no end or it matches. */
fun formatDateRange(start: CycleDate, end: CycleDate?): String = when {
    end == null || end == start -> start.format(DAY_MONTH_YEAR)
    end.year != start.year -> "${start.format(DAY_MONTH_YEAR)} – ${end.format(DAY_MONTH_YEAR)}"
    end.month == start.month -> "${start.format(DAY_MONTH)}–${end.format(DAY_MONTH_YEAR)}"
    else -> "${start.format(DAY_MONTH_YEAR)} – ${end.format(DAY_MONTH_YEAR)}"
}

/** "Thursday 2 October" — the header above the Today card. */
fun formatHeadingDate(date: CycleDate): String = date.format(WEEKDAY_DAY_MONTH)

/** Spoken form for content descriptions: "2 October 2026" rather than "2 10 2026". */
fun speakDate(date: CycleDate): String = date.format(SPEAKABLE)

// The material date picker speaks UTC epoch millis; a LocalDate is a calendar date with no
// zone at all. Converting here — once, in one file — stops an off-by-one day leaking in
// from a device timezone (plan.md §6.2).
fun CycleDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.toCycleDate(): CycleDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

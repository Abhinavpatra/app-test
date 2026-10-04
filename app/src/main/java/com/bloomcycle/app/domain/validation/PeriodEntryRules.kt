package com.bloomcycle.app.domain.validation

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.PeriodEvent

/** Why a period entry cannot be saved. Each carries the sentence shown to the user. */
enum class PeriodEntryError(val message: String) {
    MISSING_START("Add the day your period started."),
    START_IN_FUTURE("Pick a day that has already happened."),
    END_BEFORE_START("The end date cannot be before the start date."),
    OVERLAPS("That range overlaps a period you have already logged."),
}

/**
 * The rules behind the Log period sheet. Pure so they can be tested without an emulator,
 * and so the sheet and any future entry point (calendar day tap, wearable) share one
 * definition of "valid".
 */
object PeriodEntryRules {

    /**
     * @param editingId id of the entry being edited, or 0 when creating. Its own row must
     *   not be treated as an overlap, otherwise saving an edit without changing anything
     *   would always fail.
     */
    fun validate(
        startDate: CycleDate?,
        endDate: CycleDate?,
        today: CycleDate,
        existing: List<PeriodEvent>,
        editingId: Long = 0L,
    ): PeriodEntryError? {
        if (startDate == null) return PeriodEntryError.MISSING_START
        if (startDate.isAfter(today)) return PeriodEntryError.START_IN_FUTURE
        if (endDate != null && endDate.isBefore(startDate)) return PeriodEntryError.END_BEFORE_START

        val rangeEnd = endDate ?: startDate
        val overlaps = existing.any { event ->
            if (event.id == editingId) return@any false
            // An event with no end date is still running, so it reaches today at least.
            val eventEnd = event.endDate ?: today
            !event.startDate.isAfter(rangeEnd) && !eventEnd.isBefore(startDate)
        }
        return if (overlaps) PeriodEntryError.OVERLAPS else null
    }
}

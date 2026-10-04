package com.bloomcycle.app.domain.validation

import com.bloomcycle.app.domain.model.PeriodEvent
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeriodEntryRulesTest {

    private fun event(id: Long = 0L, start: String, end: String? = null) = PeriodEvent(
        id = id,
        startDate = LocalDate.parse(start),
        endDate = end?.let(LocalDate::parse),
    )

    private val today = LocalDate.parse("2026-01-10")

    private fun validate(
        start: String?,
        end: String? = null,
        existing: List<PeriodEvent> = emptyList(),
        editingId: Long = 0L,
    ) = PeriodEntryRules.validate(
        startDate = start?.let(LocalDate::parse),
        endDate = end?.let(LocalDate::parse),
        today = today,
        existing = existing,
        editingId = editingId,
    )

    @Test
    fun `a start date is required`() {
        assertEquals(PeriodEntryError.MISSING_START, validate(start = null))
    }

    @Test
    fun `today is a valid start date, tomorrow is not`() {
        assertNull(validate(start = "2026-01-10"))
        assertEquals(PeriodEntryError.START_IN_FUTURE, validate(start = "2026-01-11"))
    }

    @Test
    fun `the end date cannot precede the start date`() {
        assertEquals(
            PeriodEntryError.END_BEFORE_START,
            validate(start = "2026-01-05", end = "2026-01-04"),
        )
    }

    @Test
    fun `an entry that touches its neighbour exactly does not count as an overlap`() {
        val existing = listOf(event(id = 1, start = "2026-01-05", end = "2026-01-07"))

        assertNull(validate(start = "2026-01-08", existing = existing))
        assertNull(validate(start = "2026-01-04", existing = existing))
        assertEquals(
            PeriodEntryError.OVERLAPS,
            validate(start = "2026-01-06", existing = existing),
        )
        assertEquals(
            PeriodEntryError.OVERLAPS,
            validate(start = "2026-01-04", end = "2026-01-05", existing = existing),
        )
    }

    @Test
    fun `an ongoing event still reaches today and blocks anything inside it`() {
        val ongoing = listOf(event(id = 7, start = "2026-01-01"))

        assertEquals(
            PeriodEntryError.OVERLAPS,
            validate(start = "2026-01-05", existing = ongoing),
        )
        assertNull(validate(start = "2026-01-02", existing = ongoing, editingId = 7L))
    }

    @Test
    fun `editing an entry does not collide with itself`() {
        val existing = listOf(event(id = 42, start = "2026-01-05", end = "2026-01-07"))

        assertNull(
            validate(
                start = "2026-01-05",
                end = "2026-01-08",
                existing = existing,
                editingId = 42L,
            ),
        )
        // ...but it still collides with a different entry.
        assertEquals(
            PeriodEntryError.OVERLAPS,
            validate(
                start = "2026-01-06",
                existing = existing + event(id = 9, start = "2026-01-04"),
                editingId = 42L,
            ),
        )
    }

    @Test
    fun `the range runs to today when no end date is chosen yet`() {
        // A range that ends today must not reach into a period that starts tomorrow, but it
        // must see one that starts today.
        assertEquals(
            PeriodEntryError.OVERLAPS,
            validate(start = "2026-01-10", existing = listOf(event(id = 3, start = "2026-01-10"))),
        )
    }
}

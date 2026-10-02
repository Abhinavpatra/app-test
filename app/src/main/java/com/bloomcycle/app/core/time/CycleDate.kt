package com.bloomcycle.app.core.time

import java.time.Instant
import java.time.LocalDate

/**
 * A calendar date with no time or timezone attached.
 *
 * Period tracking is fundamentally about *days*, so dates are kept as [LocalDate] and
 * persisted as ISO-8601 strings ("2026-10-02") rather than epoch millis. Epoch millis
 * silently shift by a day when the device timezone changes, which corrupts every
 * prediction built on top of them.
 */
typealias CycleDate = LocalDate

/** Injectable clock so the cycle engine can be tested against a pinned "today". */
interface CycleClock {
    fun today(): CycleDate
    fun now(): Instant
}

class SystemCycleClock : CycleClock {
    override fun today(): CycleDate = LocalDate.now()
    override fun now(): Instant = Instant.now()
}

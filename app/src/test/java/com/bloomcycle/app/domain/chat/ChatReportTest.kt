package com.bloomcycle.app.domain.chat

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReportTest {

    private val report = ChatReport(
        messageId = "msg-1",
        authorName = "Quiet Fern",
        reason = ReportReason.HARASSMENT,
        createdAt = Instant.parse("2026-10-06T12:00:00Z"),
    )

    @Test
    fun `a report round-trips through its encoding`() {
        assertEquals(report, ChatReport.decode(report.encode()))
    }

    @Test
    fun `malformed records decode to null instead of throwing`() {
        val bad = listOf(
            "",
            "too|few|parts",
            "id|author|NOT_A_REASON|123",
            "id|author|SPAM|not-a-number",
            "|author|SPAM|123",
            "id||SPAM|123",
            "a|b|c|d|e|f",
        )

        bad.forEach { raw -> assertNull(raw, ChatReport.decode(raw)) }
    }

    @Test
    fun `adding a report keeps it and survives an encode and decode pass`() {
        val log = ChatReports.add(emptySet(), report)

        assertEquals(setOf(report), ChatReports.decodeAll(log).toSet())
    }

    @Test
    fun `the log is capped at its oldest records being dropped first`() {
        var log: Set<String> = emptySet()
        val base = Instant.parse("2026-10-01T00:00:00Z")
        repeat(ChatReports.MAX_RETAINED + 25) { i ->
            val newer = report.copy(
                messageId = "msg-$i",
                createdAt = base.plusSeconds(i.toLong()),
            )
            log = ChatReports.add(log, newer)
        }

        assertEquals(ChatReports.MAX_RETAINED, log.size)
        val kept = ChatReports.decodeAll(log)
        assertTrue(kept.none { it.messageId == "msg-0" })
        assertTrue(kept.any { it.messageId == "msg-124" })
    }

    @Test
    fun `message ids are extracted for hiding, and strangers' ids are not`() {
        val log = ChatReports.add(emptySet(), report)

        assertEquals(setOf("msg-1"), ChatReports.messageIds(log))
    }
}

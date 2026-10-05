package com.bloomcycle.app.domain.chat

import com.bloomcycle.app.domain.model.ChatMessage
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatVisibilityTest {

    private val now = Instant.parse("2026-10-06T12:00:00Z")

    private fun message(id: String, author: String) = ChatMessage(
        id = id,
        text = "hello",
        authorName = author,
        phaseBucket = "near ovulation",
        isOwn = false,
        createdAt = now,
    )

    private val one = message("m1", "Quiet Fern")
    private val two = message("m2", "Soft Willow")
    private val mine = message("m3", "Me")

    @Test
    fun `nothing reported or blocked returns the list untouched`() {
        val messages = listOf(one, two, mine)

        assertEquals(messages, ChatVisibility.visible(messages, emptySet(), emptySet()))
    }

    @Test
    fun `a reported message disappears for the reporter`() {
        val log = ChatReports.add(
            emptySet(),
            ChatReport("m1", "Quiet Fern", ReportReason.SPAM, now),
        )

        val visible = ChatVisibility.visible(listOf(one, two), log, emptySet())

        assertEquals(listOf(two), visible)
    }

    @Test
    fun `a blocked author takes all of their messages with them`() {
        val visible = ChatVisibility.visible(
            listOf(one, two),
            reportLog = emptySet(),
            blockedAuthors = setOf("Quiet Fern"),
        )

        assertEquals(listOf(two), visible)
    }

    @Test
    fun `reporting one message never hides someone else's`() {
        val log = ChatReports.add(
            emptySet(),
            ChatReport("m1", "Quiet Fern", ReportReason.HARMFUL, now),
        )

        val visible = ChatVisibility.visible(listOf(one, two, mine), log, emptySet())

        assertTrue(visible.none { it.id == "m1" })
        assertTrue(visible.any { it.id == "m2" })
        assertTrue(visible.any { it.id == "m3" })
    }

    @Test
    fun `filters compose - reports and blocks stack`() {
        val log = ChatReports.add(
            emptySet(),
            ChatReport("m1", "Quiet Fern", ReportReason.HARASSMENT, now),
        )

        val visible = ChatVisibility.visible(
            listOf(one, two, mine),
            reportLog = log,
            blockedAuthors = setOf("Soft Willow"),
        )

        assertEquals(listOf(mine), visible)
    }
}

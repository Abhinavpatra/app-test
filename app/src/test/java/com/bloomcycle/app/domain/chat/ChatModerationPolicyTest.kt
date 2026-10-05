package com.bloomcycle.app.domain.chat

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatModerationPolicyTest {

    private val now: Instant = Instant.parse("2026-10-06T12:00:00Z")

    @Test
    fun `an empty or whitespace-only message is blocked with a helpful line`() {
        listOf("", "   ", "\n\t ").forEach { raw ->
            val verdict = ChatModerationPolicy.check(raw, null, now)
            assertFalse(verdict.allowed)
            assertEquals(SendBlockReason.EMPTY, verdict.blocked)
            assertTrue(verdict.message.isNotBlank())
        }
    }

    @Test
    fun `a message at the length limit is allowed and one character over is not`() {
        val atLimit = "a".repeat(ChatModerationPolicy.MAX_LENGTH)
        assertTrue(ChatModerationPolicy.check(atLimit, null, now).allowed)

        val overLimit = atLimit + "a"
        val verdict = ChatModerationPolicy.check(overLimit, null, now)
        assertEquals(SendBlockReason.TOO_LONG, verdict.blocked)
        assertTrue(verdict.message.contains(overLimit.length.toString()))
    }

    @Test
    fun `sending again inside the rate limit is blocked and says how long to wait`() {
        val almostOut = ChatModerationPolicy.check(
            "hello",
            now.minusSeconds(ChatModerationPolicy.MIN_INTERVAL_SECONDS - 1),
            now,
        )
        assertEquals(SendBlockReason.TOO_FAST, almostOut.blocked)
        assertTrue(almostOut.message.contains("moment"))

        val fiveToGo = ChatModerationPolicy.check(
            "hello",
            now.minusSeconds(ChatModerationPolicy.MIN_INTERVAL_SECONDS - 5),
            now,
        )
        assertEquals(SendBlockReason.TOO_FAST, fiveToGo.blocked)
        assertTrue(fiveToGo.message.contains("5s"))
    }

    @Test
    fun `sending again once the rate limit has passed is allowed`() {
        val lastSent = now.minusSeconds(ChatModerationPolicy.MIN_INTERVAL_SECONDS)

        assertTrue(ChatModerationPolicy.check("hello", lastSent, now).allowed)
    }

    @Test
    fun `a banned term is caught whatever the casing around it`() {
        val verdict = ChatModerationPolicy.check("Well that was a load of SHIT", null, now)

        assertEquals(SendBlockReason.INAPPROPRIATE, verdict.blocked)
        assertTrue(verdict.message.contains("rephrasing"))
    }

    @Test
    fun `an ordinary message passes with an empty message field`() {
        val verdict = ChatModerationPolicy.check("Has anyone else got cramps this month?", null, now)

        assertTrue(verdict.allowed)
        assertEquals("", verdict.message)
    }

    @Test
    fun `the ban list never matches inside an unrelated word`() {
        // "Shittake" is not a word anyone types, but "assess"/"class" style collisions are
        // the classic profanity-filter failure and must not block a genuine message.
        assertTrue(
            ChatModerationPolicy
                .check("I assessed my symptoms and classed it as mild", null, now)
                .allowed,
        )
    }

    @Test
    fun `sanitize collapses runs of spaces, trims, and keeps line breaks`() {
        val raw = "  hello   there \n  friend\ttab  "

        val clean = ChatModerationPolicy.sanitize(raw)

        assertEquals("hello there\nfriend tab", clean)
    }

    @Test
    fun `verdicts are deterministic for the same inputs`() {
        val a = ChatModerationPolicy.check("one", null, now)
        val b = ChatModerationPolicy.check("one", null, now)

        assertEquals(a, b)
    }
}

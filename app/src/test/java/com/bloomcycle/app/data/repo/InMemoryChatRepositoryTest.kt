package com.bloomcycle.app.data.repo

import app.cash.turbine.test
import com.bloomcycle.app.core.time.CycleClock
import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.ChatIdentity
import com.bloomcycle.app.domain.repository.ChatConnection
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryChatRepositoryTest {

    private class FixedClock(private val instant: Instant) : CycleClock {
        override fun today(): CycleDate = CycleDate.of(2026, 10, 6)
        override fun now(): Instant = instant
    }

    private val now = Instant.parse("2026-10-06T12:00:00Z")
    private val repository = InMemoryChatRepository(FixedClock(now))

    private val identity = ChatIdentity(
        displayName = "Quiet Fern",
        phaseBucket = "near ovulation",
        cycleLengthBand = "a typical cycle",
    )

    @Test
    fun `a sent message is stored trimmed, as the sender, in the chosen room`() = runTest {
        repository.send("  hello there  ", "global", identity)

        repository.messages("global").test {
            val stored = awaitItem().single()
            assertEquals("hello there", stored.text)
            assertTrue(stored.isOwn)
            assertEquals("Quiet Fern", stored.authorName)
            assertEquals("near ovulation", stored.phaseBucket)
            assertEquals(now, stored.createdAt)
        }
    }

    @Test
    fun `rooms are isolated - a global send never appears in the phase room`() = runTest {
        repository.send("global hello", "global", identity)
        repository.send("phase hello", "phase-menstrual", identity)

        repository.messages("global").test {
            assertEquals(listOf("global hello"), awaitItem().map { it.text })
        }
        repository.messages("phase-menstrual").test {
            assertEquals(listOf("phase hello"), awaitItem().map { it.text })
        }
    }

    @Test
    fun `the messages flow re-emits when something new arrives`() = runTest {
        repository.messages("global").test {
            assertEquals(emptyList<String>(), awaitItem().map { it.text })

            repository.send("first", "global", identity)
            assertEquals(listOf("first"), awaitItem().map { it.text })

            repository.send("second", "global", identity)
            assertEquals(listOf("first", "second"), awaitItem().map { it.text })
        }
    }

    @Test
    fun `messages come back oldest first, in the order they were sent`() = runTest {
        repository.send("one", "global", identity)
        repository.send("two", "global", identity)
        repository.send("three", "global", identity)

        repository.messages("global").test {
            assertEquals(listOf("one", "two", "three"), awaitItem().map { it.text })
        }
    }

    @Test
    fun `the connection is always live - there is no server to lose`() = runTest {
        repository.connection("global").test {
            assertEquals(ChatConnection.LIVE, awaitItem())
            awaitComplete()
        }
    }
}

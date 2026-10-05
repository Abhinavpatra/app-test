package com.bloomcycle.app.domain.chat

import com.bloomcycle.app.domain.model.ChatBuckets
import com.bloomcycle.app.domain.model.PhaseType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatIdentityFactoryTest {

    @Test
    fun `generated names are two words and repeatable for a given seed`() {
        val a = ChatIdentityFactory.generatedName(Random(42))
        val b = ChatIdentityFactory.generatedName(Random(42))

        assertEquals(a, b)
        assertEquals(2, a.split(" ").size)
        assertTrue(a.isNotBlank())
    }

    @Test
    fun `display names are stripped of the separators the report log relies on`() {
        val dirty = " Quiet | Fern / Two "

        val clean = ChatIdentityFactory.sanitizeDisplayName(dirty)

        assertEquals("Quiet  Fern  Two", clean)
        assertTrue("|" !in clean)
        assertTrue("/" !in clean)
    }

    @Test
    fun `display names are capped so one name cannot dominate a row`() {
        val long = "a".repeat(200)

        assertEquals(ChatIdentityFactory.MAX_NAME_LENGTH, ChatIdentityFactory.sanitizeDisplayName(long).length)
    }

    @Test
    fun `a blank name falls back to a generated pseudonym`() {
        val identity = ChatIdentityFactory.create(
            displayName = "   ",
            phase = PhaseType.OVULATORY,
            averageCycleLength = 28,
        )

        assertTrue(identity.displayName.isNotBlank())
        assertTrue(ChatIdentityFactory.sanitizeDisplayName(identity.displayName).isNotEmpty())
    }

    @Test
    fun `the identity carries only coarse buckets, never a date`() {
        val identity = ChatIdentityFactory.create(
            displayName = "Quiet Fern",
            phase = PhaseType.LUTEAL,
            averageCycleLength = 21,
        )

        assertEquals(ChatBuckets.forPhase(PhaseType.LUTEAL), identity.phaseBucket)
        assertEquals("a shorter cycle", identity.cycleLengthBand)
        assertEquals("Quiet Fern", identity.displayName)
    }

    @Test
    fun `unknown phase and cycle fall back to the honest buckets`() {
        val identity = ChatIdentityFactory.create(
            displayName = "Quiet Fern",
            phase = null,
            averageCycleLength = null,
        )

        assertEquals(ChatBuckets.UNKNOWN, identity.phaseBucket)
        assertEquals("still learning", identity.cycleLengthBand)
    }
}

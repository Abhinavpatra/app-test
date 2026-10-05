package com.bloomcycle.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementStateTest {

    @Test
    fun `nothing is unlocked by default`() {
        val state = EntitlementState()

        PremiumFeature.entries.forEach { assertFalse(state.isUnlocked(it)) }
    }

    @Test
    fun `unlocking one feature releases only that one`() {
        val state = EntitlementState(unlocked = setOf(PremiumFeature.ANALYSIS))

        assertTrue(state.isUnlocked(PremiumFeature.ANALYSIS))
        PremiumFeature.entries
            .filterNot { it == PremiumFeature.ANALYSIS }
            .forEach { assertFalse(state.isUnlocked(it)) }
    }

    @Test
    fun `the debug switch opens every feature`() {
        val state = EntitlementState(isDebugUnlocked = true)

        PremiumFeature.entries.forEach { assertTrue(state.isUnlocked(it)) }
    }

    @Test
    fun `turning the debug switch off locks everything again`() {
        val state = EntitlementState(unlocked = emptySet(), isDebugUnlocked = false)

        assertFalse(state.isUnlocked(PremiumFeature.WORKOUT_PLAN))
        assertFalse(state.isUnlocked(PremiumFeature.FERTILITY_WINDOW))
    }

    @Test
    fun `every premium feature carries a title and a blurb`() {
        PremiumFeature.entries.forEach { feature ->
            assertTrue("${feature.name} title", feature.title.isNotBlank())
            assertTrue("${feature.name} blurb", feature.blurb.isNotBlank())
        }
    }

    @Test
    fun `the paywall lists exactly the five planned features`() {
        assertEquals(
            setOf(
                PremiumFeature.ANALYSIS,
                PremiumFeature.FERTILITY_WINDOW,
                PremiumFeature.LONG_HORIZON,
                PremiumFeature.WORKOUT_PLAN,
                PremiumFeature.PERSONALITY_READING,
            ),
            PremiumFeature.entries.toSet(),
        )
    }
}

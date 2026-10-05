package com.bloomcycle.app.domain.chat

import com.bloomcycle.app.domain.model.ChatBuckets
import com.bloomcycle.app.domain.model.ChatIdentity
import com.bloomcycle.app.domain.model.PhaseType
import kotlin.random.Random

/**
 * Everything a send knows about its sender (plan.md §7.4): a pseudonym, a coarse phase
 * bucket and a cycle-length band. Names are generated here, on device — no account, no
 * email, no phone number ever enters the chat path.
 */
object ChatIdentityFactory {
    const val MAX_NAME_LENGTH = 24

    private val adjectives = listOf(
        "Quiet", "Soft", "Warm", "Bright", "Steady", "Kind", "Calm", "Brave",
    )
    private val nouns = listOf(
        "Fern", "Willow", "Sparrow", "River", "Moss", "Lark", "Hazel", "Rowan",
    )

    fun generatedName(random: Random = Random.Default): String =
        "${adjectives.random(random)} ${nouns.random(random)}"

    /**
     * Strip the separators that would corrupt the report log's encoding, and cap the
     * length so one name cannot dominate a message row. Whitespace-only becomes blank,
     * which callers fall back from to [generatedName].
     */
    fun sanitizeDisplayName(raw: String): String =
        raw.replace("|", "").replace("/", "").trim().take(MAX_NAME_LENGTH)

    fun create(
        displayName: String,
        phase: PhaseType?,
        averageCycleLength: Int?,
    ): ChatIdentity = ChatIdentity(
        displayName = sanitizeDisplayName(displayName).ifBlank { generatedName() },
        phaseBucket = ChatBuckets.forPhase(phase),
        cycleLengthBand = ChatBuckets.forCycleLength(averageCycleLength),
    )
}

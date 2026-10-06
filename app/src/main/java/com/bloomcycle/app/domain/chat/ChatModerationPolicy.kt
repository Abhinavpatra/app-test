package com.bloomcycle.app.domain.chat

import java.time.Duration
import java.time.Instant

/** Why a send was refused, so the composer can say what to do next rather than just "no". */
enum class SendBlockReason { EMPTY, TOO_LONG, TOO_FAST, INAPPROPRIATE }

/** [blocked] is null when the message may be sent. [message] is empty when allowed. */
data class SendVerdict(val blocked: SendBlockReason?, val message: String) {
    val allowed: Boolean get() = blocked == null
}

/**
 * The client-side half of chat safety (plan.md Phase 11): length cap, a simple send
 * rate limit, and a basic abuse filter. This is a courtesy gate, not the real one —
 * the Firestore security rules that land with the backend are what actually enforce
 * message shape and authorship. Everything here is pure and deterministic so the
 * tests can pin it down without a device.
 */
object ChatModerationPolicy {
    const val MAX_LENGTH = 500
    const val MIN_INTERVAL_SECONDS = 15L

    /**
     * Deliberately short and reviewable. A real deployment replaces this with
     * server-side filtering plus App Check; a word list the client ships is only
     * ever going to catch the obvious.
     */
    private val bannedTerms = listOf(
        "kill yourself",
        "kys",
        "fuck",
        "shit",
        "bitch",
        "cunt",
        "whore",
        "slut",
        "retard",
    )

    /** Trim, collapse runs of spaces/tabs, keep line breaks. Never mutates wording. */
    fun sanitize(raw: String): String =
        raw.lines()
            .joinToString("\n") { it.replace(Regex("[ \\t]+"), " ").trim() }
            .trim()

    fun check(text: String, lastSentAt: Instant?, now: Instant): SendVerdict {
        val clean = text.trim()
        if (clean.isEmpty()) {
            return SendVerdict(SendBlockReason.EMPTY, "Write something first, then send.")
        }
        if (clean.length > MAX_LENGTH) {
            return SendVerdict(
                SendBlockReason.TOO_LONG,
                "Keep it to $MAX_LENGTH characters — this one is ${clean.length}.",
            )
        }
        if (lastSentAt != null) {
            val elapsed = Duration.between(lastSentAt, now).seconds
            if (elapsed < MIN_INTERVAL_SECONDS) {
                val wait = MIN_INTERVAL_SECONDS - elapsed
                return SendVerdict(
                    SendBlockReason.TOO_FAST,
                    if (wait <= 1) "You can send again in a moment." else "You can send again in ${wait}s.",
                )
            }
        }
        if (containsBannedTerm(clean)) {
            return SendVerdict(
                SendBlockReason.INAPPROPRIATE,
                "That wording isn't allowed here. Try rephrasing it.",
            )
        }
        return SendVerdict(blocked = null, message = "")
    }

    private fun containsBannedTerm(clean: String): Boolean {
        val lower = clean.lowercase()
        return bannedTerms.any { lower.contains(it) }
    }
}

package com.bloomcycle.app.domain.chat

import java.time.Instant

/** Why someone reported a message. Short on purpose: four taps, no free text. */
enum class ReportReason(val label: String) {
    HARASSMENT("Harassment or bullying"),
    HARMFUL("Dangerous or harmful advice"),
    SPAM("Spam or advertising"),
    OTHER("Something else"),
}

/**
 * One report, kept on device until a backend exists to receive it. The author name is
 * stored because the message may already be gone from the room by the time anyone
 * reviews the log; it is a pseudonym either way.
 */
data class ChatReport(
    val messageId: String,
    val authorName: String,
    val reason: ReportReason,
    val createdAt: Instant,
) {
    fun encode(): String =
        listOf(messageId, authorName, reason.name, createdAt.epochSecond.toString())
            .joinToString("|")

    companion object {
        /**
         * Returns null for anything malformed rather than throwing: a hand-edited or
         * truncated preference must never take the settings flow down.
         */
        fun decode(raw: String): ChatReport? {
            val parts = raw.split("|")
            if (parts.size != 4) return null
            val (id, author, reasonName, epoch) = parts
            if (id.isEmpty() || author.isEmpty()) return null
            val reason = ReportReason.entries.firstOrNull { it.name == reasonName } ?: return null
            val seconds = epoch.toLongOrNull() ?: return null
            return ChatReport(id, author, reason, Instant.ofEpochSecond(seconds))
        }
    }
}

/**
 * The report log is a `Set<String>` in local settings — encoded records, capped, oldest
 * dropped first. Capping keeps a determined spammer from growing one preference forever.
 */
object ChatReports {
    const val MAX_RETAINED = 100

    fun decodeAll(log: Set<String>): List<ChatReport> =
        log.mapNotNull(ChatReport::decode)

    /** Ids of reported messages, used to hide them from this user's rooms. */
    fun messageIds(log: Set<String>): Set<String> =
        decodeAll(log).mapTo(mutableSetOf()) { it.messageId }

    /** Authors of reported messages — separate from the explicit block list. */
    fun reportedAuthors(log: Set<String>): Set<String> =
        decodeAll(log).mapTo(mutableSetOf()) { it.authorName }

    fun add(log: Set<String>, report: ChatReport): Set<String> =
        (log + report.encode())
            .sortedByDescending { ChatReport.decode(it)?.createdAt?.toEpochMilli() ?: 0L }
            .take(MAX_RETAINED)
            .toSet()
}

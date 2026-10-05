package com.bloomcycle.app.domain.chat

import com.bloomcycle.app.domain.model.ChatMessage

/**
 * What actually renders in a room. A reported message disappears immediately for the
 * person who reported it, and a blocked author's messages disappear entirely — both
 * decisions live locally, before any moderation reaches the server.
 *
 * Note the asymmetry: a report hides one message, a block hides a person. Reporting
 * therefore never silences someone for everyone else from this device alone.
 */
object ChatVisibility {
    fun visible(
        messages: List<ChatMessage>,
        reportLog: Set<String>,
        blockedAuthors: Set<String>,
    ): List<ChatMessage> {
        if (reportLog.isEmpty() && blockedAuthors.isEmpty()) return messages
        val reported = ChatReports.messageIds(reportLog)
        return messages.filter { message ->
            message.id !in reported && message.authorName !in blockedAuthors
        }
    }
}

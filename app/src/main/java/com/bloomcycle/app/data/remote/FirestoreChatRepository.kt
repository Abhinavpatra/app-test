package com.bloomcycle.app.data.remote

import com.bloomcycle.app.domain.model.ChatIdentity
import com.bloomcycle.app.domain.model.ChatMessage
import com.bloomcycle.app.domain.repository.ChatConnection
import com.bloomcycle.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Chat backed by Firestore. The same [RemoteChatDataSource] stream feeds both the
 * messages and the connection state: a snapshot served from cache means the server
 * is unreachable, which the UI shows as an offline banner rather than silence.
 *
 * Each entry point resolves the uid first (triggering the silent anonymous
 * sign-in on first use) and then subscribes. Two collectors on the same room mean
 * two snapshot listeners on the same query, which the Firestore SDK multiplexes
 * onto one server listen — no extra reads beyond the documents themselves.
 */
class FirestoreChatRepository(
    private val remote: RemoteChatDataSource,
    private val auth: AuthSession,
) : ChatRepository {

    override fun messages(roomId: String): Flow<List<ChatMessage>> = flow {
        emitAll(remote.observe(roomId, auth.uid()).map { it.messages })
    }

    override fun connection(roomId: String): Flow<ChatConnection> = flow {
        emitAll(
            remote.observe(roomId, auth.uid()).map { update ->
                if (update.fromCache) ChatConnection.OFFLINE else ChatConnection.LIVE
            },
        )
    }

    override suspend fun send(text: String, roomId: String, identity: ChatIdentity) {
        remote.send(roomId, text, identity, auth.uid())
    }
}

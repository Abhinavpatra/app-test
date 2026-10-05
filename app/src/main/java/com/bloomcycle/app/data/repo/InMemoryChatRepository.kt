package com.bloomcycle.app.data.repo

import com.bloomcycle.app.core.time.CycleClock
import com.bloomcycle.app.domain.model.ChatIdentity
import com.bloomcycle.app.domain.model.ChatMessage
import com.bloomcycle.app.domain.repository.ChatConnection
import com.bloomcycle.app.domain.repository.ChatRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Chat that never leaves the device. This is what runs wherever `FIREBASE_CHAT`
 * is false — fresh clones without `google-services.json`, and every unit test
 * that needs a repository without a backend.
 *
 * Rooms are isolated keys in a map: a message sent to one room never appears in
 * another, which is exactly the property the Firestore implementation must keep.
 * Connection is always LIVE — there is no server to lose.
 */
class InMemoryChatRepository(
    private val clock: CycleClock,
) : ChatRepository {

    private val rooms = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    override fun messages(roomId: String): Flow<List<ChatMessage>> =
        rooms.map { it[roomId].orEmpty() }

    override fun connection(roomId: String): Flow<ChatConnection> =
        flowOf(ChatConnection.LIVE)

    override suspend fun send(text: String, roomId: String, identity: ChatIdentity) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            authorName = identity.displayName,
            phaseBucket = identity.phaseBucket,
            isOwn = true,
            createdAt = clock.now(),
        )
        rooms.update { current ->
            current + (roomId to (current[roomId].orEmpty() + message))
        }
    }
}

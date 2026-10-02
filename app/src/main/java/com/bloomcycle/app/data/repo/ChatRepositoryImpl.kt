package com.bloomcycle.app.data.repo

import com.bloomcycle.app.data.local.ChatMessageDao
import com.bloomcycle.app.data.local.toDomain
import com.bloomcycle.app.data.local.toEntity
import com.bloomcycle.app.domain.model.ChatIdentity
import com.bloomcycle.app.domain.model.ChatMessage
import com.bloomcycle.app.domain.model.ChatScope
import com.bloomcycle.app.domain.repository.ChatRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * On-device chat. Messages are written to the encrypted Room database and never leave
 * the device, which is what makes this safe to ship before a backend exists.
 *
 * When the Firestore implementation is wired in (plan.md Phase 11), this class is
 * replaced and the `chat_messages` table is dropped — chat is the one part of the app
 * that will travel to a server, so it must not remain in the encrypted store.
 */
class ChatRepositoryImpl(
    private val chatDao: ChatMessageDao,
    private val clock: com.bloomcycle.app.core.time.CycleClock,
) : ChatRepository {

    override fun messages(scope: ChatScope): Flow<List<ChatMessage>> =
        chatDao.observeAll()
            .map { rows -> rows.filter { it.scope == scope.name }.map { it.toDomain() } }

    override suspend fun send(text: String, scope: ChatScope, identity: ChatIdentity) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            authorName = identity.displayName,
            phaseBucket = identity.phaseBucket,
            isOwn = true,
            createdAt = clock.now(),
        )
        chatDao.upsert(message.toEntity(scope.name))
    }
}

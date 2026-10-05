package com.bloomcycle.app.data.remote

import com.bloomcycle.app.domain.model.ChatIdentity
import com.bloomcycle.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * One snapshot of a room: its messages plus whether they came from the server or
 * from the local cache. The cache flag is what drives the "can't reach the circle"
 * banner — no separate reachability check needed.
 */
data class RemoteChatUpdate(
    val messages: List<ChatMessage>,
    val fromCache: Boolean,
)

/**
 * The Firestore-shaped side of chat. `selfUid` is passed in (rather than read from
 * auth here) so this layer stays a pure document mapper: the repository owns the
 * [AuthSession], this layer owns the field names.
 */
interface RemoteChatDataSource {
    fun observe(roomId: String, selfUid: String): Flow<RemoteChatUpdate>
    suspend fun send(roomId: String, text: String, identity: ChatIdentity, authorUid: String)
}

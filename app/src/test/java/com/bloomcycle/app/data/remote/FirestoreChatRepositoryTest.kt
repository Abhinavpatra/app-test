package com.bloomcycle.app.data.remote

import app.cash.turbine.test
import com.bloomcycle.app.domain.model.ChatIdentity
import com.bloomcycle.app.domain.model.ChatMessage
import com.bloomcycle.app.domain.repository.ChatConnection
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreChatRepositoryTest {

    private class FakeAuth(private val uidValue: String = "uid-1") : AuthSession {
        var calls = 0
        override suspend fun uid(): String {
            calls += 1
            return uidValue
        }
    }

    private data class Sent(
        val roomId: String,
        val text: String,
        val identity: ChatIdentity,
        val authorUid: String,
    )

    private class FakeRemote : RemoteChatDataSource {
        var lastRoomId: String? = null
        var lastSelfUid: String? = null
        val updates = MutableStateFlow(RemoteChatUpdate(emptyList(), fromCache = false))
        val sent = mutableListOf<Sent>()

        override fun observe(roomId: String, selfUid: String): Flow<RemoteChatUpdate> {
            lastRoomId = roomId
            lastSelfUid = selfUid
            return updates
        }

        override suspend fun send(
            roomId: String,
            text: String,
            identity: ChatIdentity,
            authorUid: String,
        ) {
            sent += Sent(roomId, text, identity, authorUid)
        }
    }

    private val auth = FakeAuth()
    private val remote = FakeRemote()
    private val repository = FirestoreChatRepository(remote, auth)

    private val identity = ChatIdentity(
        displayName = "Quiet Fern",
        phaseBucket = "near ovulation",
        cycleLengthBand = "a typical cycle",
    )

    private fun message(id: String, text: String) = ChatMessage(
        id = id,
        text = text,
        authorName = "Quiet Fern",
        phaseBucket = "near ovulation",
        isOwn = true,
        createdAt = Instant.parse("2026-10-06T12:00:00Z"),
    )

    @Test
    fun `messages emits whatever the remote room emits`() = runTest {
        repository.messages("global").test {
            assertEquals(emptyList<ChatMessage>(), awaitItem())

            remote.updates.value = RemoteChatUpdate(
                listOf(message("m1", "hello")),
                fromCache = false,
            )
            assertEquals(listOf("hello"), awaitItem().map { it.text })
        }
    }

    @Test
    fun `the room is subscribed with the uid from auth`() = runTest {
        repository.messages("phase-menstrual").test {
            awaitItem()
        }

        assertEquals("phase-menstrual", remote.lastRoomId)
        assertEquals("uid-1", remote.lastSelfUid)
    }

    @Test
    fun `the connection is live while snapshots come from the server`() = runTest {
        remote.updates.value = RemoteChatUpdate(emptyList(), fromCache = false)

        repository.connection("global").test {
            assertEquals(ChatConnection.LIVE, awaitItem())
        }
    }

    @Test
    fun `the connection drops to offline while snapshots come from cache`() = runTest {
        remote.updates.value = RemoteChatUpdate(emptyList(), fromCache = true)

        repository.connection("global").test {
            assertEquals(ChatConnection.OFFLINE, awaitItem())
        }
    }

    @Test
    fun `the connection follows the cache flag as it changes`() = runTest {
        repository.connection("global").test {
            assertEquals(ChatConnection.LIVE, awaitItem())

            remote.updates.value = RemoteChatUpdate(emptyList(), fromCache = true)
            assertEquals(ChatConnection.OFFLINE, awaitItem())
        }
    }

    @Test
    fun `send forwards room text identity and the auth uid`() = runTest {
        repository.send("hello there", "global", identity)

        val sent = remote.sent.single()
        assertEquals("global", sent.roomId)
        assertEquals("hello there", sent.text)
        assertEquals(identity, sent.identity)
        assertEquals("uid-1", sent.authorUid)
        assertEquals(1, auth.calls)
    }
}

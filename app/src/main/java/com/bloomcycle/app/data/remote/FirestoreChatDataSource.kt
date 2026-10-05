package com.bloomcycle.app.data.remote

import com.bloomcycle.app.domain.model.ChatIdentity
import com.bloomcycle.app.domain.model.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.time.Instant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Chat rooms as Firestore documents: `chats/{roomId}/messages/{messageId}`.
 *
 * Field names are the API contract with `firestore.rules` — rename one here and the
 * rule that validates it must move in lockstep. See plan.md §7.4 for why each field
 * exists and, more importantly, what is deliberately *not* stored (no dates, no
 * birth date, no real names).
 */
class FirestoreChatDataSource(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : RemoteChatDataSource {

    override fun observe(roomId: String, selfUid: String): Flow<RemoteChatUpdate> = callbackFlow {
        val registration = messages(roomId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val documents = snapshot?.documents.orEmpty()
                val fromCache = snapshot?.metadata?.isFromCache != false
                trySend(
                    RemoteChatUpdate(
                        messages = documents.map { it.toChatMessage(selfUid) },
                        fromCache = fromCache,
                    ),
                )
            }
        awaitClose { registration.remove() }
    }

    override suspend fun send(
        roomId: String,
        text: String,
        identity: ChatIdentity,
        authorUid: String,
    ) {
        messages(roomId).document().set(
            mapOf(
                "text" to text.trim(),
                "pseudonymousName" to identity.displayName,
                "phaseBucket" to identity.phaseBucket,
                "cycleLengthBand" to identity.cycleLengthBand,
                "authorUid" to authorUid,
                // Client millis for ordering; the server timestamp beside it is the
                // tamper-evident copy the rules can trust.
                "createdAt" to Instant.now().toEpochMilli(),
                "serverCreatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private fun messages(roomId: String) =
        firestore.collection(COLLECTION)
            .document(roomId)
            .collection(SUBCOLLECTION)

    private fun orderedMessages(roomId: String): Query =
        messages(roomId).orderBy(FIELD_CREATED_AT, Query.Direction.ASCENDING)

    private fun DocumentSnapshot.toChatMessage(selfUid: String) = ChatMessage(
        id = id,
        text = getString(FIELD_TEXT).orEmpty(),
        authorName = getString(FIELD_NAME).orEmpty(),
        phaseBucket = getString(FIELD_PHASE).orEmpty(),
        isOwn = getString(FIELD_AUTHOR_UID) == selfUid,
        createdAt = Instant.ofEpochMilli(getLong(FIELD_CREATED_AT) ?: 0L),
    )

    companion object {
        const val COLLECTION = "chats"
        const val SUBCOLLECTION = "messages"
        const val FIELD_TEXT = "text"
        const val FIELD_NAME = "pseudonymousName"
        const val FIELD_PHASE = "phaseBucket"
        const val FIELD_BAND = "cycleLengthBand"
        const val FIELD_AUTHOR_UID = "authorUid"
        const val FIELD_CREATED_AT = "createdAt"
    }
}

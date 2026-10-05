package com.bloomcycle.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

/**
 * The one thing the chat repository needs from auth: a stable sender id.
 *
 * A seam rather than a direct `Firebase.auth` call so unit tests can hand the
 * repository a fixed uid without touching the network (plan.md Phase 11b).
 */
interface AuthSession {
    suspend fun uid(): String
}

/**
 * Silent anonymous sign-in. Nobody is asked to make an account to talk in the rooms;
 * the uid only exists so the security rules can prove authorship
 * (`authorUid == request.auth.uid`) and so "you" badges render on one's own messages.
 */
class FirebaseAuthSession(
    private val auth: FirebaseAuth = Firebase.auth,
) : AuthSession {
    override suspend fun uid(): String {
        auth.currentUser?.uid?.let { return it }
        val result = auth.signInAnonymously().await()
        return requireNotNull(result.user?.uid) { "Anonymous sign-in returned no user" }
    }
}

package com.bloomcycle.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.bloomcycle.app.R
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The one sound the app makes: `fish-reel-in` from soundcn (CC0, by Kenney), installed from
 * the soundcn registry exactly as `bunx shadcn add @soundcn/fish-reel-in` would do for a web
 * project — the registry item carries the audio as a base64 data URI, which is decoded once
 * into `res/raw/fish_reel_in.mp3` rather than shipped inside a TypeScript module.
 *
 * Loaded up front, played quietly, and never more than once a second: an app about bodies
 * should not be chattering at people.
 */
object UiSounds {

    private const val VOLUME = 0.22f
    private const val MIN_GAP_MS = 1_200L

    private var pool: SoundPool? = null
    private var soundId = 0
    private var loaded = false
    private var lastPlayedAt = 0L

    fun init(context: Context) {
        if (pool != null) return
        synchronized(this) {
            if (pool != null) return
            val created = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .build()
            created.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
            soundId = created.load(context.applicationContext, R.raw.fish_reel_in, 1)
            pool = created
        }
    }

    /** Quiet, and rate-limited — see [MIN_GAP_MS]. Does nothing until [init] has run. */
    fun play() {
        val active = pool ?: return
        if (!loaded || soundId == 0) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastPlayedAt < MIN_GAP_MS) return
        lastPlayedAt = now
        active.play(soundId, VOLUME, VOLUME, 1, 0, 1f)
    }
}

/**
 * Plays [UiSounds] once when a page comes to rest after a scroll — the "reel in" moment —
 * rather than on every frame or every fling. Attached to the one scrolling page in the app,
 * so the sound only happens where the user is actually scrolling.
 */
@Composable
fun PageScrollSound(state: LazyListState) {
    LaunchedEffect(state) {
        var scrolling = false
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .collect { moving ->
                when {
                    moving -> scrolling = true
                    scrolling -> {
                        scrolling = false
                        UiSounds.play()
                    }
                }
            }
    }
}

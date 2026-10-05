package com.bloomcycle.app

import android.content.Context
import com.bloomcycle.app.BuildConfig
import com.bloomcycle.app.core.time.CycleClock
import com.bloomcycle.app.core.time.SystemCycleClock
import com.bloomcycle.app.data.crypto.PassphraseProvider
import com.bloomcycle.app.data.local.DatabaseFactory
import com.bloomcycle.app.data.remote.FirebaseAuthSession
import com.bloomcycle.app.data.remote.FirestoreChatDataSource
import com.bloomcycle.app.data.remote.FirestoreChatRepository
import com.bloomcycle.app.data.repo.BillingEntitlementRepository
import com.bloomcycle.app.data.repo.ContentRepositoryImpl
import com.bloomcycle.app.data.repo.CycleRepositoryImpl
import com.bloomcycle.app.data.repo.EntitlementRepositoryImpl
import com.bloomcycle.app.data.repo.InMemoryChatRepository
import com.bloomcycle.app.data.repo.SettingsRepositoryImpl
import com.bloomcycle.app.domain.repository.ChatRepository
import com.bloomcycle.app.domain.repository.EntitlementRepository
import com.bloomcycle.app.notifications.ReminderScheduler
import kotlinx.coroutines.flow.first

/**
 * Hand-rolled dependency container.
 *
 * Deliberately not Hilt: Hilt 2.59 cannot read Kotlin 2.4 metadata, so adopting it would
 * pin this project to an end-of-life compiler (plan.md §5.2). For a single module with
 * this many dependencies, ~40 lines of explicit wiring is cheaper than that version
 * matrix. Revisit only if the project splits into Gradle modules.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val cycleClock: CycleClock = SystemCycleClock()

    val passphraseProvider: PassphraseProvider by lazy { PassphraseProvider(appContext) }
    val database by lazy { DatabaseFactory(appContext, passphraseProvider).create() }

    val settings: SettingsRepositoryImpl by lazy { SettingsRepositoryImpl(appContext) }

    /** See [BillingEntitlementRepository] — `BuildConfig.BILLING` picks the implementation. */
    val entitlements: EntitlementRepository by lazy {
        val local = EntitlementRepositoryImpl(appContext)
        if (BuildConfig.BILLING) BillingEntitlementRepository(local) else local
    }

    val cycleRepository: CycleRepositoryImpl by lazy {
        CycleRepositoryImpl(database.periodEventDao(), database.symptomLogDao(), cycleClock)
    }

    /**
     * Mirrors [entitlements]: `BuildConfig.FIREBASE_CHAT` (true exactly when
     * `google-services.json` is present at build time) picks the Firestore rooms;
     * without it the in-memory rooms stand in and nothing touches the network.
     */
    val chatRepository: ChatRepository by lazy {
        if (BuildConfig.FIREBASE_CHAT) {
            FirestoreChatRepository(
                FirestoreChatDataSource(),
                FirebaseAuthSession(),
            )
        } else {
            InMemoryChatRepository(cycleClock)
        }
    }

    val contentRepository: ContentRepositoryImpl by lazy { ContentRepositoryImpl() }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }

    /**
     * Re-asserts the daily reminder from the current settings and the current clock.
     *
     * Called whenever the logged data changes (a new period moves the prediction the worker
     * reads), and by [com.bloomcycle.app.notifications.ReminderRescheduleReceiver] after a
     * reboot, a timezone change or a manual clock change. One place, so "scheduled" always
     * means the same thing everywhere.
     */
    suspend fun resyncReminder() {
        val current = settings.settings.first()
        if (current.remindersEnabled) {
            reminderScheduler.ensureScheduled(
                hour = current.reminderHour,
                minute = current.reminderMinute,
            )
        } else {
            reminderScheduler.cancel()
        }
    }
}

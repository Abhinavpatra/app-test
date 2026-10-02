package com.bloomcycle.app.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bloomcycle.app.domain.model.CycleContext
import com.bloomcycle.app.domain.model.UserSettings
import com.bloomcycle.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "bloom_settings")

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_complete")
        val reminders = booleanPreferencesKey("reminders_enabled")
        val hour = intPreferencesKey("reminder_hour")
        val minute = intPreferencesKey("reminder_minute")
        val dailyNote = booleanPreferencesKey("daily_note_enabled")
        val muted = booleanPreferencesKey("predictions_muted")
        val context = stringPreferencesKey("cycle_context")
        val chatName = stringPreferencesKey("chat_display_name")
        val permissionAsked = booleanPreferencesKey("notification_permission_asked")
    }

    override val settings: Flow<UserSettings> = context.settingsStore.data.map { p -> read(p) }

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.settingsStore.edit { p -> write(p, transform(read(p))) }
    }

    private fun read(p: Preferences) = UserSettings(
        onboardingComplete = p[Keys.onboarding] ?: false,
        remindersEnabled = p[Keys.reminders] ?: true,
        reminderHour = p[Keys.hour] ?: 9,
        reminderMinute = p[Keys.minute] ?: 0,
        dailyNoteEnabled = p[Keys.dailyNote] ?: true,
        predictionsMuted = p[Keys.muted] ?: false,
        cycleContext = cycleContextOf(p[Keys.context]),
        chatDisplayName = p[Keys.chatName] ?: "",
        notificationPermissionAsked = p[Keys.permissionAsked] ?: false,
    )

    private fun write(p: MutablePreferences, s: UserSettings) {
        p[Keys.onboarding] = s.onboardingComplete
        p[Keys.reminders] = s.remindersEnabled
        p[Keys.hour] = s.reminderHour
        p[Keys.minute] = s.reminderMinute
        p[Keys.dailyNote] = s.dailyNoteEnabled
        p[Keys.muted] = s.predictionsMuted
        p[Keys.context] = s.cycleContext.name
        p[Keys.chatName] = s.chatDisplayName
        p[Keys.permissionAsked] = s.notificationPermissionAsked
    }

    /**
     * Stored by enum name so a future enum entry can be added without breaking reads.
     * An unknown value (older app, newer data) falls back to NONE rather than throwing —
     * a corrupt preference must never make the settings screen unopenable.
     */
    private fun cycleContextOf(raw: String?): CycleContext =
        CycleContext.entries.firstOrNull { it.name == raw } ?: CycleContext.NONE
}

package com.bloomcycle.app.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        val chatName = stringPreferencesKey("chat_display_name")
        val permissionAsked = booleanPreferencesKey("notification_permission_asked")
    }

    override val settings: Flow<UserSettings> = context.settingsStore.data.map { p ->
        UserSettings(
            onboardingComplete = p[Keys.onboarding] ?: false,
            remindersEnabled = p[Keys.reminders] ?: true,
            reminderHour = p[Keys.hour] ?: 9,
            reminderMinute = p[Keys.minute] ?: 0,
            dailyNoteEnabled = p[Keys.dailyNote] ?: true,
            predictionsMuted = p[Keys.muted] ?: false,
            chatDisplayName = p[Keys.chatName] ?: "",
            notificationPermissionAsked = p[Keys.permissionAsked] ?: false,
        )
    }

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.settingsStore.edit { p ->
            val current = UserSettings(
                onboardingComplete = p[Keys.onboarding] ?: false,
                remindersEnabled = p[Keys.reminders] ?: true,
                reminderHour = p[Keys.hour] ?: 9,
                reminderMinute = p[Keys.minute] ?: 0,
                dailyNoteEnabled = p[Keys.dailyNote] ?: true,
                predictionsMuted = p[Keys.muted] ?: false,
                chatDisplayName = p[Keys.chatName] ?: "",
                notificationPermissionAsked = p[Keys.permissionAsked] ?: false,
            )
            val next = transform(current)
            p[Keys.onboarding] = next.onboardingComplete
            p[Keys.reminders] = next.remindersEnabled
            p[Keys.hour] = next.reminderHour
            p[Keys.minute] = next.reminderMinute
            p[Keys.dailyNote] = next.dailyNoteEnabled
            p[Keys.muted] = next.predictionsMuted
            p[Keys.chatName] = next.chatDisplayName
            p[Keys.permissionAsked] = next.notificationPermissionAsked
        }
    }
}

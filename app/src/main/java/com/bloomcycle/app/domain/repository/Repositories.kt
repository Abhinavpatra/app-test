package com.bloomcycle.app.domain.repository

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.ChatIdentity
import com.bloomcycle.app.domain.model.ChatMessage
import com.bloomcycle.app.domain.model.ChatScope
import com.bloomcycle.app.domain.model.ContentItem
import com.bloomcycle.app.domain.model.ContentCategory
import com.bloomcycle.app.domain.model.EntitlementState
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.PhaseType
import com.bloomcycle.app.domain.model.PremiumFeature
import com.bloomcycle.app.domain.model.SymptomLog
import com.bloomcycle.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** Cycle history: logged periods and symptoms, the raw material for every prediction. */
interface CycleRepository {
    fun observePeriods(): Flow<List<PeriodEvent>>
    suspend fun periods(): List<PeriodEvent>
    suspend fun logPeriod(event: PeriodEvent): Long
    suspend fun updatePeriod(event: PeriodEvent)
    suspend fun deletePeriod(event: PeriodEvent)

    fun observeSymptoms(): Flow<List<SymptomLog>>
    suspend fun symptomsOn(date: CycleDate): List<SymptomLog>
    suspend fun logSymptom(log: SymptomLog): Long
    suspend fun deleteSymptom(log: SymptomLog)

    /** Irreversible. Deliberately explicit rather than implied by a single UI tap. */
    suspend fun eraseEverything()
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(transform: (UserSettings) -> UserSettings)
}

interface EntitlementRepository {
    val state: Flow<EntitlementState>
    suspend fun isUnlocked(feature: PremiumFeature): Boolean
    suspend fun unlock(feature: PremiumFeature)
    suspend fun setDebugUnlocked(unlocked: Boolean)
}

interface ContentRepository {
    fun itemsFor(phase: PhaseType?): List<ContentItem>
    fun byCategory(phase: PhaseType?, category: ContentCategory): List<ContentItem>
}

interface ChatRepository {
    fun messages(scope: ChatScope): Flow<List<ChatMessage>>

    /**
     * [identity] carries only a pseudonym and a coarse bucket — never an exact date,
     * never a birth date. See plan.md §7.4.
     */
    suspend fun send(text: String, scope: ChatScope, identity: ChatIdentity)
}

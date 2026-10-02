package com.bloomcycle.app.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bloomcycle.app.domain.model.EntitlementState
import com.bloomcycle.app.domain.model.PremiumFeature
import com.bloomcycle.app.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.entitlementStore: DataStore<Preferences> by preferencesDataStore(name = "bloom_premium")

/**
 * v1 ships no billing SDK — see plan.md §5/Phase 9. This local stub keeps every premium
 * screen reachable for development and testing, and is the exact interface a
 * Play Billing or RevenueCat implementation will later replace.
 */
class EntitlementRepositoryImpl(private val context: Context) : EntitlementRepository {

    private object Keys {
        val unlocked = stringSetPreferencesKey("unlocked_features")
        val debug = booleanPreferencesKey("debug_unlock_all")
    }

    override val state: Flow<EntitlementState> = context.entitlementStore.data.map { p ->
        EntitlementState(
            unlocked = p[Keys.unlocked].orEmpty()
                .mapNotNull { name -> PremiumFeature.entries.firstOrNull { it.name == name } }
                .toSet(),
            isDebugUnlocked = p[Keys.debug] ?: false,
        )
    }

    override suspend fun isUnlocked(feature: PremiumFeature): Boolean =
        state.first().isUnlocked(feature)

    override suspend fun unlock(feature: PremiumFeature) {
        context.entitlementStore.edit { p ->
            p[Keys.unlocked] = p[Keys.unlocked].orEmpty() + feature.name
        }
    }

    override suspend fun setDebugUnlocked(unlocked: Boolean) {
        context.entitlementStore.edit { p -> p[Keys.debug] = unlocked }
    }
}

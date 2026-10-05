package com.bloomcycle.app.data.repo

import com.bloomcycle.app.domain.model.EntitlementState
import com.bloomcycle.app.domain.model.PremiumFeature
import com.bloomcycle.app.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.Flow

/**
 * The billing seam (plan.md Phase 9).
 *
 * v1 ships no billing SDK, so this wraps the DataStore-backed [local] store and answers
 * exactly as it does. When `BuildConfig.BILLING` flips to `true`, this is the single class
 * that changes: `state` merges `BillingClient.queryPurchasesAsync`, `isUnlocked` also asks
 * the purchase record, and `unlock` launches `launchBillingFlow` instead of writing locally.
 * Nothing above this line — paywall, teasers, Insights — needs to know the difference, which
 * is the point of having the seam at all.
 */
class BillingEntitlementRepository(
    private val local: EntitlementRepository,
) : EntitlementRepository {

    /** TODO(BILLING): merge purchased SKUs into this stream so state reflects the store. */
    override val state: Flow<EntitlementState> = local.state

    /** TODO(BILLING): `local.isUnlocked(feature) && purchased(feature)`. */
    override suspend fun isUnlocked(feature: PremiumFeature): Boolean = local.isUnlocked(feature)

    /** TODO(BILLING): `billingClient.launchBillingFlow(...)` — no local write. */
    override suspend fun unlock(feature: PremiumFeature) = local.unlock(feature)

    override suspend fun setDebugUnlocked(unlocked: Boolean) = local.setDebugUnlocked(unlocked)
}
